plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.example.campuslostfound"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.example.campuslostfound"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner =
            "androidx.test.runner.AndroidJUnitRunner"

        // Supabase credentials — fill them in gradle.properties (see SUPABASE_SETUP.md)
        buildConfigField(
            "String",
            "SUPABASE_URL",
            "\"${providers.gradleProperty("supabase.url").getOrElse("https://YOUR-PROJECT-REF.supabase.co")}\""
        )
        buildConfigField(
            "String",
            "SUPABASE_ANON_KEY",
            "\"${providers.gradleProperty("supabase.anonKey").getOrElse("YOUR-ANON-KEY")}\""
        )

        // Google Maps key (AndroidManifest meta-data) — set maps.apiKey in
        // gradle.properties so the key never has to be hardcoded in the manifest.
        manifestPlaceholders["MAPS_API_KEY"] =
            providers.gradleProperty("maps.apiKey").getOrElse("YOUR_API_KEY_HERE")

        // Web application OAuth client ID used by "Continue with Google"
        // (Credential Manager → Supabase ID-token exchange). See the
        // "Google sign-in" section of SUPABASE_SETUP.md.
        buildConfigField(
            "String",
            "GOOGLE_WEB_CLIENT_ID",
            "\"${providers.gradleProperty("google.webClientId").getOrElse("")}\""
        )
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

    buildFeatures {
        compose = true
        buildConfig = true
    }

    // Dependency versions are deliberately pinned (gradle/libs.versions.toml),
    // so the three "a newer version exists" checks can only ever be cleared by
    // a deliberate upgrade: Kotlin 2.2 -> 2.4, AGP 9.3 -> 9.4, OkHttp 4 -> 5,
    // Supabase 3.2 -> 3.8, ktor 3.3 -> 3.6 and maps-compose 4 -> 9 are each a
    // migration in its own right, not a fix, and would have to be re-verified
    // against the live backend (login, realtime, chat, photo upload).
    //
    // Every other lint finding is treated as a real defect and fixed in
    // source — nothing here is baselined or suppressed to hide a problem.
    lint {
        disable += "NewerVersionAvailable"
        disable += "GradleDependency"
        disable += "AndroidGradlePluginVersion"
    }
}

dependencies {

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    // Google Maps
    implementation(libs.maps.compose)
    implementation(libs.play.services.maps)

    // Image loading
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.okhttp)

    // Supabase (Auth + Postgres + Realtime + Storage)
    implementation(platform(libs.supabase.bom))
    implementation(libs.supabase.auth)
    implementation(libs.supabase.postgrest)
    implementation(libs.supabase.realtime)
    implementation(libs.supabase.storage)
    // Supabase's Ktor client needs an engine at runtime (Android has none
    // bundled) — OkHttp, which the app already uses for image loading.
    implementation(libs.ktor.client.okhttp)

    // "Continue with Google" — Credential Manager account picker (the token it
    // returns is exchanged for a Supabase session in AuthViewModel).
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
