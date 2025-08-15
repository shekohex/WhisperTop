package me.shadykhalifa.whispertop.data.repositories

import me.shadykhalifa.whispertop.utils.Result
import me.shadykhalifa.whispertop.utils.safeCall

abstract class BaseRepository {
    
    protected suspend fun <T> execute(action: suspend () -> T): Result<T> = safeCall(action)
    
    protected fun handleException(exception: Throwable): Throwable {
        return when (exception) {
            is IllegalArgumentException -> exception
            is IllegalStateException -> exception
            else -> RuntimeException("Repository operation failed", exception)
        }
    }
}