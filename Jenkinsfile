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
                echo 'Descargando Maven manualmente para saltarnos los bugs de Jenkins...'
                // Descargamos Maven 3.9.6 y lo descomprimimos directamente en la carpeta de trabajo
                sh '''
                    wget -q -nc https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.6/apache-maven-3.9.6-bin.tar.gz || true
                    tar -xzf apache-maven-3.9.6-bin.tar.gz
                '''
            }
        }

        stage('Build con Maven') {
            steps {
                echo 'Compilando el proyecto...'
                // Usamos el Maven que acabamos de descargar
                sh './apache-maven-3.9.6/bin/mvn clean install -DskipTests'
            }
        }

        stage('SonarQube Analysis') {
            steps {
                script {
                    withSonarQubeEnv('sonar-server') {
                        echo 'Ejecutando análisis de SonarQube...'
                        // Usamos el mismo Maven descargado para SonarQube
                        sh './apache-maven-3.9.6/bin/mvn sonar:sonar'
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
