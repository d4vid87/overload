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
        versionCode = 16
        versionName = "0.11.0"
        manifestPlaceholders["appLabel"] = "Overload"
    }
    // Stable release key. Previously `release` reused signingConfigs.debug, and CI generates a
    // fresh debug keystore on every run — so each release was signed with a DIFFERENT key, no
    // update could install over the previous one, and every upgrade meant uninstall + data loss.
    // Keystore comes from CI secrets (or ~/liftlog-keystore locally); absent, we fall back to debug.
    val keystoreFile = (findProperty("overload.keystore") as String?)
        ?: System.getenv("ANDROID_KEYSTORE_FILE")
    val keystorePassword = (findProperty("overload.keystorePassword") as String?)
        ?: System.getenv("ANDROID_KEYSTORE_PASSWORD")
    val keyAlias0 = (findProperty("overload.keyAlias") as String?)
        ?: System.getenv("ANDROID_KEY_ALIAS")
    val keyPassword0 = (findProperty("overload.keyPassword") as String?)
        ?: System.getenv("ANDROID_KEY_PASSWORD")
    val hasReleaseKey = keystoreFile != null && file(keystoreFile).exists() &&
        keystorePassword != null && keyAlias0 != null && keyPassword0 != null

    signingConfigs {
        if (hasReleaseKey) {
            create("release") {
                storeFile = file(keystoreFile!!)
                storePassword = keystorePassword
                keyAlias = keyAlias0
                keyPassword = keyPassword0
            }
        }
    }

    buildTypes {
        debug {
            // Local ADB previews can coexist with the user's release and its database.
            if (providers.gradleProperty("overload.preview").orNull == "true") {
                applicationIdSuffix = ".preview"
                versionNameSuffix = "-preview"
                manifestPlaceholders["appLabel"] = "Overload Preview"
            }
        }
        release {
            isMinifyEnabled = false
            signingConfig = if (hasReleaseKey) {
                signingConfigs.getByName("release")
            } else {
                logger.warn("No release keystore found — signing with the debug key. This APK will NOT install over a properly signed release.")
                signingConfigs.getByName("debug")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
