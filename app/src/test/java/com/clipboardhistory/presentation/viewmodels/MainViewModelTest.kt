package com.clipboardhistory.presentation.viewmodels

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
}