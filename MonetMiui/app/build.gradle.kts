plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.sanya.monetmiui"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.sanya.monetmiui"
        minSdk = 31          // system_accent1_* з'явились в Android 12
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    compileOnly("de.robv.android.xposed:api:82")
}
