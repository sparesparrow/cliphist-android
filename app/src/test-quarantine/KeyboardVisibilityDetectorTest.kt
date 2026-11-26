package com.clipboardhistory

import android.app.Activity
import android.graphics.Rect
import android.view.View
import android.view.ViewTreeObserver
import androidx.test.core.app.ApplicationProvider
import com.clipboardhistory.utils.KeyboardVisibilityDetector
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest

@RunWith(RobolectricTestRunner::class)
class KeyboardVisibilityDetectorTest {

    private lateinit var activity: Activity
    private lateinit var detector: KeyboardVisibilityDetector

    @Mock
    private lateinit var mockDecorView: View

    @Mock
    private lateinit var mockRootView: View

    @Mock
    private lateinit var mockViewTreeObserver: ViewTreeObserver

    private var globalLayoutListener: ViewTreeObserver.OnGlobalLayoutListener? = null

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)

        // Create a real activity for testing
        activity = Robolectric.buildActivity(Activity::class.java).create().start().resume().get()

        // Mock the decor view and root view
        `when`(activity.window.decorView).thenReturn(mockDecorView)
        `when`(mockDecorView.rootView).thenReturn(mockRootView)
        `when`(mockRootView.viewTreeObserver).thenReturn(mockViewTreeObserver)
        `when`(mockRootView.height).thenReturn(1920) // Mock screen height

        // Capture the global layout listener
        `when`(mockViewTreeObserver.addOnGlobalLayoutListener(any())).thenAnswer { invocation ->
            globalLayoutListener = invocation.getArgument(0)
            null
        }

        `when`(mockViewTreeObserver.removeOnGlobalLayoutListener(any())).thenAnswer { invocation ->
            if (globalLayoutListener == invocation.getArgument<ViewTreeObserver.OnGlobalLayoutListener>(0)) {
                globalLayoutListener = null
            }
            null
        }

        detector = KeyboardVisibilityDetector(activity)
    }

    @After
    fun tearDown() {
        detector.stopMonitoring()
    }

    @Test
    fun `initial state should have keyboard not visible`() = runTest {
        assertFalse(detector.isKeyboardVisible.first())
        assertEquals(0, detector.keyboardHeight.first())
    }

    @Test
    fun `keyboard should be detected as visible when height difference is significant`() = runTest {
        detector.startMonitoring()

        // Simulate keyboard showing (300px height difference)
        `when`(mockRootView.getWindowVisibleDisplayFrame(any<Rect>())).thenAnswer { invocation ->
            val rect = invocation.getArgument<Rect>(0)
            rect.set(0, 0, 1080, 1620) // Visible area is smaller
            null
        }

        // Trigger layout change
        globalLayoutListener?.onGlobalLayout()

        // Verify keyboard is detected as visible
        assertTrue(detector.isKeyboardVisible.first())
        assertEquals(300, detector.keyboardHeight.first())
    }

    @Test
    fun `keyboard should not be detected when height difference is below threshold`() = runTest {
        detector.startMonitoring()

        // Simulate small height difference (50px - below threshold)
        `when`(mockRootView.getWindowVisibleDisplayFrame(any<Rect>())).thenAnswer { invocation ->
            val rect = invocation.getArgument<Rect>(0)
            rect.set(0, 0, 1080, 1870) // Only 50px difference
            null
        }

        // Trigger layout change
        globalLayoutListener?.onGlobalLayout()

        // Verify keyboard is not detected as visible
        assertFalse(detector.isKeyboardVisible.first())
        assertEquals(0, detector.keyboardHeight.first())
    }

    @Test
    fun `keyboard should be detected as hidden when no height difference`() = runTest {
        detector.startMonitoring()

        // First show keyboard
        `when`(mockRootView.getWindowVisibleDisplayFrame(any<Rect>())).thenAnswer { invocation ->
            val rect = invocation.getArgument<Rect>(0)
            rect.set(0, 0, 1080, 1620) // 300px difference
            null
        }
        globalLayoutListener?.onGlobalLayout()
        assertTrue(detector.isKeyboardVisible.first())

        // Then hide keyboard
        `when`(mockRootView.getWindowVisibleDisplayFrame(any<Rect>())).thenAnswer { invocation ->
            val rect = invocation.getArgument<Rect>(0)
            rect.set(0, 0, 1080, 1920) // Full height visible
            null
        }
        globalLayoutListener?.onGlobalLayout()

        // Verify keyboard is detected as hidden
        assertFalse(detector.isKeyboardVisible.first())
        assertEquals(0, detector.keyboardHeight.first())
    }

    @Test
    fun `getCurrentKeyboardState should return correct state`() {
        detector.startMonitoring()

        // Simulate keyboard visible
        `when`(mockRootView.getWindowVisibleDisplayFrame(any<Rect>())).thenAnswer { invocation ->
            val rect = invocation.getArgument<Rect>(0)
            rect.set(0, 0, 1080, 1620) // 300px difference
            null
        }
        globalLayoutListener?.onGlobalLayout()

        val state = detector.getCurrentKeyboardState()
        assertTrue(state.isVisible)
        assertEquals(300, state.height)
    }

    @Test
    fun `rapid layout changes should be debounced`() = runTest {
        detector.startMonitoring()

        // Simulate rapid keyboard show/hide changes
        `when`(mockRootView.getWindowVisibleDisplayFrame(any<Rect>())).thenAnswer { invocation ->
            val rect = invocation.getArgument<Rect>(0)
            rect.set(0, 0, 1080, 1620) // Show keyboard
            null
        }

        // Trigger multiple rapid changes
        globalLayoutListener?.onGlobalLayout()
        globalLayoutListener?.onGlobalLayout()
        globalLayoutListener?.onGlobalLayout()

        // Should still detect keyboard as visible (debounced)
        assertTrue(detector.isKeyboardVisible.first())
    }

    @Test
    fun `stopMonitoring should remove global layout listener`() {
        detector.startMonitoring()
        assertTrue(globalLayoutListener != null)

        detector.stopMonitoring()
        verify(mockViewTreeObserver).removeOnGlobalLayoutListener(globalLayoutListener)
    }

    @Test
    fun `exception during layout detection should not crash`() = runTest {
        detector.startMonitoring()

        // Simulate exception during layout detection
        `when`(mockRootView.getWindowVisibleDisplayFrame(any<Rect>())).thenThrow(RuntimeException("Test exception"))

        // Should not crash and maintain previous state
        globalLayoutListener?.onGlobalLayout()

        // State should remain unchanged (initially false)
        assertFalse(detector.isKeyboardVisible.first())
    }
}