package com.clipboardhistory

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * Test utilities for coroutine testing
 */
@ExperimentalCoroutinesApi
class MainDispatcherRule(
    private val testDispatcher: TestDispatcher = StandardTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) {
        super.starting(description)
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        super.finished(description)
        Dispatchers.resetMain()
    }
}

/**
 * Test data generators
 */
object TestDataGenerator {
    fun generateClipboardItem(
        id: String = "1",
        content: String = "Test content",
        timestamp: Long = System.currentTimeMillis(),
        contentType: com.clipboardhistory.domain.model.ContentType = com.clipboardhistory.domain.model.ContentType.TEXT,
        isEncrypted: Boolean = false,
        size: Int = content.length,
    ) = com.clipboardhistory.domain.model.ClipboardItem(
        id = id,
        content = content,
        timestamp = timestamp,
        contentType = contentType,
        isEncrypted = isEncrypted,
        size = size,
    )

    fun generateSmartAction(
        label: String = "Copy",
        action: com.clipboardhistory.domain.model.BubbleState = com.clipboardhistory.domain.model.BubbleState.REPLACE,
    ) = com.clipboardhistory.domain.model.SmartAction(
        label = label,
        action = action,
    )
}
