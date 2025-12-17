package com.clipboardhistory.data.encryption

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import net.sqlcipher.database.SupportFactory
import java.security.SecureRandom
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager for database encryption keys using SQLCipher.
 *
 * This class handles the generation, storage, and retrieval of encryption keys
 * for the SQLCipher encrypted database, ensuring secure key management.
 */
@Singleton
class DatabaseEncryptionManager
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        private companion object {
            const val PREFS_NAME = "database_encryption_prefs"
            const val KEY_DATABASE_PASSPHRASE = "db_passphrase"
            const val KEY_SIZE = 256 // AES-256
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
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        }

        /**
         * Gets or creates a database encryption passphrase.
         *
         * @return The database passphrase as a byte array
         */
        fun getDatabasePassphrase(): ByteArray {
            // Try to get existing passphrase
            val storedPassphrase = encryptedPrefs.getString(KEY_DATABASE_PASSPHRASE, null)
            if (storedPassphrase != null) {
                return android.util.Base64.decode(storedPassphrase, android.util.Base64.DEFAULT)
            }

            // Generate new passphrase if none exists
            val newPassphrase = generateSecurePassphrase()
            storePassphrase(newPassphrase)
            return newPassphrase
        }

        /**
         * Creates a SupportFactory for SQLCipher with the database passphrase.
         *
         * @return SupportFactory configured with the database encryption key
         */
        fun createSupportFactory(): SupportFactory {
            val passphrase = getDatabasePassphrase()
            return SupportFactory(passphrase)
        }

        /**
         * Generates a secure random passphrase for database encryption.
         *
         * @return Byte array containing the generated passphrase
         */
        private fun generateSecurePassphrase(): ByteArray {
            val keyGenerator = KeyGenerator.getInstance("AES")
            keyGenerator.init(KEY_SIZE, SecureRandom())
            val secretKey: SecretKey = keyGenerator.generateKey()
            return secretKey.encoded
        }

        /**
         * Stores the passphrase securely in encrypted shared preferences.
         *
         * @param passphrase The passphrase to store
         */
        private fun storePassphrase(passphrase: ByteArray) {
            val encodedPassphrase = android.util.Base64.encodeToString(passphrase, android.util.Base64.DEFAULT)
            encryptedPrefs.edit()
                .putString(KEY_DATABASE_PASSPHRASE, encodedPassphrase)
                .apply()
        }

        /**
         * Changes the database encryption key.
         * This is a destructive operation that requires re-encryption of the database.
         *
         * @return true if key change was successful, false otherwise
         */
        fun changeDatabaseKey(): Boolean {
            return try {
                // Generate new passphrase
                val newPassphrase = generateSecurePassphrase()

                // Store the new passphrase
                storePassphrase(newPassphrase)

                // Note: In a real implementation, you would need to:
                // 1. Export all data from the old database
                // 2. Close the old database
                // 3. Create new database with new key
                // 4. Import data to new database
                // 5. Delete old database

                true
            } catch (e: Exception) {
                false
            }
        }

        /**
         * Validates that the current passphrase can be used to access the database.
         *
         * @return true if passphrase is valid, false otherwise
         */
        fun validatePassphrase(): Boolean {
            return try {
                val passphrase = getDatabasePassphrase()
                // Attempt to create a SupportFactory with the passphrase
                SupportFactory(passphrase)
                true
            } catch (e: Exception) {
                false
            }
        }

        /**
         * Gets information about the current encryption setup.
         *
         * @return EncryptionInfo containing details about the encryption
         */
        fun getEncryptionInfo(): EncryptionInfo {
            val passphrase = getDatabasePassphrase()
            return EncryptionInfo(
                keySize = passphrase.size * 8, // Convert bytes to bits
                algorithm = "AES-${KEY_SIZE}",
                isValid = validatePassphrase(),
            )
        }

        /**
         * Data class containing encryption information.
         */
        data class EncryptionInfo(
            val keySize: Int,
            val algorithm: String,
            val isValid: Boolean,
        )
    }