import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}


val localProperties = Properties().apply {
    rootProject.file("local.properties")
        .inputStream()
        .use { load(it) }
}

val supabaseUrl =
    localProperties.getProperty("SUPABASE_URL", "")

val supabaseKey =
    localProperties.getProperty(
        "SUPABASE_PUBLISHABLE_KEY", ""
    )


android {
    namespace = "com.csiss.inventory"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.csiss.inventory"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField(
            "String",
            "https://zujynwdptdmnopfhbuxn.supabase.co",
            "\"$supabaseUrl\""
        )

        buildConfigField(
            "String",
            "sb_publishable_f3FneO3mvz9VbDCqITeJNQ_BeWP7yvh",
            "\"$supabaseKey\""
        )
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(platform(libs.supabase.bom))
    implementation(libs.supabase.auth)
    implementation(libs.supabase.postgrest)
}