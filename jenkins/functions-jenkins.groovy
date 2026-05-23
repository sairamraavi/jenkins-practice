def deploy(envName){
    echo "Deploying to ${envName} environment"
}
pipeline {
    agent any
    // agent {
    //     label 'docker'
    // }
    parameters {
        string(
            name: 'ENV_NAME', 
            choices: ['dev', 'prod'],
            defaultValue: 'dev', 
            description: 'Environment to deploy to'
        )

    }
    stages {
        stage('Deploy to Dev') {
            steps {
                script {
                    deploy(params.ENV_NAME)
                }
            }
        }
        stage('Deploy to Prod') {
            steps {
                script {
                    deploy(params.ENV_NAME)
                }
            }
        }
    }
}