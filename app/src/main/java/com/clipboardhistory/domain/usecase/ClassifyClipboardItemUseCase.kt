package com.clipboardhistory.domain.usecase

import com.clipboardhistory.domain.model.ClipboardItem
import com.clipboardhistory.domain.model.ContentAnalyzer
import javax.inject.Inject

enum class IntelligenceType { URL, EMAIL, PHONE, JSON, XML, CODE, SHORT_TEXT, TEXT }

class ClassifyClipboardItemUseCase @Inject constructor() {

    fun classifyIntelligence(content: String): IntelligenceType {
        val trimmed = content.trim()
        return when {
            trimmed.startsWith("http://") || trimmed.startsWith("https://") -> IntelligenceType.URL
            Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$").matches(trimmed) -> IntelligenceType.EMAIL
            Regex("^\\+?[\\d\\s()\\-.]+$").matches(trimmed) && trimmed.count { it.isDigit() } >= 7 -> IntelligenceType.PHONE
            Regex("^\\{.*\\}$", RegexOption.DOT_MATCHES_ALL).matches(trimmed) -> IntelligenceType.JSON
            trimmed.startsWith("<") && trimmed.contains("</") -> IntelligenceType.XML
            CODE_KEYWORDS.any { content.contains(it) } -> IntelligenceType.CODE
            content.length < 50 -> IntelligenceType.SHORT_TEXT
            else -> IntelligenceType.TEXT
        }
    }

    fun classify(item: ClipboardItem): ClassifiedItem {
        val domainType = ContentAnalyzer.analyzeContentType(item.content)
        val context = detectContext(item.sourceApp)
        val confidence = calculateConfidence(domainType, item.content)
        val intelligenceType = classifyIntelligence(item.content)
        return ClassifiedItem(item, domainType, context, confidence, intelligenceType)
    }

    companion object {
        val CODE_KEYWORDS = listOf("fun ", "def ", "class ", "import ", "#include", "const ", "return ")
    }

    fun detectContext(sourceApp: String?): ClipboardContext {
        if (sourceApp == null) return ClipboardContext(ContextType.UNKNOWN, null, "Unknown")
        return when {
            sourceApp.contains("com.android.vscode") || sourceApp.contains("com.cursor") ->
                ClipboardContext(ContextType.IDE, sourceApp, "VS Code / Cursor")
            sourceApp.contains("com.jetbrains.androidstudio") || sourceApp.contains("com.android.studio") ->
                ClipboardContext(ContextType.IDE, sourceApp, "Android Studio")
            sourceApp.contains("com.jetbrains.ideaU") ->
                ClipboardContext(ContextType.IDE, sourceApp, "IntelliJ IDEA")
            sourceApp.contains("com.termux") || sourceApp.contains("jackpal.androidterm") ->
                ClipboardContext(ContextType.TERMINAL, sourceApp, "Terminal")
            sourceApp.contains("com.android.chrome") || sourceApp.contains("org.mozilla.firefox") ->
                ClipboardContext(ContextType.BROWSER, sourceApp, "Browser")
            sourceApp.contains("com.slack.android") || sourceApp.contains("com.discord") ->
                ClipboardContext(ContextType.COMMUNICATION, sourceApp, "Messaging")
            else -> ClipboardContext(ContextType.UNKNOWN, sourceApp, "Unknown")
        }
    }

    private fun calculateConfidence(type: ContentAnalyzer.Type, content: String): Float {
        return when (type) {
            ContentAnalyzer.Type.URL -> 1.0f
            ContentAnalyzer.Type.EMAIL -> 0.95f
            ContentAnalyzer.Type.PHONE -> 0.85f
            ContentAnalyzer.Type.MAPS -> 0.7f
            ContentAnalyzer.Type.TEXT -> 0.5f
        }
    }
}

enum class ContextType { IDE, BROWSER, TERMINAL, COMMUNICATION, UNKNOWN }

data class ClipboardContext(
    val type: ContextType,
    val appPackage: String?,
    val displayName: String,
)

data class ClassifiedItem(
    val item: ClipboardItem,
    val domainType: ContentAnalyzer.Type,
    val context: ClipboardContext,
    val confidence: Float,
    val intelligenceType: IntelligenceType = IntelligenceType.TEXT,
)
