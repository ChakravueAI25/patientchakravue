import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.googleGmsGoogleServices)
}

kotlin {
    applyDefaultHierarchyTemplate()

    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }
    
    sourceSets {

        androidMain.dependencies {
            // Only include preview in debug builds
            implementation(libs.androidx.activity.compose)
            implementation(libs.ktor.client.okhttp) // Android Engine
            implementation(libs.kotlinx.coroutines.android)
            implementation("com.google.firebase:firebase-messaging-ktx:24.1.2")
            // Agora Video SDK - using voice-only SDK for smaller size if video not needed
            // For video calls, use full-sdk but exclude unused extensions via jniLibs excludes
            implementation("io.agora.rtc:full-sdk:4.5.0")
            implementation("androidx.fragment:fragment-ktx:1.8.5")
            implementation("androidx.core:core-splashscreen:1.0.1")
            // Material Icons Core for VideoCallScreen (standard icons only, not extended)
            implementation("androidx.compose.material:material-icons-core:1.7.6")
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin) // iOS Engine
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            // Use specific icons instead of materialIconsExtended to reduce APK size
            // materialIconsExtended adds ~20MB to the APK
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
            implementation(libs.kotlinx.datetime)

            // Networking
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.peekaboo.image.picker)

            // Storage
            implementation(libs.multiplatform.settings)
            implementation(libs.multiplatform.settings.no.arg)

            // Images
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

// Remove ktor logging in release builds
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        freeCompilerArgs.add("-opt-in=kotlin.RequiresOptIn")
    }
}

android {
    namespace = "com.org.patientchakravue"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.org.patientchakravue"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"

        // Limit to required ABIs only (reduces native library size significantly)
        ndk {
            abiFilters.addAll(listOf("arm64-v8a", "armeabi-v7a"))
        }
    }

    // Enable resource optimization - limit to supported languages
    androidResources {
        localeFilters += listOf("en", "hi", "ta")
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/NOTICE.md"
            excludes += "/META-INF/LICENSE.md"
            excludes += "/META-INF/INDEX.LIST"
            excludes += "/META-INF/*.kotlin_module"
            excludes += "/META-INF/versions/**"
            excludes += "/kotlin/**"
            excludes += "/*.txt"
            excludes += "/*.properties"
            excludes += "/DebugProbesKt.bin"
        }
        // Remove unnecessary native libraries (Agora extensions)
        jniLibs {
            excludes += "**/libagora_video_av1_decoder_extension.so"
            excludes += "**/libagora_video_av1_encoder_extension.so"
            excludes += "**/libagora_face_capture_extension.so"
            excludes += "**/libagora_face_detection_extension.so"
            excludes += "**/libagora_lip_sync_extension.so"
            excludes += "**/libagora_video_quality_analyzer_extension.so"
            excludes += "**/libagora_spatial_audio_extension.so"
            excludes += "**/libagora_ai_echo_cancellation_extension.so"
            excludes += "**/libagora_ai_noise_suppression_extension.so"
            excludes += "**/libagora_content_inspect_extension.so"
            excludes += "**/libagora_screen_capture_extension.so"
            excludes += "**/libagora_segmentation_extension.so"
            excludes += "**/libagora_super_resolution_extension.so"
            excludes += "**/libagora_clear_vision_extension.so"
            excludes += "**/libagora_drm_loader_extension.so"
            excludes += "**/libagora_pvc_extension.so"
            excludes += "**/libagora_video_decoder_extension.so"
            excludes += "**/libagora_video_encoder_extension.so"
            excludes += "**/libagora_udrm3_extension.so"
            excludes += "**/libagora_dav1d_extension.so"
            excludes += "**/libagora_jnd_extension.so"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        getByName("debug") {
            isMinifyEnabled = false
            isShrinkResources = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    // Enable build features optimization
    buildFeatures {
        buildConfig = false
        resValues = false
    }

    // Lint optimization
    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }
}

dependencies {
    implementation(libs.firebase.messaging)
    debugImplementation(compose.uiTooling)
}

