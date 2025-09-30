package com.clipboardhistory.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import net.sqlcipher.database.SupportFactory

/**
 * Main Room database class for the Clipboard History application.
 *
 * This class provides the main database interface and handles
 * database creation, encryption, and access to DAOs.
 */
@Database(
    entities = [ClipboardItemEntity::class],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class ClipboardDatabase : RoomDatabase() {
    /**
     * Provides access to the clipboard items DAO.
     *
     * @return The ClipboardItemDao instance
     */
    abstract fun clipboardItemDao(): ClipboardItemDao

    companion object {
        private const val DATABASE_NAME = "clipboard_history.db"
        private const val DATABASE_PASSPHRASE = "clipboard_secure_key_2024"

        /**
         * Creates a new instance of the database with encryption.
         *
         * @param context The application context
         * @return A new database instance
         */
        fun create(context: Context): ClipboardDatabase {
            val supportFactory = SupportFactory(DATABASE_PASSPHRASE.toByteArray())

            return Room.databaseBuilder(
                context,
                ClipboardDatabase::class.java,
                DATABASE_NAME,
            )
                .openHelperFactory(supportFactory)
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}
