import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.text.SimpleDateFormat
import java.util.Date



plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.jacoco)
    id("org.jetbrains.kotlin.plugin.parcelize")
}

// Version management functions
fun generateVersionCode(): Int {
    // Generate version code from timestamp for auto-increment
    // Format: YYMMDDXX where XX is build number of the day
    val date = SimpleDateFormat("yyMMdd").format(Date())
    val buildNumber = System.getProperty("BUILD_NUMBER") ?: "01"
    return "${date}${buildNumber.padStart(2, '0')}".toInt()
}

fun generateVersionName(): String {
    val major = 1
    val minor = 0
    val patch = 0
    val buildType = if (gradle.startParameter.taskNames.any { it.contains("Release") }) "release" else "debug"
    return "$major.$minor.$patch-$buildType"
}

fun getBuildTimestamp(): String {
    return SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date())
}

// Git commit hash provider for configuration cache compatibility
val gitCommitHashProvider = providers.exec {
    commandLine("git", "rev-parse", "--short", "HEAD")
}.standardOutput.asText.map { it.trim() }.orElse("unknown")

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    
    sourceSets {
        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.lifecycle.service)
            implementation("androidx.lifecycle:lifecycle-process:${libs.versions.androidx.lifecycle.get()}")
            implementation("androidx.lifecycle:lifecycle-common:${libs.versions.androidx.lifecycle.get()}")
            implementation(libs.androidx.navigation.compose)
            implementation(libs.androidx.security.crypto)
            implementation(libs.androidx.work.runtime.ktx)
            implementation(libs.koin.android)
            implementation(libs.koin.compose)
            implementation(libs.androidx.paging.compose)
            implementation(libs.androidx.room.runtime)
            implementation(libs.androidx.room.ktx)
            implementation(libs.vico.compose)
            implementation(libs.vico.compose.m3)
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
            implementation(projects.shared)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.koin.test)
        }
        androidUnitTest.dependencies {
            implementation(libs.junit)
            implementation(libs.kotlin.testJunit)
            implementation(libs.robolectric)
            implementation(libs.mockito.core)
            implementation(libs.mockito.kotlin)
            implementation(libs.mockk)
            implementation(libs.mockk.android)
            implementation(libs.koin.test)
            implementation(libs.turbine)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.compose.ui.test.junit4)
            implementation(libs.compose.ui.test.manifest)
            implementation(libs.androidx.work.testing)
        }
    }
}

android {
    namespace = "me.shadykhalifa.whispertop"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "me.shadykhalifa.whispertop"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = generateVersionCode()
        versionName = generateVersionName()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        // Configure vector drawables for API < 21
        vectorDrawables.useSupportLibrary = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    
    signingConfigs {
        create("release") {
            // Production signing config using environment variables
            storeFile = file(System.getenv("KEYSTORE_FILE") ?: "release.keystore")
            storePassword = System.getenv("KEYSTORE_PASSWORD") ?: "defaultPassword"
            keyAlias = System.getenv("KEY_ALIAS") ?: "defaultAlias"
            keyPassword = System.getenv("KEY_PASSWORD") ?: "defaultPassword"
            
            // Enable V1 and V2 signature schemes
            enableV1Signing = true
            enableV2Signing = true
            enableV3Signing = true
            enableV4Signing = true
        }
        
        getByName("debug") {
            // Debug signing remains unchanged
        }
    }
    
    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a", "x86_64")
            isUniversalApk = true
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            isJniDebuggable = false

            isPseudoLocalesEnabled = false
            
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            
            // Use release signing config (falls back to debug if env vars not set)
            signingConfig = if (System.getenv("KEYSTORE_FILE") != null) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
            
            // Enhanced build configuration fields
            buildConfigField("String", "BUILD_TIME", "\"${getBuildTimestamp()}\"")
            buildConfigField("String", "VERSION_TYPE", "\"release\"")
            buildConfigField("String", "GIT_COMMIT", "\"${gitCommitHashProvider.get()}\"")
            buildConfigField("long", "BUILD_TIMESTAMP", "${System.currentTimeMillis()}L")
            buildConfigField("boolean", "IS_DEBUG_BUILD", "false")
        }
        
        getByName("debug") {
            isMinifyEnabled = false
            isDebuggable = true
            isJniDebuggable = true
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            
            // Enhanced build configuration fields for debug
            buildConfigField("String", "BUILD_TIME", "\"${getBuildTimestamp()}\"")
            buildConfigField("String", "VERSION_TYPE", "\"debug\"")
            buildConfigField("String", "GIT_COMMIT", "\"${gitCommitHashProvider.get()}\"")
            buildConfigField("long", "BUILD_TIMESTAMP", "${System.currentTimeMillis()}L")
            buildConfigField("boolean", "IS_DEBUG_BUILD", "true")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    flavorDimensions += "distribution"
    
    productFlavors {
        create("playstore") {
            dimension = "distribution"
            // Play Store specific configuration
            buildConfigField("String", "DISTRIBUTION_CHANNEL", "\"playstore\"")
            buildConfigField("boolean", "ENABLE_ANALYTICS", "true")
        }
        
        create("sideload") {
            dimension = "distribution"
            applicationIdSuffix = ".sideload"
            // Sideload specific configuration
            buildConfigField("String", "DISTRIBUTION_CHANNEL", "\"sideload\"")
            buildConfigField("boolean", "ENABLE_ANALYTICS", "false")
        }
    }
    
    buildFeatures {
        compose = true
        buildConfig = true
    }
    
    testOptions {
        unitTests {
            isReturnDefaultValues = true
            isIncludeAndroidResources = true
            all {
                it.systemProperty("robolectric.enabledSdks", "29")
                it.systemProperty("robolectric.dependency.repo.url", "https://repo1.maven.org/maven2")
                it.systemProperty("robolectric.dependency.repo.id", "central")
                it.jvmArgs = listOf("-noverify")
            }
        }
    }

    configurations.all {
        resolutionStrategy {
            force("org.ow2.asm:asm:9.7")
            force("org.ow2.asm:asm-commons:9.7")  
            force("org.ow2.asm:asm-tree:9.7")
            force("org.ow2.asm:asm-analysis:9.7")
            force("org.ow2.asm:asm-util:9.7")
            // Exclude conflicting annotations
            exclude(group = "org.jetbrains", module = "annotations-java5")
        }
    }
}

