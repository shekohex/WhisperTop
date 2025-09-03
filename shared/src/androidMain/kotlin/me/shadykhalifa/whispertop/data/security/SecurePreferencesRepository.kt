  override suspend fun getDataPrivacyMode(): Result<DataPrivacyMode> = withContext(Dispatchers.IO) {
        try {
            val modeName = encryptedPrefs.getString(KEY_DATA_PRIVACY_MODE, DataPrivacyMode.FULL.name)
            val mode = DataPrivacyMode.fromString(modeName ?: DataPrivacyMode.FULL.name)
            Result.Success(mode)
        } catch (e: Exception) {
            Result.Error(Exception("Failed to retrieve data privacy mode", e))
        }
    }
    
    override fun getDataPrivacyModeFlow(): Flow<Result<DataPrivacyMode>> = callbackFlow {
        try {
            // Get initial value
            val modeName = encryptedPrefs.getString(KEY_DATA_PRIVACY_MODE, DataPrivacyMode.FULL.name)
            val mode = DataPrivacyMode.fromString(modeName ?: DataPrivacyMode.FULL.name)
            trySend(Result.Success(mode))
            
            // Set up listener for changes
            val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                if (key == KEY_DATA_PRIVACY_MODE) {
                    try {
                        val newModeName = encryptedPrefs.getString(KEY_DATA_PRIVACY_MODE, DataPrivacyMode.FULL.name)
                        val newMode = DataPrivacyMode.fromString(newModeName ?: DataPrivacyMode.FULL.name)
                        trySend(Result.Success(newMode))
                    } catch (e: Exception) {
                        trySend(Result.Error(Exception("Failed to retrieve data privacy mode", e)))
                    }
                }
            }
            
            encryptedPrefs.registerOnSharedPreferenceChangeListener(listener)
            
            // Remove listener when flow is cancelled
            awaitClose {
                encryptedPrefs.unregisterOnSharedPreferenceChangeListener(listener)
            }
        } catch (e: Exception) {
            trySend(Result.Error(Exception("Failed to setup data privacy mode flow", e)))
            close(e)
        }
    }