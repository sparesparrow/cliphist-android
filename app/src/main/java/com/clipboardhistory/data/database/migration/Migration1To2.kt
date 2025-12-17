package com.clipboardhistory.data.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration from database version 1 to version 2.
 *
 * This migration recreates the clipboard_items table with the new schema
 * that includes source_app, is_favorite, is_deleted, and encryption_key fields.
 */
class Migration1To2 : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Create new table with updated schema
        database.execSQL("""
            CREATE TABLE clipboard_items_new (
                id TEXT PRIMARY KEY NOT NULL,
                content TEXT NOT NULL,
                content_type TEXT NOT NULL,
                timestamp INTEGER NOT NULL,
                is_encrypted INTEGER NOT NULL DEFAULT 0,
                size INTEGER NOT NULL,
                source_app TEXT,
                is_favorite INTEGER NOT NULL DEFAULT 0,
                is_deleted INTEGER NOT NULL DEFAULT 0,
                encryption_key TEXT
            )
        """.trimIndent())

        // Migrate data from old table to new table
        // Note: Old table had different structure, so we map what we can
        database.execSQL("""
            INSERT INTO clipboard_items_new (id, content, content_type, timestamp, is_encrypted, size, source_app, is_favorite, is_deleted, encryption_key)
            SELECT id, content, content_type, timestamp, is_encrypted, size, NULL, 0, 0, NULL
            FROM clipboard_items
        """.trimIndent())

        // Drop old table
        database.execSQL("DROP TABLE clipboard_items")

        // Rename new table to replace old table
        database.execSQL("ALTER TABLE clipboard_items_new RENAME TO clipboard_items")

        // Create indexes for better performance
        database.execSQL("CREATE INDEX IF NOT EXISTS index_clipboard_items_timestamp ON clipboard_items(timestamp)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_clipboard_items_content_type ON clipboard_items(content_type)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_clipboard_items_is_favorite ON clipboard_items(is_favorite)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_clipboard_items_is_deleted ON clipboard_items(is_deleted)")
    }
}