dependencies {
    debugImplementation(compose.uiTooling)
    debugImplementation(libs.compose.ui.test.manifest)
    
    // LeakCanary for memory leak detection
    debugImplementation(libs.leakcanary.android)
    androidTestImplementation(libs.leakcanary.android.instrumentation)
    
    // Android Test dependencies
    androidTestImplementation(libs.androidx.testExt.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation("androidx.test:rules:1.6.1")
    androidTestImplementation(libs.mockito.android)
    androidTestImplementation(libs.mockito.kotlin)
    androidTestImplementation(libs.koin.test)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.compose.ui.test.junit4)
    androidTestImplementation(libs.mockk.android)

}

// JaCoCo Coverage Configuration
jacoco {
    toolVersion = libs.versions.jacoco.get()
}

val coverageExclusions = listOf(
    // Android generated files
    "**/R.class",
    "**/R$*.class", 
    "**/BuildConfig.*",
    "**/Manifest*.*",
    "**/*\$*.class",
    
    // Data binding
    "**/DataBinding*.*",
    "**/BR.*",
    "**/*_ViewBinding*.*",
    
    // Compose generated
    "**/*\$Companion.*",
    "**/*ComposableSingletons*.*",
    "**/*_Factory*.*",
    "**/ComposableSingletons*.*",
    
    // Kotlin generated
    "**/*\$serializer.*",
    "**/*\$WhenMappings.*",
    
    // DI/Koin generated
    "**/di/*Module*.*",
    
    // Models/DTOs (data classes)
    "**/models/**",
    "**/dto/**",
    "**/data/models/**",
    
    // Application class
    "**/WhisperTopApplication.*"
)

tasks.withType<Test> {
    configure<JacocoTaskExtension> {
        isIncludeNoLocationClasses = true
        excludes = listOf("jdk.internal.*")
    }
}

tasks.register<JacocoReport>("jacocoTestReport") {
    dependsOn("testPlaystoreDebugUnitTest", "testSideloadDebugUnitTest")
    
    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }
    
    val fileFilter = coverageExclusions
    val debugTree = fileTree("${layout.buildDirectory.get()}/tmp/kotlin-classes/playstoreDebug")
    val mainSrc = "${project.projectDir}/src/androidMain/kotlin"
    val composeSrc = "${project.projectDir}/src/commonMain/kotlin"
    
    sourceDirectories.setFrom(files(listOf(mainSrc, composeSrc)))
    classDirectories.setFrom(files(listOf(debugTree)))
    classDirectories.setFrom(files(classDirectories.files.map {
        fileTree(it).apply {
            exclude(fileFilter)
        }
    }))
    
    executionData.setFrom(fileTree(layout.buildDirectory.get()).include(
        "outputs/unit_test_code_coverage/playstoreDebugUnitTest/testPlaystoreDebugUnitTest.exec",
        "outputs/unit_test_code_coverage/sideloadDebugUnitTest/testSideloadDebugUnitTest.exec",
        "outputs/code_coverage/playstoreDebugAndroidTest/connected/*/coverage.ec",
        "outputs/code_coverage/sideloadDebugAndroidTest/connected/*/coverage.ec"
    ))
}

