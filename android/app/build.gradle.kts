import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val defaultBuildNumber = rootProject.file("build-number").readText().trim()
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.isFile) file.inputStream().use(::load)
}
val openRouterApiKey = localProperties.getProperty("OPENROUTER_API_KEY", "")
    .replace("\\", "\\\\")
    .replace("\"", "\\\"")
val buildNumber = providers.gradleProperty("buildNumber")
    .orElse(defaultBuildNumber)
    .map { it.toIntOrNull() ?: error("buildNumber must be an integer") }
    .get()
require(buildNumber > 0) { "buildNumber must be greater than zero" }

android {
    namespace = "com.smolcase.companion"
    compileSdk = 34

    buildFeatures {
        buildConfig = true
        compose = true
    }

    defaultConfig {
        applicationId = "com.smolcase.companion"
        minSdk = 26
        targetSdk = 34
        // The sideload script supplies the monotonically increasing build number.
        versionCode = buildNumber
        versionName = "0.9-gpu-face.$buildNumber"

        // The key is loaded only from ignored android/local.properties. It is
        // still present in the APK, so rotate it if the APK is distributed.
        buildConfigField("String", "OPENROUTER_API_KEY", "\"$openRouterApiKey\"")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-ktx:1.9.3")
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.3")

    // CameraX — analysis only, no preview (the screen is the face)
    implementation("androidx.camera:camera-core:1.3.4")
    implementation("androidx.camera:camera-camera2:1.3.4")
    implementation("androidx.camera:camera-lifecycle:1.3.4")

    // ML Kit face detection (on-device, no network)
    implementation("com.google.mlkit:face-detection:16.1.7")

    // Gemini Nano via ML Kit GenAI Prompt API (on-device, AICore)
    implementation("com.google.mlkit:genai-prompt:1.0.0-beta4")

    // Gemma 4 E2B via LiteRT-LM (on-device, sideloaded .litertlm — no AICore).
    // 0.10.2 lacks DYNAMIC_UPDATE_SLICE op needed by Gemma 4 E2B model.
    // 0.16.0 previously livelocked in Engine.initialize() on Tensor G3 but
    // works with current model export. Revert to 0.10.2 if livelock returns.
    implementation("com.google.ai.edge.litertlm:litertlm-android:0.16.0")

    // Coroutines for LLM calls
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")

    // Unit tests (pure-Kotlin face math runs on the JVM)
    testImplementation("junit:junit:4.13.2")
    // Real org.json for unit tests (Android SDK stubs throw "not mocked")
    testImplementation("org.json:json:20231013")
}
