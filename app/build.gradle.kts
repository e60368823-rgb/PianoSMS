plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.pianosms"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.example.pianosms"
        minSdk = 23
        targetSdk = 32
        versionCode = 1
        versionName = "1.0"
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
}
