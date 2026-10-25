pipeline {
    agent any

    tools {
        nodejs 'NodeJS-22'
    }

    options {
        skipDefaultCheckout(true)
        timestamps()
        timeout(time: 45, unit: 'MINUTES')
    }

    stages {
        stage('GitHub Checkout') {
            steps {
                checkout scm
                script {
                    def selectedBranch = env.BRANCH_NAME ?: env.GIT_BRANCH ?: 'SCM-configured branch'
                    echo "Checked out branch: ${selectedBranch}"
                }
            }
        }

        stage('Backend Build and Tests') {
            steps {
                script {
                    if (isUnix()) {
                        sh 'java -version'
                        sh 'cd POS---Microservices && sh ./mvnw -B verify'
                    } else {
                        bat 'java -version'
                        bat 'cd /d POS---Microservices && mvnw.cmd -B verify'
                    }
                }
            }
            post {
                always {
                    junit testResults: 'POS---Microservices/**/target/surefire-reports/TEST-*.xml'
                }
            }
        }

        stage('Frontend Build and Tests') {
            steps {
                script {
                    if (isUnix()) {
                        sh 'node --version && npm --version && cd POS---System-frontend && npm ci && npm run lint && npm test && npm run build'
                    } else {
                        bat 'node --version && npm --version && cd /d POS---System-frontend && npm ci && npm run lint && npm test && npm run build'
                    }
                }
            }
        }

        stage('Pipeline Result') {
            steps {
                echo 'Backend verification, frontend lint/tests, and the production build all passed.'
            }
        }
    }

    post {
        always {
            echo "Final CI result: ${currentBuild.currentResult ?: 'SUCCESS'}"
        }
        success {
            archiveArtifacts artifacts: 'POS---System-frontend/dist/**,POS---Microservices/**/target/*.jar',
                             allowEmptyArchive: true,
                             fingerprint: true
        }
    }
}
