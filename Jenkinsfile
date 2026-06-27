// 🚀 Jenkinsfile para Sis-Inmobiliaria (Spring Boot + MySQL)
pipeline {
    agent any

    tools {
        maven 'MAVEN_HOME'
    }

    environment {
        REPO_URL         = 'https://github.com/DavidRC1629/Sis-Inmobiliaria-.git'
        APP_NAME         = 'sis-inmobiliaria'
        DOCKER_IMAGE     = 'sis-inmobiliaria:latest'
        SONAR_CREDENTIAL_ID = 'sonar-server'
    }

    stages {

        stage('📥 Checkout') {
            steps {
                echo "Clonando proyecto desde ${REPO_URL}..."
                cleanWs()
                git branch: 'main', url: "${REPO_URL}"
            }
        }

        stage('🏗️ Build & Test') {
            steps {
                echo 'Compilando y ejecutando pruebas con JaCoCo...'
                sh 'mvn clean install -Dmaven.test.failure.ignore=true -f backend/pom.xml'
            }
        }

        stage('Sonar Analysis') {
            steps {
                timeout(time: 10, unit: 'MINUTES') {
                    echo '📊 === INICIO: ANÁLISIS DE CALIDAD ==='
                    withSonarQubeEnv('sonar-server') {
                        sh """
                            mvn org.sonarsource.scanner.maven:sonar-maven-plugin:3.9.0.2155:sonar \
                            -f backend/pom.xml \
                            -Dsonar.projectKey=${APP_NAME} \
                            -Dsonar.projectName=${APP_NAME} \
                            -Dsonar.coverage.jacoco.xmlReportPaths=backend/target/site/jacoco/jacoco.xml
                        """
                    }
                    echo '✅ === FIN: ANÁLISIS DE CALIDAD COMPLETADO ==='
                }
            }
        }

        stage('🎯 Quality Gate') {
            steps {
                echo 'Esperando resultado de SonarQube...'
                timeout(time: 5, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        stage('🚀 Docker Build & Deploy') {
            steps {
                script {
                    echo 'Construyendo imagen Docker...'
                    sh "docker build -t ${DOCKER_IMAGE} -f docker/Dockerfile backend/"

                    echo 'Desplegando contenedor...'
                    sh "docker stop ${APP_NAME} || true"
                    sh "docker rm   ${APP_NAME} || true"

                    sh "docker run -d --name ${APP_NAME} -p 8080:8080 ${DOCKER_IMAGE}"
                }
            }
        }
    }

    post {
        success {
            echo '✅ ¡Pipeline finalizado con éxito!'
        }
        failure {
            echo '❌ El pipeline falló.'
        }
    }
}
