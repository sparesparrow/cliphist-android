package com.clipboardhistory

import com.clipboardhistory.domain.model.ClipboardItem
import com.clipboardhistory.domain.model.ContentType
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Performance tests for critical operations.
 */
class PerformanceTest {
    @Test
    fun `large clipboard item creation performance`() {
        val largeContent = "A".repeat(10000) // 10KB content

        val startTime = System.currentTimeMillis()

        repeat(1000) {
            ClipboardItem(
                id = "perf-test-$it",
                content = largeContent,
                timestamp = System.currentTimeMillis(),
                contentType = ContentType.TEXT,
                isEncrypted = false,
                size = largeContent.toByteArray().size,
            )
        }

        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime

        // Should create 1000 items in less than 1 second
        assertTrue(duration < 1000, "Creating 1000 clipboard items took ${duration}ms, expected < 1000ms")
    }

    @Test
    fun `content type analysis performance`() {
        val testContents =
            listOf(
                "https://example.com",
                "user@example.com",
                "+1234567890",
                "123 Main Street New York",
                "Regular text content",
            )

        val startTime = System.currentTimeMillis()

        repeat(10000) {
            testContents.forEach { content ->
                com.clipboardhistory.domain.model.ContentAnalyzer.analyzeContentType(content)
            }
        }

        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime

        // Should analyze 50,000 content items in less than 2 seconds
        assertTrue(duration < 2000, "Content analysis took ${duration}ms, expected < 2000ms")
    }

    @Test
    fun `smart action generation performance`() {
        val types = com.clipboardhistory.domain.model.ContentAnalyzer.Type.values()

        val startTime = System.currentTimeMillis()

        repeat(10000) {
            types.forEach { type ->
                com.clipboardhistory.domain.model.ContentAnalyzer.getSmartActions(type, "test content")
            }
        }

        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime

        // Should generate 50,000 smart actions in less than 1 second
        assertTrue(duration < 1000, "Smart action generation took ${duration}ms, expected < 1000ms")
    }

    @Test
    fun `bubble theme access performance`() {
        val startTime = System.currentTimeMillis()

        repeat(100000) {
            com.clipboardhistory.domain.model.BubbleThemes.ALL_THEMES.forEach { theme ->
                theme.colors.empty
                theme.colors.storing
                theme.colors.replace
                theme.colors.append
                theme.colors.prepend
            }
        }

        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime

        // Should access theme colors 400,000 times in less than 1 second
        assertTrue(duration < 1000, "Theme access took ${duration}ms, expected < 1000ms")
    }

    @Test
    fun `settings creation and access performance`() {
        val startTime = System.currentTimeMillis()

        repeat(100000) {
            val settings =
                com.clipboardhistory.domain.model.ClipboardSettings(
                    maxHistorySize = 200,
                    autoDeleteAfterHours = 48,
                    enableEncryption = true,
                    bubbleSize = 4,
                    bubbleOpacity = 0.9f,
                    selectedTheme = "Dark",
                    bubbleType = com.clipboardhistory.domain.model.BubbleType.CUBE,
                )

            // Access all properties
            settings.maxHistorySize
            settings.autoDeleteAfterHours
            settings.enableEncryption
            settings.bubbleSize
            settings.bubbleOpacity
            settings.selectedTheme
            settings.bubbleType
        }

        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime

        // Should create and access 100,000 settings in less than 1 second
        assertTrue(duration < 1000, "Settings operations took ${duration}ms, expected < 1000ms")
    }
}
