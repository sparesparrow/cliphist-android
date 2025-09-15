package com.clipboardhistory.data.repository

import com.clipboardhistory.data.database.ClipboardItemDao
import com.clipboardhistory.data.database.ClipboardItemEntity
import com.clipboardhistory.data.encryption.EncryptionManager
import com.clipboardhistory.domain.model.ClipboardItem

import com.clipboardhistory.domain.model.ClipboardSettings
import com.clipboardhistory.domain.model.ContentType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.mockito.kotlin.verify
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for ClipboardRepositoryImpl.
 * 
 * This test class verifies the data layer operations and
 * encryption/decryption functionality.
 */
class ClipboardRepositoryImplTest {
    
    @get:Rule
    val mockitoRule: MockitoRule = MockitoJUnit.rule()

    @Mock
    private lateinit var clipboardItemDao: ClipboardItemDao
    
    @Mock
    private lateinit var encryptionManager: EncryptionManager
    
    private lateinit var repository: ClipboardRepositoryImpl
    
    @Before
    fun setup() {
        repository = ClipboardRepositoryImpl(clipboardItemDao, encryptionManager)
    }
    
    @Test
    fun `getAllItems returns mapped items correctly`() = runTest {
        val testEntities = listOf(
            createTestEntity("Test content 1"),
            createTestEntity("Test content 2")
        )
        
        whenever(clipboardItemDao.getAllItems()).thenReturn(flowOf(testEntities))
        whenever(encryptionManager.decrypt("Test content 1")).thenReturn("Test content 1")
        whenever(encryptionManager.decrypt("Test content 2")).thenReturn("Test content 2")
        
        val result = repository.getAllItems().first()
        
        assertEquals(2, result.size)
        assertEquals("Test content 1", result[0].content)
        assertEquals("Test content 2", result[1].content)
    }
    
    @Test
    fun `insertItem encrypts and stores item correctly`() = runTest {
        val testItem = createTestItem("Test content")
        
        whenever(encryptionManager.encrypt("Test content")).thenReturn("encrypted_content")
        
        repository.insertItem(testItem)
        
        verify(encryptionManager).encrypt("Test content")
        val entityCaptor = argumentCaptor<ClipboardItemEntity>()
        verify(clipboardItemDao).insertItem(entityCaptor.capture())
        val captured = entityCaptor.firstValue
        assertEquals(testItem.id, captured.id)
        assertEquals("encrypted_content", captured.content)
        assertEquals(testItem.timestamp, captured.timestamp)
        assertEquals(testItem.contentType, captured.contentType)
        assertEquals(testItem.isEncrypted, captured.isEncrypted)
        assertEquals(testItem.size, captured.size)
    }
    
    @Test
    fun `getSettings returns correct settings`() = runTest {
        whenever(encryptionManager.getSecureString("max_history_size", "100")).thenReturn("200")
        whenever(encryptionManager.getSecureString("auto_delete_hours", "24")).thenReturn("48")
        whenever(encryptionManager.getSecureString("enable_encryption", "true")).thenReturn("true")
        whenever(encryptionManager.getSecureString("bubble_size", "3")).thenReturn("4")
        whenever(encryptionManager.getSecureString("bubble_opacity", "0.8")).thenReturn("0.9")

        
        val result = repository.getSettings()
        
        assertEquals(200, result.maxHistorySize)
        assertEquals(48, result.autoDeleteAfterHours)
        assertEquals(true, result.enableEncryption)
        assertEquals(4, result.bubbleSize)
        assertEquals(0.9f, result.bubbleOpacity)
    }
    
    @Test
    fun `updateSettings stores settings correctly`() = runTest {
        val testSettings = ClipboardSettings(
            maxHistorySize = 150,
            autoDeleteAfterHours = 72,
            enableEncryption = false,
            bubbleSize = 5,
            bubbleOpacity = 0.5f
        )
        
        repository.updateSettings(testSettings)
        
        verify(encryptionManager).storeSecureString("max_history_size", "150")
        verify(encryptionManager).storeSecureString("auto_delete_hours", "72")
        verify(encryptionManager).storeSecureString("enable_encryption", "false")
        verify(encryptionManager).storeSecureString("bubble_size", "5")
        verify(encryptionManager).storeSecureString("bubble_opacity", "0.5")
        verify(encryptionManager).storeSecureString("clipboard_mode", "EXTEND")
    }
    
    @Test
    fun `deleteItemsOlderThan calls dao with correct timestamp`() = runTest {
        val hours = 24
        val expectedThreshold = System.currentTimeMillis() - (hours * 60 * 60 * 1000)
        
        repository.deleteItemsOlderThan(hours)
        
        val tsCaptor = argumentCaptor<Long>()
        verify(clipboardItemDao).deleteItemsOlderThan(tsCaptor.capture())
        assertTrue(kotlin.math.abs(tsCaptor.firstValue - expectedThreshold) < 2000)
    }
    
    private fun createTestEntity(content: String): ClipboardItemEntity {
        return ClipboardItemEntity(
            id = "test-id",
            content = content,
            timestamp = System.currentTimeMillis(),
            contentType = ContentType.TEXT,
            isEncrypted = true,
            size = content.length
        )
    }
    
    private fun createTestItem(content: String): ClipboardItem {
        return ClipboardItem(
            id = "test-id",
            content = content,
            timestamp = System.currentTimeMillis(),
            contentType = ContentType.TEXT,
            isEncrypted = true,
            size = content.length
        )
    }
}