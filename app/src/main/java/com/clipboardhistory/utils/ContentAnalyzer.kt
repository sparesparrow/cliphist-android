package com.clipboardhistory.utils

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.util.regex.Pattern

/**
 * On-device content analysis engine for intelligent clipboard processing.
 * Provides content understanding, categorization, and actionable insights.
 */
class ContentAnalyzer(private val context: Context) {

    private val mainScope = CoroutineScope(Dispatchers.Main)
    private val ioScope = CoroutineScope(Dispatchers.IO)

    // Content type detection patterns
    private val urlPattern = Pattern.compile(
        "\\bhttps?://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]",
        Pattern.CASE_INSENSITIVE
    )

    private val emailPattern = Pattern.compile(
        "[a-zA-Z0-9+._%\\-]{1,256}@[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}(\\.[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25})",
        Pattern.CASE_INSENSITIVE
    )

    private val phonePattern = Pattern.compile(
        "(\\+?\\d{1,3}[-.\\s]?)?\\(?\\d{3}\\)?[-.\\s]?\\d{3}[-.\\s]?\\d{4}",
        Pattern.CASE_INSENSITIVE
    )

    private val jsonPattern = Pattern.compile("^\\s*\\{.*\\}\\s*$", Pattern.DOTALL)
    private val xmlPattern = Pattern.compile("^\\s*<.*>.*</.*>\\s*$", Pattern.DOTALL)

    private val codePatterns = listOf(
        Pattern.compile("\\bfun\\s+\\w+\\s*\\("), // Kotlin function
        Pattern.compile("\\bfunction\\s+\\w+\\s*\\("), // JavaScript function
        Pattern.compile("\\bdef\\s+\\w+\\s*\\("), // Python function
        Pattern.compile("\\bclass\\s+\\w+"), // Class definition
        Pattern.compile("\\bimport\\s+\\w+"), // Import statement
        Pattern.compile("\\bpackage\\s+[\\w.]+"), // Package declaration
        Pattern.compile("#include\\s*<.*>"), // C/C++ include
        Pattern.compile("\\bpublic\\s+\\w+\\s+\\w+\\s*\\("), // Java method
    )

