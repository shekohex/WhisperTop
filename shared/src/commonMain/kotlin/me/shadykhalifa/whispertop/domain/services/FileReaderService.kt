package me.shadykhalifa.whispertop.domain.services

interface FileReaderService {
    suspend fun readFileAsBytes(filePath: String): ByteArray
}