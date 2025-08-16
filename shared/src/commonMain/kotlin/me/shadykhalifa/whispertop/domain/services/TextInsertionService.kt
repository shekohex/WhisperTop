package me.shadykhalifa.whispertop.domain.services

interface TextInsertionService {
    suspend fun insertText(text: String): Boolean
    fun isServiceAvailable(): Boolean
}