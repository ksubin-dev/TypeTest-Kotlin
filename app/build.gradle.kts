import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kover)
}

android {
    namespace = "com.bankingtest_kotlin"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.bankingtest_kotlin"
        minSdk = 24
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
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures {
        compose = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.runtime.ktx)
    implementation(libs.ads.mobile.sdk)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

kover {
    reports {
        variant("debug") {
            filters {
                excludes {
                    classes(
                        "com.bankingtest_kotlin.MainActivity",
                        "com.bankingtest_kotlin.MainActivityKt*",
                        "com.bankingtest_kotlin.ComposableSingletons*",
                        "com.bankingtest_kotlin.navigation.*",
                        "com.bankingtest_kotlin.presentation.QuizViewModel\$Companion*",
                        "com.bankingtest_kotlin.ui.*",
                        "com.bankingtest_kotlin.domain.Answer",
                        "com.bankingtest_kotlin.domain.Question",
                        "com.bankingtest_kotlin.domain.Quiz",
                        "com.bankingtest_kotlin.domain.QuizResult",
                        "com.bankingtest_kotlin.data.*Dto*",
                        "com.bankingtest_kotlin.data.AndroidDrawableResourceMapper",
                        "com.bankingtest_kotlin.data.AssetQuizRepository",
                    )
                }
            }
        }
    }
}
