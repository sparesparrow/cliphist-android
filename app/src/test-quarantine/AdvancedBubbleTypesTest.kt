package com.clipboardhistory

import androidx.compose.ui.geometry.Offset
import com.clipboardhistory.presentation.ui.bubble.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AdvancedBubbleTypesTest {

    @Test
    fun `SearchBubble should be relevant for text content and keyboard visible`() {
        val searchBubble = AdvancedBubbleType.SEARCH_BUBBLE

        // Should be relevant for text content
        assertTrue(searchBubble.isRelevantForContent("sample text", ContentType.TEXT))

        // Should not be relevant for images
        assertFalse(searchBubble.isRelevantForContent("image data", ContentType.IMAGE))

        // Should show when keyboard is visible
        assertTrue(searchBubble.shouldBeVisible(true))

        // Should not show when keyboard is hidden
        assertFalse(searchBubble.shouldBeVisible(false))
    }

    @Test
    fun `TemplateBubble should be relevant for various content types`() {
        val templateBubble = AdvancedBubbleType.TEMPLATE_BUBBLE

        // Should be generally relevant (templates can be used anytime)
        assertTrue(templateBubble.isRelevantForContent("any content", ContentType.TEXT))
        assertTrue(templateBubble.isRelevantForContent("", ContentType.UNKNOWN))

        // Should minimize when keyboard appears
        assertTrue(templateBubble.shouldBeMinimized(true))
        assertFalse(templateBubble.shouldBeMinimized(false))
    }

    @Test
    fun `CalculatorBubble should be relevant for numeric content`() {
        val calculatorBubble = AdvancedBubbleType.CALCULATOR_BUBBLE

        // Should be relevant for numbers and math expressions
        assertTrue(calculatorBubble.isRelevantForContent("123 + 456", ContentType.NUMBER))
        assertTrue(calculatorBubble.isRelevantForContent("calculate this", ContentType.TEXT))

        // Should reposition when keyboard appears
        assertTrue(calculatorBubble.shouldBeRepositioned(true))
        assertFalse(calculatorBubble.shouldBeRepositioned(false))
    }

    @Test
    fun `TranslationBubble should be relevant for substantial text content`() {
        val translationBubble = AdvancedBubbleType.TRANSLATION_BUBBLE

        // Should be relevant for longer text content
        assertTrue(translationBubble.isRelevantForContent("This is a longer text that should be translated", ContentType.TEXT))

        // Should not be relevant for very short text
        assertFalse(translationBubble.isRelevantForContent("hi", ContentType.TEXT))

        // Should minimize when keyboard appears
        assertTrue(translationBubble.shouldBeMinimized(true))
    }

    @Test
    fun `ContextActions bubble should be relevant for specific content types`() {
        val contextActions = AdvancedBubbleType.CONTEXT_ACTIONS

        // Should be relevant for URLs
        assertTrue(contextActions.isRelevantForContent("https://example.com", ContentType.URL))

        // Should be relevant for code
        assertTrue(contextActions.isRelevantForContent("function test() {}", ContentType.CODE))

        // Should be relevant for images
        assertTrue(contextActions.isRelevantForContent("image data", ContentType.IMAGE))
    }

    @Test
    fun `URLActions bubble should only be relevant for URLs`() {
        val urlActions = AdvancedBubbleType.URL_ACTIONS

        // Should be relevant for URLs
        assertTrue(urlActions.isRelevantForContent("https://example.com", ContentType.URL))
        assertTrue(urlActions.isRelevantForContent("http://test.com", ContentType.URL))

        // Should not be relevant for other content
        assertFalse(urlActions.isRelevantForContent("regular text", ContentType.TEXT))
        assertFalse(urlActions.isRelevantForContent("123", ContentType.NUMBER))
    }

    @Test
    fun `CodeActions bubble should only be relevant for code`() {
        val codeActions = AdvancedBubbleType.CODE_ACTIONS

        // Should be relevant for code
        assertTrue(codeActions.isRelevantForContent("function test() {}", ContentType.CODE))
        assertTrue(codeActions.isRelevantForContent("<html></html>", ContentType.CODE))

        // Should not be relevant for regular text
        assertFalse(codeActions.isRelevantForContent("regular text", ContentType.TEXT))
    }

    @Test
    fun `ColorPicker bubble should only be relevant for images`() {
        val colorPicker = AdvancedBubbleType.COLOR_PICKER

        // Should be relevant for images
        assertTrue(colorPicker.isRelevantForContent("image data", ContentType.IMAGE))

        // Should not be relevant for other content
        assertFalse(colorPicker.isRelevantForContent("text content", ContentType.TEXT))
        assertFalse(colorPicker.isRelevantForContent("https://example.com", ContentType.URL))
    }

    @Test
    fun `bubble contextual priority should increase with relevance and keyboard state`() {
        val searchBubble = AdvancedBubbleType.SEARCH_BUBBLE
        val basePriority = searchBubble.zIndexPriority

        // Base priority for irrelevant content
        val irrelevantPriority = searchBubble.getContextualPriority("image", ContentType.IMAGE, false)
        assertEquals(basePriority, irrelevantPriority)

        // Higher priority for relevant content
        val relevantPriority = searchBubble.getContextualPriority("search this text", ContentType.TEXT, false)
        assertTrue(relevantPriority > basePriority)

        // Even higher priority when keyboard is visible
        val keyboardPriority = searchBubble.getContextualPriority("search this text", ContentType.TEXT, true)
        assertTrue(keyboardPriority > relevantPriority)
    }

    @Test
    fun `getRelevantBubbles should return top recommendations for content`() {
        val textContent = "This is a sample text for translation and searching"

        val relevantBubbles = AdvancedBubbleType.getRelevantBubbles(
            textContent,
            ContentType.TEXT,
            keyboardVisible = true
        )

        // Should include translation and search bubbles
        assertTrue(relevantBubbles.any { it == AdvancedBubbleType.TRANSLATION_BUBBLE })
        assertTrue(relevantBubbles.any { it == AdvancedBubbleType.SEARCH_BUBBLE })

        // Should be limited to top 5
        assertTrue(relevantBubbles.size <= 5)
    }

    @Test
    fun `getBubblesByCategory should return correct category groupings`() {
        val searchBubbles = AdvancedBubbleType.getBubblesByCategory(BubbleCategory.SEARCH)
        assertTrue(searchBubbles.contains(AdvancedBubbleType.SEARCH_BUBBLE))

        val creativeBubbles = AdvancedBubbleType.getBubblesByCategory(BubbleCategory.CREATIVE)
        assertTrue(creativeBubbles.contains(AdvancedBubbleType.COLOR_PICKER))
        assertTrue(creativeBubbles.contains(AdvancedBubbleType.IMAGE_TOOLS))
    }

    @Test
    fun `bubble size should adapt to minimization and keyboard state`() {
        val toolbeltBubble = AdvancedBubbleType.TOOLBELT

        // Normal size
        assertEquals(280.dp, toolbeltBubble.getSize(false, false))

        // Minimized size
        assertEquals(40.dp, toolbeltBubble.getSize(true, true))

        // When minimized due to keyboard
        assertEquals(40.dp, toolbeltBubble.getSize(true, false))
    }

    @Test
    fun `system bubbles should ignore keyboard state changes`() {
        val syncBubble = AdvancedBubbleType.CLIPBOARD_SYNC

        // Should always be visible regardless of keyboard
        assertTrue(syncBubble.shouldBeVisible(true))
        assertTrue(syncBubble.shouldBeVisible(false))

        // Should not minimize or reposition
        assertFalse(syncBubble.shouldBeMinimized(true))
        assertFalse(syncBubble.shouldBeRepositioned(true))
    }

    @Test
    fun `bubble auto-hide delays should be appropriate for usage patterns`() {
        // Short auto-hide for quick actions
        assertEquals(15000L, AdvancedBubbleType.QUICK_ACTION.autoHideDelay)

        // Medium auto-hide for processing tasks
        assertEquals(30000L, AdvancedBubbleType.CALCULATOR_BUBBLE.autoHideDelay)

        // Long auto-hide for complex tasks
        assertEquals(60000L, AdvancedBubbleType.TRANSLATION_BUBBLE.autoHideDelay)

        // No auto-hide for persistent tools
        assertEquals(0L, AdvancedBubbleType.TOOLBELT.autoHideDelay)
        assertEquals(0L, AdvancedBubbleType.FAVORITES_PALETTE.autoHideDelay)
    }

    @Test
    fun `bubble instance limits should prevent UI overcrowding`() {
        // Single instance bubbles
        assertEquals(1, AdvancedBubbleType.TOOLBELT.maxInstances)
        assertEquals(1, AdvancedBubbleType.SEARCH_BUBBLE.maxInstances)

        // Limited instances for resource-intensive bubbles
        assertEquals(3, AdvancedBubbleType.SYSTEM_NOTIFICATION.maxInstances)
        assertEquals(3, AdvancedBubbleType.QUICK_ACTION.maxInstances)

        // More permissive for content bubbles
        assertEquals(5, AdvancedBubbleType.PINNED_ITEM.maxInstances)
    }

    @Test
    fun `AdvancedBubbleSpec create method should instantiate correct types`() {
        // Search bubble
        val searchSpec = AdvancedBubbleSpec.create(AdvancedBubbleType.SEARCH_BUBBLE, content = "search query")
        assertTrue(searchSpec is AdvancedBubbleSpec.SearchBubble)

        // Template bubble
        val templates = listOf(TextTemplate("1", "Test", "content", "category"))
        val templateSpec = AdvancedBubbleSpec.create(AdvancedBubbleType.TEMPLATE_BUBBLE, content = templates)
        assertTrue(templateSpec is AdvancedBubbleSpec.TemplateBubble)
    }

    @Test
    fun `bubble categories should be correctly assigned`() {
        assertEquals(BubbleCategory.SEARCH, AdvancedBubbleType.SEARCH_BUBBLE.category)
        assertEquals(BubbleCategory.CONTENT, AdvancedBubbleType.TEMPLATE_BUBBLE.category)
        assertEquals(BubbleCategory.PROCESSING, AdvancedBubbleType.CALCULATOR_BUBBLE.category)
        assertEquals(BubbleCategory.PROCESSING, AdvancedBubbleType.TRANSLATION_BUBBLE.category)
        assertEquals(BubbleCategory.SHARING, AdvancedBubbleType.SHARE_BUBBLE.category)
        assertEquals(BubbleCategory.HISTORY, AdvancedBubbleType.TIMELINE_BUBBLE.category)
        assertEquals(BubbleCategory.ORGANIZATION, AdvancedBubbleType.FAVORITES_PALETTE.category)
        assertEquals(BubbleCategory.CONTEXT, AdvancedBubbleType.CONTEXT_ACTIONS.category)
        assertEquals(BubbleCategory.PRODUCTIVITY, AdvancedBubbleType.NOTE_SCRIBBLE.category)
        assertEquals(BubbleCategory.SYSTEM, AdvancedBubbleType.CLIPBOARD_SYNC.category)
        assertEquals(BubbleCategory.CREATIVE, AdvancedBubbleType.COLOR_PICKER.category)
    }
}