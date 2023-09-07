pipeline {
    agent dockerfile {
        file 'Dockerfile.build'
        args '-v /root/.m2:/root/.m2'
    }

    stage('Test') {
        steps {
            sh 'mvn test'
        }
        post {
            always {
                junit 'target/surefire-reports/*.xml'
            }
        }
    }
}