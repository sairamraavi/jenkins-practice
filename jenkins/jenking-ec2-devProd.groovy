pipeline {
    agent any

    parameters {
        choice(
            name: 'ENVIRONMENT',
            choices: ['dev', 'prod'],
            description: 'Select Environment'
        )
    }

    environment {
        GIT_REPO = "https://github.com/sairamraavi/jenkins-practice.git"

        SSH_CRED_ID = "sairam-b16a-aws"
        EC2_USERNAME = "ubuntu"

        DEV_EC2_HOST  = "13.127.79.58"
        PROD_EC2_HOST = "13.232.169.197"

        DOCKER_IMAGE = "sairamraavi/jenkins-prod"
        DOCKER_CRED  = "sairam_docker_cred"

        CONTAINER_NAME = "jenkins-practice-app"
        APP_PORT = "80"
    }

    stages {

        stage('Set Environment') {
            steps {
                script {

                    if (params.ENVIRONMENT == "dev") {
                        env.SELECTED_BRANCH = "develop"
                        env.DOCKER_TAG = "dev_${BUILD_NUMBER}"
                        env.TARGET_HOST = "${DEV_EC2_HOST}"
                    } else {
                        env.SELECTED_BRANCH = "prod"
                        env.DOCKER_TAG = "prod_${BUILD_NUMBER}"
                        env.TARGET_HOST = "${PROD_EC2_HOST}"
                    }

                    echo "Environment : ${params.ENVIRONMENT}"
                    echo "Branch      : ${env.SELECTED_BRANCH}"
                    echo "Docker Tag  : ${env.DOCKER_TAG}"
                    echo "Target Host : ${env.TARGET_HOST}"
                }
            }
        }

        stage('Clean Workspace') {
            steps {
                cleanWs()
            }
        }

        stage('Clone Repository') {
            steps {
                git branch: "${env.SELECTED_BRANCH}",
                    url: "${GIT_REPO}"
            }
        }

        stage('Verify Files') {
            steps {
                sh 'pwd'
                sh 'ls -la'
            }
        }

        stage('Build Docker Image') {
            steps {
                sh """
                    docker build --no-cache \
                    -t ${DOCKER_IMAGE}:${DOCKER_TAG} .
                """
            }
        }

        stage('Push Docker Image') {

            when {
                expression {
                    params.ENVIRONMENT == "prod"
                }
            }

            steps {
                script {
                    docker.withRegistry('', "${DOCKER_CRED}") {
                        sh """
                            docker push ${DOCKER_IMAGE}:${DOCKER_TAG}
                        """
                    }
                }
            }
        }

        stage('Deploy to DEV Server') {

            when {
                expression {
                    params.ENVIRONMENT == "dev"
                }
            }

            steps {

                sshagent(credentials: ["${SSH_CRED_ID}"]) {

                    sh """
                        docker save ${DOCKER_IMAGE}:${DOCKER_TAG} > image.tar
                    """

                    sh """
                        scp -o StrictHostKeyChecking=no \
                        image.tar \
                        ${EC2_USERNAME}@${TARGET_HOST}:/home/${EC2_USERNAME}/
                    """

                    sh """
                        ssh -o StrictHostKeyChecking=no \
                        ${EC2_USERNAME}@${TARGET_HOST} '

                            docker load < image.tar

                            docker stop ${CONTAINER_NAME} || true
                            docker rm ${CONTAINER_NAME} || true

                            docker run -d \
                            --name ${CONTAINER_NAME} \
                            -p ${APP_PORT}:80 \
                            ${DOCKER_IMAGE}:${DOCKER_TAG}

                            docker image prune -f
                        '
                    """
                }
            }
        }

        stage('Deploy to PROD Server') {

            when {
                expression {
                    params.ENVIRONMENT == "prod"
                }
            }

            steps {

                sshagent(credentials: ["${SSH_CRED_ID}"]) {

                    sh """
                        ssh -o StrictHostKeyChecking=no \
                        ${EC2_USERNAME}@${TARGET_HOST} '

                            docker pull ${DOCKER_IMAGE}:${DOCKER_TAG}

                            docker stop ${CONTAINER_NAME} || true
                            docker rm ${CONTAINER_NAME} || true

                            docker run -d \
                            --name ${CONTAINER_NAME} \
                            -p ${APP_PORT}:80 \
                            ${DOCKER_IMAGE}:${DOCKER_TAG}

                            docker image prune -f
                        '
                    """
                }
            }
        }
    }

    post {

        success {
            echo "Deployment Successful"
        }

        failure {
            echo "Pipeline Failed"
        }

        always {
            cleanWs()
        }
    }
}