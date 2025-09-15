package com.clipboardhistory.data.repository

import com.clipboardhistory.data.database.ClipboardItemDao
import com.clipboardhistory.data.database.ClipboardItemEntity
import com.clipboardhistory.data.encryption.EncryptionManager
import com.clipboardhistory.domain.repository.SettingsRepository
import com.clipboardhistory.domain.model.ClipboardItem
import com.clipboardhistory.domain.model.ClipboardSettings
import com.clipboardhistory.domain.repository.ClipboardRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of ClipboardRepository interface.
 * 
 * This class provides concrete implementation of clipboard operations
 * with encryption support and database persistence.
 */
@Singleton
class ClipboardRepositoryImpl @Inject constructor(
    private val clipboardItemDao: ClipboardItemDao,
    private val encryptionManager: EncryptionManager,
    private val settingsRepository: SettingsRepository
) : ClipboardRepository {
    
    override fun getAllItems(): Flow<List<ClipboardItem>> {
        return clipboardItemDao.getAllItems().map { entities ->
            entities.map { entity ->
                mapEntityToItem(entity)
            }
        }
    }
    
    override suspend fun getItemById(id: String): ClipboardItem? {
        return clipboardItemDao.getItemById(id)?.let { entity ->
            mapEntityToItem(entity)
        }
    }
    
    override suspend fun insertItem(item: ClipboardItem) {
        val entity = mapItemToEntity(item)
        clipboardItemDao.insertItem(entity)
    }
    
    override suspend fun updateItem(item: ClipboardItem) {
        val entity = mapItemToEntity(item)
        clipboardItemDao.updateItem(entity)
    }
    
    override suspend fun deleteItem(item: ClipboardItem) {
        val entity = mapItemToEntity(item)
        clipboardItemDao.deleteItem(entity)
    }
    
    override suspend fun deleteItemById(id: String) {
        clipboardItemDao.deleteItemById(id)
    }
    
    override suspend fun deleteAllItems() {
        clipboardItemDao.deleteAllItems()
    }
    
    override suspend fun deleteItemsOlderThan(hours: Int) {
        val threshold = System.currentTimeMillis() - (hours * 60 * 60 * 1000)
        clipboardItemDao.deleteItemsOlderThan(threshold)
    }
    
    override suspend fun getSettings(): ClipboardSettings = settingsRepository.getSettings()
    
    override suspend fun updateSettings(settings: ClipboardSettings) = settingsRepository.updateSettings(settings)
    
    override suspend fun getItemsWithPagination(limit: Int, offset: Int): List<ClipboardItem> {
        return clipboardItemDao.getItemsWithPagination(limit, offset).map { entity ->
            mapEntityToItem(entity)
        }
    }
    
    /**
     * Maps a database entity to a domain model.
     * 
     * @param entity The database entity to map
     * @return The domain model
     */
    private fun mapEntityToItem(entity: ClipboardItemEntity): ClipboardItem {
        val content = if (entity.isEncrypted) {
            encryptionManager.decrypt(entity.content) ?: entity.content
        } else {
            entity.content
        }
        
        return ClipboardItem(
            id = entity.id,
            content = content,
            timestamp = entity.timestamp,
            contentType = entity.contentType,
            isEncrypted = entity.isEncrypted,
            size = entity.size
        )
    }
    
    /**
     * Maps a domain model to a database entity.
     * 
     * @param item The domain model to map
     * @return The database entity
     */
    private fun mapItemToEntity(item: ClipboardItem): ClipboardItemEntity {
        val content = if (item.isEncrypted) {
            encryptionManager.encrypt(item.content) ?: item.content
        } else {
            item.content
        }
        
        return ClipboardItemEntity(
            id = item.id,
            content = content,
            timestamp = item.timestamp,
            contentType = item.contentType,
            isEncrypted = item.isEncrypted,
            size = item.size
        )
    }
}