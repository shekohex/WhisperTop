package me.shadykhalifa.whispertop.data.services

actual fun getDebugBuildFlag(): Boolean {
    return try {
        // Try to access BuildConfig through reflection to avoid dependency issues
        val buildConfigClass = Class.forName("me.shadykhalifa.whispertop.BuildConfig")
        val debugField = buildConfigClass.getField("DEBUG")
        debugField.getBoolean(null)
    } catch (e: Exception) {
        // Fallback: check system properties or default to false
        System.getProperty("debug")?.toBoolean() ?: false
    }
}