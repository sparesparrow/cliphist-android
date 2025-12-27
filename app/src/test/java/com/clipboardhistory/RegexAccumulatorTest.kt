package com.clipboardhistory

import androidx.compose.ui.geometry.Offset
import com.clipboardhistory.presentation.ui.bubble.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RegexAccumulatorTest {

    private val samplePattern = AdvancedBubbleSpec.RegexPattern(
        id = "test_pattern",
        name = "Test Pattern",
        pattern = "\\b\\d{3}-\\d{3}-\\d{4}\\b", // Phone number pattern
        description = "US phone numbers",
        delimiter = AdvancedBubbleSpec.RegexPattern.Delimiter.NEWLINE
    )

    @Test
    fun `RegexAccumulator should accumulate matching content`() {
        val accumulator = AdvancedBubbleSpec.RegexAccumulator(pattern = samplePattern)

        val testContent = "Call me at 555-123-4567 or 555-987-6543 for details."
        val result = accumulator.tryAccumulate(testContent, "test_source")

        assertEquals(2, result.accumulatedItems.size)
        assertEquals("555-123-4567", result.accumulatedItems[0].content)
        assertEquals("555-987-6543", result.accumulatedItems[1].content)
        assertEquals("test_source", result.accumulatedItems[0].source)
    }

    @Test
    fun `RegexAccumulator should not accumulate non-matching content`() {
        val accumulator = AdvancedBubbleSpec.RegexAccumulator(pattern = samplePattern)

        val testContent = "This is just regular text without phone numbers."
        val result = accumulator.tryAccumulate(testContent)

        assertEquals(0, result.accumulatedItems.size)
    }

    @Test
    fun `RegexAccumulator should handle invalid regex patterns gracefully`() {
        val invalidPattern = samplePattern.copy(pattern = "[invalid")
        val accumulator = AdvancedBubbleSpec.RegexAccumulator(pattern = invalidPattern)

        val testContent = "Some content"
        val result = accumulator.tryAccumulate(testContent)

        // Should not crash and return unchanged accumulator
        assertEquals(accumulator, result)
        assertEquals(0, result.accumulatedItems.size)
    }

    @Test
    fun `RegexAccumulator should respect showDuplicates setting`() {
        val accumulator = AdvancedBubbleSpec.RegexAccumulator(
            pattern = samplePattern,
            showDuplicates = false
        )

        // First accumulation
        val result1 = accumulator.tryAccumulate("Call 555-123-4567")
        assertEquals(1, result1.accumulatedItems.size)

        // Second accumulation with same number (should be ignored)
        val result2 = result1.tryAccumulate("Contact: 555-123-4567")
        assertEquals(1, result2.accumulatedItems.size)

        // Third accumulation with different number (should be added)
        val result3 = result2.tryAccumulate("Call 555-999-8888")
        assertEquals(2, result3.accumulatedItems.size)
    }

    @Test
    fun `RegexAccumulator should respect maxItems limit`() {
        val pattern = samplePattern.copy(maxItems = 3)
        val accumulator = AdvancedBubbleSpec.RegexAccumulator(pattern = pattern)

        // Add 5 items
        val result = accumulator
            .tryAccumulate("555-111-1111")
            .tryAccumulate("555-222-2222")
            .tryAccumulate("555-333-3333")
            .tryAccumulate("555-444-4444")
            .tryAccumulate("555-555-5555")

        assertEquals(3, result.accumulatedItems.size)
        // Should keep the most recent items
        assertEquals("555-333-3333", result.accumulatedItems[0].content)
        assertEquals("555-444-4444", result.accumulatedItems[1].content)
        assertEquals("555-555-5555", result.accumulatedItems[2].content)
    }

    @Test
    fun `RegexAccumulator should export items with correct delimiter`() {
        val newlinePattern = samplePattern.copy(delimiter = AdvancedBubbleSpec.RegexPattern.Delimiter.NEWLINE)
        val accumulator = AdvancedBubbleSpec.RegexAccumulator(pattern = newlinePattern)
            .tryAccumulate("555-111-1111 and 555-222-2222")

        val exported = accumulator.exportAccumulated()
        assertEquals("555-111-1111\n555-222-2222", exported)
    }

    @Test
    fun `RegexAccumulator should export with space delimiter`() {
        val spacePattern = samplePattern.copy(delimiter = AdvancedBubbleSpec.RegexPattern.Delimiter.SPACE)
        val accumulator = AdvancedBubbleSpec.RegexAccumulator(pattern = spacePattern)
            .tryAccumulate("555-111-1111 and 555-222-2222")

        val exported = accumulator.exportAccumulated()
        assertEquals("555-111-1111 555-222-2222", exported)
    }

    @Test
    fun `RegexAccumulator should clear accumulated items`() {
        val accumulator = AdvancedBubbleSpec.RegexAccumulator(pattern = samplePattern)
            .tryAccumulate("555-111-1111")

        assertEquals(1, accumulator.accumulatedItems.size)

        val cleared = accumulator.clearAccumulated()
        assertEquals(0, cleared.accumulatedItems.size)
    }

    @Test
    fun `RegexAccumulator should calculate dynamic size based on item count`() {
        val accumulator = AdvancedBubbleSpec.RegexAccumulator(pattern = samplePattern)
        val baseSize = 200.dp

        // Empty
        assertEquals(baseSize, accumulator.getDynamicSize(baseSize))

        // Few items
        val with5Items = accumulator.copy(accumulatedItems = List(5) {
            AdvancedBubbleSpec.AccumulatedItem("item$it")
        })
        assertEquals(baseSize, with5Items.getDynamicSize(baseSize))

        // Medium count
        val with15Items = accumulator.copy(accumulatedItems = List(15) {
            AdvancedBubbleSpec.AccumulatedItem("item$it")
        })
        assertEquals((baseSize.value * 1.4f).dp, with15Items.getDynamicSize(baseSize))

        // Large count
        val with100Items = accumulator.copy(accumulatedItems = List(100) {
            AdvancedBubbleSpec.AccumulatedItem("item$it")
        })
        assertEquals((baseSize.value * 1.8f).dp, with100Items.getDynamicSize(baseSize))
    }

    @Test
    fun `RegexAccumulator should detect new content since timestamp`() {
        val accumulator = AdvancedBubbleSpec.RegexAccumulator(pattern = samplePattern)
        val initialTime = System.currentTimeMillis()

        // Add item at current time
        val withItem = accumulator.tryAccumulate("555-111-1111")
        Thread.sleep(10) // Ensure time difference

        val checkTime = System.currentTimeMillis()
        assertTrue(withItem.hasNewContent(initialTime))
        assertFalse(withItem.hasNewContent(checkTime))
    }

    @Test
    fun `RegexAccumulator should handle multiple matches in single content`() {
        val emailPattern = samplePattern.copy(
            pattern = "\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b"
        )
        val accumulator = AdvancedBubbleSpec.RegexAccumulator(pattern = emailPattern)

        val testContent = "Contact john@example.com or jane@test.org for support."
        val result = accumulator.tryAccumulate(testContent)

        assertEquals(2, result.accumulatedItems.size)
        assertTrue(result.accumulatedItems.any { it.content == "john@example.com" })
        assertTrue(result.accumulatedItems.any { it.content == "jane@test.org" })
    }

    @Test
    fun `RegexAccumulator should create with factory method`() {
        val pattern = AdvancedBubbleSpec.RegexPattern(
            id = "url_pattern",
            name = "URL Collector",
            pattern = "https?://\\S+",
            description = "Collects URLs"
        )

        val accumulator = AdvancedBubbleSpec.create(
            type = AdvancedBubbleType.REGEX_ACCUMULATOR,
            position = Offset(100f, 200f),
            content = mapOf("pattern" to pattern)
        )

        assertTrue(accumulator is AdvancedBubbleSpec.RegexAccumulator)
        val regexAccumulator = accumulator as AdvancedBubbleSpec.RegexAccumulator
        assertEquals(pattern, regexAccumulator.pattern)
        assertEquals(Offset(100f, 200f), regexAccumulator.position)
    }

    @Test
    fun `RegexPattern delimiter should provide correct separators`() {
        assertEquals("\n", AdvancedBubbleSpec.RegexPattern.Delimiter.NEWLINE.getSeparator())
        assertEquals(" ", AdvancedBubbleSpec.RegexPattern.Delimiter.SPACE.getSeparator())
        assertEquals(", ", AdvancedBubbleSpec.RegexPattern.Delimiter.COMMA.getSeparator())
        assertEquals("; ", AdvancedBubbleSpec.RegexPattern.Delimiter.SEMICOLON.getSeparator())
        assertEquals("", AdvancedBubbleSpec.RegexPattern.Delimiter.CUSTOM.getSeparator())
    }
}