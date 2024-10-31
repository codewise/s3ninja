pipeline {
    agent {
        kubernetes {
            inheritFrom 'maven-21-docker-large'
            defaultContainer 'maven-java-21'
        }
    }

    parameters {
        gitParameter branchFilter: 'origin/(.*)', defaultValue: 'master', name: 'BRANCH', type: 'PT_BRANCH'
    }

    stages {
        stage('Init') {
            steps {
                script {
                    branch = "${params.BRANCH}"
                    commit = "${GIT_COMMIT}".substring(0, 8)
                    version = "${BUILD_NUMBER}-${branch}-${commit}"

                    currentBuild.displayName = "${version}"
                }
            }
        }
        stage('Build') {
            steps {
                sh """
                    echo "Setting final version to ${version}"
                    mvn versions:set -DnewVersion=${version}
                    mvn install
                """
            }
        }
        stage('Docker image build') {
            steps {
                sh "echo Building docker with following parameters: ${params}"
                sh "docker buildx create --driver docker-container --use"

                sh """docker buildx build \
                                    --progress=plain \
                                    --platform linux/amd64,linux/arm64 \
                                    --tag nexus3.zeropark.codewise.com:5100/zeropark-s3-ninja:${branch} \
                                    --push .
                                """
            }
        }
    }
    post {
        always {
            junit(
                    allowEmptyResults: true,
                    testResults: '**/target/surefire-reports/*.xml'
            )
        }
    }
}