plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

base.archivesName.set("GlyphPay")

android {
    namespace = "dev.glyphpay"
    compileSdk = 36

    defaultConfig {
        applicationId = "dev.glyphpay"
        // Glyph SDK работает только на Nothing OS с Android 14+
        minSdk = 34
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // Подписываем debug-ключом, чтобы APK из CI сразу устанавливался на телефон
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
        abortOnError = false
    }
}

dependencies {
    // Официальный Glyph SDK от Nothing (лежит в репозитории)
    implementation(files("libs/glyph-matrix-sdk-2.0.aar"))
    implementation("androidx.core:core-ktx:1.15.0")
}
