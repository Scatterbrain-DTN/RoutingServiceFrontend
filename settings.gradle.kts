import java.io.File

// TODO: fix
includeBuild("ScatterRoutingService/meshtastic-sdk/")

include(":app")
include(":ScatterRoutingService:app")
project(":ScatterRoutingService:app").projectDir = File("ScatterRoutingService/app")
include(":scatterbrainSDK")
project(":scatterbrainSDK").projectDir = File("ScatterRoutingService/scatterbrainSDK/scatterbrainSDK/")
include(":annotation")
project(":annotation").projectDir = File("ScatterRoutingService/annotation")
include(":ser")
project(":ser").projectDir = File("ScatterRoutingService/ser")
//include ':lazysodiumjava'
//project(':lazysodiumjava').projectDir = new File('ScatterRoutingService/lazysodium-android/lazysodium-java')