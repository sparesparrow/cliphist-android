package com.clipboardhistory.domain.usecase

import com.clipboardhistory.domain.model.ClipboardItem
import com.clipboardhistory.domain.repository.ClipboardRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class ClipboardIntelligenceUseCase @Inject constructor(
    private val repository: ClipboardRepository,
    private val classify: ClassifyClipboardItemUseCase,
) {

    suspend fun generateSmartSuggestions(): IntelligenceSummary {
        val items = repository.getAllItems().first()
        return IntelligenceSummary(
            duplicates = findDuplicates(items),
            staleItems = findStaleItems(items),
            sessions = detectSessions(items),
            codeBundles = findCodeBundles(items),
        )
    }

    fun findDuplicates(items: List<ClipboardItem>): List<DuplicateGroup> =
        items.groupBy { it.content }
            .filter { it.value.size >= 2 }
            .map { (content, group) ->
                val sorted = group.sortedByDescending { it.timestamp }
                DuplicateGroup(
                    content = content,
                    keepItem = sorted.first(),
                    duplicatesToDelete = sorted.drop(1),
                )
            }

    fun findStaleItems(items: List<ClipboardItem>, daysThreshold: Int = 14): List<ClipboardItem> {
        val cutoff = System.currentTimeMillis() - daysThreshold * 24 * 60 * 60 * 1000L
        return items.filter { it.timestamp < cutoff && !it.isFavorite }
    }

    fun detectSessions(items: List<ClipboardItem>, windowMinutes: Int = 5): List<ClipboardSession> {
        val windowMs = windowMinutes * 60 * 1000L
        return items
            .filter { it.sourceApp != null }
            .groupBy { it.sourceApp!! }
            .flatMap { (app, appItems) ->
                val sorted = appItems.sortedBy { it.timestamp }
                val sessions = mutableListOf<ClipboardSession>()
                var window = mutableListOf(sorted.first())
                for (item in sorted.drop(1)) {
                    if (item.timestamp - window.last().timestamp <= windowMs) {
                        window.add(item)
                    } else {
                        if (window.size >= 2) sessions.add(ClipboardSession(app, window.toList()))
                        window = mutableListOf(item)
                    }
                }
                if (window.size >= 2) sessions.add(ClipboardSession(app, window.toList()))
                sessions
            }
    }

    fun findCodeBundles(items: List<ClipboardItem>): List<CodeBundle> =
        items.filter { classify.classifyIntelligence(it.content) == IntelligenceType.CODE }
            .filter { it.sourceApp != null }
            .groupBy { it.sourceApp!! }
            .filter { it.value.size >= 3 }
            .map { (app, codeItems) ->
                CodeBundle(
                    sourceApp = app,
                    items = codeItems,
                    bundledContent = codeItems.joinToString("\n---\n") { it.content },
                )
            }
}

data class DuplicateGroup(
    val content: String,
    val keepItem: ClipboardItem,
    val duplicatesToDelete: List<ClipboardItem>,
)

data class ClipboardSession(
    val sourceApp: String,
    val items: List<ClipboardItem>,
) {
    val startTime: Long get() = items.minOf { it.timestamp }
    val endTime: Long get() = items.maxOf { it.timestamp }
}

data class CodeBundle(
    val sourceApp: String,
    val items: List<ClipboardItem>,
    val bundledContent: String,
)

data class IntelligenceSummary(
    val duplicates: List<DuplicateGroup>,
    val staleItems: List<ClipboardItem>,
    val sessions: List<ClipboardSession>,
    val codeBundles: List<CodeBundle>,
) {
    val totalDuplicatesToDelete: Int get() = duplicates.sumOf { it.duplicatesToDelete.size }
}
