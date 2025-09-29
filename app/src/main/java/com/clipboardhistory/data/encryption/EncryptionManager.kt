package com.clipboardhistory.data.encryption

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
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
    @ApplicationContext private val context: Context,
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
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
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
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
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
}
