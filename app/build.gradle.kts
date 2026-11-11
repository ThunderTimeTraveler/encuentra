import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "app.encuentra"
    compileSdk = 36

    defaultConfig {
        applicationId = "app.encuentra"
        // 30 = Android 11: desde ahi existe "Acceso a todos los archivos".
        minSdk = 30
        targetSdk = 35
        versionCode = 2
        versionName = "0.2.0"
    }

    // La llave con la que se firmaron TODAS las versiones publicadas. Vive fuera
    // del repo (keystore.properties, ver keystore.properties.ejemplo). Sin ella
    // el release sale SIN firmar en vez de firmarse con otra llave a escondidas:
    // un APK con otra firma no se instala encima del que la gente ya tiene.
    val propsFirma = Properties().apply {
        val archivo = rootProject.file("keystore.properties")
        if (archivo.exists()) archivo.inputStream().use { load(it) }
    }
    val hayFirma = propsFirma.getProperty("storeFile") != null
    signingConfigs {
        create("encuentra") {
            if (hayFirma) {
                storeFile = rootProject.file(propsFirma.getProperty("storeFile"))
                storePassword = propsFirma.getProperty("storePassword")
                keyAlias = propsFirma.getProperty("keyAlias")
                keyPassword = propsFirma.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (hayFirma) signingConfig = signingConfigs.getByName("encuentra")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.10.01"))
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
}
