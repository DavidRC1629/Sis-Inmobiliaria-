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
                echo 'Compilando el proyecto con la instalación oficial de maven-3.9...'
                script {
                    // Aquí usamos el nombre exacto 'maven-3.9' que está en tu Jenkins Tools
                    def mvnHome = tool 'maven-3.9'
                    
                    // Ejecutamos el binario usando la ruta absoluta exacta en el contenedor
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
                        echo 'Ejecutando análisis de SonarQube...'
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
