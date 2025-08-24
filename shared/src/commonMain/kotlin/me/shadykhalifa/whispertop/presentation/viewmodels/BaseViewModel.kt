package me.shadykhalifa.whispertop.presentation.viewmodels

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import me.shadykhalifa.whispertop.domain.models.ErrorInfo
import me.shadykhalifa.whispertop.presentation.utils.ViewModelErrorHandler

abstract class BaseViewModel(
    private val errorHandler: ViewModelErrorHandler
) {
    
    private val job = SupervisorJob()
    protected val viewModelScope = CoroutineScope(Dispatchers.Main + job)
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorInfo = MutableStateFlow<ErrorInfo?>(null)
    val errorInfo: StateFlow<ErrorInfo?> = _errorInfo.asStateFlow()
    
    @Deprecated("Use errorInfo instead", ReplaceWith("errorInfo"))
    val errorMessage: StateFlow<String?> = _errorInfo.asStateFlow().let { errorInfoFlow ->
        MutableStateFlow<String?>(null).also { deprecatedFlow ->
            viewModelScope.launch {
                errorInfoFlow.collect { errorInfo ->
                    deprecatedFlow.value = errorInfo?.message
                }
            }
        }.asStateFlow()
    }
    
    protected fun launchSafely(
        onError: ((Throwable) -> Unit)? = null,
        block: suspend () -> Unit
    ) {
        val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
            handleError(throwable)
            onError?.invoke(throwable)
        }
        
        viewModelScope.launch(exceptionHandler) {
            setLoading(true)
            try {
                block()
            } finally {
                setLoading(false)
            }
        }
    }
    
    protected fun setLoading(loading: Boolean) {
        _isLoading.value = loading
    }
    
    protected fun handleError(throwable: Throwable, context: String? = null) {
        _errorInfo.value = errorHandler.handleError(throwable, context)
    }
    
    protected fun handleErrorWithNotification(throwable: Throwable, context: String? = null) {
        _errorInfo.value = errorHandler.handleErrorWithNotification(throwable, context)
    }
    
    fun clearError() {
        _errorInfo.value = null
    }
    
    fun onCleared() {
        viewModelScope.cancel()
    }
}