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

abstract class BaseViewModel {
    
    private val job = SupervisorJob()
    protected val viewModelScope = CoroutineScope(Dispatchers.Main + job)
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
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
    
    protected fun handleError(throwable: Throwable) {
        _errorMessage.value = throwable.message ?: "An unknown error occurred"
    }
    
    fun clearError() {
        _errorMessage.value = null
    }
    
    fun onCleared() {
        viewModelScope.cancel()
    }
}