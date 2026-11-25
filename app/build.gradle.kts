plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.multiplatform")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("org.jetbrains.compose")
}


kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "FarmMobile"
            isStatic = true
        }
    }

    applyDefaultHierarchyTemplate()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.materialIconsExtended)
                implementation(compose.ui)
                implementation(compose.animation)

                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")

                val ktorVersion = "3.0.3"
                implementation("io.ktor:ktor-client-core:$ktorVersion")
                implementation("io.ktor:ktor-client-websockets:$ktorVersion")
                implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
                implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")

                                implementation("androidx.lifecycle:lifecycle-viewmodel:2.9.4")
                implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.4")
                implementation("androidx.datastore:datastore-preferences:1.1.1")
                implementation("io.coil-kt.coil3:coil-compose:3.0.4")
            }
        }

        val androidMain by getting {
            dependencies {
                implementation("androidx.compose.ui:ui:1.7.6")
                implementation("androidx.compose.foundation:foundation:1.7.6")
                implementation("androidx.compose.material3:material3:1.3.1")
                implementation("androidx.compose.material:material-icons-extended:1.7.6")
                implementation("androidx.compose.animation:animation:1.7.6")
                implementation("androidx.activity:activity-compose:1.9.3")

                implementation("androidx.core:core-ktx:1.15.0")
                implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4")
                implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
                implementation("io.ktor:ktor-client-okhttp:3.0.3")
                implementation("io.coil-kt.coil3:coil-network-okhttp:3.0.4")
            }
        }

        val iosMain by getting {
            dependencies {
                implementation("io.ktor:ktor-client-darwin:3.0.3")
                implementation("io.coil-kt.coil3:coil-network-ktor3:3.0.4")
            }
        }
    }
}

android {
    lint {
        // AGP 8.7.3 的 lint 内置 Kotlin 编译器(2.0) 与 stdlib 2.2 元数据不兼容，打包无需 lint
        checkReleaseBuilds = false
    }
    namespace = "com.qqfarm.mobile"
    compileSdk = 35

    defaultConfig {
        applicationId = "io.github.ccpopy.farmmobile"
        minSdk = 26
        targetSdk = 35
        versionCode = 15
        versionName = "0.1.5"

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}
