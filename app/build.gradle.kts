plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.example.progetto"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.example.progetto"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.play.services.location)
    implementation(libs.accompanist.permissions)
    implementation(libs.maps.compose)
    implementation(libs.compose)
    implementation(libs.compose.m3)
    implementation(libs.core)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.compose.runtime.livedata)
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation(libs.coil.compose)
    val room_version = "2.8.3"
    ksp("androidx.room:room-compiler:$room_version")

    implementation("androidx.room:room-runtime:$room_version")
    implementation("androidx.room:room-runtime:${room_version}")
    implementation("androidx.room:room-ktx:${room_version}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.10.2")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    // Material Icons (for Icons.Default.*)
    implementation("androidx.compose.material:material-icons-extended")

    // Vico charts (2.x APIs used in StatisticsScreen)
    implementation("com.patrykandpatrick.vico:compose-m3:2.1.3")
    implementation("com.patrykandpatrick.vico:compose:2.1.3")
    implementation("com.patrykandpatrick.vico:core:2.1.3")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
