package com.clipboardhistory.data.encryption

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.test.core.app.ApplicationProvider
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Unit tests for EncryptionManager.
 * 
 * This test class verifies the encryption and decryption functionality
 * using Robolectric for Android context.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28]) // Use SDK 28 for compatibility
class EncryptionManagerTest {
    
    private lateinit var context: Context
    private lateinit var encryptionManager: EncryptionManager
    
    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        encryptionManager = EncryptionManager(context)
    }
    
    @Test
    fun `encrypt returns non-null result for valid input`() {
        val plaintext = "Test clipboard content"
        
        val encrypted = encryptionManager.encrypt(plaintext)
        
        assertNotNull(encrypted)
        // Encrypted text should be different from plaintext
        kotlin.test.assertNotEquals(plaintext, encrypted)
    }
    
    @Test
    fun `decrypt returns original text for valid encrypted input`() {
        val plaintext = "Test clipboard content"
        
        val encrypted = encryptionManager.encrypt(plaintext)
        assertNotNull(encrypted)
        
        val decrypted = encryptionManager.decrypt(encrypted)
        
        assertEquals(plaintext, decrypted)
    }
    
    @Test
    fun `encrypt and decrypt handle empty string correctly`() {
        val plaintext = ""
        
        val encrypted = encryptionManager.encrypt(plaintext)
        assertNotNull(encrypted)
        
        val decrypted = encryptionManager.decrypt(encrypted)
        assertEquals(plaintext, decrypted)
    }
    
    @Test
    fun `encrypt and decrypt handle long text correctly`() {
        val plaintext = "A".repeat(10000) // 10KB of text
        
        val encrypted = encryptionManager.encrypt(plaintext)
        assertNotNull(encrypted)
        
        val decrypted = encryptionManager.decrypt(encrypted)
        assertEquals(plaintext, decrypted)
    }
    
    @Test
    fun `decrypt returns null for invalid encrypted input`() {
        val invalidEncrypted = "invalid_encrypted_data"
        
        val decrypted = encryptionManager.decrypt(invalidEncrypted)
        
        assertNull(decrypted)
    }
    
    @Test
    fun `storeSecureString and getSecureString work correctly`() {
        val key = "test_key"
        val value = "test_value"
        
        encryptionManager.storeSecureString(key, value)
        val retrieved = encryptionManager.getSecureString(key)
        
        assertEquals(value, retrieved)
    }
    
    @Test
    fun `getSecureString returns default value for non-existent key`() {
        val key = "non_existent_key"
        val defaultValue = "default_value"
        
        val result = encryptionManager.getSecureString(key, defaultValue)
        
        assertEquals(defaultValue, result)
    }
}