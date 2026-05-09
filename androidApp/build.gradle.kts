plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.game.circlepopper"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.game.circlepopper"
        minSdk = 23
        targetSdk = 37
        versionCode = 2
        versionName = "1.3"
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("debug")
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

base {
    archivesName = "CirclePopper"
}

dependencies {
    implementation(project(":app"))
    implementation(libs.activity.compose)
    implementation(libs.compose.ui.tooling.preview)
}
