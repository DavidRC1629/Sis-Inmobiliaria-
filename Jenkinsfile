pipeline {
    agent any

    environment {
        // ID de la credencial que creamos en Jenkins
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
                echo 'Compilando el proyecto con ruta absoluta...'
                script {
                    // Esto extrae la ruta exacta donde Jenkins instaló Maven 3.9
                    def mvnHome = tool 'maven-3.9'
                    
                    // Ejecutamos el binario directamente usando su ruta absoluta
                    sh "${mvnHome}/bin/mvn clean install -DskipTests"
                }
            }
        }

        stage('SonarQube Analysis') {
            steps {
                script {
                    def mvnHome = tool 'maven-3.9'
                    
                    // 'sonar-server' debe ser el nombre que configuraste en Manage Jenkins > System
                    withSonarQubeEnv('sonar-server') {
                        echo 'Ejecutando análisis de SonarQube con ruta absoluta...'
                        sh "${mvnHome}/bin/mvn sonar:sonar"
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
            echo 'Hubo un error en la construcción o el análisis. Revisa la consola.'
        }
    }
}
