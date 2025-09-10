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
            string(name: 'PR_ID', defaultValue: 'NOT_A_PR', description: 'NOT_A_PR if not part of PR and otherwise the name of the first outer most job og the PR')
            string(name: 'TRIGGERED_BY', defaultValue: 'GITHUB_EVENT', description: 'GITHUB_EVENT if top-most job')
    }

    stages {

        stage('Checkout sourcecode') {
            steps{
                script{
                    checkout scm
                }
            }
        }

        stage('Echo Environment Variables') {
            steps {
                echo "Build: ${env.Build}"
                echo "PR_ID: ${env.PR_ID}"
                echo "TRIGGERED_BY: ${env.TRIGGERED_BY}"
            }
        }

        stage('Change version if PR') {
            when {
                expression { env.BRANCH_NAME ==~ 'PR-[0-9]+' || env.PR_ID ==~ 'PR-[0-9]+' }
            }
            steps {
                    script {
                        sh "mvn -s ${env.MVN_SETTINGS} versions:set -DnewVersion=${env.BRANCH_NAME}-ds-storage-SNAPSHOT"
                        if ( env.PR_ID ==~ 'PR-[0-9]+'){ // Not relevant for ds-storage / ds-kaltura
                            "mvn -s ${env.MVN_SETTINGS} versions:use-dep-version -Dincludes=dk.kb.storage:* -DdepVersion=${env.PR_ID}-${env.TRIGGERED_BY} -DforceVersion=true"
                        }
                        echo "Changing MVN version to ${env.BRANCH_NAME}-ds-storage-SNAPSHOT"
                    }
            }
        }

        stage('Build') {
            when { expression { params.Build == true && env.BRANCH_NAME ==~ "master|release_v[0-9]+|PR-[0-9]+"} }
            steps {
                script {

                    // Execute Maven build
                    sh "mvn -s ${env.MVN_SETTINGS} clean package"
                }
            }
        }

        stage('Push to Nexus') {
            when {
                // Check if Build was successful
                expression { params.Build == true && currentBuild.result == null && env.BRANCH_NAME ==~ "master|release_v[0-9]+|PR-[0-9]+"}
            }
            steps {
                sh "mvn -s ${env.MVN_SETTINGS} clean deploy -DskipTests=true" // Kan vi skippe build let
            }
        }

        stage('Trigger License Build') {
            when {
                expression { params.Build == true && currentBuild.result == null && env.BRANCH_NAME ==~ "PR-[0-9]+" }
            }
            steps {
                script {
                    echo "Triggering: DS-GitHub/ds-license/${env.CHANGE_TARGET}"
                    def result = build job: "DS-GitHub/ds-license/${env.CHANGE_TARGET}",
                        parameters: [
                        string(name: 'PR_ID', value: env.BRANCH_NAME),
                        string(name: 'TRIGGERED_BY', value: 'ds-storage')
                        ]
                        wait: true // Wait for the pipeline to finish
                    echo "Child Pipeline Result: ${result}"
                }
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