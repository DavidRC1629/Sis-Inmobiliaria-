pipeline {
    agent any

    environment {
        // Asegúrate de que este ID coincida con el que creaste en Jenkins (Manage Jenkins > Credentials)
        SONAR_TOKEN_ID = 'sonar-token'
    }

    stages {
        stage('Checkout') {
            steps {
                // Descarga el código desde GitHub
                checkout scm
            }
        }

        stage('Build con Maven') {
            steps {
                echo 'Compilando el proyecto...'
                // Usamos 'bat' porque estás en Windows. 
                // Si esto falla, cámbialo a 'powershell'
                bat 'mvn clean install -DskipTests'
            }
        }

        stage('SonarQube Analysis') {
            steps {
                script {
                    // 'sonar-server' debe coincidir con el nombre configurado en Manage Jenkins > System
                    withSonarQubeEnv('sonar-server') {
                        echo 'Ejecutando análisis de SonarQube...'
                        bat 'mvn sonar:sonar'
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
            echo '¡Éxito! El código es de alta calidad.'
        }
        failure {
            echo 'Hubo un error en la construcción o el análisis.'
        }
    }
}
