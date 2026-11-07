plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.glyphpay.nfc"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.glyphpay.nfc"
        minSdk = 34
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        // Ключ Nothing для Glyph SDK. Для отладки достаточно "test"
        // (+ adb shell settings put global nt_glyph_interface_debug_enable 1).
        // Свой ключ можно передать через GitHub Secret NOTHING_API_KEY.
        val nothingKey = System.getenv("NOTHING_API_KEY")?.takeIf { it.isNotBlank() } ?: "test"
        manifestPlaceholders["nothingKey"] = nothingKey
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // Подписываем debug-ключом, чтобы APK из сборки сразу можно было установить
            signingConfig = signingConfigs.getByName("debug")
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
        checkReleaseBuilds = false
        abortOnError = false
    }
}

dependencies {
    // Официальный Nothing Glyph SDK (Glyph Developer Kit)
    implementation(files("libs/glyph-sdk.aar"))

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
}
