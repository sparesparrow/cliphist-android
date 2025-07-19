package com.clipboardhistory.data.database

import androidx.room.TypeConverter
import com.clipboardhistory.domain.model.ContentType

/**
 * Room type converters for custom data types.
 * 
 * These converters handle the conversion between custom types
 * and types that Room can persist in the database.
 */
class Converters {
    
    /**
     * Converts ContentType enum to String for database storage.
     * 
     * @param contentType The ContentType enum value
     * @return The string representation of the content type
     */
    @TypeConverter
    fun fromContentType(contentType: ContentType): String {
        return contentType.name
    }
    
    /**
     * Converts String to ContentType enum from database.
     * 
     * @param contentType The string representation of the content type
     * @return The ContentType enum value
     */
    @TypeConverter
    fun toContentType(contentType: String): ContentType {
        return ContentType.valueOf(contentType)
    }
}