package com.clipboardhistory.data.database

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for ClipboardItemEntity.
 *
 * This test class verifies the data model and its properties.
 */
class ClipboardItemEntityTest {

    @Test
    fun `entity has correct default values`() {
        val entity = ClipboardItemEntity(
            content = "test content",
            contentType = "text/plain",
            timestamp = 123456789L
        )

        assertEquals(0L, entity.id) // Auto-generated ID starts at 0
        assertEquals("test content", entity.content)
        assertEquals("text/plain", entity.contentType)
        assertEquals(123456789L, entity.timestamp)
        assertEquals(null, entity.sourceApp)
        assertFalse(entity.isFavorite)
        assertFalse(entity.isDeleted)
        assertEquals(null, entity.encryptionKey)
    }

    @Test
    fun `entity with all fields set correctly`() {
        val entity = ClipboardItemEntity(
            id = 42L,
            content = "encrypted content",
            contentType = "text/plain",
            timestamp = 987654321L,
            sourceApp = "com.example.app",
            isFavorite = true,
            isDeleted = false,
            encryptionKey = "key123"
        )

        assertEquals(42L, entity.id)
        assertEquals("encrypted content", entity.content)
        assertEquals("text/plain", entity.contentType)
        assertEquals(987654321L, entity.timestamp)
        assertEquals("com.example.app", entity.sourceApp)
        assertTrue(entity.isFavorite)
        assertFalse(entity.isDeleted)
        assertEquals("key123", entity.encryptionKey)
    }

    @Test
    fun `entity equality based on all fields`() {
        val entity1 = ClipboardItemEntity(
            id = 1L,
            content = "content",
            contentType = "text/plain",
            timestamp = 1000L,
            sourceApp = "app1",
            isFavorite = true,
            isDeleted = false,
            encryptionKey = "key1"
        )

        val entity2 = ClipboardItemEntity(
            id = 1L,
            content = "content",
            contentType = "text/plain",
            timestamp = 1000L,
            sourceApp = "app1",
            isFavorite = true,
            isDeleted = false,
            encryptionKey = "key1"
        )

        val entity3 = ClipboardItemEntity(
            id = 2L,
            content = "content",
            contentType = "text/plain",
            timestamp = 1000L,
            sourceApp = "app1",
            isFavorite = true,
            isDeleted = false,
            encryptionKey = "key1"
        )

        assertEquals(entity1, entity2)
        assertEquals(entity1.hashCode(), entity2.hashCode())
        assertFalse(entity1.equals(entity3))
    }

    @Test
    fun `entity copy works correctly`() {
        val original = ClipboardItemEntity(
            id = 1L,
            content = "original content",
            contentType = "text/plain",
            timestamp = 1000L,
            sourceApp = "app1",
            isFavorite = false,
            isDeleted = false,
            encryptionKey = "key1"
        )

        val copied = original.copy(
            content = "modified content",
            isFavorite = true
        )

        assertEquals(1L, copied.id)
        assertEquals("modified content", copied.content)
        assertEquals("text/plain", copied.contentType)
        assertEquals(1000L, copied.timestamp)
        assertEquals("app1", copied.sourceApp)
        assertTrue(copied.isFavorite)
        assertFalse(copied.isDeleted)
        assertEquals("key1", copied.encryptionKey)
    }
}