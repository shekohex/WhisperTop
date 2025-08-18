package me.shadykhalifa.whispertop.data.audio

expect class AACFileWriter() {
    fun encodeAacFile(outputPath: String, data: ShortArray)
    fun decodeAacFile(filePath: String): FloatArray
}