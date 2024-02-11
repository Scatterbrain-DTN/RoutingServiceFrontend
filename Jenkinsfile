pipeline {
    agent { label 'build' }

    options {
        skipStagesAfterUnstable()
    }

    stages {
        stage('Build Scatterbrain') {
            steps {
				sh 'git submodule update --init --recursive'
                withGradle {
                    sh './gradlew assembleRelease'
                }
				stash name: 'apk', includes: 'app/build/outputs/apk/release/app-release-unsigned.apk'
            }
        }
    }
    post {
        always {
            cleanWs()
        }
    }
}
