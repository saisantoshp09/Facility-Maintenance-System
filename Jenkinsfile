pipeline {
    agent any

    options {
        timestamps()
        timeout(time: 30, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '20'))
        disableConcurrentBuilds()
    }

    environment {
        CHROME_BIN = '/usr/bin/chromium'
        CI = 'true'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build & Test') {
            parallel {
                stage('Backend') {
                    steps {
                        dir('backend') {
                            sh 'chmod +x mvnw'
                            sh './mvnw -B -ntp verify'
                        }
                    }
                    post {
                        always {
                            junit testResults: 'backend/services/*/target/surefire-reports/*.xml', allowEmptyResults: true
                        }
                    }
                }

                stage('Frontend') {
                    steps {
                        dir('frontend') {
                            sh 'node -v && npm -v'
                            sh 'npm ci'
                            sh 'npm run test:ci'
                            sh 'npm run build'
                        }
                    }
                }
            }
        }
    }

    post {
        success { echo 'FMS CI build succeeded.' }
        failure { echo 'FMS CI build failed.' }
    }
}
