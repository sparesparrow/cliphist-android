package com.clipboardhistory.domain.model

/**
 * A generic result wrapper for handling success and error states.
 *
 * This sealed class provides a type-safe way to handle operations that can
 * either succeed with data or fail with an error message and optional exception.
 *
 * Usage example:
 * ```kotlin
 * fun addItem(content: String): ClipboardResult<ClipboardItem> {
 *     return try {
 *         val item = repository.add(content)
 *         ClipboardResult.Success(item)
 *     } catch (e: Exception) {
 *         ClipboardResult.Error("Failed to add item", e)
 *     }
 * }
 *
 * when (val result = addItem("text")) {
 *     is ClipboardResult.Success -> println("Added: ${result.data}")
 *     is ClipboardResult.Error -> println("Error: ${result.message}")
 * }
 * ```
 *
 * @param T The type of data on success
 */
sealed class ClipboardResult<out T> {
    /**
     * Represents a successful operation with data.
     *
     * @property data The result data
     */
    data class Success<T>(val data: T) : ClipboardResult<T>()

    /**
     * Represents a failed operation with an error message.
     *
     * @property message Human-readable error message
     * @property exception Optional exception that caused the error
     */
    data class Error(
        val message: String,
        val exception: Exception? = null,
    ) : ClipboardResult<Nothing>()

    /**
     * Returns true if this result is a success.
     */
    val isSuccess: Boolean
        get() = this is Success

    /**
     * Returns true if this result is an error.
     */
    val isError: Boolean
        get() = this is Error

    /**
     * Returns the data if successful, or null if error.
     */
    fun getOrNull(): T? =
        when (this) {
            is Success -> data
            is Error -> null
        }

    /**
     * Returns the data if successful, or the default value if error.
     *
     * @param default The default value to return on error
     * @return The data or default value
     */
    fun getOrDefault(default: @UnsafeVariance T): T =
        when (this) {
            is Success -> data
            is Error -> default
        }

    /**
     * Returns the data if successful, or throws the exception if error.
     *
     * @throws IllegalStateException if the result is an error without exception
     * @throws Exception if the result is an error with exception
     * @return The data
     */
    fun getOrThrow(): T =
        when (this) {
            is Success -> data
            is Error -> throw exception ?: IllegalStateException(message)
        }

    /**
     * Transforms the success data using the given mapper function.
     *
     * @param mapper The function to transform the data
     * @return A new result with transformed data or the same error
     */
    inline fun <R> map(mapper: (T) -> R): ClipboardResult<R> =
        when (this) {
            is Success -> Success(mapper(data))
            is Error -> this
        }

    /**
     * Transforms the success data using the given mapper function that returns a result.
     *
     * @param mapper The function to transform the data
     * @return The result from the mapper or the same error
     */
    inline fun <R> flatMap(mapper: (T) -> ClipboardResult<R>): ClipboardResult<R> =
        when (this) {
            is Success -> mapper(data)
            is Error -> this
        }

    /**
     * Executes the given action if the result is successful.
     *
     * @param action The action to execute with the data
     * @return This result for chaining
     */
    inline fun onSuccess(action: (T) -> Unit): ClipboardResult<T> {
        if (this is Success) {
            action(data)
        }
        return this
    }

    /**
     * Executes the given action if the result is an error.
     *
     * @param action The action to execute with the error message and exception
     * @return This result for chaining
     */
    inline fun onError(action: (message: String, exception: Exception?) -> Unit): ClipboardResult<T> {
        if (this is Error) {
            action(message, exception)
        }
        return this
    }

    /**
     * Executes the given action on the result, regardless of success or error.
     *
     * @param action The action to execute
     * @return This result for chaining
     */
    inline fun onFinally(action: () -> Unit): ClipboardResult<T> {
        action()
        return this
    }

    companion object {
        /**
         * Creates a success result from the given data.
         *
         * @param data The success data
         * @return A success result
         */
        fun <T> success(data: T): ClipboardResult<T> = Success(data)

        /**
         * Creates an error result with the given message.
         *
         * @param message The error message
         * @param exception Optional exception
         * @return An error result
         */
        fun <T> error(
            message: String,
            exception: Exception? = null,
        ): ClipboardResult<T> = Error(message, exception)

        /**
         * Wraps a block of code in a try-catch and returns a result.
         *
         * @param block The code block to execute
         * @return Success with the block result or Error with the exception
         */
        inline fun <T> runCatching(block: () -> T): ClipboardResult<T> =
            try {
                Success(block())
            } catch (e: Exception) {
                Error(e.message ?: "Unknown error", e)
            }
    }
}
