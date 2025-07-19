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
    val size: Int
)