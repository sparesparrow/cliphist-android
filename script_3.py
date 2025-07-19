# Create DAO interface
clipboard_dao = '''package com.clipboardhistory.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for clipboard items.
 * 
 * This interface defines the database operations for clipboard items
 * using Room's annotation-based query system.
 */
@Dao
interface ClipboardItemDao {
    
    /**
     * Get all clipboard items ordered by timestamp (newest first).
     * 
     * @return Flow of list of clipboard items
     */
    @Query("SELECT * FROM clipboard_items ORDER BY timestamp DESC")
    fun getAllItems(): Flow<List<ClipboardItemEntity>>
    
    /**
     * Get a specific clipboard item by ID.
     * 
     * @param id The ID of the clipboard item
     * @return The clipboard item entity or null if not found
     */
    @Query("SELECT * FROM clipboard_items WHERE id = :id")
    suspend fun getItemById(id: String): ClipboardItemEntity?
    
    /**
     * Insert a new clipboard item.
     * 
     * @param item The clipboard item to insert
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ClipboardItemEntity)
    
    /**
     * Insert multiple clipboard items.
     * 
     * @param items The clipboard items to insert
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<ClipboardItemEntity>)
    
    /**
     * Update an existing clipboard item.
     * 
     * @param item The clipboard item to update
     */
    @Update
    suspend fun updateItem(item: ClipboardItemEntity)
    
    /**
     * Delete a clipboard item.
     * 
     * @param item The clipboard item to delete
     */
    @Delete
    suspend fun deleteItem(item: ClipboardItemEntity)
    
    /**
     * Delete a clipboard item by ID.
     * 
     * @param id The ID of the clipboard item to delete
     */
    @Query("DELETE FROM clipboard_items WHERE id = :id")
    suspend fun deleteItemById(id: String)
    
    /**
     * Delete all clipboard items.
     */
    @Query("DELETE FROM clipboard_items")
    suspend fun deleteAllItems()
    
    /**
     * Delete items older than the specified timestamp.
     * 
     * @param timestamp The timestamp threshold
     */
    @Query("DELETE FROM clipboard_items WHERE timestamp < :timestamp")
    suspend fun deleteItemsOlderThan(timestamp: Long)
    
    /**
     * Get the count of clipboard items.
     * 
     * @return The total number of clipboard items
     */
    @Query("SELECT COUNT(*) FROM clipboard_items")
    suspend fun getItemCount(): Int
    
    /**
     * Get clipboard items with a limit (for pagination).
     * 
     * @param limit The maximum number of items to return
     * @param offset The offset for pagination
     * @return List of clipboard items
     */
    @Query("SELECT * FROM clipboard_items ORDER BY timestamp DESC LIMIT :limit OFFSET :offset")
    suspend fun getItemsWithPagination(limit: Int, offset: Int): List<ClipboardItemEntity>
}'''

# Create database class
clipboard_database = '''package com.clipboardhistory.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
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
    exportSchema = false
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
                DATABASE_NAME
            )
            .openHelperFactory(supportFactory)
            .fallbackToDestructiveMigration()
            .build()
        }
    }
}'''

# Create type converters
type_converters = '''package com.clipboardhistory.data.database

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
}'''

# Create encryption utility
encryption_util = '''package com.clipboardhistory.data.encryption

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.IvParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Utility class for encrypting and decrypting clipboard data.
 * 
 * This class uses Android's Security library and Keystore to provide
 * secure encryption for sensitive clipboard content.
 */
@Singleton
class EncryptionManager @Inject constructor(
    private val context: Context
) {
    
    private companion object {
        const val KEY_ALIAS = "clipboard_encryption_key"
        const val TRANSFORMATION = "AES/CBC/PKCS7Padding"
        const val KEYSTORE_TYPE = "AndroidKeyStore"
        const val PREFS_NAME = "encrypted_clipboard_prefs"
    }
    
    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance(KEYSTORE_TYPE).apply {
            load(null)
        }
    }
    
    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }
    
    private val encryptedPrefs by lazy {
        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
    
    init {
        generateKeyIfNotExists()
    }
    
    /**
     * Generates an encryption key if it doesn't exist.
     */
    private fun generateKeyIfNotExists() {
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_TYPE)
            val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                .build()
            
            keyGenerator.init(keyGenParameterSpec)
            keyGenerator.generateKey()
        }
    }
    
    /**
     * Encrypts the given text using AES encryption.
     * 
     * @param plaintext The text to encrypt
     * @return The encrypted text as a base64 string, or null if encryption fails
     */
    fun encrypt(plaintext: String): String? {
        try {
            val secretKey = keyStore.getKey(KEY_ALIAS, null)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            
            val encryptedBytes = cipher.doFinal(plaintext.toByteArray())
            val iv = cipher.iv
            
            // Combine IV and encrypted data
            val combined = iv + encryptedBytes
            return android.util.Base64.encodeToString(combined, android.util.Base64.DEFAULT)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
    
    /**
     * Decrypts the given encrypted text.
     * 
     * @param encryptedText The encrypted text as a base64 string
     * @return The decrypted text, or null if decryption fails
     */
    fun decrypt(encryptedText: String): String? {
        try {
            val combined = android.util.Base64.decode(encryptedText, android.util.Base64.DEFAULT)
            val iv = combined.sliceArray(0..15) // IV is 16 bytes for AES
            val encryptedBytes = combined.sliceArray(16 until combined.size)
            
            val secretKey = keyStore.getKey(KEY_ALIAS, null)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(iv))
            
            val decryptedBytes = cipher.doFinal(encryptedBytes)
            return String(decryptedBytes)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
    
    /**
     * Stores a key-value pair in encrypted preferences.
     * 
     * @param key The key to store
     * @param value The value to store
     */
    fun storeSecureString(key: String, value: String) {
        encryptedPrefs.edit().putString(key, value).apply()
    }
    
    /**
     * Retrieves a value from encrypted preferences.
     * 
     * @param key The key to retrieve
     * @param defaultValue The default value if key is not found
     * @return The stored value or default value
     */
    fun getSecureString(key: String, defaultValue: String = ""): String {
        return encryptedPrefs.getString(key, defaultValue) ?: defaultValue
    }
}'''

# Write the database files
with open('app/src/main/java/com/clipboardhistory/data/database/ClipboardItemDao.kt', 'w') as f:
    f.write(clipboard_dao)

with open('app/src/main/java/com/clipboardhistory/data/database/ClipboardDatabase.kt', 'w') as f:
    f.write(clipboard_database)

with open('app/src/main/java/com/clipboardhistory/data/database/Converters.kt', 'w') as f:
    f.write(type_converters)

with open('app/src/main/java/com/clipboardhistory/data/encryption/EncryptionManager.kt', 'w') as f:
    f.write(encryption_util)

print("Database and encryption files created!")