// Workflow : Git(SCM) -> Compilation -> Construction -> Test + Analyse + Quality Gate
//            -> Creation + push de l'image dans le Registry (-> Deploiement)
// Prerequis : SonarQube (localhost:9000), registry (localhost:5000), credential "sonar-token"

pipeline {

    agent any

    parameters {
        booleanParam(name: 'ANALYSE_FRONTEND', defaultValue: true,
                     description: 'Analyser aussi le frontend Angular (informatif, non bloquant)')
        booleanParam(name: 'DEPLOY', defaultValue: true,
                     description: 'Deployer la stack avec docker compose apres le push')
    }

    environment {
        COMPOSE_PROJECT_NAME = 'gestion-projets'
        SONAR_HOST_URL     = 'http://localhost:9000'
        SONAR_PUBLIC_URL   = 'http://192.168.33.10:9000'
        SONAR_BACKEND_KEY  = 'gestion-projets-backend'
        SONAR_FRONTEND_KEY = 'gestion-projets-frontend'
        REGISTRY       = 'localhost:5000'
        REGISTRY_CREDS = 'dockerhub-creds'
    }

    options {
        timestamps()
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '10'))
        timeout(time: 45, unit: 'MINUTES')
    }

    stages {

        stage('1. Git (SCM)') {
            steps {
                checkout scm
                sh '''
                    chmod +x ci/*.sh
                    mkdir -p reports
                    git log -1 --oneline
                '''
            }
        }

        stage('1b. Verifier l\'environnement') {
            steps {
                sh '''
                    java -version 2>&1 | head -1
                    JV=$(java -version 2>&1 | awk -F\\" '/version/ {print $2}' | cut -d. -f1)
                    [ "$JV" -ge 21 ] || { echo "[X] Java 21 minimum requis (scanner SonarQube)"; exit 1; }
                    docker --version
                    docker compose version
                    jq --version

                    docker info > /dev/null || { echo "[X] sudo usermod -aG docker jenkins && sudo systemctl restart jenkins"; exit 1; }

                    curl -fs "$SONAR_HOST_URL/api/system/status" | jq -e '.status == "UP"' > /dev/null \\
                        || { echo "[X] SonarQube indisponible : cd ci-tools && ./setup-ci-tools.sh <mdp>"; exit 1; }
                    echo "SonarQube : UP"

                    case "$REGISTRY" in
                        localhost:*) curl -fs "http://$REGISTRY/v2/" > /dev/null \\
                                       || { echo "[X] Registry indisponible : docker compose up -d dans ci-tools"; exit 1; }
                                     echo "Registry $REGISTRY : OK" ;;
                        *)           echo "Registry distant : $REGISTRY" ;;
                    esac
                '''
            }
        }

        stage('2. Compilation') {
            steps {
                dir('backend') {
                    sh 'bash ./mvnw -B -ntp clean compile'
                }
            }
        }

        stage('3. Construction') {
            steps {
                dir('backend') {
                    sh 'bash ./mvnw -B -ntp package -DskipTests'
                }
            }
            post {
                success {
                    archiveArtifacts artifacts: 'backend/target/*.jar', fingerprint: true
                }
            }
        }

        stage('4. Test + Analyse + Quality Gate') {
            stages {

                stage('4.1 Tests unitaires') {
                    steps {
                        dir('backend') {
                            sh 'bash ./mvnw -B -ntp test'
                        }
                    }
                    post {
                        always {
                            junit allowEmptyResults: true, testResults: 'backend/target/surefire-reports/*.xml'
                            archiveArtifacts artifacts: 'backend/target/site/jacoco/**', allowEmptyArchive: true
                        }
                    }
                }

                stage('4.2 Dashboard SonarQube - AVANT analyse') {
                    steps {
                        withCredentials([string(credentialsId: 'sonar-token', variable: 'SONAR_TOKEN')]) {
                            sh '''
                                ci/sonar-report.sh dashboard "$SONAR_BACKEND_KEY" \\
                                    "DASHBOARD AVANT ANALYSE - BACKEND" reports/sonar-avant-backend.txt
                                if [ "$ANALYSE_FRONTEND" = "true" ]; then
                                    ci/sonar-report.sh dashboard "$SONAR_FRONTEND_KEY" \\
                                        "DASHBOARD AVANT ANALYSE - FRONTEND" reports/sonar-avant-frontend.txt
                                fi
                            '''
                        }
                        script {
                            currentBuild.description = "SonarQube : ${env.SONAR_PUBLIC_URL}/dashboard?id=${env.SONAR_BACKEND_KEY}"
                        }
                    }
                }

                stage('4.3 Analyse SonarQube - backend') {
                    steps {
                        withCredentials([string(credentialsId: 'sonar-token', variable: 'SONAR_TOKEN')]) {
                            dir('backend') {
                                sh '''
                                    bash ./mvnw -B -ntp org.sonarsource.scanner.maven:sonar-maven-plugin:sonar \\
                                        -Dsonar.projectKey="$SONAR_BACKEND_KEY" \\
                                        -Dsonar.projectName="Gestion Projets - Backend (Spring Boot)" \\
                                        -Dsonar.host.url="$SONAR_HOST_URL" \\
                                        -Dsonar.token="$SONAR_TOKEN"
                                '''
                            }
                            sh 'ci/sonar-report.sh wait "$SONAR_BACKEND_KEY"'
                        }
                    }
                }

                stage('4.4 Analyse SonarQube - frontend (informatif)') {
                    when { expression { params.ANALYSE_FRONTEND } }
                    steps {
                        catchError(buildResult: 'SUCCESS', stageResult: 'UNSTABLE') {
                            withCredentials([string(credentialsId: 'sonar-token', variable: 'SONAR_TOKEN')]) {
                                sh '''
                                    docker run --rm --network host \\
                                        -e SONAR_HOST_URL="$SONAR_HOST_URL" -e SONAR_TOKEN \\
                                        -v "$WORKSPACE/frontend:/usr/src:ro" \\
                                        sonarsource/sonar-scanner-cli \\
                                        -Dsonar.projectKey="$SONAR_FRONTEND_KEY" \\
                                        -Dsonar.projectName="Gestion Projets - Frontend (Angular)" \\
                                        -Dsonar.sources=src \\
                                        -Dsonar.exclusions="**/*.spec.ts,**/node_modules/**" \\
                                        -Dsonar.scm.disabled=true \\
                                        -Dsonar.working.directory=/tmp/.scannerwork
                                    ci/sonar-report.sh wait "$SONAR_FRONTEND_KEY"
                                '''
                            }
                        }
                    }
                }

                stage('4.5 Dashboard SonarQube - APRES analyse') {
                    steps {
                        withCredentials([string(credentialsId: 'sonar-token', variable: 'SONAR_TOKEN')]) {
                            sh '''
                                ci/sonar-report.sh dashboard "$SONAR_BACKEND_KEY" \\
                                    "DASHBOARD APRES ANALYSE - BACKEND" reports/sonar-apres-backend.txt
                                if [ "$ANALYSE_FRONTEND" = "true" ]; then
                                    ci/sonar-report.sh dashboard "$SONAR_FRONTEND_KEY" \\
                                        "DASHBOARD APRES ANALYSE - FRONTEND" reports/sonar-apres-frontend.txt
                                fi
                            '''
                        }
                    }
                }

                stage('4.6 Quality Gate') {
                    steps {
                        withCredentials([string(credentialsId: 'sonar-token', variable: 'SONAR_TOKEN')]) {
                            catchError(buildResult: 'SUCCESS', stageResult: 'UNSTABLE') {
                                sh '''
                                    if [ "$ANALYSE_FRONTEND" = "true" ]; then
                                        ci/sonar-report.sh gate "$SONAR_FRONTEND_KEY"
                                    fi
                                '''
                            }
                            // Backend : BLOQUANT. Si le gate echoue, aucune image n'est construite.
                            sh 'ci/sonar-report.sh gate "$SONAR_BACKEND_KEY"'
                        }
                    }
                }
            }
        }

        stage('5. Image Docker + Registry') {
            stages {

                stage('5.1 Creation des images') {
                    steps {
                        sh '''
                            docker build -t "$REGISTRY/gestion-backend:$BUILD_NUMBER"  -t "$REGISTRY/gestion-backend:latest"  backend
                            docker build -t "$REGISTRY/gestion-frontend:$BUILD_NUMBER" -t "$REGISTRY/gestion-frontend:latest" frontend
                            docker images | grep -E "REPOSITORY|gestion-(backend|frontend)"
                        '''
                    }
                }

                stage('5.2 Push vers le registry') {
                    steps {
                        script {
                            if (env.REGISTRY_CREDS) {
                                withCredentials([usernamePassword(credentialsId: env.REGISTRY_CREDS,
                                                                  usernameVariable: 'REG_USER',
                                                                  passwordVariable: 'REG_PASS')]) {
                                    sh 'echo "$REG_PASS" | docker login "${REGISTRY%%/*}" -u "$REG_USER" --password-stdin'
                                    pousserImages()
                                }
                            } else {
                                pousserImages()
                            }
                        }
                        sh '''
                            case "$REGISTRY" in
                                localhost:*)
                                    echo "--- Contenu du registry ---"
                                    curl -s "http://$REGISTRY/v2/_catalog"; echo
                                    curl -s "http://$REGISTRY/v2/gestion-backend/tags/list"; echo
                                    curl -s "http://$REGISTRY/v2/gestion-frontend/tags/list"; echo ;;
                            esac
                        '''
                    }
                }
            }
        }

        stage('6. Deploiement (docker compose)') {
            when { expression { params.DEPLOY } }
            stages {

                stage('6.1 Deployer') {
                    steps {
                        sh '''
                            export IMAGE_TAG="$BUILD_NUMBER"
                            docker compose down --remove-orphans || true
                            docker compose pull backend frontend
                            docker compose up -d
                            docker compose ps
                        '''
                    }
                }

                stage('6.2 Tests de sante') {
                    steps {
                        sh '''
                            for i in $(seq 1 40); do
                                if curl -fs http://localhost:8089/entreprise/all > /dev/null; then
                                    echo "Backend operationnel"
                                    break
                                fi
                                echo "  attente du backend... ($i/40)"
                                sleep 5
                            done

                            echo "--- Backend ---"
                            curl -fs http://localhost:8089/entreprise/all; echo
                            echo "--- Frontend ---"
                            curl -fsI http://localhost:4200 | head -1
                            echo "--- Frontend -> Backend (via Nginx) ---"
                            curl -fs http://localhost:4200/entreprise/all; echo
                        '''
                    }
                }
            }
        }
    }

    post {
        success {
            echo """
            ======================================================
              PIPELINE REUSSI - build #${env.BUILD_NUMBER}
              Application : http://192.168.33.10:4200
              SonarQube   : ${env.SONAR_PUBLIC_URL}/dashboard?id=${env.SONAR_BACKEND_KEY}
              Registry    : ${env.REGISTRY}/gestion-backend:${env.BUILD_NUMBER}
            ======================================================
            """
        }
        failure {
            echo 'ECHEC : consulte la Console Output (et le Quality Gate si SonarQube a bloque).'
            sh 'docker compose logs --tail=40 || true'
        }
        always {
            archiveArtifacts artifacts: 'reports/**', allowEmptyArchive: true
            sh 'docker image prune -f || true'
        }
    }
}

def pousserImages() {
    sh '''
        for img in gestion-backend gestion-frontend; do
            docker push "$REGISTRY/$img:$BUILD_NUMBER"
            docker push "$REGISTRY/$img:latest"
        done
    '''
}
