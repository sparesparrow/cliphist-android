package com.clipboardhistory.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for ClipboardItem and related domain models.
 */
class ClipboardItemTest {
    
    @Test
    fun `ClipboardItem creation with all properties`() {
        val item = ClipboardItem(
            id = "test-id",
            content = "Test content",
            timestamp = 1234567890L,
            contentType = ContentType.TEXT,
            isEncrypted = true,
            size = 12
        )
        
        assertEquals("test-id", item.id)
        assertEquals("Test content", item.content)
        assertEquals(1234567890L, item.timestamp)
        assertEquals(ContentType.TEXT, item.contentType)
        assertTrue(item.isEncrypted)
        assertEquals(12, item.size)
    }
    
    @Test
    fun `ClipboardItem with default encryption value`() {
        val item = ClipboardItem(
            id = "test-id",
            content = "Test content",
            timestamp = 1234567890L,
            contentType = ContentType.TEXT,
            size = 12
        )
        
        assertTrue(item.isEncrypted) // Default should be true
    }
    
    @Test
    fun `ContentType enum values`() {
        assertEquals("TEXT", ContentType.TEXT.name)
        assertEquals("IMAGE", ContentType.IMAGE.name)
        assertEquals("URL", ContentType.URL.name)
        assertEquals("FILE", ContentType.FILE.name)
        assertEquals("OTHER", ContentType.OTHER.name)
    }
    
    @Test
    fun `BubbleState enum values`() {
        assertEquals("EMPTY", BubbleState.EMPTY.name)
        assertEquals("STORING", BubbleState.STORING.name)
        assertEquals("REPLACE", BubbleState.REPLACE.name)
        assertEquals("APPEND", BubbleState.APPEND.name)
        assertEquals("PREPEND", BubbleState.PREPEND.name)
    }
    
    @Test
    fun `BubbleType enum values`() {
        assertEquals("CIRCLE", BubbleType.CIRCLE.name)
        assertEquals("CUBE", BubbleType.CUBE.name)
        assertEquals("HEXAGON", BubbleType.HEXAGON.name)
        assertEquals("SQUARE", BubbleType.SQUARE.name)
    }
    
    @Test
    fun `BubbleTheme creation`() {
        val theme = BubbleTheme(
            name = "Test Theme",
            description = "Test description",
            colors = BubbleColors(
                empty = 0xFF000000.toInt(),
                storing = 0xFF111111.toInt(),
                replace = 0xFF222222.toInt(),
                append = 0xFF333333.toInt(),
                prepend = 0xFF444444.toInt()
            )
        )
        
        assertEquals("Test Theme", theme.name)
        assertEquals("Test description", theme.description)
        assertEquals(0xFF000000.toInt(), theme.colors.empty)
        assertEquals(0xFF111111.toInt(), theme.colors.storing)
        assertEquals(0xFF222222.toInt(), theme.colors.replace)
        assertEquals(0xFF333333.toInt(), theme.colors.append)
        assertEquals(0xFF444444.toInt(), theme.colors.prepend)
    }
    
    @Test
    fun `BubbleThemes predefined themes exist`() {
        assertTrue(BubbleThemes.ALL_THEMES.isNotEmpty())
        assertTrue(BubbleThemes.ALL_THEMES.contains(BubbleThemes.DEFAULT))
        assertTrue(BubbleThemes.ALL_THEMES.contains(BubbleThemes.DARK))
        assertTrue(BubbleThemes.ALL_THEMES.contains(BubbleThemes.PASTEL))
        assertTrue(BubbleThemes.ALL_THEMES.contains(BubbleThemes.NEON))
    }
    
    @Test
    fun `BubbleThemes DEFAULT theme properties`() {
        val theme = BubbleThemes.DEFAULT
        
        assertEquals("Default", theme.name)
        assertEquals("Material Design colors", theme.description)
        assertEquals(0xFFE0E0E0.toInt(), theme.colors.empty)
        assertEquals(0xFF2196F3.toInt(), theme.colors.storing)
        assertEquals(0xFFFF5722.toInt(), theme.colors.replace)
        assertEquals(0xFF4CAF50.toInt(), theme.colors.append)
        assertEquals(0xFF9C27B0.toInt(), theme.colors.prepend)
    }
    
    @Test
    fun `ClipboardSettings default values`() {
        val settings = ClipboardSettings()
        
        assertEquals(100, settings.maxHistorySize)
        assertEquals(24, settings.autoDeleteAfterHours)
        assertTrue(settings.enableEncryption)
        assertEquals(3, settings.bubbleSize)
        assertEquals(0.8f, settings.bubbleOpacity)
        assertEquals("Default", settings.selectedTheme)
        assertEquals(BubbleType.CIRCLE, settings.bubbleType)
    }
    
    @Test
    fun `ClipboardSettings with custom values`() {
        val settings = ClipboardSettings(
            maxHistorySize = 200,
            autoDeleteAfterHours = 48,
            enableEncryption = false,
            bubbleSize = 5,
            bubbleOpacity = 0.5f,
            selectedTheme = "Dark",
            bubbleType = BubbleType.CUBE
        )
        
        assertEquals(200, settings.maxHistorySize)
        assertEquals(48, settings.autoDeleteAfterHours)
        assertFalse(settings.enableEncryption)
        assertEquals(5, settings.bubbleSize)
        assertEquals(0.5f, settings.bubbleOpacity)
        assertEquals("Dark", settings.selectedTheme)
        assertEquals(BubbleType.CUBE, settings.bubbleType)
    }
    
    @Test
    fun `ClipboardSettings validation ranges`() {
        // Test bubble size range (should be 1-5)
        val settings1 = ClipboardSettings(bubbleSize = 1)
        assertEquals(1, settings1.bubbleSize)
        
        val settings5 = ClipboardSettings(bubbleSize = 5)
        assertEquals(5, settings5.bubbleSize)
        
        // Test bubble opacity range (should be 0.1-1.0)
        val settingsOpacity1 = ClipboardSettings(bubbleOpacity = 0.1f)
        assertEquals(0.1f, settingsOpacity1.bubbleOpacity)
        
        val settingsOpacity2 = ClipboardSettings(bubbleOpacity = 1.0f)
        assertEquals(1.0f, settingsOpacity2.bubbleOpacity)
    }
}