pipeline {
    agent any

    tools {
        // Nombre exacto registrado en Manage Jenkins > Tools > Maven installations
        maven 'Maven'
    }

    stages {

        stage('Checkout SCM') {
            steps {
                checkout scm
            }
        }

        stage('Build & Test') {
            steps {
                dir('backend') {
                    sh 'mvn clean verify -B'
                }
            }
        }

        stage('Publish Coverage') {
            steps {
                jacoco(
                    execPattern:  'backend/target/jacoco.exec',
                    classPattern: 'backend/target/classes',
                    sourcePattern: 'backend/src/main/java'
                )
            }
        }

    }

    post {
        always {
            junit allowEmptyResults: true,
                  testResults: 'backend/target/surefire-reports/*.xml'
        }
        success {
            echo 'Pipeline completado exitosamente.'
        }
        failure {
            echo 'El pipeline falló. Revisa los logs de Build & Test.'
        }
    }
}
