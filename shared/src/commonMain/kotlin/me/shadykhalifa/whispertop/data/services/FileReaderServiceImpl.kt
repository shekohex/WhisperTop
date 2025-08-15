package me.shadykhalifa.whispertop.data.services

import me.shadykhalifa.whispertop.data.repositories.FileReader
import me.shadykhalifa.whispertop.domain.services.FileReaderService

class FileReaderServiceImpl(
    private val fileReader: FileReader
) : FileReaderService {
    
    override suspend fun readFileAsBytes(filePath: String): ByteArray {
        return fileReader.readFileAsBytes(filePath)
    }
}