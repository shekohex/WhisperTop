package me.shadykhalifa.whispertop.data.audio

expect class MP3FileWriter() {
    fun encodeMp3File(outputPath: String, data: ShortArray)
    fun decodeMp3File(filePath: String): FloatArray
}