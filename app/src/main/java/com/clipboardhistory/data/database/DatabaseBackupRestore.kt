package com.clipboardhistory.data.database

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utility class for database backup and restore operations.
 *
 * This class provides functionality to backup the encrypted database
 * and restore it from backups, with proper error handling and security.
 */
class DatabaseBackupRestore(private val context: Context) {

    private val databaseName = "clipboard_history.db"
    private val backupDirectory = "clipboard_backups"

    /**
     * Create a backup of the current database.
     *
     * @return BackupResult indicating success or failure with details
     */
    suspend fun createBackup(): BackupResult = withContext(Dispatchers.IO) {
        try {
            val dbFile = context.getDatabasePath(databaseName)
            if (!dbFile.exists()) {
                return@withContext BackupResult.Failure("Database file does not exist")
            }

            val backupDir = File(context.filesDir, backupDirectory).apply { mkdirs() }
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val backupFile = File(backupDir, "clipboard_backup_$timestamp.db")

            // Copy database file
            FileInputStream(dbFile).use { input ->
                FileOutputStream(backupFile).use { output ->
                    input.copyTo(output)
                }
            }

            BackupResult.Success(backupFile.absolutePath)
        } catch (e: Exception) {
            BackupResult.Failure("Backup failed: ${e.message}")
        }
    }

    /**
     * Restore database from a backup file.
     *
     * @param backupPath Path to the backup file
     * @return BackupResult indicating success or failure with details
     */
    suspend fun restoreBackup(backupPath: String): BackupResult = withContext(Dispatchers.IO) {
        try {
            val backupFile = File(backupPath)
            if (!backupFile.exists()) {
                return@withContext BackupResult.Failure("Backup file does not exist: $backupPath")
            }

            val dbFile = context.getDatabasePath(databaseName)

            // Close any existing database connections before restore
            // Note: In a real implementation, you'd want to ensure the database is closed

            // Copy backup file to database location
            FileInputStream(backupFile).use { input ->
                FileOutputStream(dbFile).use { output ->
                    input.copyTo(output)
                }
            }

            BackupResult.Success("Database restored from $backupPath")
        } catch (e: Exception) {
            BackupResult.Failure("Restore failed: ${e.message}")
        }
    }

    /**
     * List all available backup files.
     *
     * @return List of backup file information
     */
    suspend fun listBackups(): List<BackupInfo> = withContext(Dispatchers.IO) {
        val backupDir = File(context.filesDir, backupDirectory)
        if (!backupDir.exists()) return@withContext emptyList()

        backupDir.listFiles { file ->
            file.isFile && file.name.startsWith("clipboard_backup_") && file.name.endsWith(".db")
        }?.map { file ->
            BackupInfo(
                path = file.absolutePath,
                name = file.name,
                size = file.length(),
                createdDate = Date(file.lastModified())
            )
        }?.sortedByDescending { it.createdDate } ?: emptyList()
    }

    /**
     * Delete a backup file.
     *
     * @param backupPath Path to the backup file to delete
     * @return true if deletion was successful, false otherwise
     */
    suspend fun deleteBackup(backupPath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            File(backupPath).delete()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Clean up old backups, keeping only the most recent ones.
     *
     * @param keepCount Number of most recent backups to keep
     * @return Number of backups deleted
     */
    suspend fun cleanupOldBackups(keepCount: Int = 5): Int = withContext(Dispatchers.IO) {
        val backups = listBackups()
        if (backups.size <= keepCount) return@withContext 0

        val backupsToDelete = backups.drop(keepCount)
        var deletedCount = 0

        backupsToDelete.forEach { backup ->
            if (deleteBackup(backup.path)) {
                deletedCount++
            }
        }

        deletedCount
    }

    /**
     * Result of backup or restore operations.
     */
    sealed class BackupResult {
        data class Success(val message: String) : BackupResult()
        data class Failure(val error: String) : BackupResult()
    }

    /**
     * Information about a backup file.
     */
    data class BackupInfo(
        val path: String,
        val name: String,
        val size: Long,
        val createdDate: Date
    )
}