package me.shadykhalifa.whispertop.domain.models

class NetworkException(message: String?, cause: Throwable? = null) : Exception(message, cause)
class TimeoutException(message: String?, cause: Throwable? = null) : Exception(message, cause)
expect class PlatformSecurityException(message: String?) : Exception