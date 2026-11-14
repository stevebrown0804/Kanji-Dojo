plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("kapt")
    kotlin("plugin.parcelize")
    kotlin("plugin.compose")
    id("com.mikepenz.aboutlibraries.plugin")
}


kotlin {
    jvmToolchain(17)
}

android {

    namespace = "ua.syt0r.kanji"

    compileSdk = 36
    defaultConfig {
        applicationId = "ua.syt0r.kanji"
        minSdk = 26
        targetSdk = 36
        versionCode = AppVersion.versionCode
        versionName = AppVersion.versionName
    }

    buildTypes {
        val debug = getByName("debug") {
            versionNameSuffix = "-debug"
            applicationIdSuffix = ".dev"
        }

        val release = getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    flavorDimensions += "version"

    productFlavors {
        create("googlePlay") {
            dimension = "version"
        }

        create("fdroid") {
            dimension = "version"
            applicationIdSuffix = ".fdroid"
        }
    }

    buildFeatures {
        compose = true
    }

    val keystoreFile = rootProject.file("keystore.jks")

    val signedBuildSigningConfig = signingConfigs.create("signedBuild") {
        storeFile = keystoreFile
        System.getenv("KEYSTORE_PASS")?.let { storePassword = it }
        System.getenv("SIGN_KEY")?.let { keyAlias = it }
        System.getenv("SIGN_PASS")?.let { keyPassword = it }
    }

    val debugSigningConfig = signingConfigs.getByName("debug")

    buildTypes.forEach {
        it.signingConfig = if (keystoreFile.exists()) {
            signedBuildSigningConfig
        } else {
            debugSigningConfig
        }
    }

    dependenciesInfo {
        // Removes a signing block with encrypted data for reproducible F-Droid builds
        includeInApk = false
    }

}

dependencies {
    implementation(project(":core"))

}

aboutLibraries {
    configPath = "core/credits"
    excludeFields = arrayOf("generated")
}


tasks.configureEach {
    if (name.contains("fdroid", ignoreCase = true) &&
        name.contains("ArtProfile", ignoreCase = true)
    ) {
        enabled = false
    }
}
