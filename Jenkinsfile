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

        stage('Rastreo de Archivos') {
            steps {
                echo 'Buscando la ubicación exacta del pom.xml en tu proyecto...'
                // Este comando buscará el pom.xml y nos mostrará la ruta exacta en la consola
                sh 'find . -name pom.xml'
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
                echo 'Entrando a la carpeta del backend...'
                // ⚠️ SI TU CARPETA EN GITHUB ES CON MAYÚSCULA, CAMBIA 'backend' POR 'Backend'
                dir('backend') {
                    sh "${WORKSPACE}/apache-maven-3.9.6/bin/mvn clean install -DskipTests"
                }
            }
        }

        stage('SonarQube Analysis') {
            steps {
                script {
                    withSonarQubeEnv('sonar-server') {
                        echo 'Ejecutando análisis de SonarQube...'
                        // ⚠️ CAMBIA AQUÍ TAMBIÉN SI ES CON MAYÚSCULA
                        dir('backend') {
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
            echo 'Hubo un error. Revisa la consola.'
        }
    }
}
