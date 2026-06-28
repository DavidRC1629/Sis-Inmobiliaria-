pipeline {
    // Esto le dice a Jenkins: "Usa un contenedor con Maven y Java para correr este pipeline"
    agent {
        docker {
            image 'maven:3.9-eclipse-temurin-17'
        }
    }

    environment {
        // Asegúrate de que este ID coincida con el que creaste en Manage Jenkins > Credentials
        SONAR_TOKEN_ID = 'sonar-token'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build con Maven') {
            steps {
                echo 'Compilando el proyecto en el contenedor...'
                // Usamos 'sh' porque el contenedor es Linux
                sh 'mvn clean install -DskipTests'
            }
        }

        stage('SonarQube Analysis') {
            steps {
                script {
                    // 'sonar-server' debe coincidir con el nombre configurado en Manage Jenkins > System
                    withSonarQubeEnv('sonar-server') {
                        echo 'Ejecutando análisis de SonarQube...'
                        sh 'mvn sonar:sonar'
                    }
                }
            }
        }
    }

    post {
        always {
            echo 'Pipeline finalizado.'
        }
        success {
            echo '¡Éxito! El código se compiló y analizó correctamente.'
        }
        failure {
            echo 'Hubo un error. Revisa la consola.'
        }
    }
}
