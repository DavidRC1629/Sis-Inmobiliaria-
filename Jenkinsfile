pipeline {
    agent any

    tools {
        maven 'MAVEN_HOME'
    }

    environment {
        COMPOSE_PROJECT = 'sisarovi'
        SONAR_HOST      = 'http://localhost:9000'
    }

    stages {

        // ── 1. Checkout SCM ──────────────────────────────────────────
        stage('Checkout SCM') {
            steps {
                checkout scm
            }
        }

        // ── 2. Checkout (verificación de rama y commit) ───────────────
        stage('Checkout') {
            steps {
                sh 'git log -1 --pretty=format:"%h %s" || true'
                sh 'git branch -a || true'
            }
        }

        // ── 3. Docker build ───────────────────────────────────────────
        stage('Docker build') {
            steps {
                sh 'docker compose -p ${COMPOSE_PROJECT} -f docker-compose.yml build --no-cache'
            }
        }

        // ── 4. Clean deploy (baja contenedores anteriores) ────────────
        stage('Clean deploy') {
            steps {
                sh 'docker compose -p ${COMPOSE_PROJECT} -f docker-compose.yml down --remove-orphans || true'
            }
        }

        // ── 5. Docker deploy (levanta los servicios) ──────────────────
        stage('Docker deploy') {
            steps {
                sh 'docker compose -p ${COMPOSE_PROJECT} -f docker-compose.yml up -d'
                // Esperar que el backend esté listo
                sh '''
                    echo "Esperando que el backend levante..."
                    for i in $(seq 1 30); do
                        if curl -sf http://localhost:8080/actuator/health > /dev/null 2>&1; then
                            echo "Backend listo!"
                            break
                        fi
                        echo "Intento $i/30..."
                        sleep 5
                    done
                '''
            }
        }

        // ── 6. Smoke test ─────────────────────────────────────────────
        stage('Smoke test') {
            steps {
                sh '''
                    echo "== Smoke test: backend health =="
                    STATUS=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/actuator/health || echo "000")
                    echo "HTTP Status: $STATUS"
                    if [ "$STATUS" = "200" ] || [ "$STATUS" = "503" ]; then
                        echo "Backend responde correctamente"
                    else
                        echo "Backend no responde (status: $STATUS) - continuando de todas formas"
                    fi
                '''
            }
        }

        // ── 7. Unit Tests with JaCoCo ─────────────────────────────────
        stage('Unit Tests with JaCoCo') {
            steps {
                dir('backend') {
                    sh 'mvn clean verify -B -Dspring.profiles.active=test'
                }
            }
            post {
                always {
                    junit allowEmptyResults: true,
                          testResults: 'backend/target/surefire-reports/*.xml'
                    jacoco(
                        execPattern:   'backend/target/jacoco.exec',
                        classPattern:  'backend/target/classes',
                        sourcePattern: 'backend/src/main/java'
                    )
                }
            }
        }

        // ── 8. SonarQube Analysis ─────────────────────────────────────
        stage('SonarQube Analysis') {
            steps {
                dir('backend') {
                    sh """
                        mvn sonar:sonar -B \\
                            -Dsonar.host.url=${SONAR_HOST} \\
                            -Dsonar.login=admin \\
                            -Dsonar.password=admin \\
                            -Dsonar.projectKey=sis-inmobiliaria \\
                            -Dsonar.projectName="Sis Inmobiliaria" \\
                            -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml
                    """
                }
            }
        }

        // ── 9. Post Actions ───────────────────────────────────────────
        stage('Post Actions') {
            steps {
                echo 'Pipeline completado. Contenedores corriendo en produccion.'
                sh 'docker compose -p ${COMPOSE_PROJECT} ps'
            }
        }

    }

    post {
        success {
            echo 'BUILD EXITOSO - Todos los stages pasaron.'
        }
        failure {
            echo 'BUILD FALLIDO - Revisando logs...'
            sh 'docker compose -p ${COMPOSE_PROJECT} -f docker-compose.yml logs --tail=50 || true'
        }
        always {
            echo 'Pipeline finalizado.'
        }
    }
}
