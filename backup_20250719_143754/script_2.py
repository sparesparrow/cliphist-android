# Create Android Manifest
android_manifest = '''<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <!-- Required permissions -->
    <uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

    <application
        android:name=".ClipboardHistoryApplication"
        android:allowBackup="true"
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:fullBackupContent="@xml/backup_rules"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.ClipboardHistory"
        tools:targetApi="31">

        <!-- Main Activity -->
        <activity
            android:name=".presentation.MainActivity"
            android:exported="true"
            android:label="@string/app_name"
            android:theme="@style/Theme.ClipboardHistory">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <!-- Clipboard Service -->
        <service
            android:name=".presentation.services.ClipboardService"
            android:enabled="true"
            android:exported="false"
            android:foregroundServiceType="specialUse">
            <property
                android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE"
                android:value="clipboard_monitoring" />
        </service>

        <!-- Floating Bubble Service -->
        <service
            android:name=".presentation.services.FloatingBubbleService"
            android:enabled="true"
            android:exported="false"
            android:foregroundServiceType="specialUse">
            <property
                android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE"
                android:value="floating_interface" />
        </service>

    </application>

</manifest>'''

# Create the main Application class
application_class = '''package com.clipboardhistory

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Main application class for the Clipboard History app.
 * 
 * This class serves as the entry point for the application and initializes
 * the Dagger Hilt dependency injection framework.
 */
@HiltAndroidApp
class ClipboardHistoryApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        // Initialize any global components here
    }
}'''

# Create data models
clipboard_item_model = '''package com.clipboardhistory.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Data model representing a clipboard item.
 * 
 * @property id Unique identifier for the clipboard item
 * @property content The text content of the clipboard item
 * @property timestamp When the item was created (in milliseconds)
 * @property contentType Type of the content (TEXT, IMAGE, etc.)
 * @property isEncrypted Whether the content is encrypted in storage
 * @property size Size of the content in bytes
 */
@Parcelize
data class ClipboardItem(
    val id: String,
    val content: String,
    val timestamp: Long,
    val contentType: ContentType,
    val isEncrypted: Boolean = true,
    val size: Int
) : Parcelable

/**
 * Enumeration of supported content types for clipboard items.
 */
enum class ContentType {
    TEXT,
    IMAGE,
    URL,
    FILE,
    OTHER
}

/**
 * Settings model for the clipboard history application.
 * 
 * @property maxHistorySize Maximum number of items to keep in history
 * @property autoDeleteAfterHours Automatically delete items after this many hours
 * @property enableEncryption Whether to encrypt clipboard data
 * @property bubbleSize Size of the floating bubbles (1-5 scale)
 * @property bubbleOpacity Opacity of the floating bubbles (0.1-1.0)
 * @property clipboardMode Current clipboard mode (REPLACE or EXTEND)
 */
data class ClipboardSettings(
    val maxHistorySize: Int = 100,
    val autoDeleteAfterHours: Int = 24,
    val enableEncryption: Boolean = true,
    val bubbleSize: Int = 3,
    val bubbleOpacity: Float = 0.8f,
    val clipboardMode: ClipboardMode = ClipboardMode.REPLACE
)

/**
 * Enumeration of clipboard operation modes.
 */
enum class ClipboardMode {
    REPLACE,
    EXTEND
}'''

# Create database entities
clipboard_entity = '''package com.clipboardhistory.data.database

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
)'''

# Write the files
with open('app/src/main/AndroidManifest.xml', 'w') as f:
    f.write(android_manifest)

with open('app/src/main/java/com/clipboardhistory/ClipboardHistoryApplication.kt', 'w') as f:
    f.write(application_class)

with open('app/src/main/java/com/clipboardhistory/domain/model/ClipboardItem.kt', 'w') as f:
    f.write(clipboard_item_model)

with open('app/src/main/java/com/clipboardhistory/data/database/ClipboardItemEntity.kt', 'w') as f:
    f.write(clipboard_entity)

print("Core application files created!")