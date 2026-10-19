plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val baleToken = providers.gradleProperty("BALE_BOT_TOKEN")
    .orElse(providers.environmentVariable("BALE_BOT_TOKEN")).getOrElse("")
val baleChatId = providers.gradleProperty("BALE_CHAT_ID")
    .orElse(providers.environmentVariable("BALE_CHAT_ID")).getOrElse("")
fun String.asBuildConfigString(): String = "\"" + replace("\\", "\\\\").replace("\"", "\\\"") + "\""

android {
    namespace = "com.example.pianosms"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.pianosms"
        minSdk = 23
        targetSdk = 32
        versionCode = 3
        versionName = "3.0"
        buildConfigField("String", "BALE_BOT_TOKEN", baleToken.asBuildConfigString())
        buildConfigField("String", "BALE_CHAT_ID", baleChatId.asBuildConfigString())
    }
    buildFeatures { buildConfig = true }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
}
