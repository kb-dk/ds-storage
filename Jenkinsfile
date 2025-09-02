pipeline {
    agent { label 'DS agent' }

    environment {
        MVN_SETTINGS = '/etc/m2/settings.xml' //This should be changed in Jenkins config for the DS agent
    }

    triggers {
        // This triggers the pipeline when a PR is opened or updated or so I hope
        githubPush()
    }

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
                sh "mvn -s ${env.MVN_SETTINGS} clean package "
            }
        }
        stage('Push to Nexus if Master') {
            when {
                // Check if Build was successful
                expression { params.Build == true && currentBuild.result == null && env.BRANCH_NAME == 'DRA-2011_Jenkins_build' }
            }
            steps {

                echo "Branch name ${env.BRANCH_NAME}"
                sh "mvn -s ${env.MVN_SETTINGS} clean deploy -DskipTests=true"
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