package me.shadykhalifa.whispertop.data.repositories

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

actual class FileReader {
    actual suspend fun readFileAsBytes(filePath: String): ByteArray = withContext(Dispatchers.IO) {
        val file = File(filePath)
        if (!file.exists()) {
            throw IllegalArgumentException("File does not exist: $filePath")
        }
        
        if (!file.canRead()) {
            throw IllegalArgumentException("Cannot read file: $filePath")
        }
        
        try {
            file.readBytes()
        } catch (e: Exception) {
            throw Exception("Failed to read file: $filePath", e)
        }
    }
}