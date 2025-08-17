# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# ==============================
# WhisperTop App ProGuard Rules
# ==============================

# Keep line numbers and source file names for debugging crashes
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep runtime annotations for reflection and serialization
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault

# ==============================
# Kotlinx Serialization Rules
# ==============================
# Note: kotlinx.serialization includes automatic ProGuard rules since v1.5.0
# These rules provide additional safety for edge cases

# Keep @Serializable classes
-keep @kotlinx.serialization.Serializable class ** { *; }

# Keep `Companion` object fields of serializable classes.
# This avoids serializer lookup through `getDeclaredClasses` as done for named companion objects.
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
   static <1>$Companion Companion;
}

# Keep `serializer()` on companion objects (both default and named) of serializable classes.
-if @kotlinx.serialization.Serializable class ** {
   static **$* *;
}
-keepclassmembers class <2>$<3> {
   kotlinx.serialization.KSerializer serializer(...);
}

# Keep `INSTANCE.serializer()` of serializable objects.
-if @kotlinx.serialization.Serializable class ** {
   public static ** INSTANCE;
}
-keepclassmembers class <1> {
   public static <1> INSTANCE;
   kotlinx.serialization.KSerializer serializer(...);
}

# Keep all serializer classes
-keep class **$$serializer { *; }

# Keep classes with @SerialName annotation field names
-keep class * {
 @kotlinx.serialization.SerialName <fields>;
}

# ==============================
# Ktor Client Rules
# ==============================

# Keep Ktor client core classes
-keep class io.ktor.client.** { *; }
-keep class io.ktor.client.engine.** { *; }
-keep class io.ktor.client.plugins.** { *; }

# Keep OkHttp engine classes (used by Ktor on Android)
-keep class io.ktor.client.engine.okhttp.** { *; }
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

# Keep Ktor serialization classes
-keep class io.ktor.serialization.** { *; }
-keep class io.ktor.serialization.kotlinx.** { *; }

# Keep content negotiation classes
-keep class io.ktor.client.plugins.contentnegotiation.** { *; }

# Keep logging classes
-keep class io.ktor.client.plugins.logging.** { *; }

# ==============================
# Koin Dependency Injection Rules
# ==============================

# Keep Koin core classes
-keep class org.koin.** { *; }
-keep class io.insert-koin.** { *; }

# Keep module classes and DSL functions
-keep class **.*Module* { *; }
-keep class **.*ModuleKt { *; }

# Keep classes annotated with Koin annotations
-keep @org.koin.core.annotation.** class * { *; }

# Keep Koin Android classes
-keep class org.koin.android.** { *; }
-keep class org.koin.androidx.** { *; }

# Keep Koin Compose classes
-keep class org.koin.compose.** { *; }

# ==============================
# AndroidX Security Crypto Rules
# ==============================

# Keep AndroidX Security classes
-keep class androidx.security.crypto.** { *; }

# Keep EncryptedSharedPreferences classes
-keep class androidx.security.crypto.EncryptedSharedPreferences { *; }
-keep class androidx.security.crypto.MasterKey { *; }

# Keep Tink crypto classes used by AndroidX Security
-keep class com.google.crypto.tink.** { *; }
-dontwarn com.google.crypto.tink.**

# ==============================
# Compose UI Rules
# ==============================

# Keep Compose runtime classes
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.ui.** { *; }
-keep class androidx.compose.foundation.** { *; }
-keep class androidx.compose.material3.** { *; }

# Keep @Composable functions
-keep @androidx.compose.runtime.Composable class * { *; }

# Keep Compose resources classes
-keep class org.jetbrains.compose.resources.** { *; }

# ==============================
# AndroidX Lifecycle Rules
# ==============================

# Keep ViewModel classes
-keep class androidx.lifecycle.ViewModel { *; }
-keep class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

# Keep lifecycle classes
-keep class androidx.lifecycle.** { *; }

# ==============================
# AndroidX Navigation Rules
# ==============================

# Keep Navigation Compose classes
-keep class androidx.navigation.compose.** { *; }
-keep class androidx.navigation.** { *; }

# ==============================
# AndroidX Work Rules
# ==============================

# Keep Work Manager classes
-keep class androidx.work.** { *; }
-keep class * extends androidx.work.Worker {
    <init>(android.content.Context, androidx.work.WorkerParameters);
}

# ==============================
# App-specific Service Rules
# ==============================

# Keep all Android Services
-keep public class * extends android.app.Service {
    public <init>(...);
}

# Keep WhisperTop services
-keep class me.shadykhalifa.whispertop.service.** { *; }

# Keep accessibility service
-keep class * extends android.accessibilityservice.AccessibilityService {
    <init>(...);
}

# Keep foreground service
-keep class * extends android.app.Service {
    <init>(...);
}

# ==============================
# Domain Model Rules
# ==============================

# Keep all domain models (for API serialization safety)
-keep class me.shadykhalifa.whispertop.domain.models.** { *; }
-keep class me.shadykhalifa.whispertop.data.models.** { *; }

# ==============================
# Coroutines Rules
# ==============================

# Keep coroutines classes
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# ServiceLoader support for coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# ==============================
# Reflection and Annotations
# ==============================

# Keep all annotations
-keepattributes *Annotation*

# Keep inner classes
-keepattributes InnerClasses

# Keep generic signature for reflection
-keepattributes Signature

# ==============================
# Optimization Settings
# ==============================

# Enable aggressive optimization
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5
-allowaccessmodification

# Remove logging in release builds
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}

# Remove Kotlin logging
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    static void checkParameterIsNotNull(java.lang.Object, java.lang.String);
}

# ==============================
# Error Prevention Rules
# ==============================

# Don't warn about missing classes that are not used
-dontwarn javax.annotation.**
-dontwarn javax.inject.**
-dontwarn sun.misc.Unsafe

# Ktor-specific dontwarn rules (Java management classes not available on Android)
-dontwarn java.lang.management.ManagementFactory
-dontwarn java.lang.management.RuntimeMXBean

# Keep crash reporting information
-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception

# Keep enum classes
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}