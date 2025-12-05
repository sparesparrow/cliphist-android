package com.clipboardhistory.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for ClipboardResult.
 */
class ClipboardResultTest {
    @Test
    fun `Success result contains data`() {
        val result = ClipboardResult.Success("test data")

        assertTrue(result.isSuccess)
        assertFalse(result.isError)
        assertEquals("test data", result.data)
    }

    @Test
    fun `Error result contains message`() {
        val result = ClipboardResult.Error("error message")

        assertFalse(result.isSuccess)
        assertTrue(result.isError)
        assertEquals("error message", result.message)
        assertNull(result.exception)
    }

    @Test
    fun `Error result contains exception`() {
        val exception = RuntimeException("test exception")
        val result = ClipboardResult.Error("error message", exception)

        assertTrue(result.isError)
        assertEquals("error message", result.message)
        assertEquals(exception, result.exception)
    }

    @Test
    fun `getOrNull returns data on success`() {
        val result: ClipboardResult<String> = ClipboardResult.Success("test")

        assertEquals("test", result.getOrNull())
    }

    @Test
    fun `getOrNull returns null on error`() {
        val result: ClipboardResult<String> = ClipboardResult.Error("error")

        assertNull(result.getOrNull())
    }

    @Test
    fun `getOrDefault returns data on success`() {
        val result: ClipboardResult<String> = ClipboardResult.Success("test")

        assertEquals("test", result.getOrDefault("default"))
    }

    @Test
    fun `getOrDefault returns default on error`() {
        val result: ClipboardResult<String> = ClipboardResult.Error("error")

        assertEquals("default", result.getOrDefault("default"))
    }

    @Test
    fun `getOrThrow returns data on success`() {
        val result: ClipboardResult<String> = ClipboardResult.Success("test")

        assertEquals("test", result.getOrThrow())
    }

    @Test(expected = RuntimeException::class)
    fun `getOrThrow throws exception on error`() {
        val result: ClipboardResult<String> = ClipboardResult.Error("error", RuntimeException("test"))

        result.getOrThrow()
    }

    @Test
    fun `map transforms success data`() {
        val result: ClipboardResult<Int> = ClipboardResult.Success(5)

        val mapped = result.map { it * 2 }

        assertIs<ClipboardResult.Success<Int>>(mapped)
        assertEquals(10, mapped.data)
    }

    @Test
    fun `map preserves error`() {
        val result: ClipboardResult<Int> = ClipboardResult.Error("error")

        val mapped = result.map { it * 2 }

        assertIs<ClipboardResult.Error>(mapped)
        assertEquals("error", mapped.message)
    }

    @Test
    fun `flatMap chains success results`() {
        val result: ClipboardResult<Int> = ClipboardResult.Success(5)

        val flatMapped = result.flatMap { ClipboardResult.Success(it.toString()) }

        assertIs<ClipboardResult.Success<String>>(flatMapped)
        assertEquals("5", flatMapped.data)
    }

    @Test
    fun `flatMap preserves error`() {
        val result: ClipboardResult<Int> = ClipboardResult.Error("error")

        val flatMapped = result.flatMap { ClipboardResult.Success(it.toString()) }

        assertIs<ClipboardResult.Error>(flatMapped)
        assertEquals("error", flatMapped.message)
    }

    @Test
    fun `onSuccess executes action on success`() {
        var executed = false
        val result: ClipboardResult<String> = ClipboardResult.Success("test")

        result.onSuccess { executed = true }

        assertTrue(executed)
    }

    @Test
    fun `onSuccess does not execute action on error`() {
        var executed = false
        val result: ClipboardResult<String> = ClipboardResult.Error("error")

        result.onSuccess { executed = true }

        assertFalse(executed)
    }

    @Test
    fun `onError executes action on error`() {
        var executedMessage: String? = null
        val result: ClipboardResult<String> = ClipboardResult.Error("error")

        result.onError { message, _ -> executedMessage = message }

        assertEquals("error", executedMessage)
    }

    @Test
    fun `onError does not execute action on success`() {
        var executed = false
        val result: ClipboardResult<String> = ClipboardResult.Success("test")

        result.onError { _, _ -> executed = true }

        assertFalse(executed)
    }

    @Test
    fun `onFinally executes on success`() {
        var executed = false
        val result: ClipboardResult<String> = ClipboardResult.Success("test")

        result.onFinally { executed = true }

        assertTrue(executed)
    }

    @Test
    fun `onFinally executes on error`() {
        var executed = false
        val result: ClipboardResult<String> = ClipboardResult.Error("error")

        result.onFinally { executed = true }

        assertTrue(executed)
    }

    @Test
    fun `success companion function creates Success`() {
        val result = ClipboardResult.success("test")

        assertIs<ClipboardResult.Success<String>>(result)
        assertEquals("test", result.data)
    }

    @Test
    fun `error companion function creates Error`() {
        val result: ClipboardResult<String> = ClipboardResult.error("error")

        assertIs<ClipboardResult.Error>(result)
        assertEquals("error", result.message)
    }

    @Test
    fun `runCatching returns Success on successful block`() {
        val result = ClipboardResult.runCatching { "success" }

        assertIs<ClipboardResult.Success<String>>(result)
        assertEquals("success", result.data)
    }

    @Test
    fun `runCatching returns Error on exception`() {
        val result =
            ClipboardResult.runCatching<String> {
                throw RuntimeException("test exception")
            }

        assertIs<ClipboardResult.Error>(result)
        assertEquals("test exception", result.message)
        assertIs<RuntimeException>(result.exception)
    }

    @Test
    fun `chaining operations works correctly`() {
        val result: ClipboardResult<Int> =
            ClipboardResult.Success(5)
                .map { it * 2 }
                .flatMap { ClipboardResult.Success(it + 1) }
                .map { it.toString() }
                .flatMap { ClipboardResult.Success(it.length) }

        assertIs<ClipboardResult.Success<Int>>(result)
        assertEquals(2, result.data) // "11".length = 2
    }
}
