pipeline {
    agent any
    
    environment {
        // Asegúrate de que este ID coincida con el que creaste en Credenciales
        SONAR_CREDENTIAL_ID = 'sonar-token'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        
        stage('Build') {
            steps {
                // Si instalaste Maven en "Tools" como MAVEN_HOME, usa esto:
                // sh 'mvn clean install'
                // O si prefieres llamar a mvn directamente:
                bat 'mvn clean install'
            }
        }
        
        stage('SonarQube Analysis') {
            steps {
                script {
                    // Asegúrate de que 'sonar-server' sea el nombre en Manage Jenkins > System
                    withSonarQubeEnv('sonar-server') {
                        bat 'mvn sonar:sonar'
                    }
                }
            }
        }
    }
}
