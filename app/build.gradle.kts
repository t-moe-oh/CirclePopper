plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.android.kotlin.multiplatform.library")
}

kotlin {
    android {
        namespace = "com.game.circlepopper.library"
        compileSdk = 37
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    listOf(iosX64(), iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "shared"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation("org.jetbrains.androidx.lifecycle:lifecycle-viewmodel:2.9.6")
            implementation("org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose:2.9.6")
        }
        androidMain.dependencies {
            implementation("androidx.activity:activity-compose:1.10.0")
            implementation("androidx.core:core-ktx:1.15.0")
        }
    }
}
