package me.shadykhalifa.whispertop.data.repositories

import kotlinx.cinterop.*
import platform.Foundation.*
import platform.posix.memcpy

@OptIn(ExperimentalForeignApi::class)
actual class FileReader {
    actual suspend fun readFileAsBytes(filePath: String): ByteArray {
        val fileManager = NSFileManager.defaultManager
        
        if (!fileManager.fileExistsAtPath(filePath)) {
            throw IllegalArgumentException("File does not exist: $filePath")
        }
        
        val data = NSData.dataWithContentsOfFile(filePath)
            ?: throw Exception("Failed to read file: $filePath")
        
        return data.toByteArray()
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    return ByteArray(length.toInt()).apply {
        usePinned { pinned ->
            memcpy(pinned.addressOf(0), bytes, length)
        }
    }
}