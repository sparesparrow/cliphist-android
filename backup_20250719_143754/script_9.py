# Create unit tests
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
    fun `loadClipboardItems updates state correctly`() = runTest {
        val testItems = listOf(
            createTestClipboardItem("Test content 1"),
            createTestClipboardItem("Test content 2")
        )
        
        whenever(getAllClipboardItemsUseCase()).thenReturn(flowOf(testItems))
        
        // Create a new viewModel to trigger the init block
        val newViewModel = MainViewModel(
            getAllClipboardItemsUseCase,
            addClipboardItemUseCase,
            deleteClipboardItemUseCase,
            getClipboardSettingsUseCase,
            updateClipboardSettingsUseCase,
            cleanupOldItemsUseCase
        )
        
        val uiState = newViewModel.uiState.value
        assertEquals(testItems, uiState.clipboardItems)
        assertFalse(uiState.isLoading)
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
        // Set an error state first
        viewModel.addClipboardItem("") // This might cause an error
        
        viewModel.clearError()
        
        assertNull(viewModel.uiState.value.error)
    }
    
    @Test
    fun `cleanupOldItems calls use case with current settings`() = runTest {
        val testSettings = ClipboardSettings(autoDeleteAfterHours = 48)
        whenever(getClipboardSettingsUseCase()).thenReturn(testSettings)
        
        // Create new viewModel to load the settings
        val newViewModel = MainViewModel(
            getAllClipboardItemsUseCase,
            addClipboardItemUseCase,
            deleteClipboardItemUseCase,
            getClipboardSettingsUseCase,
            updateClipboardSettingsUseCase,
            cleanupOldItemsUseCase
        )
        
        newViewModel.cleanupOldItems()
        
        verify(cleanupOldItemsUseCase).invoke(testSettings)
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

# Create repository tests
clipboard_repository_test = '''package com.clipboardhistory.data.repository

import com.clipboardhistory.data.database.ClipboardItemDao
import com.clipboardhistory.data.database.ClipboardItemEntity
import com.clipboardhistory.data.encryption.EncryptionManager
import com.clipboardhistory.domain.model.ClipboardItem
import com.clipboardhistory.domain.model.ClipboardMode
import com.clipboardhistory.domain.model.ClipboardSettings
import com.clipboardhistory.domain.model.ContentType
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

/**
 * Unit tests for ClipboardRepositoryImpl.
 * 
 * This test class verifies the data layer operations and
 * encryption/decryption functionality.
 */
class ClipboardRepositoryImplTest {
    
    @Mock
    private lateinit var clipboardItemDao: ClipboardItemDao
    
    @Mock
    private lateinit var encryptionManager: EncryptionManager
    
    private lateinit var repository: ClipboardRepositoryImpl
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
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
        verify(clipboardItemDao).insertItem(
            ClipboardItemEntity(
                id = testItem.id,
                content = "encrypted_content",
                timestamp = testItem.timestamp,
                contentType = testItem.contentType,
                isEncrypted = testItem.isEncrypted,
                size = testItem.size
            )
        )
    }
    
    @Test
    fun `getSettings returns correct settings`() = runTest {
        whenever(encryptionManager.getSecureString("max_history_size", "100")).thenReturn("200")
        whenever(encryptionManager.getSecureString("auto_delete_hours", "24")).thenReturn("48")
        whenever(encryptionManager.getSecureString("enable_encryption", "true")).thenReturn("true")
        whenever(encryptionManager.getSecureString("bubble_size", "3")).thenReturn("4")
        whenever(encryptionManager.getSecureString("bubble_opacity", "0.8")).thenReturn("0.9")
        whenever(encryptionManager.getSecureString("clipboard_mode", "REPLACE")).thenReturn("EXTEND")
        
        val result = repository.getSettings()
        
        assertEquals(200, result.maxHistorySize)
        assertEquals(48, result.autoDeleteAfterHours)
        assertEquals(true, result.enableEncryption)
        assertEquals(4, result.bubbleSize)
        assertEquals(0.9f, result.bubbleOpacity)
        assertEquals(ClipboardMode.EXTEND, result.clipboardMode)
    }
    
    @Test
    fun `updateSettings stores settings correctly`() = runTest {
        val testSettings = ClipboardSettings(
            maxHistorySize = 150,
            autoDeleteAfterHours = 72,
            enableEncryption = false,
            bubbleSize = 5,
            bubbleOpacity = 0.5f,
            clipboardMode = ClipboardMode.EXTEND
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
        
        // Verify with a range since timing might vary slightly
        verify(clipboardItemDao).deleteItemsOlderThan(org.mockito.kotlin.check { timestamp ->
            kotlin.math.abs(timestamp - expectedThreshold) < 1000 // Within 1 second
        })
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
}'''

# Create encryption manager tests
encryption_manager_test = '''package com.clipboardhistory.data.encryption

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
}'''

# Create UI tests
main_screen_test = '''package com.clipboardhistory.presentation.ui.screens

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.clipboardhistory.domain.model.ClipboardItem
import com.clipboardhistory.domain.model.ClipboardSettings
import com.clipboardhistory.domain.model.ContentType
import com.clipboardhistory.presentation.ui.theme.ClipboardHistoryTheme
import com.clipboardhistory.presentation.viewmodels.MainViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * UI tests for MainScreen composable.
 * 
 * This test class verifies the UI behavior and user interactions
 * of the main screen.
 */
@RunWith(AndroidJUnit4::class)
class MainScreenTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun `main screen displays correctly when service is stopped`() {
        val mockViewModel = mock(MainViewModel::class.java)
        val uiState = MainViewModel.MainUiState(
            clipboardItems = emptyList(),
            settings = ClipboardSettings(),
            isServiceRunning = false
        )
        
        whenever(mockViewModel.uiState).thenReturn(MutableStateFlow(uiState))
        
        composeTestRule.setContent {
            ClipboardHistoryTheme {
                MainScreen(
                    viewModel = mockViewModel,
                    onStartServices = {},
                    onStopServices = {}
                )
            }
        }
        
        // Verify UI elements are displayed
        composeTestRule.onNodeWithText("Clipboard History").assertIsDisplayed()
        composeTestRule.onNodeWithText("Clipboard service is stopped").assertIsDisplayed()
        composeTestRule.onNodeWithText("No clipboard items yet").assertIsDisplayed()
    }
    
    @Test
    fun `main screen displays clipboard items correctly`() {
        val mockViewModel = mock(MainViewModel::class.java)
        val testItems = listOf(
            ClipboardItem(
                id = "1",
                content = "Test content 1",
                timestamp = System.currentTimeMillis(),
                contentType = ContentType.TEXT,
                isEncrypted = false,
                size = 14
            ),
            ClipboardItem(
                id = "2",
                content = "Test content 2",
                timestamp = System.currentTimeMillis(),
                contentType = ContentType.TEXT,
                isEncrypted = true,
                size = 14
            )
        )
        
        val uiState = MainViewModel.MainUiState(
            clipboardItems = testItems,
            settings = ClipboardSettings(),
            isServiceRunning = true
        )
        
        whenever(mockViewModel.uiState).thenReturn(MutableStateFlow(uiState))
        
        composeTestRule.setContent {
            ClipboardHistoryTheme {
                MainScreen(
                    viewModel = mockViewModel,
                    onStartServices = {},
                    onStopServices = {}
                )
            }
        }
        
        // Verify clipboard items are displayed
        composeTestRule.onNodeWithText("Test content 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Test content 2").assertIsDisplayed()
        composeTestRule.onNodeWithText("🔒 Encrypted").assertIsDisplayed()
    }
    
    @Test
    fun `floating action button toggles service state`() {
        val mockViewModel = mock(MainViewModel::class.java)
        var startServicesCalled = false
        var stopServicesCalled = false
        
        val uiState = MainViewModel.MainUiState(
            clipboardItems = emptyList(),
            settings = ClipboardSettings(),
            isServiceRunning = false
        )
        
        whenever(mockViewModel.uiState).thenReturn(MutableStateFlow(uiState))
        
        composeTestRule.setContent {
            ClipboardHistoryTheme {
                MainScreen(
                    viewModel = mockViewModel,
                    onStartServices = { startServicesCalled = true },
                    onStopServices = { stopServicesCalled = true }
                )
            }
        }
        
        // Click the FAB
        composeTestRule.onNodeWithContentDescription("Start Service").performClick()
        
        // Verify start services was called
        assert(startServicesCalled)
        verify(mockViewModel).updateServiceRunningState(true)
    }
    
    @Test
    fun `settings button opens settings dialog`() {
        val mockViewModel = mock(MainViewModel::class.java)
        val uiState = MainViewModel.MainUiState(
            clipboardItems = emptyList(),
            settings = ClipboardSettings(),
            isServiceRunning = false
        )
        
        whenever(mockViewModel.uiState).thenReturn(MutableStateFlow(uiState))
        
        composeTestRule.setContent {
            ClipboardHistoryTheme {
                MainScreen(
                    viewModel = mockViewModel,
                    onStartServices = {},
                    onStopServices = {}
                )
            }
        }
        
        // Click settings button
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        
        // Verify settings dialog is displayed
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
        composeTestRule.onNodeWithText("Max History Size").assertIsDisplayed()
        composeTestRule.onNodeWithText("Enable Encryption").assertIsDisplayed()
    }
    
    @Test
    fun `add button opens add dialog`() {
        val mockViewModel = mock(MainViewModel::class.java)
        val uiState = MainViewModel.MainUiState(
            clipboardItems = emptyList(),
            settings = ClipboardSettings(),
            isServiceRunning = false
        )
        
        whenever(mockViewModel.uiState).thenReturn(MutableStateFlow(uiState))
        
        composeTestRule.setContent {
            ClipboardHistoryTheme {
                MainScreen(
                    viewModel = mockViewModel,
                    onStartServices = {},
                    onStopServices = {}
                )
            }
        }
        
        // Click add button
        composeTestRule.onNodeWithContentDescription("Add Item").performClick()
        
        // Verify add dialog is displayed
        composeTestRule.onNodeWithText("Add Clipboard Item").assertIsDisplayed()
        composeTestRule.onNodeWithText("Content").assertIsDisplayed()
    }
}'''

# Write test files
with open('app/src/test/java/com/clipboardhistory/presentation/viewmodels/MainViewModelTest.kt', 'w') as f:
    f.write(main_view_model_test)

with open('app/src/test/java/com/clipboardhistory/data/repository/ClipboardRepositoryImplTest.kt', 'w') as f:
    f.write(clipboard_repository_test)

with open('app/src/test/java/com/clipboardhistory/data/encryption/EncryptionManagerTest.kt', 'w') as f:
    f.write(encryption_manager_test)

with open('app/src/androidTest/java/com/clipboardhistory/presentation/ui/screens/MainScreenTest.kt', 'w') as f:
    f.write(main_screen_test)

print("Test files (unit tests and UI tests) created!")