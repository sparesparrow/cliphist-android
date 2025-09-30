package com.clipboardhistory

import org.junit.runner.RunWith
import org.junit.runners.Suite

/**
 * Comprehensive test suite for the Clipboard History application.
 *
 * This suite runs all unit tests in the correct order to ensure
 * proper test execution and coverage reporting.
 */
@RunWith(Suite::class)
@Suite.SuiteClasses(
    // Domain model tests
    com.clipboardhistory.domain.model.ClipboardItemTest::class,
    com.clipboardhistory.domain.model.ContentAnalyzerTest::class,
    com.clipboardhistory.domain.model.SmartActionTest::class,
    // Use case tests
    com.clipboardhistory.domain.usecase.ClipboardUseCasesTest::class,
    // ViewModel tests
    com.clipboardhistory.presentation.viewmodels.MainViewModelTest::class,
    // Repository tests
    com.clipboardhistory.data.repository.ClipboardRepositoryImplTest::class,
    // Encryption tests
    com.clipboardhistory.data.encryption.EncryptionManagerTest::class,
    // Performance tests
    com.clipboardhistory.PerformanceTest::class,
    // Utility tests
    com.clipboardhistory.TestUtils::class,
)
class TestSuite
