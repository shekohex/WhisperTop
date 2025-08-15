package me.shadykhalifa.whispertop.data.audio

expect fun getCurrentTimeMillis(): Long

class AudioFileManager {
    
    companion object {
        private const val RECORDINGS_DIR = "recordings"
        private const val MAX_RECORDINGS = 50 // Keep only last 50 recordings
    }
    
    fun generateFileName(): String {
        val timestamp = getCurrentTimeMillis()
        return "recording_${timestamp}.wav"
    }
    
    fun getRecordingsPath(basePath: String): String {
        return "$basePath/$RECORDINGS_DIR"
    }
    
    fun cleanupOldRecordings(recordingsPath: String) {
        // Platform-specific implementation
        // This would list files, sort by date, and remove old ones
        // Implementation depends on platform file system APIs
    }
    
    fun validateWAVFile(filePath: String): Boolean {
        // Basic WAV file validation
        // Could be expanded to check header integrity
        return filePath.endsWith(".wav", ignoreCase = true)
    }
    
    fun calculateDuration(audioDataSize: Int, sampleRate: Int = 16000, channels: Int = 1, bitsPerSample: Int = 16): Long {
        val bytesPerSample = bitsPerSample / 8
        val totalSamples = audioDataSize / (channels * bytesPerSample)
        return (totalSamples * 1000L) / sampleRate
    }
}