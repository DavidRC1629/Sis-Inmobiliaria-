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

        stage('Preparar Maven') {
            steps {
                echo 'Descargando Maven usando curl...'
                sh '''
                    curl -sLO https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.6/apache-maven-3.9.6-bin.tar.gz
                    tar -xzf apache-maven-3.9.6-bin.tar.gz
                '''
            }
        }

        stage('Build con Maven') {
            steps {
                echo 'Entrando a la carpeta exacta del backend...'
                // Ruta exacta basada en tu repositorio de GitHub
                dir('Backend/inmobiliario-backend') {
                    sh "${WORKSPACE}/apache-maven-3.9.6/bin/mvn clean install -DskipTests"
                }
            }
        }

        stage('SonarQube Analysis') {
            steps {
                script {
                    withSonarQubeEnv('sonar-server') {
                        echo 'Ejecutando análisis de SonarQube...'
                        // Misma ruta exacta para SonarQube
                        dir('Backend/inmobiliario-backend') {
                            sh "${WORKSPACE}/apache-maven-3.9.6/bin/mvn sonar:sonar"
                        }
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
