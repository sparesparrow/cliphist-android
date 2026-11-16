package com.clipboardhistory.integration

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.clipboardhistory.data.database.ClipboardDatabase
import com.clipboardhistory.data.encryption.EncryptionManager
import com.clipboardhistory.data.repository.ClipboardRepositoryImpl
import com.clipboardhistory.domain.model.ClipboardItem
import com.clipboardhistory.domain.model.ContentType
import com.clipboardhistory.domain.repository.ClipboardRepository
import com.clipboardhistory.domain.usecase.ClassifyClipboardItemUseCase
import com.clipboardhistory.domain.usecase.ClipboardIntelligenceUseCase
import com.clipboardhistory.domain.usecase.ContextType
import com.clipboardhistory.domain.usecase.IntelligenceType
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/**
 * Instrumented integration test for the Clipboard Intelligence feature (issue #31).
 *
 * Runs the real Room database (in-memory), the real ClipboardRepositoryImpl with the
 * device EncryptionManager, and the real intelligence/classification use cases together.
 */
@RunWith(AndroidJUnit4::class)
class ClipboardIntelligenceIntegrationTest {
    private lateinit var database: ClipboardDatabase
    private lateinit var repository: ClipboardRepository
    private lateinit var classify: ClassifyClipboardItemUseCase
    private lateinit var intelligence: ClipboardIntelligenceUseCase

