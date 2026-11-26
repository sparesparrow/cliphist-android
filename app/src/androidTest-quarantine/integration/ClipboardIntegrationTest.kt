package com.clipboardhistory.integration

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.clipboardhistory.data.database.ClipboardDatabase
import com.clipboardhistory.data.encryption.DatabaseEncryptionManager
import com.clipboardhistory.data.repository.ClipboardRepositoryImpl
import com.clipboardhistory.domain.model.ClipboardItem
import com.clipboardhistory.domain.repository.ClipboardRepository
import com.clipboardhistory.domain.usecase.AddClipboardItemUseCase
import com.clipboardhistory.presentation.viewmodels.MainViewModel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests for clipboard functionality.
 *
 * Tests the interaction between repository, use cases, and view models.
 */
@RunWith(AndroidJUnit4::class)
class ClipboardIntegrationTest {

    private lateinit var database: ClipboardDatabase
    private lateinit var repository: ClipboardRepository
    private lateinit var addItemUseCase: AddClipboardItemUseCase
    private lateinit var viewModel: MainViewModel

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        // Create in-memory database for testing
        val encryptionManager = DatabaseEncryptionManager(context)
        database = ClipboardDatabase.create(context, encryptionManager)

        // Initialize repository
        repository = ClipboardRepositoryImpl(database.clipboardItemDao(), encryptionManager)

        // Initialize use case
        addItemUseCase = AddClipboardItemUseCase(repository)

        // Initialize view model
        viewModel = MainViewModel(repository, addItemUseCase)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun integration_addItemAndRetrieve() = runBlocking {
        // Given: A clipboard item
        val testContent = "Integration test content"
        val testType = "text/plain"

        // When: Add item via use case
        val addedItem = addItemUseCase(testContent, testType)

        // Then: Item should be added successfully
        assertNotNull("Item should be added successfully", addedItem)
        assertEquals("Content should match", testContent, addedItem?.content)
        assertEquals("Content type should match", testType, addedItem?.contentType)

        // And: Item should be retrievable from repository
        val retrievedItem = repository.getItemById(addedItem?.id.toString())
        assertNotNull("Item should be retrievable", retrievedItem)
        assertEquals("Retrieved content should match", testContent, retrievedItem?.content)
    }

    @Test
    fun integration_duplicateContentPrevention() = runBlocking {
        // Given: Same content added twice
        val testContent = "Duplicate test content"
        val testType = "text/plain"

        // When: Add same item twice
        val firstItem = addItemUseCase(testContent, testType)
        val secondItem = addItemUseCase(testContent, testType)

        // Then: Second addition should return null (duplicate prevention)
        assertNotNull("First item should be added", firstItem)
        assertNotNull("Second item should be null (duplicate)", secondItem)
    }

    @Test
    fun integration_repositoryStatistics() = runBlocking {
        // Given: Multiple items added
        val items = listOf(
            "First item" to "text/plain",
            "Second item" to "text/url",
            "Third item" to "text/email",
        )

        // When: Add all items
        items.forEach { (content, type) ->
            addItemUseCase(content, type)
        }

        // Then: Statistics should be correct
        val statistics = repository.getStatistics()
        assertTrue("Should have at least 3 total items", statistics.totalItems >= 3)
        assertTrue("Should have at least 3 items today", statistics.itemsToday >= 3)
    }

    @Test
    fun integration_viewModelItemManagement() = runBlocking {
        // Given: View model with repository
        val initialItems = repository.getAllItems().first()
        val initialCount = initialItems.size

        // When: Add item via view model
        val testItem = ClipboardItem(
            id = 0L,
            content = "ViewModel test content",
            contentType = "text/plain",
            timestamp = System.currentTimeMillis(),
        )

        val addedId = repository.insertItem(testItem)
        val finalItems = repository.getAllItems().first()

        // Then: Item count should increase
        assertEquals("Item count should increase by 1", initialCount + 1, finalItems.size)

        // And: New item should be retrievable
        val retrievedItem = repository.getItemById(addedId.toString())
        assertNotNull("Added item should be retrievable", retrievedItem)
        assertEquals("Retrieved content should match", testItem.content, retrievedItem?.content)
    }

    @Test
    fun integration_contentTypeFiltering() = runBlocking {
        // Given: Items of different content types
        val testItems = listOf(
            "Plain text content" to "text/plain",
            "https://example.com" to "text/url",
            "test@example.com" to "text/email",
            "+1234567890" to "text/phone",
        )

        // When: Add all items
        testItems.forEach { (content, type) ->
            addItemUseCase(content, type)
        }

        // Then: Should be able to filter by content type
        val urlItems = repository.getItemsByContentType("text/url", 10)
        val emailItems = repository.getItemsByContentType("text/email", 10)

        assertTrue("Should find URL items", urlItems.isNotEmpty())
        assertTrue("Should find email items", emailItems.isNotEmpty())

        // All returned items should have the correct content type
        assertTrue("All URL items should have correct type",
            urlItems.all { it.contentType == "text/url" })
        assertTrue("All email items should have correct type",
            emailItems.all { it.contentType == "text/email" })
    }

    @Test
    fun integration_favoritesManagement() = runBlocking {
        // Given: An item added to repository
        val testItem = ClipboardItem(
            id = 0L,
            content = "Favorite test content",
            contentType = "text/plain",
            timestamp = System.currentTimeMillis(),
        )

        val addedId = repository.insertItem(testItem)

        // When: Toggle favorite status
        val toggleResult = repository.toggleFavoriteStatus(addedId, true)
        assertTrue("Favorite toggle should succeed", toggleResult)

        // Then: Item should appear in favorites
        val favoriteItems = repository.getFavoriteItems()
        assertTrue("Item should be in favorites", favoriteItems.any { it.id == addedId })

        // And: Favorite status should be updated
        val retrievedItem = repository.getItemById(addedId.toString())
        assertNotNull("Item should be retrievable", retrievedItem)
        assertTrue("Item should be marked as favorite", retrievedItem?.isFavorite == true)
    }
}