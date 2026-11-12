plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.lotamaster.dreampage"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.lotamaster.dreampage"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }

    // Release signing comes from the environment (CI secrets or your shell),
    // so no key or password ever lives in the repo. Without it, the release
    // build is left unsigned.
    val keystore = System.getenv("DREAMPAGE_KEYSTORE")
    signingConfigs {
        if (keystore != null) {
            create("release") {
                storeFile = file(keystore)
                storePassword = System.getenv("DREAMPAGE_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("DREAMPAGE_KEY_ALIAS")
                keyPassword = System.getenv("DREAMPAGE_KEY_PASSWORD")
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
}
