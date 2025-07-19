# Create test directory structure
import os

test_directories = [
    'app/src/test/java/com/clipboardhistory/presentation/viewmodels',
    'app/src/test/java/com/clipboardhistory/data/repository',
    'app/src/test/java/com/clipboardhistory/data/encryption',
    'app/src/test/java/com/clipboardhistory/domain/usecase',
    'app/src/androidTest/java/com/clipboardhistory/presentation/ui/screens',
    'app/src/androidTest/java/com/clipboardhistory/data/database',
    'app/src/androidTest/java/com/clipboardhistory/presentation/services'
]

for directory in test_directories:
    os.makedirs(directory, exist_ok=True)

print("Test directory structure created!")

# Now create the test files
main_view_model_test = '''package com.clipboardhistory.presentation.viewmodels

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.clipboardhistory.domain.model.ClipboardItem
import com.clipboardhistory.domain.model.ClipboardMode
import com.clipboardhistory.domain.model.ClipboardSettings
import com.clipboardhistory.domain.model.ContentType
import com.clipboardhistory.domain.usecase.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for MainViewModel.
 * 
 * This test class verifies the business logic and state management
 * of the MainViewModel class.
 */
@ExperimentalCoroutinesApi
class MainViewModelTest {
    
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()
    
    @Mock
    private lateinit var getAllClipboardItemsUseCase: GetAllClipboardItemsUseCase
    
    @Mock
    private lateinit var addClipboardItemUseCase: AddClipboardItemUseCase
    
    @Mock
    private lateinit var deleteClipboardItemUseCase: DeleteClipboardItemUseCase
    
    @Mock
    private lateinit var getClipboardSettingsUseCase: GetClipboardSettingsUseCase
    
    @Mock
    private lateinit var updateClipboardSettingsUseCase: UpdateClipboardSettingsUseCase
    
    @Mock
    private lateinit var cleanupOldItemsUseCase: CleanupOldItemsUseCase
    
    private lateinit var viewModel: MainViewModel
    private val testDispatcher = UnconfinedTestDispatcher()
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        
        // Setup default mock behaviors
        whenever(getAllClipboardItemsUseCase()).thenReturn(flowOf(emptyList()))
        whenever(getClipboardSettingsUseCase()).thenReturn(ClipboardSettings())
        
        viewModel = MainViewModel(
            getAllClipboardItemsUseCase,
            addClipboardItemUseCase,
            deleteClipboardItemUseCase,
            getClipboardSettingsUseCase,
            updateClipboardSettingsUseCase,
            cleanupOldItemsUseCase
        )
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
    
    @Test
    fun `initial state is correct`() {
        val uiState = viewModel.uiState.value
        
        assertEquals(emptyList(), uiState.clipboardItems)
        assertEquals(ClipboardSettings(), uiState.settings)
        assertFalse(uiState.isLoading)
        assertNull(uiState.error)
        assertFalse(uiState.isServiceRunning)
    }
    
    @Test
    fun `addClipboardItem calls use case correctly`() = runTest {
        val testContent = "Test clipboard content"
        
        viewModel.addClipboardItem(testContent)
        
        verify(addClipboardItemUseCase).invoke(testContent)
    }
    
    @Test
    fun `deleteClipboardItem calls use case correctly`() = runTest {
        val testItem = createTestClipboardItem("Test content")
        
        viewModel.deleteClipboardItem(testItem)
        
        verify(deleteClipboardItemUseCase).invoke(testItem)
    }
    
    @Test
    fun `updateSettings calls use case and updates state`() = runTest {
        val newSettings = ClipboardSettings(
            maxHistorySize = 200,
            clipboardMode = ClipboardMode.EXTEND
        )
        
        viewModel.updateSettings(newSettings)
        
        verify(updateClipboardSettingsUseCase).invoke(newSettings)
        assertEquals(newSettings, viewModel.uiState.value.settings)
    }
    
    @Test
    fun `updateServiceRunningState updates state correctly`() {
        viewModel.updateServiceRunningState(true)
        
        assertTrue(viewModel.uiState.value.isServiceRunning)
        
        viewModel.updateServiceRunningState(false)
        
        assertFalse(viewModel.uiState.value.isServiceRunning)
    }
    
    @Test
    fun `clearError clears error state`() = runTest {
        viewModel.clearError()
        
        assertNull(viewModel.uiState.value.error)
    }
    
    private fun createTestClipboardItem(content: String): ClipboardItem {
        return ClipboardItem(
            id = "test-id",
            content = content,
            timestamp = System.currentTimeMillis(),
            contentType = ContentType.TEXT,
            isEncrypted = false,
            size = content.length
        )
    }
}'''

# Write test files
with open('app/src/test/java/com/clipboardhistory/presentation/viewmodels/MainViewModelTest.kt', 'w') as f:
    f.write(main_view_model_test)

# Create a simple database test
database_test = '''package com.clipboardhistory.data.database

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.clipboardhistory.domain.model.ContentType
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Database tests for ClipboardDatabase.
 * 
 * This test class verifies database operations and data persistence.
 */
@RunWith(AndroidJUnit4::class)
class ClipboardDatabaseTest {
    
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()
    
    private lateinit var database: ClipboardDatabase
    private lateinit var clipboardItemDao: ClipboardItemDao
    
    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ClipboardDatabase::class.java
        ).allowMainThreadQueries().build()
        
        clipboardItemDao = database.clipboardItemDao()
    }
    
    @After
    fun tearDown() {
        database.close()
    }
    
    @Test
    fun insertAndGetClipboardItem() = runTest {
        val testEntity = ClipboardItemEntity(
            id = "test-id",
            content = "Test content",
            timestamp = System.currentTimeMillis(),
            contentType = ContentType.TEXT,
            isEncrypted = false,
            size = 12
        )
        
        clipboardItemDao.insertItem(testEntity)
        
        val retrievedItem = clipboardItemDao.getItemById("test-id")
        
        assertEquals(testEntity, retrievedItem)
    }
    
    @Test
    fun deleteClipboardItem() = runTest {
        val testEntity = ClipboardItemEntity(
            id = "test-id",
            content = "Test content",
            timestamp = System.currentTimeMillis(),
            contentType = ContentType.TEXT,
            isEncrypted = false,
            size = 12
        )
        
        clipboardItemDao.insertItem(testEntity)
        clipboardItemDao.deleteItem(testEntity)
        
        val retrievedItem = clipboardItemDao.getItemById("test-id")
        
        assertEquals(null, retrievedItem)
    }
    
    @Test
    fun getItemCount() = runTest {
        val testEntity1 = ClipboardItemEntity(
            id = "test-id-1",
            content = "Test content 1",
            timestamp = System.currentTimeMillis(),
            contentType = ContentType.TEXT,
            isEncrypted = false,
            size = 14
        )
        
        val testEntity2 = ClipboardItemEntity(
            id = "test-id-2",
            content = "Test content 2",
            timestamp = System.currentTimeMillis(),
            contentType = ContentType.TEXT,
            isEncrypted = false,
            size = 14
        )
        
        clipboardItemDao.insertItem(testEntity1)
        clipboardItemDao.insertItem(testEntity2)
        
        val count = clipboardItemDao.getItemCount()
        
        assertEquals(2, count)
    }
}'''

with open('app/src/androidTest/java/com/clipboardhistory/data/database/ClipboardDatabaseTest.kt', 'w') as f:
    f.write(database_test)

print("Test files created successfully!")