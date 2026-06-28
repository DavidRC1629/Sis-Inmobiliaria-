pipeline {
    agent any

    // Esto le indica a Jenkins que busque la instalación de Maven 
    // que configuraremos en Manage Jenkins > Tools
    tools {
        maven 'maven-3.9'
    }

    environment {
        // ID de la credencial que creamos en Jenkins (Credentials > System)
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
                // Usamos sh porque Jenkins corre en un entorno Linux (contenedor)
                sh 'mvn clean install -DskipTests'
            }
        }

        stage('SonarQube Analysis') {
            steps {
                script {
                    // 'sonar-server' es el nombre que diste en Manage Jenkins > System
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
            echo 'Hubo un error en la construcción o el análisis. Revisa la consola.'
        }
    }
}
