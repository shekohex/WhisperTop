package me.shadykhalifa.whispertop.data.audio

// Simple stub - AAC encoding handled by MediaRecorder directly
actual class AACFileWriter {
    actual fun encodeAacFile(outputPath: String, data: ShortArray) {
        // Not used - MediaRecorder handles AAC encoding directly
        throw UnsupportedOperationException("Use MediaRecorder for AAC encoding")
    }
    
    actual fun decodeAacFile(filePath: String): FloatArray {
        // Not implemented for this project
        throw UnsupportedOperationException("AAC decoding not implemented")
    }
}