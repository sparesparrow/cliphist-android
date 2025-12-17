package com.clipboardhistory.data.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.clipboardhistory.domain.model.ContentType

/**
 * Room database entity for clipboard items.
 *
 * This entity represents the database table structure for storing
 * clipboard history items with encryption support.
 */
@Entity(tableName = "clipboard_items")
data class ClipboardItemEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "content")
    val content: String,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long,

    @ColumnInfo(name = "content_type")
    val contentType: ContentType,

    @ColumnInfo(name = "is_encrypted")
    val isEncrypted: Boolean,

    @ColumnInfo(name = "size")
    val size: Int,

    @ColumnInfo(name = "is_deleted")
    val isDeleted: Boolean = false,

    @ColumnInfo(name = "is_favorite")
    val isFavorite: Boolean = false,

    @ColumnInfo(name = "source_app")
    val sourceApp: String? = null,

    @ColumnInfo(name = "encryption_key")
    val encryptionKey: String? = null,
)
