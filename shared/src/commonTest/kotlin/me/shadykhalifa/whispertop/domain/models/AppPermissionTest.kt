package me.shadykhalifa.whispertop.domain.models

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AppPermissionTest {

    private val json = Json

    @Test
    fun `should serialize and deserialize AppPermission enum values`() {
        val permissions = AppPermission.values()
        
        permissions.forEach { permission ->
            val jsonString = json.encodeToString(permission)
            assertNotNull(jsonString)
            
            val deserializedPermission = json.decodeFromString<AppPermission>(jsonString)
            assertEquals(permission, deserializedPermission)
        }
    }

    @Test
    fun `should have correct display names and descriptions`() {
        val recordAudio = AppPermission.RECORD_AUDIO
        assertEquals("Record Audio", recordAudio.displayName)
        assertTrue(recordAudio.description.contains("capture voice"))
        
        val systemAlert = AppPermission.SYSTEM_ALERT_WINDOW
        assertEquals("Display over other apps", systemAlert.displayName)
        assertTrue(systemAlert.description.contains("floating microphone"))
        
        val accessibility = AppPermission.ACCESSIBILITY_SERVICE
        assertEquals("Accessibility Service", accessibility.displayName)
        assertTrue(accessibility.description.contains("insert transcribed text"))
    }

    @Test
    fun `should contain all required permissions`() {
        val permissions = AppPermission.values()
        assertEquals(3, permissions.size)
        
        val permissionNames = permissions.map { it.name }.toSet()
        assertTrue(permissionNames.contains("RECORD_AUDIO"))
        assertTrue(permissionNames.contains("SYSTEM_ALERT_WINDOW"))
        assertTrue(permissionNames.contains("ACCESSIBILITY_SERVICE"))
    }

    @Test
    fun `should have non-empty display names and descriptions`() {
        AppPermission.values().forEach { permission ->
            assertTrue(permission.displayName.isNotBlank(), "Display name should not be blank for ${permission.name}")
            assertTrue(permission.description.isNotBlank(), "Description should not be blank for ${permission.name}")
        }
    }

    @Test
    fun `should serialize enum as string`() {
        val permission = AppPermission.RECORD_AUDIO
        val jsonString = json.encodeToString(permission)
        
        // Should serialize as a simple string, not an object
        assertTrue(jsonString.startsWith("\"") && jsonString.endsWith("\""))
        assertTrue(jsonString.contains("RECORD_AUDIO"))
    }
}