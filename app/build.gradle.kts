plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.steve.newapp"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.steve.newapp"
        minSdk = 26
        targetSdk = 34
        versionCode = 38
        versionName = "38.0"
    }

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        buildConfigField(
            "String",
            "NEW_API_KEY",
            "\"${System.getenv("NEW_API_KEY") ?: ""}\""
        )
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
}
