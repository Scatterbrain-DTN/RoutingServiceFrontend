pipeline {
    agent any
    environment {
        ANDROID_SDK_ROOT = "$HOME/Android/Sdk"
        ANDROID_HOME = "$ANDROID_SDK_ROOT"
    }

    options {
        skipStagesAfterUnstable()
    }

    stages {
        stage('Build RxAndroidBle') {
            steps {
                dir('RxAndroidBle') {
                    withGradle {
                        sh './gradlew assembleRelease test uploadArchives'
                    }
                }
            }
        }
        stage('Build Scatterbrain') {
            steps {
                withGradle {
                    sh './gradlew assembleRelease'
                }
                signAndroidApks (
                    androidHome: "$ANDROID_HOME",
                    apksToSign: 'app/build/outputs/apk/release/app-release-unsigned.apk',
                    keyStoreId: 'android-dev',
                    keyAlias: 'piracy',
                    skipZipalign: true,
                    archiveSignedApks: true
                )
            }
        }
        stage('Upload artifacts') {
            steps {
                telegramUploader(
                    chatId: '-1001163314914',
                    filter: 'SignApksBuilder-out/android-dev/piracy/app-release-unsigned.apk/app-release.apk',
                    caption: "Scatterbrain frontend ${env.BUILD_TAG}",
                    silent: true,
                    failBuildIfUploadFailed: true
                )
            }
        }
    }
    post {
        always {
            cleanWs()
        }
    }
}
