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
                echo 'Compilando el proyecto con maven-3.9.16...'
                script {
                    // Actualizado al nuevo nombre
                    def mvnHome = tool 'maven-3.9.16'
                    
                    sh "${mvnHome}/bin/mvn clean install -DskipTests"
                }
            }
        }

        stage('SonarQube Analysis') {
            steps {
                script {
                    // Actualizado al nuevo nombre
                    def mvnHome = tool 'maven-3.9.16'
                    
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
