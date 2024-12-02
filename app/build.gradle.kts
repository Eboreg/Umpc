@file:Suppress("UnstableApiUsage")

import com.google.protobuf.gradle.id
import java.io.FileInputStream
import java.util.Properties

val keystoreProperties = Properties()
val secretsProperties = Properties()

keystoreProperties.load(FileInputStream(rootProject.file("keystore.properties")))
secretsProperties.load(FileInputStream(rootProject.file("secrets.properties")))

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.kapt")
    id("com.google.dagger.hilt.android")
    id("kotlin-parcelize")
    id("com.google.protobuf")
}

kotlin {
    jvmToolchain(17)
}

android {
    signingConfigs {
        create("release") {
            storeFile = file(keystoreProperties["storeFile"] as String)
            storePassword = keystoreProperties["storePassword"] as String
            keyAlias = keystoreProperties["keyAlias"] as String
            keyPassword = keystoreProperties["keyPassword"] as String
        }
    }

    namespace = "us.huseli.umpc"
    compileSdk = 34

    defaultConfig {
        val spotifyClientId = secretsProperties["spotifyClientId"] as String
        val spotifyClientSecret = secretsProperties["spotifyClientSecret"] as String

        applicationId = "us.huseli.umpc"
        minSdk = 26
        targetSdk = 34
        versionCode = 9
        versionName = "1.0.0-beta.9"
        // multiDexEnabled = true

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true

        setProperty("archivesBaseName", "umpc_$versionName")
        buildConfigField("String", "spotifyClientId", "\"$spotifyClientId\"")
        buildConfigField("String", "spotifyClientSecret", "\"$spotifyClientSecret\"")
    }

    buildTypes {
        debug {
            isDebuggable = true
            isRenderscriptDebuggable = true
            applicationIdSuffix = ".debug"
        }
        release {
            // isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
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

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        viewBinding = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.3"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.23.4"
    }

    // Generates the java Protobuf-lite code for the Protobufs in this project. See
    // https://github.com/google/protobuf-gradle-plugin#customizing-protobuf-compilation
    // for more information.
    generateProtoTasks {
        all().forEach { task ->
            task.builtins {
                id("java") {
                    option("lite")
                }
            }
        }
    }
}

val lifecycleVersion = "2.6.2"
val composeVersion = "1.5.4"

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.preference:preference-ktx:1.2.1")

    // Compose:
    implementation("androidx.compose.ui:ui:$composeVersion")
    implementation("androidx.compose.ui:ui-graphics:$composeVersion")
    implementation("androidx.activity:activity-compose:1.8.0")
    implementation("androidx.navigation:navigation-compose:2.7.4")

    // Material:
    implementation("androidx.compose.material:material:$composeVersion")
    implementation("androidx.compose.material3:material3:1.1.2")
    implementation("androidx.compose.material:material-icons-extended:$composeVersion")

    // Lifecycle:
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:$lifecycleVersion")

    // Hilt:
    implementation("com.google.dagger:hilt-android:2.48.1")
    implementation("androidx.hilt:hilt-navigation-compose:1.0.0")
    kapt("com.google.dagger:hilt-compiler:2.48.1")

    // Exoplayer:
    implementation("androidx.media3:media3-exoplayer:1.1.1")

    // Reorder:
    implementation("org.burnoutcrew.composereorderable:reorderable:0.9.6")

    // Gson:
    implementation("com.google.code.gson:gson:2.10.1")

    // Datastore (Proto):
    implementation("androidx.datastore:datastore:1.0.0")
    implementation("com.google.protobuf:protobuf-javalite:3.23.4")

    // MediaButtonReceiver:
    implementation("androidx.media:media:1.6.0")

    // Splashscreen:
    implementation("androidx.core:core-splashscreen:1.0.1")

    // Theme:
    implementation("com.github.Eboreg:RetainTheme:2.2.1")

    // Volley:
    implementation("com.android.volley:volley:1.2.1")
}
