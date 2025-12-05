package com.clipboardhistory.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Unit tests for ContentAnalyzer.
 */
class ContentAnalyzerTest {
    @Test
    fun `analyzeContentType detects URL with http`() {
        val result = ContentAnalyzer.analyzeContentType("http://example.com")
        assertEquals(ContentAnalyzer.Type.URL, result)
    }

    @Test
    fun `analyzeContentType detects URL with https`() {
        val result = ContentAnalyzer.analyzeContentType("https://example.com")
        assertEquals(ContentAnalyzer.Type.URL, result)
    }

    @Test
    fun `analyzeContentType detects URL with www`() {
        val result = ContentAnalyzer.analyzeContentType("https://www.example.com")
        assertEquals(ContentAnalyzer.Type.URL, result)
    }

    @Test
    fun `analyzeContentType detects phone number with plus`() {
        val result = ContentAnalyzer.analyzeContentType("+1234567890")
        assertEquals(ContentAnalyzer.Type.PHONE, result)
    }

    @Test
    fun `analyzeContentType detects phone number without plus`() {
        val result = ContentAnalyzer.analyzeContentType("1234567890")
        assertEquals(ContentAnalyzer.Type.PHONE, result)
    }

    @Test
    fun `analyzeContentType detects phone number with spaces`() {
        val result = ContentAnalyzer.analyzeContentType("123 456 7890")
        assertEquals(ContentAnalyzer.Type.PHONE, result)
    }

    @Test
    fun `analyzeContentType detects phone number with dashes`() {
        val result = ContentAnalyzer.analyzeContentType("123-456-7890")
        assertEquals(ContentAnalyzer.Type.PHONE, result)
    }

    @Test
    fun `analyzeContentType detects email address`() {
        val result = ContentAnalyzer.analyzeContentType("test@example.com")
        assertEquals(ContentAnalyzer.Type.EMAIL, result)
    }

    @Test
    fun `analyzeContentType detects email with complex domain`() {
        val result = ContentAnalyzer.analyzeContentType("user.name+tag@subdomain.example.co.uk")
        assertEquals(ContentAnalyzer.Type.EMAIL, result)
    }

    @Test
    fun `analyzeContentType detects maps location`() {
        val result = ContentAnalyzer.analyzeContentType("123 Main Street New York")
        assertEquals(ContentAnalyzer.Type.MAPS, result)
    }

    @Test
    fun `analyzeContentType detects maps location with city and state`() {
        val result = ContentAnalyzer.analyzeContentType("Central Park New York NY")
        assertEquals(ContentAnalyzer.Type.MAPS, result)
    }

    @Test
    fun `analyzeContentType falls back to TEXT for short content`() {
        val result = ContentAnalyzer.analyzeContentType("Hi")
        assertEquals(ContentAnalyzer.Type.TEXT, result)
    }

    @Test
    fun `analyzeContentType falls back to TEXT for long content`() {
        val result =
            ContentAnalyzer.analyzeContentType(
                "This is a very long text content that exceeds the maps detection " +
                    "threshold and should be classified as regular text content",
            )
        assertEquals(ContentAnalyzer.Type.TEXT, result)
    }

    @Test
    fun `analyzeContentType trims whitespace`() {
        val result = ContentAnalyzer.analyzeContentType("  https://example.com  ")
        assertEquals(ContentAnalyzer.Type.URL, result)
    }

    @Test
    fun `getSmartActions returns correct actions for URL`() {
        val actions = ContentAnalyzer.getSmartActions(ContentAnalyzer.Type.URL, "https://example.com")

        assertEquals(1, actions.size)
        assertEquals("Open Link", actions[0].label)
        assertEquals(BubbleState.REPLACE, actions[0].action)
    }

    @Test
    fun `getSmartActions returns correct actions for PHONE`() {
        val actions = ContentAnalyzer.getSmartActions(ContentAnalyzer.Type.PHONE, "+1234567890")

        assertEquals(1, actions.size)
        assertEquals("Call Number", actions[0].label)
        assertEquals(BubbleState.REPLACE, actions[0].action)
    }

    @Test
    fun `getSmartActions returns correct actions for EMAIL`() {
        val actions = ContentAnalyzer.getSmartActions(ContentAnalyzer.Type.EMAIL, "test@example.com")

        assertEquals(1, actions.size)
        assertEquals("Send Email", actions[0].label)
        assertEquals(BubbleState.REPLACE, actions[0].action)
    }

    @Test
    fun `getSmartActions returns correct actions for MAPS`() {
        val actions = ContentAnalyzer.getSmartActions(ContentAnalyzer.Type.MAPS, "123 Main Street")

        assertEquals(1, actions.size)
        assertEquals("Open Maps", actions[0].label)
        assertEquals(BubbleState.REPLACE, actions[0].action)
    }

    @Test
    fun `getSmartActions returns correct actions for TEXT`() {
        val actions = ContentAnalyzer.getSmartActions(ContentAnalyzer.Type.TEXT, "Hello world")

        assertEquals(1, actions.size)
        assertEquals("Search Text", actions[0].label)
        assertEquals(BubbleState.REPLACE, actions[0].action)
    }

    @Test
    fun `analyzeContentType handles empty string`() {
        val result = ContentAnalyzer.analyzeContentType("")
        assertEquals(ContentAnalyzer.Type.TEXT, result)
    }

    @Test
    fun `analyzeContentType handles whitespace only`() {
        val result = ContentAnalyzer.analyzeContentType("   ")
        assertEquals(ContentAnalyzer.Type.TEXT, result)
    }

    @Test
    fun `analyzeContentType handles special characters in phone`() {
        val result = ContentAnalyzer.analyzeContentType("+1 (555) 123-4567")
        assertEquals(ContentAnalyzer.Type.PHONE, result)
    }

    @Test
    fun `analyzeContentType handles international phone format`() {
        val result = ContentAnalyzer.analyzeContentType("+44 20 7946 0958")
        assertEquals(ContentAnalyzer.Type.PHONE, result)
    }

    @Test
    fun `analyzeContentType handles phone with dots`() {
        val result = ContentAnalyzer.analyzeContentType("555.123.4567")
        assertEquals(ContentAnalyzer.Type.PHONE, result)
    }

    @Test
    fun `analyzeContentType handles US phone format with area code`() {
        val result = ContentAnalyzer.analyzeContentType("(212) 555-1234")
        assertEquals(ContentAnalyzer.Type.PHONE, result)
    }

    @Test
    fun `analyzeContentType handles German phone format`() {
        val result = ContentAnalyzer.analyzeContentType("+49 30 12345678")
        assertEquals(ContentAnalyzer.Type.PHONE, result)
    }

    @Test
    fun `analyzeContentType rejects too short phone number`() {
        // Only 5 digits - not a valid phone number
        val result = ContentAnalyzer.analyzeContentType("12345")
        assertEquals(ContentAnalyzer.Type.TEXT, result)
    }

    @Test
    fun `analyzeContentType rejects number with letters`() {
        // Contains letters - not a phone number
        val result = ContentAnalyzer.analyzeContentType("123-ABC-4567")
        assertEquals(ContentAnalyzer.Type.TEXT, result)
    }

    @Test
    fun `analyzeContentType detects 7 digit phone number`() {
        // Minimum valid phone number
        val result = ContentAnalyzer.analyzeContentType("5551234")
        assertEquals(ContentAnalyzer.Type.PHONE, result)
    }

    @Test
    fun `analyzeContentType handles phone with extension notation`() {
        val result = ContentAnalyzer.analyzeContentType("555-123-4567")
        assertEquals(ContentAnalyzer.Type.PHONE, result)
    }
}
