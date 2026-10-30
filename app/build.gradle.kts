plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.glasgram.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.glasgram.app"
        minSdk = 26
        targetSdk = 36
        // Keep applicationId stable forever for in-place APK upgrades.
        // CI release workflow supplies a monotonically increasing version code.
        versionCode = providers.gradleProperty("GLASGRAM_VERSION_CODE")
            .orElse(providers.environmentVariable("GLASGRAM_VERSION_CODE"))
            .map { it.toInt() }
            .getOrElse(1)
        versionName = providers.gradleProperty("GLASGRAM_VERSION_NAME")
            .orElse(providers.environmentVariable("GLASGRAM_VERSION_NAME"))
            .getOrElse("0.2.0")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
        val telegramApiId = providers.gradleProperty("TELEGRAM_API_ID")
            .orElse(providers.environmentVariable("TELEGRAM_API_ID")).getOrElse("0").toIntOrNull() ?: 0
        val telegramApiHash = providers.gradleProperty("TELEGRAM_API_HASH")
            .orElse(providers.environmentVariable("TELEGRAM_API_HASH")).getOrElse("")
        buildConfigField("int", "TELEGRAM_API_ID", telegramApiId.toString())
        buildConfigField("String", "TELEGRAM_API_HASH", "\"${telegramApiHash}\"")
    }

    signingConfigs {
        create("release") {
            val storePath = providers.environmentVariable("GLASGRAM_STORE_FILE").orNull
            val storePass = providers.environmentVariable("GLASGRAM_STORE_PASSWORD").orNull
            val alias = providers.environmentVariable("GLASGRAM_KEY_ALIAS").orNull
            val keyPass = providers.environmentVariable("GLASGRAM_KEY_PASSWORD").orNull
            if (!storePath.isNullOrBlank() && !storePass.isNullOrBlank() &&
                !alias.isNullOrBlank() && !keyPass.isNullOrBlank()) {
                storeFile = file(storePath)
                storePassword = storePass
                keyAlias = alias
                keyPassword = keyPass
            }
        }
    }

    buildTypes {
        debug {
            // Debug builds intentionally use a separate package and debug certificate.
            // They are not upgrade packages for production installations.
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            // Production upgrades require the exact same signing key for every release.
            signingConfig = signingConfigs.getByName("release")
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
    kotlinOptions { jvmTarget = "17" }

    buildFeatures { compose = true; buildConfig = true }
    packaging {
        jniLibs.useLegacyPackaging = true
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // Bundles TDLib Java API and matching Android JNI binaries for supported ABIs.
    implementation("com.github.capullo-tech:lib-tdlib-android:11850efeb5791d4fd386c0eeec1ebce4c5080435")

    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
}

