plugins {
    id("com.android.application")
}

android {
    namespace = "com.example.iptv"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.iptv"
        minSdk = 24
        targetSdk = 35
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    // ✅ Use Media3 (new ExoPlayer)
    implementation ("androidx.media3:media3-exoplayer:1.3.1")  // Latest stable version
    implementation ("androidx.media3:media3-exoplayer-hls:1.3.1")
    implementation ("androidx.media3:media3-ui:1.3.1")

    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation(libs.media3.common)
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
