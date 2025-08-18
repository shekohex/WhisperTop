package me.shadykhalifa.whispertop.data.audio

// Simple stub - MP3 encoding handled by MediaRecorder directly  
actual class MP3FileWriter {
    actual fun encodeMp3File(outputPath: String, data: ShortArray) {
        // Not used - MediaRecorder handles MP3/AAC encoding directly
        throw UnsupportedOperationException("Use MediaRecorder for MP3 encoding")
    }
    
    actual fun decodeMp3File(filePath: String): FloatArray {
        // Not implemented for this project
        throw UnsupportedOperationException("MP3 decoding not implemented")
    }
}