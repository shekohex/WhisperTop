package me.shadykhalifa.whispertop.data.audio

actual class AACFileWriter {
    actual fun encodeAacFile(outputPath: String, data: ShortArray) {
        // iOS implementation would use AVAudioConverter or similar
        // For now, fallback to WAV
        WAVFileWriter().encodeWaveFile(outputPath.replace(".m4a", ".wav"), data)
    }
    
    actual fun decodeAacFile(filePath: String): FloatArray {
        return FloatArray(0)
    }
}