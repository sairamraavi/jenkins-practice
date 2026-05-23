pipeline {
    agent any

    parameters {

        choice(
            name: 'ENVIRONMENT',
            choices: ['dev', 'prod'],
            description: 'Select Environment'
        )

        choice(
            name: 'BRANCH_NAME',
            choices: ['develop', 'prod'],
            description: 'Select Branch'
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

        stage('Clean Workspace') {
            steps {
                cleanWs()
            }
        }

        stage('Clone Repository') {
            steps {

                git branch: "${params.BRANCH_NAME}",
                    url: "${GIT_REPO}"

                sh 'pwd'
                sh 'ls -la'
            }
        }

        stage('Build DEV Docker Image') {

            when {
                allOf {
                    expression { params.ENVIRONMENT == 'dev' }
                    expression { params.BRANCH_NAME == 'develop' }
                }
            }

            steps {

                sh """
                    docker build --no-cache \
                    -t ${DOCKER_IMAGE}:dev_${BUILD_NUMBER} .
                """
            }
        }

        stage('Build PROD Docker Image') {

            when {
                allOf {
                    expression { params.ENVIRONMENT == 'prod' }
                    expression { params.BRANCH_NAME == 'prod' }
                }
            }

            steps {

                sh """
                    docker build --no-cache \
                    -t ${DOCKER_IMAGE}:prod_${BUILD_NUMBER} .
                """
            }
        }

        stage('Push Docker Image') {

            when {
                allOf {
                    expression { params.ENVIRONMENT == 'prod' }
                    expression { params.BRANCH_NAME == 'prod' }
                }
            }

            steps {

                script {

                    docker.withRegistry('', "${DOCKER_CRED}") {

                        sh """
                            docker push ${DOCKER_IMAGE}:prod_${BUILD_NUMBER}
                        """
                    }
                }
            }
        }

        stage('Deploy DEV') {

            when {
                allOf {
                    expression { params.ENVIRONMENT == 'dev' }
                    expression { params.BRANCH_NAME == 'develop' }
                }
            }

            steps {

                sshagent(credentials: ["${SSH_CRED_ID}"]) {

                    sh """
                        docker save ${DOCKER_IMAGE}:dev_${BUILD_NUMBER} \
                        > image.tar
                    """

                    sh """
                        scp -o StrictHostKeyChecking=no \
                        image.tar \
                        ${EC2_USERNAME}@${DEV_EC2_HOST}:/home/${EC2_USERNAME}/
                    """

                    sh """
                        ssh -o StrictHostKeyChecking=no \
                        ${EC2_USERNAME}@${DEV_EC2_HOST} '

                            docker load < image.tar

                            docker stop ${CONTAINER_NAME} || true
                            docker rm ${CONTAINER_NAME} || true

                            docker run -d \
                            --name ${CONTAINER_NAME} \
                            -p ${APP_PORT}:80 \
                            ${DOCKER_IMAGE}:dev_${BUILD_NUMBER}

                            docker image prune -f
                        '
                    """
                }
            }
        }

        stage('Deploy PROD') {

            when {
                allOf {
                    expression { params.ENVIRONMENT == 'prod' }
                    expression { params.BRANCH_NAME == 'prod' }
                }
            }

            steps {

                sshagent(credentials: ["${SSH_CRED_ID}"]) {

                    sh """
                        ssh -o StrictHostKeyChecking=no \
                        ${EC2_USERNAME}@${PROD_EC2_HOST} '

                            docker pull ${DOCKER_IMAGE}:prod_${BUILD_NUMBER}

                            docker stop ${CONTAINER_NAME} || true
                            docker rm ${CONTAINER_NAME} || true

                            docker run -d \
                            --name ${CONTAINER_NAME} \
                            -p ${APP_PORT}:80 \
                            ${DOCKER_IMAGE}:prod_${BUILD_NUMBER}

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