package com.clipboardhistory.data.database

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
        database =
            Room.inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext(),
                ClipboardDatabase::class.java,
            ).allowMainThreadQueries().build()

        clipboardItemDao = database.clipboardItemDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndGetClipboardItem() =
        runTest {
            val testEntity =
                ClipboardItemEntity(
                    id = "test-id",
                    content = "Test content",
                    timestamp = System.currentTimeMillis(),
                    contentType = ContentType.TEXT,
                    isEncrypted = false,
                    size = 12,
                )

            clipboardItemDao.insertItem(testEntity)

            val retrievedItem = clipboardItemDao.getItemById("test-id")

            assertEquals(testEntity, retrievedItem)
        }

    @Test
    fun deleteClipboardItem() =
        runTest {
            val testEntity =
                ClipboardItemEntity(
                    id = "test-id",
                    content = "Test content",
                    timestamp = System.currentTimeMillis(),
                    contentType = ContentType.TEXT,
                    isEncrypted = false,
                    size = 12,
                )

            clipboardItemDao.insertItem(testEntity)
            clipboardItemDao.deleteItem(testEntity)

            val retrievedItem = clipboardItemDao.getItemById("test-id")

            assertEquals(null, retrievedItem)
        }

    @Test
    fun getItemCount() =
        runTest {
            val testEntity1 =
                ClipboardItemEntity(
                    id = "test-id-1",
                    content = "Test content 1",
                    timestamp = System.currentTimeMillis(),
                    contentType = ContentType.TEXT,
                    isEncrypted = false,
                    size = 14,
                )

            val testEntity2 =
                ClipboardItemEntity(
                    id = "test-id-2",
                    content = "Test content 2",
                    timestamp = System.currentTimeMillis(),
                    contentType = ContentType.TEXT,
                    isEncrypted = false,
                    size = 14,
                )

            clipboardItemDao.insertItem(testEntity1)
            clipboardItemDao.insertItem(testEntity2)

            val count = clipboardItemDao.getItemCount()

            assertEquals(2, count)
        }
}
