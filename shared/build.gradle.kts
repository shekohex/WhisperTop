import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.androidx.room)
    alias(libs.plugins.ksp)
    alias(libs.plugins.jacoco)
    alias(libs.plugins.dokka)
}

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
            freeCompilerArgs.addAll("-Xexpect-actual-classes")
        }
    }
    
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }
    
    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
            implementation(libs.koin.core)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.client.contentNegotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.androidx.paging.common)
        }
        
        androidMain.dependencies {
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.androidx.security.crypto)
            implementation(libs.androidx.datastore.preferences)
            implementation(libs.androidx.room.runtime)
            implementation(libs.androidx.room.ktx)
            implementation(libs.androidx.room.paging)
            implementation(libs.androidx.paging.runtime)
            implementation(libs.sqlcipher.android)
            implementation(libs.androidx.sqlite)
        }
        
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.koin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
            implementation(libs.turbine)
            implementation(libs.mockk)
        }
        
        androidUnitTest.dependencies {
            implementation(libs.junit)
            implementation(libs.androidx.room.testing)
            implementation(libs.mockk)
        }
        
        androidInstrumentedTest.dependencies {
            implementation(libs.junit)
            implementation(libs.kotlin.test)
            implementation(libs.kotlin.testJunit)
            implementation(libs.androidx.test.core)
            implementation(libs.androidx.test.runner)
            implementation(libs.androidx.testExt.junit)
            implementation(libs.androidx.room.testing)
            implementation(libs.koin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.mockk.android)
        }
    }
}

android {
    namespace = "me.shadykhalifa.whispertop.shared"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
        consumerProguardFiles("consumer-rules.pro")
    }
    testOptions {
        unitTests {
            isReturnDefaultValues = true
        }
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    add("kspAndroid", libs.androidx.room.compiler)
}

// JaCoCo Coverage Configuration for Shared Module
jacoco {
    toolVersion = libs.versions.jacoco.get()
}

val sharedCoverageExclusions = listOf(
    // Kotlin generated
    "**/*\$serializer.*",
    "**/*\$WhenMappings.*",
    "**/*\$Companion.*",
    
    // Data models (focus on business logic)
    "**/models/**",
    "**/dto/**",
    "**/data/models/**",
    
    // DI modules
    "**/di/*Module*.*",
    
    // Platform-specific expect/actual implementations 
    "**/platform/**"
)

tasks.withType<Test> {
    configure<JacocoTaskExtension> {
        isIncludeNoLocationClasses = true
        excludes = listOf("jdk.internal.*")
    }
}

tasks.register<JacocoReport>("jacocoSharedTestReport") {
    dependsOn("testDebugUnitTest", "allTests")
    
    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }
    
    val fileFilter = sharedCoverageExclusions
    val debugTree = fileTree("${layout.buildDirectory.get()}/tmp/kotlin-classes/debug")
    val mainSrc = "${project.projectDir}/src/commonMain/kotlin"
    
    sourceDirectories.setFrom(files(listOf(mainSrc)))
    classDirectories.setFrom(files(listOf(debugTree)))
    classDirectories.setFrom(files(classDirectories.files.map {
        fileTree(it).apply {
            exclude(fileFilter)
        }
    }))
    
    executionData.setFrom(fileTree(layout.buildDirectory.get()).include(
        "**/*.exec"
    ))
}

// Dokka Configuration for API Documentation
tasks.withType<org.jetbrains.dokka.gradle.DokkaTask>().configureEach {
    moduleName.set("WhisperTop Shared")
    
    dokkaSourceSets {
        named("commonMain") {
            displayName.set("Common")
            includes.from("docs/api/module.md")
            
            sourceLink {
                localDirectory.set(file("src/commonMain/kotlin"))
                remoteUrl.set(uri("https://github.com/shekohex/WhisperTop/tree/main/shared/src/commonMain/kotlin").toURL())
                remoteLineSuffix.set("#L")
            }
            
            externalDocumentationLink {
                url.set(uri("https://kotlinlang.org/api/kotlinx.coroutines/").toURL())
            }
            
            externalDocumentationLink {
                url.set(uri("https://kotlinlang.org/api/kotlinx.serialization/").toURL())
            }
            
            externalDocumentationLink {
                url.set(uri("https://kotlinlang.org/api/kotlinx-datetime/").toURL())
            }
        }
        
        named("androidMain") {
            displayName.set("Android")
            
            sourceLink {
                localDirectory.set(file("src/androidMain/kotlin"))
                remoteUrl.set(uri("https://github.com/shekohex/WhisperTop/tree/main/shared/src/androidMain/kotlin").toURL())
                remoteLineSuffix.set("#L")
            }
        }
        
        named("iosMain") {
            displayName.set("iOS")
            
            sourceLink {
                localDirectory.set(file("src/iosMain/kotlin"))
                remoteUrl.set(uri("https://github.com/shekohex/WhisperTop/tree/main/shared/src/iosMain/kotlin").toURL())
                remoteLineSuffix.set("#L")
            }
        }
    }
}