    private val now = System.currentTimeMillis()
    private val minute = 60_000L
    private val day = 24 * 60 * minute

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database =
            Room.inMemoryDatabaseBuilder(context, ClipboardDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        repository = ClipboardRepositoryImpl(database.clipboardItemDao(), EncryptionManager(context))
        classify = ClassifyClipboardItemUseCase()
        intelligence = ClipboardIntelligenceUseCase(repository, classify)
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun item(
        content: String,
        timestamp: Long = now,
        sourceApp: String? = null,
        isFavorite: Boolean = false,
        isEncrypted: Boolean = false,
    ) = ClipboardItem(
        id = UUID.randomUUID().toString(),
        content = content,
        timestamp = timestamp,
        contentType = ContentType.TEXT,
        isEncrypted = isEncrypted,
        size = content.length,
        sourceApp = sourceApp,
        isFavorite = isFavorite,
    )

    @Test
    fun duplicates_areDetectedAfterEncryptedRoundTrip_andNewestIsKept() =
        runBlocking {
            val oldest = item("same secret text", now - 3 * minute, isEncrypted = true)
            val middle = item("same secret text", now - 2 * minute, isEncrypted = true)
            val newest = item("same secret text", now - minute, isEncrypted = true)
            val unique = item("something else", now, isEncrypted = true)
            listOf(oldest, middle, newest, unique).forEach { repository.insertItem(it) }

            val summary = intelligence.generateSmartSuggestions()

            assertEquals(1, summary.duplicates.size)
            val group = summary.duplicates.single()
            assertEquals("same secret text", group.content)
            assertEquals(newest.id, group.keepItem.id)
            assertEquals(setOf(oldest.id, middle.id), group.duplicatesToDelete.map { it.id }.toSet())
            assertEquals(2, summary.totalDuplicatesToDelete)
        }

    @Test
    fun staleItems_excludeFavoritesAndRecentItems() =
        runBlocking {
            val stale = item("old and forgotten", now - 20 * day)
            val staleFavorite = item("old but precious", now - 20 * day)
            val recent = item("fresh", now - day)
            listOf(stale, staleFavorite, recent).forEach { repository.insertItem(it) }
            assertTrue(repository.toggleFavoriteStatus(staleFavorite.id))

            val summary = intelligence.generateSmartSuggestions()

            assertEquals(listOf(stale.id), summary.staleItems.map { it.id })
        }

    @Test
    fun sessions_groupItemsFromSameAppWithinFiveMinutes() =
        runBlocking {
            val base = now - 60 * minute
            val a1 = item("a1", base, sourceApp = "com.android.chrome")
            val a2 = item("a2", base + minute, sourceApp = "com.android.chrome")
            val a3 = item("a3", base + 3 * minute, sourceApp = "com.android.chrome")
            val aLater = item("a-later", base + 30 * minute, sourceApp = "com.android.chrome")
            val b1 = item("b1", base, sourceApp = "com.termux")
            val b2 = item("b2", base + 10 * minute, sourceApp = "com.termux")
            val noApp = item("no app", base + minute)
            listOf(a1, a2, a3, aLater, b1, b2, noApp).forEach { repository.insertItem(it) }

            val sessions = intelligence.generateSmartSuggestions().sessions

            assertEquals(1, sessions.size)
            val session = sessions.single()
            assertEquals("com.android.chrome", session.sourceApp)
            assertEquals(setOf(a1.id, a2.id, a3.id), session.items.map { it.id }.toSet())
            assertEquals(base, session.startTime)
            assertEquals(base + 3 * minute, session.endTime)
        }

    @Test
    fun codeBundles_requireThreeCodeItemsFromSameApp() =
        runBlocking {
            val ide = "com.cursor.app"
            val codeA = item("fun add(a: Int, b: Int) = a + b", sourceApp = ide)
            val codeB = item("class Foo { val x = 1 }", sourceApp = ide)
            val codeC = item("import kotlin.math.max", sourceApp = ide)
            val termuxA = item("def run(): return 1", sourceApp = "com.termux")
            val termuxB = item("const answer = 42", sourceApp = "com.termux")
            val prose = item("just a normal sentence that is not code at all, but is long", sourceApp = ide)
            listOf(codeA, codeB, codeC, termuxA, termuxB, prose).forEach { repository.insertItem(it) }

            val bundles = intelligence.generateSmartSuggestions().codeBundles

            assertEquals(1, bundles.size)
            val bundle = bundles.single()
            assertEquals(ide, bundle.sourceApp)
            assertEquals(3, bundle.items.size)
            assertEquals(
                setOf(codeA.content, codeB.content, codeC.content),
                bundle.bundledContent.split("\n---\n").toSet(),
            )
        }

    @Test
    fun classification_survivesPersistence_andDetectsContext() =
        runBlocking {
            val cases =
                listOf(
                    "https://example.com/page" to IntelligenceType.URL,
                    "jan.novak@example.com" to IntelligenceType.EMAIL,
                    "+420 123 456 789" to IntelligenceType.PHONE,
                    "{\"key\": \"value\"}" to IntelligenceType.JSON,
                    "<root><a>1</a></root>" to IntelligenceType.XML,
                    "fun main() { println(1) }" to IntelligenceType.CODE,
                    "short note" to IntelligenceType.SHORT_TEXT,
                    "This is a longer piece of plain prose that goes past fifty characters." to IntelligenceType.TEXT,
                )
            val stored =
                cases.map { (content, expected) ->
                    val original = item(content, sourceApp = "com.jetbrains.androidstudio", isEncrypted = true)
                    repository.insertItem(original)
                    Triple(original.id, content, expected)
                }

            stored.forEach { (id, content, expected) ->
                val loaded = repository.getItemById(id)
                assertNotNull("Item $id should be persisted", loaded)
                assertEquals(content, loaded!!.content)
                val classified = classify.classify(loaded)
                assertEquals("Wrong type for: $content", expected, classified.intelligenceType)
                assertEquals(ContextType.IDE, classified.context.type)
                assertEquals("Android Studio", classified.context.displayName)
            }
        }

    @Test
    fun emptyDatabase_producesEmptySummary() =
        runBlocking {
            val summary = intelligence.generateSmartSuggestions()

            assertTrue(summary.duplicates.isEmpty())
            assertTrue(summary.staleItems.isEmpty())
            assertTrue(summary.sessions.isEmpty())
            assertTrue(summary.codeBundles.isEmpty())
            assertEquals(0, summary.totalDuplicatesToDelete)
        }
}
