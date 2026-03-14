import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    // KSP 2.3+ requires AGP 9.0+. Uncomment when Android Studio supports AGP 9.
    // alias(libs.plugins.ksp)
    alias(libs.plugins.google.gms.google.services)
}

// Read secrets from local.properties (gitignored)
val localProps = Properties().also { props ->
    val file = rootProject.file("local.properties")
    if (file.exists()) file.reader().use { props.load(it) }
}

android {
    namespace = "com.at.recallly"
    compileSdk {
        version = release(36)
    }

    ndkVersion = "26.3.11579264"

    defaultConfig {
        applicationId = "com.at.recallly"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "WEB_CLIENT_ID", "\"${localProps.getProperty("WEB_CLIENT_ID", "")}\"")
        buildConfigField("String", "GEMINI_API_KEY", "\"${localProps.getProperty("GEMINI_API_KEY", "")}\"")
        buildConfigField("String", "ADMOB_REWARDED_PRE_RECORD_ID", "\"${localProps.getProperty("ADMOB_REWARDED_PRE_RECORD_ID", "ca-app-pub-3940256099942544/5224354917")}\"")
        buildConfigField("String", "ADMOB_REWARDED_POST_SAVE_ID", "\"${localProps.getProperty("ADMOB_REWARDED_POST_SAVE_ID", "ca-app-pub-3940256099942544/5224354917")}\"")
        buildConfigField("String", "BILLING_SUBSCRIPTION_ID", "\"${localProps.getProperty("BILLING_SUBSCRIPTION_ID", "recallly_premium_monthly")}\"")

        manifestPlaceholders["ADMOB_APP_ID"] = localProps.getProperty("ADMOB_APP_ID", "ca-app-pub-3940256099942544~3347511713")

        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }
        externalNativeBuild {
            cmake {
                arguments += listOf(
                    "-DWHISPER_BUILD_TESTS=OFF",
                    "-DWHISPER_BUILD_EXAMPLES=OFF",
                    "-DGGML_OPENMP=OFF"
                )
            }
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
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
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/INDEX.LIST"
            excludes += "/META-INF/DEPENDENCIES"
        }
    }
}

// KSP config — uncomment when KSP plugin is enabled (requires AGP 9.0+)
// ksp {
//     arg("room.schemaLocation", "$projectDir/schemas")
// }

dependencies {
    // AndroidX Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)

    // Lifecycle
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // Compose
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation("androidx.compose.material:material-icons-extended")

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Room (runtime only — KSP compiler requires AGP 9.0+, uncomment when available)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    // ksp(libs.androidx.room.compiler)

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // WorkManager
    implementation(libs.androidx.work.runtime.ktx)

    // Splash Screen
    implementation(libs.androidx.splashscreen)

    // Koin
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Serialization
    implementation(libs.kotlinx.serialization.json)

    // Firebase
    implementation(libs.firebase.auth)

    // Credentials / Google Sign-In
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(libs.play.services.auth)

    // Google API Client + Calendar
    implementation(libs.google.api.client.android) {
        exclude(group = "org.apache.httpcomponents")
    }
    implementation(libs.google.api.services.calendar) {
        exclude(group = "org.apache.httpcomponents")
    }

    // Play Billing
    implementation(libs.play.billing)
    implementation(libs.play.billing.ktx)

    // Timber
    implementation(libs.timber)

    // Google Mobile Ads
    implementation(libs.google.ads)

    // Gemini AI
    implementation(libs.google.generativeai)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
