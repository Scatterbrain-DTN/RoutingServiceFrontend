pipeline {
    agent { label 'build' }
     tools {
        gradle "8.4"
     }
    environment {
      ANDROID_HOME = '/opt/android-sdk-linux'
      ANDROID_SDK_ROOT = "$ANDROID_HOME"
      TARGET_VERSION = 34
      EMULATOR_PID = "/build/$BUILD_ID-emu.pid"
      AVD_NAME = "${EXECUTOR_NUMBER}-avd"
    }
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
