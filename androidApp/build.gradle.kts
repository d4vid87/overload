plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    androidTarget()
    sourceSets {
        androidMain.dependencies {
            implementation(project(":shared"))
            implementation(libs.activity.compose)
            // lint InvalidFragmentVersionForActivityResult: transitive fragment < 1.3 breaks registerForActivityResult
            implementation("androidx.fragment:fragment-ktx:1.8.5")
            implementation(libs.barcode.scanner)
            implementation(libs.wearable)
            implementation("androidx.glance:glance-appwidget:1.1.1")
        }
    }
}

android {
    namespace = "dev.dwm.liftlog"
    compileSdk = 36
    defaultConfig {
        applicationId = "dev.dwm.liftlog"
        minSdk = 31
        targetSdk = 36
        versionCode = 12
        versionName = "0.9.3"
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            // ponytail: debug-signed release, fine for sideloading; real keystore if ever distributed
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
