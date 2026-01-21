plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.gouqinglin.stickyheader"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    // Only configure custom signing if keystore exists (for local builds)
    val keystoreFile = file("./common.keystore")
    if (keystoreFile.exists()) {
        signingConfigs {
            getByName("debug") {
                storeFile = keystoreFile
                storePassword = "000000"
                keyAlias = "000000"
                keyPassword = "000000"
            }
        }
    }

    defaultConfig {
        applicationId = "com.gouqinglin.stickyheader"
        minSdk = 24
        targetSdk = 36
        versionCode = 101
        versionName = "1.0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        multiDexEnabled = true

        // Only use custom signing config if keystore exists
        if (keystoreFile.exists()) {
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    implementation(project(":stickyheader"))
    // implementation("com.github.SherlockGougou:StickyHeaders:1.0.0")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}