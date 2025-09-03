fun updateDataPrivacyMode(mode: DataPrivacyMode) {
        launchSafely(
            onError = { throwable ->
                handleError(throwable, "Failed to update data privacy mode")
            }
        ) {
            // Optimistic update - immediately update UI for responsive feel
            val currentSettings = _uiState.value.settings
            val updatedSettings = currentSettings.copy(dataPrivacyMode = mode)
            _uiState.value = _uiState.value.copy(settings = updatedSettings)
            
            when (val result = settingsRepository.updateDataPrivacyMode(mode)) {
                is Result.Success -> {
                    // Success handled by flow collection, optimistic update already applied
                }
                is Result.Error -> {
                    handleError(result.exception)
                    // Revert optimistic change on error
                    _uiState.value = _uiState.value.copy(settings = currentSettings)
                }
                is Result.Loading -> {
                    // Loading state handled by individual operations
                }
            }
        }
    }