
import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.gms.google-services")
}

repositories {
    google()
    mavenCentral()
    maven {
        url = uri(rootProject.rootDir.resolve("repo"))
    }
}

val keystorePropsFile = rootProject.file("release.properties")
val keystoreProps = Properties()

if (keystorePropsFile.exists()) {
    keystoreProps.load(FileInputStream(keystorePropsFile))
}

val hasValidSigningProps = keystorePropsFile.exists().also { exists ->
    if (exists) {
        FileInputStream(keystorePropsFile).use { keystoreProps.load(it) }
    }
}.let {
    listOf("storeFile", "storePassword", 
            "keyAlias", "keyPassword").all { key ->
        keystoreProps[key] != null
    }
}

android {
    namespace = "com.beouttv.app"
    compileSdk = 35 
    
    lint {
        checkReleaseBuilds = false
    }
        
    signingConfigs {
        if (hasValidSigningProps) {
            create("release") {
                storeFile = rootProject.file(keystoreProps["storeFile"] as String)
                storePassword = keystoreProps["storePassword"] as String
                keyAlias = keystoreProps["keyAlias"] as String
                keyPassword = keystoreProps["keyPassword"] as String
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
                enableV4Signing = true
            }
        }
    }

    defaultConfig {
        applicationId = "com.beouttv.app"
        minSdk = 21 
        targetSdk = 34  
        versionCode = 3
        versionName = "3.0"

        vectorDrawables { 
            useSupportLibrary = true
        }

        // ── Native C/C++ Multi-ABI Configuration ───────────────────────────
        // Prebuilt native libraries for all 4 ABIs reside in src/main/jniLibs:
        // - armeabi-v7a: libbeoutsec.so (32-bit ARM), libc++_shared.so (32-bit ARM)
        // - arm64-v8a:   libbeoutsec.so (64-bit ARM), libc++_shared.so (64-bit ARM), libnext.so (64-bit ARM)
        // - x86:         libbeoutsec.so (32-bit x86), libc++_shared.so (32-bit x86)
        // - x86_64:      libbeoutsec.so (64-bit x86), libc++_shared.so (64-bit x86)
        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
        }
    }

    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17 
        targetCompatibility = JavaVersion.VERSION_17 
    }

    buildTypes {
        release {
            if (hasValidSigningProps) {
                signingConfig = signingConfigs.getByName("release")
            }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            if (hasValidSigningProps) {
                signingConfig = signingConfigs.getByName("release")
            }
            isMinifyEnabled = false
            isShrinkResources = false
        }
    }

    buildFeatures {
        viewBinding = true
        dataBinding = false
        compose = true
    }
    packaging {
        jniLibs {
            useLegacyPackaging = true
            keepDebugSymbols.clear()
            // NOTE: Do NOT add excludes.add("lib/**") here.
            // That line was previously excluding ALL native libraries from the APK,
            // causing INSTALL_FAILED_NO_MATCHING_ABIS on 32-bit devices and
            // UnsatisfiedLinkError at runtime on all devices.
        }
        resources {
            resources.excludes.add("/META-INF/{AL2.0,LGPL2.1}")
            resources.excludes.add("META-INF/kotlinx_coroutines_core.version")
            resources.excludes.add("META-INF/DEPENDENCIES")
            resources.excludes.add("META-INF/LICENSE*")
            resources.excludes.add("META-INF/NOTICE*")
            resources.excludes.add("META-INF/*.version")

            resources.pickFirsts.add("nonJvmMain/default/linkdata/package_androidx/0_androidx.knm")
            resources.pickFirsts.add("nonJvmMain/default/linkdata/root_package/0_.knm")
            resources.pickFirsts.add("nonJvmMain/default/linkdata/module")

            resources.pickFirsts.add("nativeMain/default/linkdata/root_package/0_.knm")
            resources.pickFirsts.add("nativeMain/default/linkdata/module")

            resources.pickFirsts.add("commonMain/default/linkdata/root_package/0_.knm")
            resources.pickFirsts.add("commonMain/default/linkdata/module")
            resources.pickFirsts.add("commonMain/default/linkdata/package_androidx/0_androidx.knm")

            resources.pickFirsts.add("META-INF/kotlin-project-structure-metadata.json")

            resources.merges.add("commonMain/default/manifest")
            resources.merges.add("nonJvmMain/default/manifest")
            resources.merges.add("nativeMain/default/manifest")
        }
    }
    
    configurations.all {
        resolutionStrategy {
            force("org.jetbrains.kotlin:kotlin-stdlib:2.0.21")
            force("org.jetbrains.kotlin:kotlin-stdlib-jdk7:2.0.21")
            force("org.jetbrains.kotlin:kotlin-stdlib-jdk8:2.0.21")
            force("androidx.collection:collection:1.4.2")
            force("androidx.annotation:annotation:1.8.1")
        }
    }
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("-Xlint:deprecation")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")

    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.7.7")

    implementation("androidx.media3:media3-exoplayer:1.4.0")
    implementation("androidx.media3:media3-exoplayer-hls:1.4.0")
    implementation("androidx.media3:media3-exoplayer-dash:1.4.0")
    implementation("androidx.media3:media3-datasource:1.4.0")
    implementation("androidx.media3:media3-database:1.4.0")
    implementation("androidx.media3:media3-ui:1.4.0")
    implementation("androidx.media3:media3-common:1.4.0")

    implementation("io.coil-kt:coil-compose:2.6.0")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.code.gson:gson:2.11.0")

    // Firebase Suite
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage")
    implementation("com.google.firebase:firebase-database")
    implementation("com.google.firebase:firebase-messaging")
    implementation("com.google.firebase:firebase-config")

    debugImplementation("androidx.compose.ui:ui-tooling")
}

// Automatic build pipeline: Directly exports the signed universal APK to Downloads upon every build or run
tasks.matching { it.name in listOf("assembleRelease", "assembleDebug", "assemble") }.configureEach {
    doLast {
        try {
            val downloadDir = file("/storage/emulated/0/Download")
            if (downloadDir.exists()) {
                val releaseApk = layout.buildDirectory.file("outputs/apk/release/app-release.apk").get().asFile
                val debugApk = layout.buildDirectory.file("outputs/apk/debug/app-debug.apk").get().asFile
                val src = when {
                    releaseApk.exists() && releaseApk.length() > 0 -> releaseApk
                    debugApk.exists() && debugApk.length() > 0 -> debugApk
                    else -> null
                }
                src?.let {
                    it.copyTo(downloadDir.resolve("BeoutTV-Universal-v3.0.apk"), overwrite = true)
                    it.copyTo(downloadDir.resolve("BeoutTV-Universal-v3.0 الاصدار الثالث للهواتف والشاشات.apk"), overwrite = true)
                }
            }
        } catch (_: Exception) {}
    }
}



