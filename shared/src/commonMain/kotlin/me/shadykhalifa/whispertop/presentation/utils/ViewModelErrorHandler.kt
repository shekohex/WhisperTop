package me.shadykhalifa.whispertop.presentation.utils

import me.shadykhalifa.whispertop.domain.models.ErrorContext
import me.shadykhalifa.whispertop.domain.models.ErrorInfo
import me.shadykhalifa.whispertop.domain.models.ErrorMapper
import me.shadykhalifa.whispertop.domain.models.ErrorNotificationService

class ViewModelErrorHandler(
    private val errorMapper: ErrorMapper,
    private val errorNotificationService: ErrorNotificationService
) {
    
    fun handleError(
        error: Throwable, 
        context: String? = null,
        showNotification: Boolean = false
    ): ErrorInfo {
        val errorInfo = errorMapper.mapToErrorInfo(error, context)
        
        if (showNotification) {
            errorNotificationService.showError(error, context)
        }
        
        return errorInfo
    }
    
    fun handleErrorWithContext(
        error: Throwable,
        errorContext: ErrorContext,
        showNotification: Boolean = false
    ): ErrorInfo {
        val errorInfo = errorMapper.mapToErrorInfoWithContext(error, errorContext)
        
        if (showNotification) {
            errorNotificationService.showError(error, errorContext.operationName)
        }
        
        return errorInfo
    }
    
    fun handleErrorWithNotification(
        error: Throwable,
        context: String? = null
    ): ErrorInfo {
        return handleError(error, context, showNotification = true)
    }
}