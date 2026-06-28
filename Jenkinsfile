pipeline {
    agent any

    environment {
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
                echo 'Compilando el proyecto...'
                // Cambiamos 'bat' por 'sh' para que funcione en el contenedor Linux de Jenkins
                sh 'mvn clean install -DskipTests'
            }
        }

        stage('SonarQube Analysis') {
            steps {
                script {
                    withSonarQubeEnv('sonar-server') {
                        echo 'Ejecutando análisis de SonarQube...'
                        sh 'mvn sonar:sonar'
                    }
                }
            }
        }
    }
}
