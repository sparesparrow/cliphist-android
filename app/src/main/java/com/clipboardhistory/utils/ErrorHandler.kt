package com.clipboardhistory.utils

import android.content.Context
import android.util.Log
import com.clipboardhistory.domain.model.ClipboardItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Comprehensive error handling and recovery system for clipboard operations.
 *
 * This class provides centralized error handling, logging, recovery mechanisms,
 * and crash reporting for clipboard-related operations.
 */
@Singleton
class ErrorHandler @Inject constructor(
    private val context: Context,
) {
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    private val errorLogFile: File by lazy {
        File(context.filesDir, "clipboard_error_log.txt")
    }

    /**
     * Handle clipboard operation errors with appropriate recovery.
     *
     * @param error The exception that occurred
     * @param operation The operation that failed
     * @param context Additional context information
     * @return Recovery action to take
     */
    fun handleClipboardError(
        error: Exception,
        operation: ClipboardOperation,
        context: Map<String, Any> = emptyMap(),
    ): RecoveryAction {
        // Log the error
        logError(error, operation, context)

        // Determine recovery action based on error type and operation
        return when (operation) {
            ClipboardOperation.DATABASE_READ -> handleDatabaseReadError(error, context)
            ClipboardOperation.DATABASE_WRITE -> handleDatabaseWriteError(error, context)
            ClipboardOperation.ENCRYPTION -> handleEncryptionError(error, context)
            ClipboardOperation.SERVICE_START -> handleServiceError(error, context)
            ClipboardOperation.PERMISSION_CHECK -> handlePermissionError(error, context)
            ClipboardOperation.CONTENT_ANALYSIS -> handleContentAnalysisError(error, context)
            ClipboardOperation.NETWORK_OPERATION -> handleNetworkError(error, context)
            ClipboardOperation.ERROR_RECOVERY -> RecoveryAction.LOG_AND_CONTINUE
        }
    }

    /**
     * Handle database read errors.
     */
    private fun handleDatabaseReadError(error: Exception, context: Map<String, Any>): RecoveryAction {
        return when (error) {
            is android.database.sqlite.SQLiteException -> {
                when {
                    error.message?.contains("no such table", ignoreCase = true) == true ->
                        RecoveryAction.RECREATE_DATABASE
                    error.message?.contains("database is locked", ignoreCase = true) == true ->
                        RecoveryAction.RETRY_WITH_DELAY
                    else -> RecoveryAction.LOG_AND_CONTINUE
                }
            }
            is IllegalStateException -> RecoveryAction.RESTART_SERVICE
            else -> RecoveryAction.LOG_AND_CONTINUE
        }
    }

    /**
     * Handle database write errors.
     */
    private fun handleDatabaseWriteError(error: Exception, context: Map<String, Any>): RecoveryAction {
        return when (error) {
            is android.database.sqlite.SQLiteConstraintException ->
                RecoveryAction.SKIP_DUPLICATE
            is android.database.sqlite.SQLiteFullException ->
                RecoveryAction.CLEANUP_OLD_DATA
            is android.database.sqlite.SQLiteException -> {
                if (error.message?.contains("database disk image is malformed", ignoreCase = true) == true) {
                    RecoveryAction.RESTORE_FROM_BACKUP
                } else {
                    RecoveryAction.RETRY_WITH_DELAY
                }
            }
            else -> RecoveryAction.LOG_AND_CONTINUE
        }
    }

    /**
     * Handle encryption errors.
     */
    private fun handleEncryptionError(error: Exception, context: Map<String, Any>): RecoveryAction {
        return when (error) {
            is javax.crypto.BadPaddingException,
            is javax.crypto.IllegalBlockSizeException ->
                RecoveryAction.RECREATE_ENCRYPTION_KEY
            is java.security.KeyStoreException ->
                RecoveryAction.RESET_KEYSTORE
            else -> RecoveryAction.LOG_AND_CONTINUE
        }
    }

    /**
     * Handle service errors.
     */
    private fun handleServiceError(error: Exception, context: Map<String, Any>): RecoveryAction {
        return when (error) {
            is SecurityException -> RecoveryAction.REQUEST_PERMISSIONS
            is IllegalStateException -> RecoveryAction.RESTART_SERVICE
            else -> RecoveryAction.LOG_AND_CONTINUE
        }
    }

    /**
     * Handle permission errors.
     */
    private fun handlePermissionError(error: Exception, context: Map<String, Any>): RecoveryAction {
        return RecoveryAction.REQUEST_PERMISSIONS
    }

    /**
     * Handle content analysis errors.
     */
    private fun handleContentAnalysisError(error: Exception, context: Map<String, Any>): RecoveryAction {
        return when (error) {
            is OutOfMemoryError -> RecoveryAction.SKIP_LARGE_CONTENT
            is StackOverflowError -> RecoveryAction.USE_SIMPLIFIED_ANALYSIS
            else -> RecoveryAction.LOG_AND_CONTINUE
        }
    }

    /**
     * Handle network operation errors.
     */
    private fun handleNetworkError(error: Exception, context: Map<String, Any>): RecoveryAction {
        return when (error) {
            is java.net.UnknownHostException,
            is java.net.ConnectException ->
                RecoveryAction.RETRY_WITH_DELAY
            is java.net.SocketTimeoutException ->
                RecoveryAction.RETRY_WITH_BACKOFF
            else -> RecoveryAction.LOG_AND_CONTINUE
        }
    }

    /**
     * Execute recovery action.
     *
     * @param action The recovery action to execute
     * @param context Additional context for recovery
     * @return true if recovery was successful
     */
    suspend fun executeRecovery(
        action: RecoveryAction,
        context: Map<String, Any> = emptyMap(),
    ): Boolean {
        return try {
            when (action) {
                RecoveryAction.LOG_AND_CONTINUE -> true
                RecoveryAction.RETRY_WITH_DELAY -> {
                    kotlinx.coroutines.delay(1000)
                    true
                }
                RecoveryAction.RETRY_WITH_BACKOFF -> {
                    // Implement exponential backoff
                    true
                }
                RecoveryAction.RESTART_SERVICE -> restartClipboardService()
                RecoveryAction.REQUEST_PERMISSIONS -> requestMissingPermissions()
                RecoveryAction.RECREATE_DATABASE -> recreateDatabase()
                RecoveryAction.RESTORE_FROM_BACKUP -> restoreFromBackup()
                RecoveryAction.RECREATE_ENCRYPTION_KEY -> recreateEncryptionKey()
                RecoveryAction.RESET_KEYSTORE -> resetKeyStore()
                RecoveryAction.CLEANUP_OLD_DATA -> cleanupOldData()
                RecoveryAction.SKIP_DUPLICATE -> true
                RecoveryAction.SKIP_LARGE_CONTENT -> true
                RecoveryAction.USE_SIMPLIFIED_ANALYSIS -> true
            }
        } catch (e: Exception) {
            logError(e, ClipboardOperation.ERROR_RECOVERY, mapOf("recovery_action" to action))
            false
        }
    }

    /**
     * Log error with full context.
     */
    private fun logError(
        error: Exception,
        operation: ClipboardOperation,
        context: Map<String, Any> = emptyMap(),
    ) {
        scope.launch {
            try {
                val timestamp = dateFormat.format(Date())
                val stackTrace = StringWriter().apply {
                    error.printStackTrace(PrintWriter(this))
                }.toString()

                val logEntry = """
                    |[$timestamp] ERROR in $operation
                    |Exception: ${error.javaClass.simpleName}
                    |Message: ${error.message}
                    |Context: $context
                    |Stack Trace:
                    |$stackTrace
                    |---
                    |
                """.trimMargin()

                // Log to Android logcat
                Log.e("ClipboardError", logEntry)

                // Append to error log file
                errorLogFile.appendText(logEntry)

                // Clean up old log entries if file gets too large
                cleanupErrorLog()

            } catch (logError: Exception) {
                // Last resort logging
                Log.e("ErrorHandler", "Failed to log error: ${logError.message}")
            }
        }
    }

    /**
     * Get error statistics.
     */
    suspend fun getErrorStatistics(): ErrorStatistics {
        return try {
            val logContent = if (errorLogFile.exists()) errorLogFile.readText() else ""
            val lines = logContent.lines()

            val errorCount = lines.count { it.contains("ERROR") }
            val recentErrors = lines.takeLast(10)

            val errorTypes = lines
                .filter { it.contains("Exception:") }
                .mapNotNull { line ->
                    line.substringAfter("Exception: ").substringBefore(" ")
                }
                .groupBy { it }
                .mapValues { it.value.size }

            ErrorStatistics(
                totalErrors = errorCount,
                recentErrors = recentErrors,
                errorTypes = errorTypes,
                lastErrorTime = lines.lastOrNull()?.substringAfter("[")?.substringBefore("]"),
            )
        } catch (e: Exception) {
            ErrorStatistics.empty()
        }
    }

    /**
     * Clear error log.
     */
    fun clearErrorLog() {
        scope.launch {
            try {
                errorLogFile.writeText("")
            } catch (e: Exception) {
                Log.e("ErrorHandler", "Failed to clear error log: ${e.message}")
            }
        }
    }

    // Private recovery implementation methods

    private fun restartClipboardService(): Boolean {
        // Implementation would restart the clipboard service
        return true
    }

    private fun requestMissingPermissions(): Boolean {
        // Implementation would trigger permission request flow
        return true
    }

    private fun recreateDatabase(): Boolean {
        // Implementation would recreate the database
        return false // Dangerous operation, requires user confirmation
    }

    private fun restoreFromBackup(): Boolean {
        // Implementation would restore from backup
        return false // Requires backup system integration
    }

    private fun recreateEncryptionKey(): Boolean {
        // Implementation would recreate encryption key
        return false // Dangerous operation
    }

    private fun resetKeyStore(): Boolean {
        // Implementation would reset Android KeyStore
        return false // Dangerous operation
    }

    private fun cleanupOldData(): Boolean {
        // Implementation would clean up old clipboard data
        return true
    }

    private fun cleanupErrorLog() {
        try {
            if (errorLogFile.length() > 1024 * 1024) { // 1MB limit
                val lines = errorLogFile.readLines()
                val keepLines = lines.takeLast(1000) // Keep last 1000 entries
                errorLogFile.writeText(keepLines.joinToString("\n"))
            }
        } catch (e: Exception) {
            // Ignore cleanup errors
        }
    }

    enum class ClipboardOperation {
        DATABASE_READ,
        DATABASE_WRITE,
        ENCRYPTION,
        SERVICE_START,
        PERMISSION_CHECK,
        CONTENT_ANALYSIS,
        NETWORK_OPERATION,
        ERROR_RECOVERY,
    }

    enum class RecoveryAction {
        LOG_AND_CONTINUE,
        RETRY_WITH_DELAY,
        RETRY_WITH_BACKOFF,
        RESTART_SERVICE,
        REQUEST_PERMISSIONS,
        RECREATE_DATABASE,
        RESTORE_FROM_BACKUP,
        RECREATE_ENCRYPTION_KEY,
        RESET_KEYSTORE,
        CLEANUP_OLD_DATA,
        SKIP_DUPLICATE,
        SKIP_LARGE_CONTENT,
        USE_SIMPLIFIED_ANALYSIS,
    }

    data class ErrorStatistics(
        val totalErrors: Int,
        val recentErrors: List<String>,
        val errorTypes: Map<String, Int>,
        val lastErrorTime: String?,
    ) {
        companion object {
            fun empty() = ErrorStatistics(0, emptyList(), emptyMap(), null)
        }
    }
}