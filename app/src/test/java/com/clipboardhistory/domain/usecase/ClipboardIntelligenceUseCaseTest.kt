package com.clipboardhistory.domain.usecase

import com.clipboardhistory.domain.model.ClipboardItem
import com.clipboardhistory.domain.model.ContentType
import com.clipboardhistory.domain.repository.ClipboardRepository
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ClipboardIntelligenceUseCaseTest {

    @Mock
    private lateinit var repository: ClipboardRepository

    private lateinit var classify: ClassifyClipboardItemUseCase
    private lateinit var useCase: ClipboardIntelligenceUseCase

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        classify = ClassifyClipboardItemUseCase()
        useCase = ClipboardIntelligenceUseCase(repository, classify)
    }

    // --- findDuplicates ---

    @Test
    fun `findDuplicates returns empty list when no duplicates exist`() {
        val items = listOf(item("a", content = "hello"), item("b", content = "world"))
        assertTrue(useCase.findDuplicates(items).isEmpty())
    }

    @Test
    fun `findDuplicates detects duplicate content group`() {
        val older = item("a", content = "dup", timestamp = 1000L)
        val newer = item("b", content = "dup", timestamp = 2000L)
        val groups = useCase.findDuplicates(listOf(older, newer))
        assertEquals(1, groups.size)
        assertEquals(newer.id, groups[0].keepItem.id)
        assertEquals(1, groups[0].duplicatesToDelete.size)
        assertEquals(older.id, groups[0].duplicatesToDelete[0].id)
    }

    @Test
    fun `findDuplicates handles three copies — keeps newest, deletes older two`() {
        val oldest = item("a", content = "same", timestamp = 100L)
        val middle = item("b", content = "same", timestamp = 200L)
        val newest = item("c", content = "same", timestamp = 300L)
        val groups = useCase.findDuplicates(listOf(oldest, middle, newest))
        assertEquals(1, groups.size)
        assertEquals(newest.id, groups[0].keepItem.id)
        assertEquals(2, groups[0].duplicatesToDelete.size)
    }

    // --- findStaleItems ---

    @Test
    fun `findStaleItems returns items older than threshold and not favorited`() {
        val stale = item("a", timestamp = daysAgo(15), isFavorite = false)
        val fresh = item("b", timestamp = daysAgo(3), isFavorite = false)
        val result = useCase.findStaleItems(listOf(stale, fresh))
        assertEquals(listOf(stale), result)
    }

    @Test
    fun `findStaleItems excludes favorited items even if old`() {
        val staleFavorite = item("a", timestamp = daysAgo(20), isFavorite = true)
        val result = useCase.findStaleItems(listOf(staleFavorite))
        assertTrue(result.isEmpty())
    }

    @Test
    fun `findStaleItems respects custom daysThreshold`() {
        val old30 = item("a", timestamp = daysAgo(30), isFavorite = false)
        val old5 = item("b", timestamp = daysAgo(5), isFavorite = false)
        val result = useCase.findStaleItems(listOf(old30, old5), daysThreshold = 7)
        assertEquals(listOf(old30), result)
    }

    // --- detectSessions ---

    @Test
    fun `detectSessions groups items from same app within time window`() {
        val t0 = System.currentTimeMillis()
        val i1 = item("a", sourceApp = "com.android.studio", timestamp = t0)
        val i2 = item("b", sourceApp = "com.android.studio", timestamp = t0 + 60_000)
        val i3 = item("c", sourceApp = "com.android.studio", timestamp = t0 + 120_000)
        val sessions = useCase.detectSessions(listOf(i1, i2, i3), windowMinutes = 5)
        assertEquals(1, sessions.size)
        assertEquals(3, sessions[0].items.size)
    }

    @Test
    fun `detectSessions does not group items outside time window`() {
        val t0 = System.currentTimeMillis()
        val i1 = item("a", sourceApp = "com.android.studio", timestamp = t0)
        val i2 = item("b", sourceApp = "com.android.studio", timestamp = t0 + 10 * 60_000)
        val sessions = useCase.detectSessions(listOf(i1, i2), windowMinutes = 5)
        assertTrue(sessions.isEmpty())
    }

    @Test
    fun `detectSessions ignores items without sourceApp`() {
        val t0 = System.currentTimeMillis()
        val i1 = item("a", sourceApp = null, timestamp = t0)
        val i2 = item("b", sourceApp = null, timestamp = t0 + 60_000)
        val sessions = useCase.detectSessions(listOf(i1, i2))
        assertTrue(sessions.isEmpty())
    }

    @Test
    fun `detectSessions separates sessions from different apps`() {
        val t0 = System.currentTimeMillis()
        val i1 = item("a", sourceApp = "com.android.studio", timestamp = t0)
        val i2 = item("b", sourceApp = "com.android.studio", timestamp = t0 + 60_000)
        val i3 = item("c", sourceApp = "com.android.chrome", timestamp = t0)
        val i4 = item("d", sourceApp = "com.android.chrome", timestamp = t0 + 60_000)
        val sessions = useCase.detectSessions(listOf(i1, i2, i3, i4))
        assertEquals(2, sessions.size)
    }

    // --- findCodeBundles ---

    @Test
    fun `findCodeBundles groups 3 or more code items from same app`() {
        val app = "com.android.studio"
        val codeItems = (1..3).map { i ->
            item("code$i", content = "fun method$i() {}", sourceApp = app)
        }
        val bundles = useCase.findCodeBundles(codeItems)
        assertEquals(1, bundles.size)
        assertEquals(app, bundles[0].sourceApp)
        assertTrue(bundles[0].bundledContent.contains("---"))
    }

    @Test
    fun `findCodeBundles ignores fewer than 3 code items from same app`() {
        val app = "com.android.studio"
        val codeItems = (1..2).map { i ->
            item("code$i", content = "fun method$i() {}", sourceApp = app)
        }
        assertTrue(useCase.findCodeBundles(codeItems).isEmpty())
    }

    @Test
    fun `findCodeBundles ignores non-code items`() {
        val app = "com.android.studio"
        val nonCode = (1..5).map { i ->
            item("text$i", content = "some plain text $i", sourceApp = app)
        }
        assertTrue(useCase.findCodeBundles(nonCode).isEmpty())
    }

    // --- generateSmartSuggestions ---

    @Test
    fun `generateSmartSuggestions returns combined summary`() = runTest {
        val items = listOf(
            item("a", content = "dup"),
            item("b", content = "dup", timestamp = System.currentTimeMillis() - 1000),
        )
        whenever(repository.getAllItems()).thenReturn(flowOf(items))
        val summary = useCase.generateSmartSuggestions()
        assertEquals(1, summary.duplicates.size)
        assertEquals(1, summary.totalDuplicatesToDelete)
    }

    // --- IntelligenceType classification ---

    @Test
    fun `classifyIntelligence detects URL`() {
        assertEquals(IntelligenceType.URL, classify.classifyIntelligence("https://example.com"))
    }

    @Test
    fun `classifyIntelligence detects EMAIL`() {
        assertEquals(IntelligenceType.EMAIL, classify.classifyIntelligence("user@example.com"))
    }

    @Test
    fun `classifyIntelligence detects PHONE`() {
        assertEquals(IntelligenceType.PHONE, classify.classifyIntelligence("+1 555-123-4567"))
    }

    @Test
    fun `classifyIntelligence detects JSON`() {
        assertEquals(IntelligenceType.JSON, classify.classifyIntelligence("""{"key": "value"}"""))
    }

    @Test
    fun `classifyIntelligence detects XML`() {
        assertEquals(IntelligenceType.XML, classify.classifyIntelligence("<root><child/></root>"))
    }

    @Test
    fun `classifyIntelligence detects CODE`() {
        assertEquals(IntelligenceType.CODE, classify.classifyIntelligence("fun main() { return }"))
    }

    @Test
    fun `classifyIntelligence detects SHORT_TEXT`() {
        assertEquals(IntelligenceType.SHORT_TEXT, classify.classifyIntelligence("hi there"))
    }

    @Test
    fun `classifyIntelligence detects TEXT as fallback`() {
        val long = "This is a normal sentence with more than fifty characters to qualify as TEXT type."
        assertEquals(IntelligenceType.TEXT, classify.classifyIntelligence(long))
    }

    // --- helpers ---

    private fun item(
        id: String,
        content: String = "content-$id",
        timestamp: Long = System.currentTimeMillis(),
        sourceApp: String? = null,
        isFavorite: Boolean = false,
    ) = ClipboardItem(
        id = id,
        content = content,
        timestamp = timestamp,
        contentType = ContentType.TEXT,
        isEncrypted = false,
        size = content.toByteArray().size,
        sourceApp = sourceApp,
        isFavorite = isFavorite,
    )

    private fun daysAgo(days: Int): Long =
        System.currentTimeMillis() - days * 24 * 60 * 60 * 1000L
}
