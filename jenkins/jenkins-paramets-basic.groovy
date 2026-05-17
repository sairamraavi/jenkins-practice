pipeline {
    agent any
    
    parameters{
        string(name: 'BRANCH_NAME', defaultValue: 'main')
        choice(name: 'ENVIRONMENT', choices: ['dev', 'staging', 'prod'])
    }
    
    environment {
        GIT_REPO = "https://github.com/sairamraavi/jenkins-practice.git"
        SSH_CRED_ID = "sairam-b16a-aws"
        EC2_USERNAME = "ubuntu"
        EC2_HOST = "13.127.79.58"
        DOCKER_IMAGE = "sairamraavi/jenkinstest"
    }

    stages {
        stage('Hello') {
            steps {
                echo 'Hello World'
                echo "${params.BRANCH_NAME}"
            }
        }
        stage("Git Checkout"){
            steps{
                git branch: "${params.BRANCH_NAME}",   url: "${env.GIT_REPO}"
            }
        }
        stage("Parallel stages example"){
            parallel{
                stage("Wonderful") {
                    steps {
                        sh """
                        ls -al
                        pwd
                        """
                    }
                }
                stage("Test"){
                    steps{
                        sh"""
                        whoami
                        """
                    }
                }
                
            }
        }
        
    }
    
    post{
        always{
            echo "This will run always"
        }
        success{
            echo "This will run only on success"
        }
        failure{
            echo "This will run only on failure"
        }
    }
}
