pipeline {
    agent {
            dockerfile {
                filename 'Dockerfile.build'
                args '-v /root/.m2:/root/.m2'
            }
    }
    stages {
        stage('Build') {
            steps {
                sh 'mvn -B -DskipTests clean compile'
            }
        }
        // stage('Test') {
        //     steps {
        //         sh 'mvn test'
        //     }
        //     post {
        //         always {
        //             junit 'target/surefire-reports/*.xml'
        //         }
        //     }
        // }
        // stage('SonarQube analysis') {
        //     steps {
        //         withSonarQubeEnv('SonarQubeServer') {
        //             sh 'mvn sonar:sonar -s .m2/settings.xml'
        //         }
        //     }
        // }
        // stage('Quality Gate') {
        //     steps {
        //         timeout(time: 2, unit: 'MINUTES') {
        //             waitForQualityGate abortPipeline: true
        //         }
        //     }
        // }
        // stage('Package and Deploy') {
        //     steps {
        //         script {
        //             // Check if the branch name starts with 'release-'
        //             if (env.BRANCH_NAME.startsWith('release-')) {
        //                 // Extract the version from the branch name
        //                 String version = env.BRANCH_NAME.replaceAll(/^release-/, '')

        //                 // Set the project version using Maven
        //                 sh "mvn versions:set -DnewVersion=${version}"
        //             }

        //             // Deploy the project (always)
        //             sh 'mvn deploy -DskipTests -s .m2/settings.xml'

    //             // Archive the deployed artifacts
    //             archiveArtifacts artifacts: '**/target/*.jar', allowEmptyArchive: true
    //         }
    //     }
    // }
    }
    post {
        success {
            script {
                emailext(
                    subject: "Build Successful: ${currentBuild.fullDisplayName}",
                    body: "The build has completed successfully. Check the artifacts here: ${env.BUILD_URL}",
                    to: "philippechampion58@gmail.com",
                )
            }
        }
        failure {
            script {
                emailext(
                    subject: "Build Failure: ${currentBuild.fullDisplayName}",
                    body: "The build failed. Check the artifacts here: ${env.BUILD_URL}",
                    to: getAuthorEmail(),
                )
            }
        }
    }
}

String getAuthorEmail(){
    return sh(script: 'git log -1 --format="%ae" \$GIT_COMMIT', returnStdout: true).trim()
}
