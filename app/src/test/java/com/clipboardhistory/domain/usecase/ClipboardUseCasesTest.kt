package com.clipboardhistory.domain.usecase

import com.clipboardhistory.domain.model.ClipboardItem
import com.clipboardhistory.domain.model.ClipboardSettings
import com.clipboardhistory.domain.model.ContentType
import com.clipboardhistory.domain.repository.ClipboardRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Unit tests for Clipboard use cases.
 */
class ClipboardUseCasesTest {
    @Mock
    private lateinit var repository: ClipboardRepository

    private lateinit var addClipboardItemUseCase: AddClipboardItemUseCase
    private lateinit var getAllClipboardItemsUseCase: GetAllClipboardItemsUseCase
    private lateinit var deleteClipboardItemUseCase: DeleteClipboardItemUseCase
    private lateinit var updateClipboardItemUseCase: UpdateClipboardItemUseCase
    private lateinit var getClipboardSettingsUseCase: GetClipboardSettingsUseCase
    private lateinit var updateClipboardSettingsUseCase: UpdateClipboardSettingsUseCase
    private lateinit var cleanupOldItemsUseCase: CleanupOldItemsUseCase

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)

        addClipboardItemUseCase = AddClipboardItemUseCase(repository)
        getAllClipboardItemsUseCase = GetAllClipboardItemsUseCase(repository)
        deleteClipboardItemUseCase = DeleteClipboardItemUseCase(repository)
        updateClipboardItemUseCase = UpdateClipboardItemUseCase(repository)
        getClipboardSettingsUseCase = GetClipboardSettingsUseCase(repository)
        updateClipboardSettingsUseCase = UpdateClipboardSettingsUseCase(repository)
        cleanupOldItemsUseCase = CleanupOldItemsUseCase(repository)
    }

    @Test
    fun `AddClipboardItemUseCase adds new item when content is unique`() =
        runTest {
            // Given
            val content = "Test content"
            val contentType = ContentType.TEXT
            val settings = ClipboardSettings(enableEncryption = true)
            val existingItems = emptyList<ClipboardItem>()

            whenever(repository.getAllItems()).thenReturn(flowOf(existingItems))
            whenever(repository.getSettings()).thenReturn(settings)

            // When
            val result = addClipboardItemUseCase(content, contentType)

            // Then
            assertNotNull(result)
            assertEquals(content, result.content)
            assertEquals(contentType, result.contentType)
            assertEquals(true, result.isEncrypted)
            verify(repository).insertItem(result)
        }

    @Test
    fun `AddClipboardItemUseCase returns null when content already exists`() =
        runTest {
            // Given
            val content = "Duplicate content"
            val existingItem = createTestClipboardItem(content)
            val existingItems = listOf(existingItem)

            whenever(repository.getAllItems()).thenReturn(flowOf(existingItems))

            // When
            val result = addClipboardItemUseCase(content)

            // Then
            assertNull(result)
        }

    @Test
    fun `AddClipboardItemUseCase respects encryption setting`() =
        runTest {
            // Given
            val content = "Test content"
            val settings = ClipboardSettings(enableEncryption = false)
            val existingItems = emptyList<ClipboardItem>()

            whenever(repository.getAllItems()).thenReturn(flowOf(existingItems))
            whenever(repository.getSettings()).thenReturn(settings)

            // When
            val result = addClipboardItemUseCase(content)

            // Then
            assertNotNull(result)
            assertEquals(false, result.isEncrypted)
        }

    @Test
    fun `GetAllClipboardItemsUseCase returns all items`() =
        runTest {
            // Given
            val items =
                listOf(
                    createTestClipboardItem("Content 1"),
                    createTestClipboardItem("Content 2"),
                )
            whenever(repository.getAllItems()).thenReturn(flowOf(items))

            // When
            val result = getAllClipboardItemsUseCase().first()

            // Then
            assertEquals(items, result)
        }

    @Test
    fun `DeleteClipboardItemUseCase deletes item`() =
        runTest {
            // Given
            val item = createTestClipboardItem("To delete")

            // When
            deleteClipboardItemUseCase(item)

            // Then
            verify(repository).deleteItem(item)
        }

    @Test
    fun `UpdateClipboardItemUseCase updates item`() =
        runTest {
            // Given
            val item = createTestClipboardItem("To update")

            // When
            updateClipboardItemUseCase(item)

            // Then
            verify(repository).updateItem(item)
        }

    @Test
    fun `GetClipboardSettingsUseCase returns settings`() =
        runTest {
            // Given
            val settings = ClipboardSettings(maxHistorySize = 200)
            whenever(repository.getSettings()).thenReturn(settings)

            // When
            val result = getClipboardSettingsUseCase()

            // Then
            assertEquals(settings, result)
        }

    @Test
    fun `UpdateClipboardSettingsUseCase updates settings`() =
        runTest {
            // Given
            val settings = ClipboardSettings(bubbleSize = 5)

            // When
            updateClipboardSettingsUseCase(settings)

            // Then
            verify(repository).updateSettings(settings)
        }

    @Test
    fun `CleanupOldItemsUseCase cleans up old items`() =
        runTest {
            // Given
            val settings = ClipboardSettings(autoDeleteAfterHours = 48)

            // When
            cleanupOldItemsUseCase(settings)

            // Then
            verify(repository).deleteItemsOlderThan(48)
        }

    @Test
    fun `AddClipboardItemUseCase with different content types`() =
        runTest {
            // Given
            val settings = ClipboardSettings()
            val existingItems = emptyList<ClipboardItem>()

            whenever(repository.getAllItems()).thenReturn(flowOf(existingItems))
            whenever(repository.getSettings()).thenReturn(settings)

            // When & Then
            val textResult = addClipboardItemUseCase("Text content", ContentType.TEXT)
            assertNotNull(textResult)
            assertEquals(ContentType.TEXT, textResult.contentType)

            val urlResult = addClipboardItemUseCase("https://example.com", ContentType.URL)
            assertNotNull(urlResult)
            assertEquals(ContentType.URL, urlResult.contentType)

            val imageResult = addClipboardItemUseCase("Image data", ContentType.IMAGE)
            assertNotNull(imageResult)
            assertEquals(ContentType.IMAGE, imageResult.contentType)
        }

    @Test
    fun `AddClipboardItemUseCase calculates correct size`() =
        runTest {
            // Given
            val content = "Hello World"
            val settings = ClipboardSettings()
            val existingItems = emptyList<ClipboardItem>()

            whenever(repository.getAllItems()).thenReturn(flowOf(existingItems))
            whenever(repository.getSettings()).thenReturn(settings)

            // When
            val result = addClipboardItemUseCase(content)

            // Then
            assertNotNull(result)
            assertEquals(content.toByteArray().size, result.size)
        }

    private fun createTestClipboardItem(content: String): ClipboardItem {
        return ClipboardItem(
            id = "test-id-${System.currentTimeMillis()}",
            content = content,
            timestamp = System.currentTimeMillis(),
            contentType = ContentType.TEXT,
            isEncrypted = false,
            size = content.toByteArray().size,
        )
    }
}
