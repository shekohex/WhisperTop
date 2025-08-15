package me.shadykhalifa.whispertop.data.audio

expect class WAVFileWriter() {
    fun encodeWaveFile(outputPath: String, data: ShortArray)
    fun decodeWaveFile(filePath: String): FloatArray
}