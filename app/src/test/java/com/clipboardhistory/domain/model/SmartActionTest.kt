package com.clipboardhistory.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Unit tests for SmartAction.
 */
class SmartActionTest {
    
    @Test
    fun `SmartAction creation with label and action`() {
        val action = SmartAction(
            label = "Test Action",
            action = BubbleState.REPLACE
        )
        
        assertEquals("Test Action", action.label)
        assertEquals(BubbleState.REPLACE, action.action)
    }
    
    @Test
    fun `SmartAction with different bubble states`() {
        val replaceAction = SmartAction("Replace", BubbleState.REPLACE)
        val appendAction = SmartAction("Append", BubbleState.APPEND)
        val prependAction = SmartAction("Prepend", BubbleState.PREPEND)
        val storingAction = SmartAction("Store", BubbleState.STORING)
        val emptyAction = SmartAction("Clear", BubbleState.EMPTY)
        
        assertEquals(BubbleState.REPLACE, replaceAction.action)
        assertEquals(BubbleState.APPEND, appendAction.action)
        assertEquals(BubbleState.PREPEND, prependAction.action)
        assertEquals(BubbleState.STORING, storingAction.action)
        assertEquals(BubbleState.EMPTY, emptyAction.action)
    }
    
    @Test
    fun `SmartAction with empty label`() {
        val action = SmartAction("", BubbleState.REPLACE)
        
        assertEquals("", action.label)
        assertEquals(BubbleState.REPLACE, action.action)
    }
    
    @Test
    fun `SmartAction with long label`() {
        val longLabel = "This is a very long action label that might be used for complex operations"
        val action = SmartAction(longLabel, BubbleState.APPEND)
        
        assertEquals(longLabel, action.label)
        assertEquals(BubbleState.APPEND, action.action)
    }
    
    @Test
    fun `SmartAction with special characters in label`() {
        val specialLabel = "Action with special chars: @#$%^&*()"
        val action = SmartAction(specialLabel, BubbleState.PREPEND)
        
        assertEquals(specialLabel, action.label)
        assertEquals(BubbleState.PREPEND, action.action)
    }
    
    @Test
    fun `SmartAction equality`() {
        val action1 = SmartAction("Test", BubbleState.REPLACE)
        val action2 = SmartAction("Test", BubbleState.REPLACE)
        val action3 = SmartAction("Different", BubbleState.REPLACE)
        val action4 = SmartAction("Test", BubbleState.APPEND)
        
        assertEquals(action1, action2)
        assert(action1 != action3)
        assert(action1 != action4)
    }
    
    @Test
    fun `SmartAction toString representation`() {
        val action = SmartAction("Test Action", BubbleState.REPLACE)
        val stringRep = action.toString()
        
        assert(stringRep.contains("Test Action"))
        assert(stringRep.contains("REPLACE"))
    }
}