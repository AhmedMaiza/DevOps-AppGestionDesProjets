#!/usr/bin/env python3
import pathlib
import re
import sys

ROOT = pathlib.Path(sys.argv[1] if len(sys.argv) > 1 else "backend/src/main/java/tn/esprit/backend")
CTL = ROOT / "controller"
DTO = ROOT / "dto"
if not CTL.is_dir():
    sys.exit(f"[X] dossier introuvable : {CTL}")
DTO.mkdir(exist_ok=True)

HEAD = "package tn.esprit.backend.dto;\n\n"

DTOS = {
    "EntrepriseRequest": HEAD + """import tn.esprit.backend.entity.Entreprise;

public record EntrepriseRequest(Long id, String nom, String adresse) {

    public Entreprise toEntity() {
        Entreprise e = new Entreprise();
        e.setId(id);
        e.setNom(nom);
        e.setAdresse(adresse);
        return e;
    }
}
""",
    "ProjetRequest": HEAD + """import tn.esprit.backend.entity.Projet;

public record ProjetRequest(Long id, String sujet) {

    public Projet toEntity() {
        Projet p = new Projet();
        p.setId(id);
        p.setSujet(sujet);
        return p;
    }
}
""",
    "ProjetDetailleRequest": HEAD + """import java.time.LocalDate;

import tn.esprit.backend.entity.ProjetDetaille;

public record ProjetDetailleRequest(Long id, String description, String technologie,
                                    Double coutProvisoire, LocalDate dateDebut) {

    public ProjetDetaille toEntity() {
        ProjetDetaille d = new ProjetDetaille();
        d.setId(id);
        d.setDescription(description);
        d.setTechnologie(technologie);
        d.setCoutProvisoire(coutProvisoire);
        d.setDateDebut(dateDebut);
        return d;
    }
}
""",
    "EquipeRequest": HEAD + """import tn.esprit.backend.entity.Entreprise;
import tn.esprit.backend.entity.Equipe;

public record EquipeRequest(Long id, String nom, String specialite, EntrepriseRef entreprise) {

    public record EntrepriseRef(Long id) { }

    public Equipe toEntity() {
        Equipe e = new Equipe();
        e.setId(id);
        e.setNom(nom);
        e.setSpecialite(specialite);
        if (entreprise != null && entreprise.id() != null) {
            Entreprise ent = new Entreprise();
            ent.setId(entreprise.id());
            e.setEntreprise(ent);
        }
        return e;
    }
}
""",
}
for name, code in DTOS.items():
    (DTO / f"{name}.java").write_text(code, encoding="utf-8")
    print(f"  [dto]        {name}.java")

total = 0
for f in sorted(CTL.glob("*Controller.java")):
    ent = f.name.replace("Controller.java", "")
    dto = ent + "Request"
    if dto not in DTOS:
        continue
    src = f.read_text(encoding="utf-8")
    if dto in src:
        print(f"  [controller] {f.name} : deja corrige")
        continue
    new, n1 = re.subn(r"@RequestBody " + ent + r" (\w+)\)", r"@RequestBody " + dto + r" \1)", src)
    new, n2 = re.subn(r"\.((?:add|update)" + ent + r")\((\w+)\)", r".\1(\2.toEntity())", new)
    if n1 != 2 or n2 != 2:
        sys.exit(f"[X] {f.name} : structure inattendue (RequestBody={n1}, appels={n2}), modification annulee")
    new = new.replace("import tn.esprit.backend.entity.",
                      f"import tn.esprit.backend.dto.{dto};\nimport tn.esprit.backend.entity.", 1)
    f.write_text(new, encoding="utf-8")
    total += 1
    print(f"  [controller] {f.name} : add/update utilisent {dto}")
print(f"==> {total} controllers corriges")
