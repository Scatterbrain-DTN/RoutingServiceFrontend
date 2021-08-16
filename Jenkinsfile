pipeline {
    agent { label 'droid' }
    environment {
        ANDROID_SDK_ROOT = "$HOME/Android/Sdk"
        ANDROID_HOME = "$ANDROID_SDK_ROOT"
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
				stage('Sign debug release') {
						steps {
								unstash 'apk'
		            signAndroidApks (
                    androidHome: "$ANDROID_HOME",
                    apksToSign: 'app/build/outputs/apk/release/app-release-unsigned.apk',
                    keyStoreId: 'android-dev',
                    keyAlias: 'piracy',
                    skipZipalign: true,
                    archiveSignedApks: true
                )
								stash name: 'signed', includes: 'SignApksBuilder-out/android-dev/piracy/app-release-unsigned.apk/app-release.apk'
						}

				}
        stage('Upload artifacts') {
            steps {
								unstash 'signed'
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
