package com.clipboardhistory.data.encryption

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@RunWith(AndroidJUnit4::class)
class EncryptionManagerInstrumentedTest {

    private lateinit var context: Context
    private lateinit var encryptionManager: EncryptionManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        encryptionManager = EncryptionManager(context)
    }

    @Test
    fun encrypt_returns_non_null_result_for_valid_input() {
        val plaintext = "Test clipboard content"

        val encrypted = encryptionManager.encrypt(plaintext)
        assertNotNull(encrypted)
        kotlin.test.assertNotEquals(plaintext, encrypted)
    }

    @Test
    fun decrypt_returns_original_text_for_valid_encrypted_input() {
        val plaintext = "Test clipboard content"

        val encrypted = encryptionManager.encrypt(plaintext)
        assertNotNull(encrypted)

        val decrypted = encryptionManager.decrypt(encrypted!!)
        assertEquals(plaintext, decrypted)
    }

    @Test
    fun encrypt_and_decrypt_handle_empty_string_correctly() {
        val plaintext = ""

        val encrypted = encryptionManager.encrypt(plaintext)
        assertNotNull(encrypted)

        val decrypted = encryptionManager.decrypt(encrypted!!)
        assertEquals(plaintext, decrypted)
    }

    @Test
    fun decrypt_returns_null_for_invalid_encrypted_input() {
        val invalidEncrypted = "invalid_encrypted_data"

        val decrypted = encryptionManager.decrypt(invalidEncrypted)
        assertNull(decrypted)
    }

    @Test
    fun storeSecureString_and_getSecureString_work_correctly() {
        val key = "test_key"
        val value = "test_value"

        encryptionManager.storeSecureString(key, value)
        val retrieved = encryptionManager.getSecureString(key)

        assertEquals(value, retrieved)
    }

    @Test
    fun getSecureString_returns_default_value_for_non_existent_key() {
        val key = "non_existent_key"
        val defaultValue = "default_value"

        val result = encryptionManager.getSecureString(key, defaultValue)
        assertEquals(defaultValue, result)
    }
}
