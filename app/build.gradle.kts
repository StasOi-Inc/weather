import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

fun apiKey(name: String, envName: String): String =
    "\"" + (System.getenv(envName) ?: localProps.getProperty(name) ?: "") + "\""

fun signingProp(name: String, envName: String): String =
    System.getenv(envName) ?: localProps.getProperty(name) ?: ""

android {
    namespace = "ru.rufp.weather"
    compileSdk = 35

    defaultConfig {
        applicationId = "ru.rufp.weather"
        minSdk = 26
        targetSdk = 35
        versionCode = 3
        versionName = "1.2"
        buildConfigField("String", "OWM_KEY", apiKey("openweathermap.key", "OWM_KEY"))
        buildConfigField("String", "WEATHERAPI_KEY", apiKey("weatherapi.key", "WEATHERAPI_KEY"))
    }

    signingConfigs {
        create("release") {
            val storeFilePath = signingProp("release.store.file", "RELEASE_STORE_FILE")
            if (storeFilePath.isNotBlank()) {
                storeFile = rootProject.file(storeFilePath)
                storePassword = signingProp("release.store.password", "RELEASE_STORE_PASSWORD")
                keyAlias = signingProp("release.key.alias", "RELEASE_KEY_ALIAS")
                keyPassword = signingProp("release.key.password", "RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = if (signingProp("release.store.file", "RELEASE_STORE_FILE").isNotBlank()) {
                signingConfigs.getByName("release")
            } else {
                null
            }
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
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.retrofit)
    implementation(libs.retrofit.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.hilt.work)
    ksp(libs.hilt.ext.compiler)
    implementation(libs.work.runtime.ktx)

    implementation(libs.glance.appwidget)
    implementation(libs.glance.material3)
    implementation(libs.datastore.preferences)
    implementation(libs.maplibre.sdk)

    testImplementation(libs.junit)
}
