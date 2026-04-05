import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.ksp)
    alias(libs.plugins.compose.compiler)
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

android {
    namespace = "com.sonara.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.sonara.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        // API keys from environment variables (GitHub Actions secrets)
        buildConfigField("String", "LASTFM_API_KEY", "\"${System.getenv("LASTFM_API_KEY") ?: ""}\""  )
        buildConfigField("String", "LASTFM_SHARED_SECRET", "\"${System.getenv("LASTFM_SHARED_SECRET") ?: ""}\"")
        buildConfigField("String", "GEMINI_API_KEY", "\"${System.getenv("GEMINI_API_KEY") ?: ""}\"")
    }

    // Force compose-group-mapping to Kotlin 2.3.20 instead of AGP's bundled 2.2.10
    configurations.all {
        resolutionStrategy.eachDependency {
            if (requested.group == "org.jetbrains.kotlin" && requested.name == "compose-group-mapping") {
                useVersion("2.3.20")
                because("AGP 9.0.0 bundles Kotlin 2.2.10 but we use 2.3.20")
            }
        }
    }

    signingConfigs {
        create("release") {
            if (keystorePropertiesFile.exists()) {
                storeFile = file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
            }
        }
    }

    buildTypes {
        debug {
            if (keystorePropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (keystorePropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin.jvmToolchain(17)

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore)

    // Compose BOM — ek jagah version, baaki sab auto
    val bom = platform(libs.compose.bom)
    implementation(bom)
    androidTestImplementation(bom)

    // Compose UI core
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.foundation)
    implementation(libs.compose.animation)
    implementation(libs.compose.runtime)
    debugImplementation(libs.compose.ui.tooling)

    // Material 3 Expressive — THE main one
    implementation(libs.material3)
    implementation(libs.material3.window)
    implementation(libs.material3.adaptive)

    // Icons — manually add karna padega M3 1.4.0+
    implementation(libs.icons.core)
    implementation(libs.icons.extended)

    // Shape Morphing (M3E exclusive — RoundedPolygon, Morph)
    implementation(libs.graphics.shapes)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.retrofit.core)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp.logging)
    implementation(libs.gson)

    implementation(libs.kotlinx.coroutines)
    implementation(libs.workmanager.runtime)
    implementation(libs.coil.compose)
}
