plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.hotel_booking_app"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.example.hotel_booking_app"
        minSdk = 35
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
    buildFeatures {
        buildConfig = true
    }

    val supabaseUrl = providers.gradleProperty("SUPABASE_URL")
        .orElse("https://sofhbcvvnkoexbyopeai.supabase.co")
        .get()
    val supabaseAnonKey = providers.gradleProperty("SUPABASE_ANON_KEY")
        .orElse("PASTE_YOUR_SUPABASE_ANON_KEY_HERE")
        .get()
    val geminiApiKey = providers.gradleProperty("GEMINI_API_KEY")
        .orElse("")
        .get()
    val geminiModel = providers.gradleProperty("GEMINI_MODEL")
        .orElse("gemini-3.1-flash-lite")
        .get()

    defaultConfig {
        buildConfigField("String", "SUPABASE_URL", "\"$supabaseUrl\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"$supabaseAnonKey\"")
        buildConfigField("String", "GEMINI_API_KEY", "\"$geminiApiKey\"")
        buildConfigField("String", "GEMINI_MODEL", "\"$geminiModel\"")
    }
}

dependencies {
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.glide)
    implementation(libs.gson)
    implementation(libs.jbcrypt)
    implementation(libs.material)
    implementation(libs.okhttp)
    implementation(libs.recyclerview)
    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}
