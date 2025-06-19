pipeline {
    agent { label 'DS agent' }

    parameters {
            booleanParam(name: 'Build', defaultValue: true, description: 'Perform mvn package')
    }

    stages {
        stage('Build') {
            when { expression { params.Build == true } }
            steps {
                script {
                    // Checkout the source code from the repository
                    checkout scm
                }
                // Execute Maven build
                sh 'mvn clean package'
            }
        }
    }

    post {
        success {
            echo 'Build completed successfully.'
        }
        failure {
            echo 'Build failed.'
        }
    }
}