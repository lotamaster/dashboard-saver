plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Set in gradle.properties; forks change it there.
val appId = providers.gradleProperty("dashboardsaver.applicationId").get()

// From the CI tag (-PversionName=1.2.3), else a development default.
// versionCode is derived as major*10000 + minor*100 + patch.
val appVersion = providers.gradleProperty("versionName").orNull?.removePrefix("v") ?: "0.2.0"
val appVersionCode = appVersion.substringBefore('-').split('.')
    .map { it.toIntOrNull() ?: 0 }
    .let { (it + listOf(0, 0, 0)).take(3) }
    .let { (major, minor, patch) -> major * 10000 + minor * 100 + patch }

// Optional, from ~/.gradle/gradle.properties or -P: never committed.
val defaultUrl = providers.gradleProperty("dashboardsaver.defaultUrl").orNull ?: ""

android {
    namespace = "com.lotamaster.dashboardsaver"
    compileSdk = 35

    defaultConfig {
        applicationId = appId
        minSdk = 26
        targetSdk = 35
        versionCode = appVersionCode
        versionName = appVersion

        buildConfigField("String", "DEFAULT_URL", "\"${defaultUrl.replace("\\", "\\\\").replace("\"", "\\\"")}\"")
        // dream.xml needs the full component name of the settings screen.
        resValue("string", "dream_settings_activity", "$appId/com.lotamaster.dashboardsaver.SettingsActivity")
    }

    buildFeatures {
        buildConfig = true
    }

    // Release signing comes from the environment (CI secrets or your shell),
    // so no key or password ever lives in the repo. Without it, the release
    // build is left unsigned.
    val keystore = System.getenv("SIGNING_KEYSTORE")
    signingConfigs {
        if (keystore != null) {
            create("release") {
                storeFile = file(keystore)
                storePassword = System.getenv("SIGNING_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("SIGNING_KEY_ALIAS")
                keyPassword = System.getenv("SIGNING_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            if (keystore != null) signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    lint {
        // compileSdk 35 is the newest this AGP (8.7) supports.
        disable += "OldTargetApi"
    }
}
