plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")
}

secrets {
    // Optionally configure the plugin
    defaultPropertiesFileName = "local.properties"
}

android {
    namespace = "com.example.dinespot"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.dinespot"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        getByName("debug") {
            buildConfigField("String", "RAZORPAY_KEY_ID", "\"rzp_test_THNrx21lyjVEE6\"")
        }
        getByName("release") {
            buildConfigField("String", "RAZORPAY_KEY_ID", "\"rzp_test_THNrx21lyjVEE6\"")
        }
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
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.swiperefreshlayout)
    
    // Firebase dependencies using version catalog
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.database)
    implementation(libs.play.services.location)
    implementation(libs.playServicesAuthLib)
    implementation(libs.gmsBase)
    
    // Other Firebase / Google services
    implementation(libs.firebase.storage)
    implementation(libs.play.services.maps)

    // Image loading
    implementation(libs.glide)
    annotationProcessor(libs.compiler)
    implementation(libs.picasso)

    // Payment
    implementation(libs.checkout)

    // Charts
    implementation(libs.mpandroidchart)
    implementation(libs.shimmer)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
