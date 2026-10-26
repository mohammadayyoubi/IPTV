import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

// ── Package id ──────────────────────────────────────────────────────────────
// Google Play rejects "com.example.*". The app ships as lb.techwrap.iptv once Firebase knows
// that id: add an Android app with it in the Firebase console and replace
// app/google-services.json. Until then the old id is used, so the build keeps working.
val releaseApplicationId = "lb.techwrap.iptv"
val firebaseConfig = file("google-services.json")
val firebaseHasReleaseId = firebaseConfig.exists() &&
        firebaseConfig.readText().contains("\"$releaseApplicationId\"")
if (!firebaseHasReleaseId) {
    logger.warn("google-services.json has no client for $releaseApplicationId: building with the old id com.example.iptv")
}

// ── Release signing ─────────────────────────────────────────────────────────
// keystore.properties (never committed) holds: storeFile, storePassword, keyAlias, keyPassword
val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) keystorePropsFile.inputStream().use { load(it) }
}

android {
    namespace = "com.example.iptv"
    compileSdk = 35

    defaultConfig {
        applicationId = if (firebaseHasReleaseId) releaseApplicationId else "com.example.iptv"
        minSdk = 24
        targetSdk = 35
        versionCode = 2
        versionName = "2.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (keystorePropsFile.exists()) {
            create("release") {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            // R8: removes unused code/resources and obfuscates (smaller, harder to tamper with)
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (keystorePropsFile.exists()) signingConfig = signingConfigs.getByName("release")
        }
    }
    testOptions {
        // android.util.Log etc. return defaults in JVM unit tests (ProbeRunnerTest)
        unitTests.isReturnDefaultValues = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    androidResources {
        // In-app language switch (English / Arabic) on Android 13+
        generateLocaleConfig = true
    }
}

dependencies {
    implementation(platform("com.google.firebase:firebase-bom:33.13.0"))
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation("com.google.firebase:firebase-analytics")
    implementation(libs.constraintlayout)
    implementation(libs.firebase.auth)

    // Media3 (ExoPlayer): all modules on the same version
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.exoplayer.hls)
    implementation(libs.media3.ui)
    implementation(libs.media3.common)
    // FFmpeg audio decoders (Jellyfin's prebuilt Media3 extension, built for Media3 1.8.0):
    // plays audio formats many phones can't decode, e.g. MP2 used by Al Jadeed, AC-3, E-AC-3
    implementation("org.jellyfin.media3:media3-ffmpeg-decoder:1.8.0+1")

    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    // Use Firestore once
    implementation("com.google.firebase:firebase-firestore:24.9.1")

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
