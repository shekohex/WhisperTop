package me.shadykhalifa.whispertop.utils

import kotlin.experimental.xor

/**
 * Lightweight encryption utilities for sensitive data fields
 * Note: This is not cryptographically secure encryption, but provides
 * basic obfuscation for sensitive data at rest. For production use,
 * consider implementing proper AES encryption or using SQLCipher.
 */
object EncryptionUtils {
    
    private const val ENCRYPTION_KEY = "WhisperTopSecureKey2024" // In production, this should be dynamically generated
    
    /**
     * Simple XOR-based obfuscation for sensitive text
     * This provides basic protection against casual database browsing
     */
    fun obfuscateText(plainText: String?): String? {
        if (plainText.isNullOrBlank()) return plainText
        
        return try {
            val keyBytes = ENCRYPTION_KEY.toByteArray()
            val textBytes = plainText.toByteArray()
            val obfuscated = ByteArray(textBytes.size)
            
            for (i in textBytes.indices) {
                obfuscated[i] = textBytes[i] xor keyBytes[i % keyBytes.size]
            }
            
            // Convert to simple hex encoding for cross-platform compatibility
            obfuscated.joinToString("") { byte -> "%02x".format(byte) }
        } catch (e: Exception) {
            // If obfuscation fails, return null to avoid storing sensitive data
            null
        }
    }
    
    /**
     * Deobfuscate text that was obfuscated with obfuscateText
     */
    fun deobfuscateText(obfuscatedText: String?): String? {
        if (obfuscatedText.isNullOrBlank()) return obfuscatedText
        
        return try {
            val keyBytes = ENCRYPTION_KEY.toByteArray()
            
            // Parse hex string back to bytes
            val obfuscatedBytes = ByteArray(obfuscatedText.length / 2)
            for (i in obfuscatedBytes.indices) {
                val hex = obfuscatedText.substring(i * 2, i * 2 + 2)
                obfuscatedBytes[i] = hex.toInt(16).toByte()
            }
            
            val plainBytes = ByteArray(obfuscatedBytes.size)
            for (i in obfuscatedBytes.indices) {
                plainBytes[i] = obfuscatedBytes[i] xor keyBytes[i % keyBytes.size]
            }
            
            String(plainBytes)
        } catch (e: Exception) {
            // If deobfuscation fails, return original text
            obfuscatedText
        }
    }
    
    /**
     * Generate a secure hash for comparison purposes
     */
    fun generateSecureHash(input: String): String {
        return (input.hashCode().toString() + ENCRYPTION_KEY.hashCode().toString()).hashCode().toString()
    }
    
    /**
     * Check if text appears to be obfuscated (hex-encoded)
     */
    fun isObfuscated(text: String?): Boolean {
        if (text.isNullOrBlank()) return false
        
        return text.matches(Regex("^[0-9a-fA-F]+$")) && text.length % 2 == 0
    }
}