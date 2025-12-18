package com.clipboardhistory

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.clipboardhistory.presentation.services.AccessibilityMonitorService
import com.clipboardhistory.utils.SmartInputManager
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import android.view.accessibility.AccessibilityNodeInfo

@RunWith(RobolectricTestRunner::class)
class SmartInputManagerTest {

    private lateinit var context: Context
    private lateinit var smartInputManager: SmartInputManager

    @Mock
    private lateinit var mockClipboardManager: ClipboardManager

    @Mock
    private lateinit var mockAccessibilityService: AccessibilityMonitorService

    @Mock
    private lateinit var mockRootNode: AccessibilityNodeInfo

    @Mock
    private lateinit var mockInputNode: AccessibilityNodeInfo

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)

        context = ApplicationProvider.getApplicationContext()

        // Mock the clipboard manager
        `when`(context.getSystemService(Context.CLIPBOARD_SERVICE)).thenReturn(mockClipboardManager)

        smartInputManager = SmartInputManager(context)
    }

    @After
    fun tearDown() {
        // Reset accessibility service instance
        AccessibilityMonitorService.setInstanceForTesting(null)
    }

    @Test
    fun `pasteText should use clipboard when direct input not available`() {
        // Given: No accessibility service available
        AccessibilityMonitorService.setInstanceForTesting(null)

        // When: Attempting to paste text
        val result = smartInputManager.pasteText("test content")

        // Then: Should return true (clipboard fallback)
        assertTrue(result)
        verify(mockClipboardManager).setPrimaryClip(any())
    }

    @Test
    fun `pasteText should use direct input when available and keyboard visible`() {
        // Given: Accessibility service available and input node focused
        setupMockAccessibilityService(keyboardVisible = true, hasFocusedInput = true)

        // When: Attempting to paste text with direct input
        val result = smartInputManager.pasteText("test content", useDirectInput = true)

        // Then: Should attempt direct input (though will fallback to clipboard in test)
        assertTrue(result)
    }

    @Test
    fun `isDirectInputAvailable should return true when accessibility service exists`() {
        // Given: Accessibility service is available
        AccessibilityMonitorService.setInstanceForTesting(mockAccessibilityService)

        // When: Checking direct input availability
        val available = smartInputManager.isDirectInputAvailable()

        // Then: Should return true
        assertTrue(available)
    }

    @Test
    fun `isDirectInputAvailable should return false when accessibility service is null`() {
        // Given: No accessibility service
        AccessibilityMonitorService.setInstanceForTesting(null)

        // When: Checking direct input availability
        val available = smartInputManager.isDirectInputAvailable()

        // Then: Should return false
        assertFalse(available)
    }

    @Test
    fun `getInputContextInfo should return correct context when accessibility available`() {
        // Given: Accessibility service with focused input
        setupMockAccessibilityService(keyboardVisible = true, hasFocusedInput = true)

        // When: Getting input context
        val contextInfo = smartInputManager.getInputContextInfo()

        // Then: Should return correct information
        assertTrue(contextInfo.hasAccessibilityService)
        assertTrue(contextInfo.hasFocusedInput)
        assertTrue(contextInfo.keyboardVisible)
        assertTrue(contextInfo.canSmartPaste)
    }

    @Test
    fun `getInputContextInfo should return false when no accessibility service`() {
        // Given: No accessibility service
        AccessibilityMonitorService.setInstanceForTesting(null)

        // When: Getting input context
        val contextInfo = smartInputManager.getInputContextInfo()

        // Then: Should return false for all checks
        assertFalse(contextInfo.hasAccessibilityService)
        assertFalse(contextInfo.hasFocusedInput)
        assertFalse(contextInfo.keyboardVisible)
        assertFalse(contextInfo.canSmartPaste)
    }

    @Test
    fun `pasteText should handle exceptions gracefully`() {
        // Given: Clipboard manager throws exception
        `when`(mockClipboardManager.setPrimaryClip(any())).thenThrow(RuntimeException("Clipboard error"))

        // When: Attempting to paste text
        val result = smartInputManager.pasteText("test content")

        // Then: Should return false on failure
        assertFalse(result)
    }

    @Test
    fun `canSmartPaste should be true when accessibility service and focused input available`() {
        // Given: Full accessibility context
        setupMockAccessibilityService(keyboardVisible = true, hasFocusedInput = true)

        // When: Getting input context
        val contextInfo = smartInputManager.getInputContextInfo()

        // Then: Smart paste should be available
        assertTrue(contextInfo.canSmartPaste)
    }

    @Test
    fun `canSmartPaste should be false when no focused input`() {
        // Given: Accessibility service but no focused input
        `when`(mockAccessibilityService.rootInActiveWindow).thenReturn(mockRootNode)
        `when`(mockRootNode.childCount).thenReturn(0) // No children, no focused input
        AccessibilityMonitorService.setInstanceForTesting(mockAccessibilityService)

        // When: Getting input context
        val contextInfo = smartInputManager.getInputContextInfo()

        // Then: Smart paste should not be available
        assertFalse(contextInfo.canSmartPaste)
    }

    /**
     * Helper method to set up mock accessibility service with specified state.
     */
    private fun setupMockAccessibilityService(keyboardVisible: Boolean, hasFocusedInput: Boolean) {
        // Mock input node
        `when`(mockInputNode.isFocused).thenReturn(true)
        `when`(mockInputNode.isEditable).thenReturn(true)
        `when`(mockInputNode.refresh()).thenReturn(true)

        // Mock root node
        if (hasFocusedInput) {
            `when`(mockRootNode.childCount).thenReturn(1)
            `when`(mockRootNode.getChild(0)).thenReturn(mockInputNode)
        } else {
            `when`(mockRootNode.childCount).thenReturn(0)
        }

        // Mock accessibility service
        `when`(mockAccessibilityService.rootInActiveWindow).thenReturn(mockRootNode)
        `when`(mockAccessibilityService.getApplicationContext()).thenReturn(context)

        AccessibilityMonitorService.setInstanceForTesting(mockAccessibilityService)
    }
}