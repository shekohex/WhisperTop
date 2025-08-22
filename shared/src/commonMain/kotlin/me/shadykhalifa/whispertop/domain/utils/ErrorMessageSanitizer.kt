package me.shadykhalifa.whispertop.domain.utils

object ErrorMessageSanitizer {
    
    fun sanitizeServiceError(throwable: Throwable): String {
        return when {
            throwable.message?.contains("permission", ignoreCase = true) == true -> 
                "Service requires additional permissions"
            throwable.message?.contains("bind", ignoreCase = true) == true -> 
                "Unable to connect to service"
            throwable.message?.contains("security", ignoreCase = true) == true -> 
                "Security restriction prevents service access"
            else -> "Service initialization failed"
        }
    }
    
    fun sanitizePermissionError(throwable: Throwable): String {
        return when {
            throwable.message?.contains("denied", ignoreCase = true) == true -> 
                "Permission request was denied"
            throwable.message?.contains("security", ignoreCase = true) == true -> 
                "Permission security check failed"
            else -> "Permission management failed"
        }
    }
    
    fun sanitizeGeneralError(throwable: Throwable): String {
        return "An unexpected error occurred"
    }
}