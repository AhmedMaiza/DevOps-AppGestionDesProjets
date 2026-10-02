#!/usr/bin/env bash
# Usage : ./setup-ci-tools.sh <nouveau_mot_de_passe_admin_sonarqube>
set -euo pipefail

VERT='\033[0;32m'; JAUNE='\033[1;33m'; ROUGE='\033[0;31m'; N='\033[0m'
info()  { echo -e "\n${VERT}==> $*${N}"; }
avert() { echo -e "${JAUNE}[!] $*${N}"; }
erreur(){ echo -e "${ROUGE}[X] $*${N}" >&2; }

NEW_PASSWORD="${1:-}"
if [ -z "$NEW_PASSWORD" ]; then
    erreur "Usage : $0 <nouveau_mot_de_passe_admin>   (12 caracteres minimum conseilles)"
    exit 1
fi

SONAR="http://localhost:9000"
cd "$(dirname "$0")"

info "0/6 Verification des ressources de la VM"
MEM_MB=$(awk '/MemTotal/ {printf "%d", $2/1024}' /proc/meminfo)
echo "  RAM totale : ${MEM_MB} Mo"
if [ "$MEM_MB" -lt 3500 ]; then
    avert "Moins de 3,5 Go de RAM : SonarQube risque d'etre tres lent ou de s'arreter."
    avert "Augmente 'vb.memory' dans le Vagrantfile (6144 recommande) puis 'vagrant reload'."
fi

info "1/6 Parametres noyau requis par SonarQube (Elasticsearch)"
sudo sysctl -w vm.max_map_count=524288 >/dev/null
sudo sysctl -w fs.file-max=131072 >/dev/null
printf 'vm.max_map_count=524288\nfs.file-max=131072\n' | sudo tee /etc/sysctl.d/99-sonarqube.conf >/dev/null
echo "  vm.max_map_count = $(sysctl -n vm.max_map_count)  (persistant apres reboot)"

command -v jq >/dev/null || { info "Installation de jq"; sudo apt-get install -y jq >/dev/null; }

info "2/6 Demarrage de la stack (SonarQube, PostgreSQL, registry)"
docker compose up -d
docker compose ps

info "3/6 Attente de SonarQube (le premier demarrage prend 2 a 4 minutes)"
for i in $(seq 1 80); do
    STATUS=$(curl -s "${SONAR}/api/system/status" 2>/dev/null | jq -r '.status' 2>/dev/null || true)
    echo "  [$i/80] statut : ${STATUS:-injoignable}"
    [ "$STATUS" = "UP" ] && break
    sleep 5
done
if [ "${STATUS:-}" != "UP" ]; then
    erreur "SonarQube n'est pas demarre. Diagnostic : docker compose logs sonarqube --tail=50"
    exit 1
fi

info "4/6 Mot de passe administrateur"
if curl -sf -u "admin:${NEW_PASSWORD}" "${SONAR}/api/authentication/validate" | jq -e '.valid == true' >/dev/null 2>&1; then
    avert "Le mot de passe est deja celui demande : etape ignoree"
else
    CODE=$(curl -s -o /tmp/sonar-pw.json -w '%{http_code}' -u admin:admin -X POST \
        "${SONAR}/api/users/change_password" \
        --data-urlencode "login=admin" \
        --data-urlencode "previousPassword=admin" \
        --data-urlencode "password=${NEW_PASSWORD}")
    if [ "$CODE" = "204" ]; then
        echo "  mot de passe admin modifie"
    else
        erreur "Echec du changement de mot de passe (HTTP $CODE) :"
        cat /tmp/sonar-pw.json; echo
        erreur "Si le mot de passe a deja ete change avec une autre valeur, relance avec CELUI-LA."
        exit 1
    fi
fi

info "5/6 Token pour Jenkins"
TOKEN_NAME="jenkins-$(date +%Y%m%d-%H%M%S)"
TOKEN=$(curl -sf -u "admin:${NEW_PASSWORD}" -X POST "${SONAR}/api/user_tokens/generate" \
        --data-urlencode "name=${TOKEN_NAME}" | jq -r '.token')
[ -n "$TOKEN" ] && [ "$TOKEN" != "null" ] || { erreur "Generation du token impossible"; exit 1; }

info "6/6 Creation des projets SonarQube"
creer_projet() {
    local cle="$1" nom="$2"
    local code
    code=$(curl -s -o /dev/null -w '%{http_code}' -u "admin:${NEW_PASSWORD}" -X POST \
        "${SONAR}/api/projects/create" \
        --data-urlencode "project=${cle}" --data-urlencode "name=${nom}")
    case "$code" in
        200) echo "  projet cree      : ${cle}" ;;
        400) echo "  projet existant  : ${cle}" ;;
        *)   avert "creation de ${cle} : HTTP ${code}" ;;
    esac
}
creer_projet "gestion-projets-backend"  "Gestion Projets - Backend (Spring Boot)"
creer_projet "gestion-projets-frontend" "Gestion Projets - Frontend (Angular)"

# Periode "nouveau code" = 30 jours : tout le code reste "nouveau" pendant la demo,
# donc le Quality Gate de la 2e analyse juge bien le code corrige.
for cle in gestion-projets-backend gestion-projets-frontend; do
    curl -s -o /dev/null -u "admin:${NEW_PASSWORD}" -X POST "${SONAR}/api/new_code_periods/set" \
        --data-urlencode "project=${cle}" --data-urlencode "type=NUMBER_OF_DAYS" --data-urlencode "value=30" || true
done

IP=$(hostname -I | awk '{print $2}'); [ -z "$IP" ] && IP=$(hostname -I | awk '{print $1}')

echo ""
echo "==============================================================="
echo "  SONARQUBE ET REGISTRY PRETS"
echo "==============================================================="
echo "  SonarQube   : http://${IP}:9000      (login : admin)"
echo "  Registry    : http://${IP}:5000/v2/_catalog"
echo ""
echo "  TOKEN JENKINS (copie-le MAINTENANT, il ne sera plus affiche) :"
echo ""
echo "      ${TOKEN}"
echo ""
echo "  A faire dans Jenkins :"
echo "   Administrer Jenkins > Credentials > System > Global credentials"
echo "   > Add Credentials > Kind : Secret text"
echo "   Secret : le token ci-dessus      ID : sonar-token"
echo "==============================================================="
