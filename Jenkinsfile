pipeline {
    agent any
    stages {
        stage('Build & Test') {
            steps {
                // Entramos primero a la carpeta que se ve en GitHub y luego al backend
                dir('Sis-Inmobiliaria-/backend') {
                    sh 'mvn clean verify'
                }
            }
        }
        stage('Publish Coverage') {
            steps {
                // Ajustamos la ruta para que Jenkins encuentre el reporte dentro de la carpeta
                jacoco buildResults: 'Sis-Inmobiliaria-/backend/target/site/jacoco',
                       execPattern: 'Sis-Inmobiliaria-/backend/target/jacoco.exec',
                       classPattern: 'Sis-Inmobiliaria-/backend/target/classes'
            }
        }
    }
}