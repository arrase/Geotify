import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
}

fun getSecretFromKWallet(entryName: String): String? {
    if (System.getenv("CI") == "true" || System.getenv("GITHUB_ACTIONS") == "true") return null
    return try {
        providers.exec {
            commandLine("kwallet-query", "-r", entryName, "-f", "Geotify", "kdewallet")
            isIgnoreExitValue = true
        }.standardOutput.asText.orNull?.trim()?.let {
            if (it.isEmpty() || it.contains("fallado")) null else it
        }
    } catch (e: Exception) {
        null
    }
}

fun getSigningSecret(entryName: String, propertyKey: String, envVar: String): String? {
    // 1. Try KWallet
    getSecretFromKWallet(entryName)?.let { return it }

    // 2. Try local.properties
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        val localProperties = Properties().apply {
            localPropertiesFile.inputStream().use { load(it) }
        }
        localProperties.getProperty(propertyKey)?.let { return it }
    }

    // 3. Try Environment Variable
    System.getenv(envVar)?.let { return it }

    return null
}

android {
    namespace = "dev.arrase.geotify"
    compileSdk = 37

    defaultConfig {
        applicationId = "dev.arrase.geotify"
        minSdk = 24
        targetSdk = 36
        versionCode = 8
        versionName = "1.0.0-alpha.8"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }

    signingConfigs {
        create("release") {
            storeFile = file("geotify-upload-key.keystore")
            storePassword = getSigningSecret("keystore_password", "signing.storePassword", "GEOTIFY_KEYSTORE_PASSWORD") ?: ""
            keyAlias = "geotify-key"
            keyPassword = getSigningSecret("key_password", "signing.keyPassword", "GEOTIFY_KEY_PASSWORD") ?: ""
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            val keystoreFile = file("geotify-upload-key.keystore")
            if (keystoreFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
                val storePass = signingConfigs.getByName("release").storePassword
                if (storePass.isNullOrEmpty()) {
                    logger.warn("WARNING: geotify-upload-key.keystore exists but storePassword could not be retrieved from KWallet, local.properties, or environment variables. Build might fail or be unsigned.")
                }
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

ksp {
    arg("room.schemaLocation", "${projectDir}/schemas")
}

dependencies {
    // Compose BOM
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    // AndroidX Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // WorkManager
    implementation(libs.androidx.work.runtime)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // AppFunctions
    implementation(libs.androidx.appfunctions)
    implementation(libs.androidx.appfunctions.service)
    ksp(libs.androidx.appfunctions.compiler)

    // Play Services Location (FusedLocationProvider + GeofencingClient)
    implementation(libs.play.services.location)
    implementation(libs.kotlinx.coroutines.play.services)

    // OSM Maps
    implementation(libs.osmdroid.android)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.kotlin)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}