    /**
     * Analyzes clipboard content and returns comprehensive analysis results.
     */
    fun analyzeContent(content: String): ContentAnalysisResult {
        return try {
            val contentType = detectContentType(content)
            val entities = extractEntities(content, contentType)
            val insights = generateInsights(content, contentType, entities)
            val actions = suggestActions(content, contentType, entities)
            val summary = generateSummary(content, contentType)

            ContentAnalysisResult(
                originalContent = content,
                contentType = contentType,
                confidence = calculateConfidence(contentType, entities),
                entities = entities,
                insights = insights,
                suggestedActions = actions,
                summary = summary,
                processingTimeMs = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            Log.e("ContentAnalyzer", "Analysis failed", e)
            ContentAnalysisResult(
                originalContent = content,
                contentType = ContentType.UNKNOWN,
                confidence = 0.0f,
                entities = emptyList(),
                insights = emptyList(),
                suggestedActions = emptyList(),
                summary = "Analysis failed: ${e.message}",
                processingTimeMs = System.currentTimeMillis()
            )
        }
    }

    /**
     * Detects the primary content type of the given text.
     */
    private fun detectContentType(content: String): ContentType {
        // Check for structured data first
        if (jsonPattern.matcher(content.trim()).matches()) {
            return ContentType.JSON
        }

        if (xmlPattern.matcher(content.trim()).matches()) {
            return ContentType.XML
        }

        // Check for URLs
        if (urlPattern.matcher(content).find()) {
            // If mostly URLs, classify as URL_LIST, otherwise TEXT_WITH_URLS
            val urlMatches = urlPattern.matcher(content).results().count()
            val totalWords = content.split("\\s+".toRegex()).size
            return if (urlMatches >= totalWords * 0.5) ContentType.URL_LIST else ContentType.TEXT_WITH_URLS
        }

        // Check for emails
        if (emailPattern.matcher(content).find()) {
            val emailMatches = emailPattern.matcher(content).results().count()
            return if (emailMatches > 1) ContentType.EMAIL_LIST else ContentType.TEXT_WITH_EMAIL
        }

        // Check for phone numbers
        if (phonePattern.matcher(content).find()) {
            return ContentType.TEXT_WITH_PHONE
        }

        // Check for code patterns
        val codeMatches = codePatterns.sumOf { pattern ->
            pattern.matcher(content).results().count()
        }
        if (codeMatches > 0) {
            return ContentType.CODE
        }

        // Check for numbers (potential calculations)
        val numberCount = "\\d+(\\.\\d+)?".toRegex().findAll(content).count()
        val totalChars = content.length
        if (numberCount > 0 && numberCount.toFloat() / totalChars > 0.1) {
            return ContentType.NUMERIC_DATA
        }

        // Check content length and structure
        return when {
            content.length < 50 -> ContentType.SHORT_TEXT
            content.contains("\n\n") || content.contains(". ") -> ContentType.ARTICLE
            content.split("\\s+".toRegex()).size > 100 -> ContentType.LONG_TEXT
            else -> ContentType.TEXT
        }
    }

    /**
     * Extracts entities from content based on type.
     */
    private fun extractEntities(content: String, contentType: ContentType): List<ExtractedEntity> {
        return when (contentType) {
            ContentType.URL_LIST, ContentType.TEXT_WITH_URLS -> {
                urlPattern.matcher(content).results()
                    .map { ExtractedEntity("url", it.group(), confidence = 0.9f) }
                    .toList()
            }
            ContentType.EMAIL_LIST, ContentType.TEXT_WITH_EMAIL -> {
                emailPattern.matcher(content).results()
                    .map { ExtractedEntity("email", it.group(), confidence = 0.95f) }
                    .toList()
            }
            ContentType.TEXT_WITH_PHONE -> {
                phonePattern.matcher(content).results()
                    .map { ExtractedEntity("phone", it.group(), confidence = 0.8f) }
                    .toList()
            }
            ContentType.JSON -> {
                try {
                    val json = JSONObject(content)
                    val keys = json.keys()
                    keys.asSequence().map { key ->
                        ExtractedEntity("json_key", key, value = json.optString(key), confidence = 1.0f)
                    }.toList()
                } catch (e: Exception) {
                    emptyList()
                }
            }
            ContentType.CODE -> {
                // Extract function names, classes, imports
                val entities = mutableListOf<ExtractedEntity>()

                // Function definitions
                Regex("\\bfun\\s+(\\w+)\\s*\\(").findAll(content).forEach { match ->
                    entities.add(ExtractedEntity("function", match.groupValues[1], confidence = 0.9f))
                }

                Regex("\\bfunction\\s+(\\w+)\\s*\\(").findAll(content).forEach { match ->
                    entities.add(ExtractedEntity("function", match.groupValues[1], confidence = 0.9f))
                }

                // Class definitions
                Regex("\\bclass\\s+(\\w+)").findAll(content).forEach { match ->
                    entities.add(ExtractedEntity("class", match.groupValues[1], confidence = 0.9f))
                }

                entities.take(10) // Limit to prevent overload
            }
            else -> {
                // Extract dates, numbers, proper nouns for general text
                val entities = mutableListOf<ExtractedEntity>()

                // Dates
                Regex("\\b\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4}\\b").findAll(content).forEach { match ->
                    entities.add(ExtractedEntity("date", match.value, confidence = 0.7f))
                }

                // Numbers
                Regex("\\b\\d+(\\.\\d+)?\\b").findAll(content).forEach { match ->
                    entities.add(ExtractedEntity("number", match.value, confidence = 0.8f))
                }

                entities.take(15)
            }
        }
    }

    /**
     * Generates insights about the content.
     */
    private fun generateInsights(
        content: String,
        contentType: ContentType,
        entities: List<ExtractedEntity>
    ): List<ContentInsight> {
        val insights = mutableListOf<ContentInsight>()

        when (contentType) {
            ContentType.URL_LIST -> {
                val urlCount = entities.count { it.type == "url" }
                insights.add(ContentInsight(
                    type = "collection",
                    title = "URL Collection",
                    description = "Contains $urlCount URLs that could be bookmarks, research links, or resources",
                    priority = InsightPriority.HIGH
                ))
            }

            ContentType.EMAIL_LIST -> {
                val emailCount = entities.count { it.type == "email" }
                insights.add(ContentInsight(
                    type = "contacts",
                    title = "Contact Information",
                    description = "Contains $emailCount email addresses for potential contacts",
                    priority = InsightPriority.HIGH
                ))
            }

            ContentType.CODE -> {
                val functions = entities.count { it.type == "function" }
                val classes = entities.count { it.type == "class" }

                insights.add(ContentInsight(
                    type = "code",
                    title = "Code Snippet",
                    description = "Detected $functions functions and $classes classes - appears to be programming code",
                    priority = InsightPriority.HIGH
                ))
            }

            ContentType.JSON -> {
                insights.add(ContentInsight(
                    type = "data",
                    title = "Structured Data",
                    description = "Valid JSON data that can be parsed and processed",
                    priority = InsightPriority.MEDIUM
                ))
            }

            ContentType.ARTICLE -> {
                val wordCount = content.split("\\s+".toRegex()).size
                insights.add(ContentInsight(
                    type = "content",
                    title = "Article Content",
                    description = "Long-form content with approximately $wordCount words",
                    priority = InsightPriority.MEDIUM
                ))
            }

            ContentType.NUMERIC_DATA -> {
                val numbers = entities.count { it.type == "number" }
                insights.add(ContentInsight(
                    type = "data",
                    title = "Numeric Data",
                    description = "Contains $numbers numbers that might be suitable for calculations",
                    priority = InsightPriority.MEDIUM
                ))
            }

            else -> {
                // General insights
                if (content.length < 50) {
                    insights.add(ContentInsight(
                        type = "content",
                        title = "Short Text",
                        description = "Brief text snippet, possibly a note or keyword",
                        priority = InsightPriority.LOW
                    ))
                }
            }
        }

        // Add entity-based insights
        if (entities.isNotEmpty()) {
            val entityTypes = entities.groupBy { it.type }.keys
            if (entityTypes.size > 1) {
                insights.add(ContentInsight(
                    type = "mixed",
                    title = "Mixed Content",
                    description = "Contains multiple types of information: ${entityTypes.joinToString(", ")}",
                    priority = InsightPriority.MEDIUM
                ))
            }
        }

        return insights
    }

    /**
     * Suggests actionable operations based on content analysis.
     */
    private fun suggestActions(
        content: String,
        contentType: ContentType,
        entities: List<ExtractedEntity>
    ): List<SuggestedAction> {
        val actions = mutableListOf<SuggestedAction>()

        when (contentType) {
            ContentType.URL_LIST, ContentType.TEXT_WITH_URLS -> {
                actions.add(SuggestedAction(
                    id = "open_urls",
                    title = "Open URLs",
                    description = "Open all detected URLs in browser",
                    icon = "open_in_browser",
                    priority = ActionPriority.HIGH
                ))

                actions.add(SuggestedAction(
                    id = "bookmark_urls",
                    title = "Bookmark URLs",
                    description = "Save URLs to bookmark collection",
                    icon = "bookmark",
                    priority = ActionPriority.MEDIUM
                ))
            }

            ContentType.EMAIL_LIST, ContentType.TEXT_WITH_EMAIL -> {
                actions.add(SuggestedAction(
                    id = "compose_email",
                    title = "Compose Email",
                    description = "Create email to detected addresses",
                    icon = "email",
                    priority = ActionPriority.HIGH
                ))

                actions.add(SuggestedAction(
                    id = "add_contacts",
                    title = "Add to Contacts",
                    description = "Save email addresses as contacts",
                    icon = "person_add",
                    priority = ActionPriority.MEDIUM
                ))
            }

            ContentType.TEXT_WITH_PHONE -> {
                actions.add(SuggestedAction(
                    id = "call_number",
                    title = "Call Number",
                    description = "Initiate call to detected phone number",
                    icon = "call",
                    priority = ActionPriority.HIGH
                ))

                actions.add(SuggestedAction(
                    id = "add_contact",
                    title = "Add Contact",
                    description = "Save phone number as contact",
                    icon = "contact_phone",
                    priority = ActionPriority.MEDIUM
                ))
            }

            ContentType.CODE -> {
                actions.add(SuggestedAction(
                    id = "format_code",
                    title = "Format Code",
                    description = "Apply code formatting and syntax highlighting",
                    icon = "format_code",
                    priority = ActionPriority.HIGH
                ))

                actions.add(SuggestedAction(
                    id = "run_code",
                    title = "Run Code",
                    description = "Execute code if supported language",
                    icon = "play_arrow",
                    priority = ActionPriority.MEDIUM
                ))
            }

            ContentType.JSON, ContentType.XML -> {
                actions.add(SuggestedAction(
                    id = "format_data",
                    title = "Format Data",
                    description = "Pretty-print and validate structured data",
                    icon = "format_indent_increase",
                    priority = ActionPriority.HIGH
                ))

                actions.add(SuggestedAction(
                    id = "validate_data",
                    title = "Validate Data",
                    description = "Check data structure and syntax",
                    icon = "check_circle",
                    priority = ActionPriority.MEDIUM
                ))
            }

            ContentType.NUMERIC_DATA -> {
                actions.add(SuggestedAction(
                    id = "calculate",
                    title = "Calculate",
                    description = "Perform calculations on numeric data",
                    icon = "calculate",
                    priority = ActionPriority.HIGH
                ))

                actions.add(SuggestedAction(
                    id = "chart_data",
                    title = "Create Chart",
                    description = "Visualize numeric data as chart",
                    icon = "show_chart",
                    priority = ActionPriority.MEDIUM
                ))
            }

            ContentType.ARTICLE, ContentType.LONG_TEXT -> {
                actions.add(SuggestedAction(
                    id = "summarize",
                    title = "Summarize",
                    description = "Generate AI summary of content",
                    icon = "summarize",
                    priority = ActionPriority.HIGH
                ))

                actions.add(SuggestedAction(
                    id = "translate",
                    title = "Translate",
                    description = "Translate content to another language",
                    icon = "translate",
                    priority = ActionPriority.MEDIUM
                ))
            }
        }

        // Add universal actions
        actions.add(SuggestedAction(
            id = "search_web",
            title = "Search Web",
            description = "Search for this content on the web",
            icon = "search",
            priority = ActionPriority.LOW
        ))

        actions.add(SuggestedAction(
            id = "share",
            title = "Share",
            description = "Share content via available apps",
            icon = "share",
            priority = ActionPriority.LOW
        ))

        return actions.sortedByDescending { it.priority.ordinal }
    }

    /**
     * Generates a concise summary of the content.
     */
    private fun generateSummary(content: String, contentType: ContentType): String {
        return when (contentType) {
            ContentType.URL_LIST -> {
                val urlCount = urlPattern.matcher(content).results().count()
                "$urlCount URLs detected"
            }
            ContentType.EMAIL_LIST -> {
                val emailCount = emailPattern.matcher(content).results().count()
                "$emailCount email addresses found"
            }
            ContentType.CODE -> {
                val lines = content.lines().size
                "Code snippet ($lines lines)"
            }
            ContentType.JSON -> "JSON data structure"
            ContentType.XML -> "XML data structure"
            ContentType.ARTICLE -> {
                val wordCount = content.split("\\s+".toRegex()).size
                "Article (~$wordCount words)"
            }
            ContentType.SHORT_TEXT -> "Brief text note"
            ContentType.NUMERIC_DATA -> "Numeric data set"
            else -> {
                val preview = content.take(50).replace("\n", " ")
                if (content.length > 50) "$preview..." else preview
            }
        }
    }

    /**
     * Calculates confidence score for the content type detection.
     */
    private fun calculateConfidence(contentType: ContentType, entities: List<ExtractedEntity>): Float {
        return when (contentType) {
            ContentType.JSON, ContentType.XML -> 1.0f // Perfect detection
            ContentType.CODE -> if (entities.isNotEmpty()) 0.9f else 0.7f
            ContentType.URL_LIST, ContentType.EMAIL_LIST -> 0.95f
            ContentType.TEXT_WITH_URLS, ContentType.TEXT_WITH_EMAIL -> 0.85f
            ContentType.NUMERIC_DATA -> 0.8f
            ContentType.ARTICLE -> 0.75f
            ContentType.TEXT -> 0.6f
            ContentType.SHORT_TEXT -> 0.5f
            ContentType.UNKNOWN -> 0.0f
            else -> 0.5f
        }
    }

    /**
     * Asynchronous content analysis.
     */
    fun analyzeContentAsync(content: String, onResult: (ContentAnalysisResult) -> Unit) {
        ioScope.launch {
            val result = analyzeContent(content)
            withContext(Dispatchers.Main) {
                onResult(result)
            }
        }
    }
}

/**
 * Result of content analysis.
 */
data class ContentAnalysisResult(
    val originalContent: String,
    val contentType: ContentType,
    val confidence: Float,
    val entities: List<ExtractedEntity>,
    val insights: List<ContentInsight>,
    val suggestedActions: List<SuggestedAction>,
    val summary: String,
    val processingTimeMs: Long
)

/**
 * Extracted entity from content.
 */
data class ExtractedEntity(
    val type: String,
    val value: String,
    val confidence: Float = 1.0f,
    val startPosition: Int = -1,
    val endPosition: Int = -1,
    val value: String? = null // Additional data for complex entities
)

/**
 * Insight about content.
 */
data class ContentInsight(
    val type: String,
    val title: String,
    val description: String,
    val priority: InsightPriority = InsightPriority.MEDIUM,
    val data: Map<String, Any> = emptyMap()
)

/**
 * Suggested action for content.
 */
data class SuggestedAction(
    val id: String,
    val title: String,
    val description: String,
    val icon: String,
    val priority: ActionPriority = ActionPriority.MEDIUM,
    val requiresNetwork: Boolean = false,
    val isDestructive: Boolean = false
)

/**
 * Content type classification.
 */
enum class ContentType {
    TEXT,
    SHORT_TEXT,
    LONG_TEXT,
    ARTICLE,
    URL_LIST,
    TEXT_WITH_URLS,
    EMAIL_LIST,
    TEXT_WITH_EMAIL,
    TEXT_WITH_PHONE,
    CODE,
    JSON,
    XML,
    NUMERIC_DATA,
    IMAGE,
    FILE_PATH,
    UNKNOWN
}

/**
 * Priority levels for insights and actions.
 */
enum class InsightPriority {
    LOW, MEDIUM, HIGH, CRITICAL
}

enum class ActionPriority {
    LOW, MEDIUM, HIGH, CRITICAL
}