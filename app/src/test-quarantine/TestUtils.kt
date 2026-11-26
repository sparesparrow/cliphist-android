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
        id: Long = 1L,
        content: String = "Test content",
        timestamp: Long = System.currentTimeMillis(),
        isEncrypted: Boolean = false,
    ) = com.clipboardhistory.domain.model.ClipboardItem(
        id = id,
        content = content,
        timestamp = timestamp,
        isEncrypted = isEncrypted,
    )

    fun generateSmartAction(
        type: String = "copy",
        label: String = "Copy",
        icon: String = "content_copy",
    ) = com.clipboardhistory.domain.model.SmartAction(
        type = type,
        label = label,
        icon = icon,
    )
}
