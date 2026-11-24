plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

android {
    namespace = "de.haushaltsbuch"
    compileSdk = 35
    defaultConfig {
        applicationId = "de.haushaltsbuch"
        minSdk = 26
        targetSdk = 35
        versionCode = 3
        versionName = "0.3 Pegel"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    // Feste Debug-Signatur: Updates lassen sich über die installierte App installieren, Daten bleiben erhalten
    signingConfigs {
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }
    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    testOptions { unitTests.isReturnDefaultValues = true }
}

ksp { arg("room.schemaLocation", "$projectDir/schemas") }

dependencies {
    val room = "2.6.1"
    implementation(platform("androidx.compose:compose-bom:2024.10.01"))
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.navigation:navigation-compose:2.8.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")

    implementation("androidx.room:room-runtime:$room")
    implementation("androidx.room:room-ktx:$room")
    ksp("androidx.room:room-compiler:$room")

    implementation("com.google.dagger:hilt-android:2.52")
    ksp("com.google.dagger:hilt-compiler:2.52")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
    implementation("androidx.hilt:hilt-work:1.2.0")
    ksp("androidx.hilt:hilt-compiler:1.2.0")

    implementation("androidx.work:work-runtime-ktx:2.9.1")
    implementation("com.patrykandpatrick.vico:compose-m3:2.0.0")

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.fragment:fragment-ktx:1.8.5")
    implementation("androidx.datastore:datastore-preferences:1.1.1")   // Einstellungen
    implementation("androidx.biometric:biometric:1.1.0")                 // App-Sperre
    implementation("androidx.glance:glance-appwidget:1.1.1")             // Widget
    implementation("io.coil-kt:coil-compose:2.7.0")                      // Beleg-Vorschau
    implementation("com.google.mlkit:text-recognition:16.0.1")           // Beleg-Texterkennung (offline)

    testImplementation("junit:junit:4.13.2")
}
