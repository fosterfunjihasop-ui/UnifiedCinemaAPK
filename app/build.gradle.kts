plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.example.unifiedcinema"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.unifiedcinema"
        minSdk = 23
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            isDebuggable = true
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
        kotlinCompilerExtensionVersion = "1.5.11"
    }

    packagingOptions {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1,LICENSE,LICENSE.txt,NOTICE,NOTICE.txt}"
        }
    }
}

dependencies {
    // AndroidX Core & Activity
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")

    // Material Components (for XML themes)
    implementation("com.google.android.material:material:1.12.0")

    // Jetpack Compose Material3
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.ui:ui-graphics")

    // Android TV Support (Leanback)
    implementation("androidx.leanback:leanback:1.2.0-alpha02")
    implementation("androidx.leanback:leanback-preference:1.2.0-alpha02")

    // Media3 ExoPlayer
    implementation("androidx.media3:media3-exoplayer:1.3.1")
    implementation("androidx.media3:media3-ui:1.3.1")
    implementation("androidx.media3:media3-common:1.3.1")
    implementation("androidx.media3:media3-datasource:1.3.1")

    // OkHttp & Networking
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // JSON
    implementation("org.json:json:20231013")

    // Kotlinx Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Navigation Compose
    implementation("androidx.navigation:navigation-compose:2.7.6")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.02.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}