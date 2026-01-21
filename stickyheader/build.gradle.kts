plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.gouqinglin.stickyheader.lib"

    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        minSdk = 24
        consumerProguardFiles("consumer-rules.pro")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    api(libs.androidx.core.ktx)

    // Material Design组件，用于AppBarLayout
    api("com.google.android.material:material:1.12.0")

    // For nested scrolling interop helpers.
    api("androidx.customview:customview:1.2.0")

    // Common nested scrolling child.
    api("androidx.recyclerview:recyclerview:1.4.0")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
