pipeline {
    agent any

    environment {
        IMAGE_NAME = 'campus-equipment-booking'
        IMAGE_TAG  = 'set-a'
        APP_NAME   = 'campus-equipment-booking'
    }

    options {
        timestamps()
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build & Test') {
            steps {
                runCmd 'mvn -B clean test'
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: 'target/surefire-reports/*.xml'
                }
            }
        }

        stage('Package') {
            steps {
                runCmd 'mvn -B package -DskipTests'
                archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
            }
        }

        stage('Docker Build') {
            steps {
                runCmd "docker build -t ${IMAGE_NAME}:${IMAGE_TAG} ."
            }
        }

        stage('Deploy to Minikube') {
            steps {
                runCmd "minikube image load ${IMAGE_NAME}:${IMAGE_TAG}"
                runCmd 'kubectl apply -f deployment.yaml'
                runCmd 'kubectl apply -f service.yaml'
                // The tag never changes, so force the pods to pick up the newly loaded image
                runCmd "kubectl rollout restart deployment/${APP_NAME}"
                runCmd "kubectl rollout status deployment/${APP_NAME} --timeout=120s"
                runCmd 'kubectl get deployments,pods,svc -o wide'
            }
        }
    }

    post {
        success {
            echo 'Pipeline finished: app built, tested, containerised and deployed to Minikube.'
        }
        failure {
            echo 'Pipeline failed. Check the stage logs above.'
        }
    }
}

// Runs a command with sh on Linux/macOS agents and bat on Windows agents
def runCmd(String cmd) {
    if (isUnix()) {
        sh cmd
    } else {
        bat cmd
    }
}
