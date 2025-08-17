package me.shadykhalifa.whispertop.domain.models

/**
 * Enum representing different logging levels with their numeric priority.
 * Higher numbers indicate higher priority/severity.
 */
enum class LogLevel(val priority: Int, val displayName: String) {
    VERBOSE(1, "Verbose"),
    DEBUG(2, "Debug"),
    INFO(3, "Info"),
    WARN(4, "Warning"),
    ERROR(5, "Error"),
    CRITICAL(6, "Critical");

    /**
     * Check if this log level should be logged based on the minimum level
     */
    fun shouldLog(minimumLevel: LogLevel): Boolean {
        return this.priority >= minimumLevel.priority
    }

    companion object {
        /**
         * Get LogLevel from string name (case-insensitive)
         */
        fun fromString(name: String): LogLevel? {
            return entries.find { it.name.equals(name, ignoreCase = true) }
        }

        /**
         * Get LogLevel from priority value
         */
        fun fromPriority(priority: Int): LogLevel? {
            return entries.find { it.priority == priority }
        }

        /**
         * Get default minimum log level for debug builds
         */
        fun getDebugDefault(): LogLevel = DEBUG

        /**
         * Get default minimum log level for release builds
         */
        fun getReleaseDefault(): LogLevel = WARN
    }
}