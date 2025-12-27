package com.clipboardhistory.presentation.ui.bubble

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Advanced bubble types with specialized functionalities and use cases.
 * Each type addresses specific user workflows and interaction patterns.
 */
enum class AdvancedBubbleType(
    val displayName: String,
    val description: String,
    val keyboardPolicy: KeyboardPolicy,
    val maxInstances: Int = 1,
    val defaultSize: Dp = 56.dp,
    val supportsDragging: Boolean = true,
    val autoHideDelay: Long = 0L,
    val zIndexPriority: Int = 0,
    val category: BubbleCategory = BubbleCategory.UTILITY
) {

    // 🔍 Search & Discovery Bubbles
    SEARCH_BUBBLE(
        displayName = "Search Clipboard",
        description = "Search through clipboard history with filters and smart suggestions",
        keyboardPolicy = KeyboardPolicy.SHOW_WHEN_KEYBOARD_VISIBLE,
        defaultSize = 320.dp,
        supportsDragging = false, // Fixed position for search UX
        autoHideDelay = 45000L, // 45 seconds for search tasks
        zIndexPriority = 10,
        category = BubbleCategory.SEARCH
    ),

    TEMPLATE_BUBBLE(
        displayName = "Text Templates",
        description = "Insert predefined text templates, code snippets, and boilerplate",
        keyboardPolicy = KeyboardPolicy.MINIMIZE_WHEN_KEYBOARD_VISIBLE,
        defaultSize = 280.dp,
        supportsDragging = true,
        autoHideDelay = 0L, // Persistent for template work
        zIndexPriority = 7,
        category = BubbleCategory.CONTENT
    ),

    // 🧮 Processing Bubbles
    CALCULATOR_BUBBLE(
        displayName = "Calculator",
        description = "Perform calculations using clipboard numbers or expressions",
        keyboardPolicy = KeyboardPolicy.REPOSITION_WHEN_KEYBOARD_VISIBLE,
        defaultSize = 200.dp,
        supportsDragging = true,
        autoHideDelay = 30000L,
        zIndexPriority = 6,
        category = BubbleCategory.PROCESSING
    ),

    TRANSLATION_BUBBLE(
        displayName = "Translator",
        description = "Translate clipboard text between languages with history",
        keyboardPolicy = KeyboardPolicy.MINIMIZE_WHEN_KEYBOARD_VISIBLE,
        defaultSize = 300.dp,
        supportsDragging = true,
        autoHideDelay = 60000L, // 1 minute for translation tasks
        zIndexPriority = 8,
        category = BubbleCategory.PROCESSING
    ),

    FORMAT_BUBBLE(
        displayName = "Text Formatter",
        description = "Format, transform, and manipulate clipboard text (JSON, XML, etc.)",
        keyboardPolicy = KeyboardPolicy.SHOW_WHEN_KEYBOARD_VISIBLE,
        defaultSize = 260.dp,
        supportsDragging = true,
        autoHideDelay = 30000L,
        zIndexPriority = 5,
        category = BubbleCategory.PROCESSING
    ),

    // 📤 Sharing & Communication Bubbles
    SHARE_BUBBLE(
        displayName = "Share Hub",
        description = "Share clipboard content to multiple apps, contacts, or cloud services",
        keyboardPolicy = KeyboardPolicy.MINIMIZE_WHEN_KEYBOARD_VISIBLE,
        defaultSize = 240.dp,
        supportsDragging = true,
        autoHideDelay = 20000L,
        zIndexPriority = 4,
        category = BubbleCategory.SHARING
    ),

    SOCIAL_BUBBLE(
        displayName = "Social Share",
        description = "Quick sharing to social media with clipboard content preview",
        keyboardPolicy = KeyboardPolicy.HIDE_WHEN_KEYBOARD_VISIBLE,
        defaultSize = 180.dp,
        supportsDragging = true,
        autoHideDelay = 15000L,
        zIndexPriority = 3,
        category = BubbleCategory.SHARING
    ),

    // 📚 History & Organization Bubbles
    TIMELINE_BUBBLE(
        displayName = "History Timeline",
        description = "Browse clipboard history chronologically with time-based filters",
        keyboardPolicy = KeyboardPolicy.REPOSITION_WHEN_KEYBOARD_VISIBLE,
        defaultSize = 340.dp,
        supportsDragging = false, // Timeline needs stable positioning
        autoHideDelay = 0L, // Persistent for history browsing
        zIndexPriority = 9,
        category = BubbleCategory.HISTORY
    ),

    FAVORITES_PALETTE(
        displayName = "Favorites",
        description = "Organized palette of favorite clipboard items with categories",
        keyboardPolicy = KeyboardPolicy.MINIMIZE_WHEN_KEYBOARD_VISIBLE,
        defaultSize = 220.dp,
        supportsDragging = true,
        autoHideDelay = 0L, // Persistent favorites
        zIndexPriority = 7,
        category = BubbleCategory.ORGANIZATION
    ),

    RECENT_CLIPS(
        displayName = "Recent Clips",
        description = "Quick access to most recently used clipboard items",
        keyboardPolicy = KeyboardPolicy.SHOW_WHEN_KEYBOARD_VISIBLE,
        defaultSize = 160.dp,
        supportsDragging = true,
        autoHideDelay = 25000L,
        zIndexPriority = 2,
        category = BubbleCategory.HISTORY
    ),

    // 🎯 Context-Aware Bubbles
    CONTEXT_ACTIONS(
        displayName = "Smart Actions",
        description = "Context-aware actions based on clipboard content type",
        keyboardPolicy = KeyboardPolicy.SHOW_WHEN_KEYBOARD_VISIBLE,
        defaultSize = 200.dp,
        supportsDragging = true,
        autoHideDelay = 20000L,
        zIndexPriority = 6,
        category = BubbleCategory.CONTEXT
    ),

    URL_ACTIONS(
        displayName = "URL Actions",
        description = "Actions for URLs: open, shorten, preview, bookmark",
        keyboardPolicy = KeyboardPolicy.SHOW_WHEN_KEYBOARD_VISIBLE,
        defaultSize = 190.dp,
        supportsDragging = true,
        autoHideDelay = 18000L,
        zIndexPriority = 5,
        category = BubbleCategory.CONTEXT
    ),

    CODE_ACTIONS(
        displayName = "Code Tools",
        description = "Code-specific actions: format, syntax highlight, run, share",
        keyboardPolicy = KeyboardPolicy.SHOW_WHEN_KEYBOARD_VISIBLE,
        defaultSize = 210.dp,
        supportsDragging = true,
        autoHideDelay = 25000L,
        zIndexPriority = 5,
        category = BubbleCategory.CONTEXT
    ),

    // 🎮 Mini-Apps & Productivity
    NOTE_SCRIBBLE(
        displayName = "Quick Note",
        description = "Take quick notes that sync with clipboard",
        keyboardPolicy = KeyboardPolicy.REPOSITION_WHEN_KEYBOARD_VISIBLE,
        defaultSize = 250.dp,
        supportsDragging = true,
        autoHideDelay = 0L, // Notes should persist
        zIndexPriority = 8,
        category = BubbleCategory.PRODUCTIVITY
    ),

    VOICE_NOTES(
        displayName = "Voice Notes",
        description = "Record voice notes and transcribe to clipboard",
        keyboardPolicy = KeyboardPolicy.HIDE_WHEN_KEYBOARD_VISIBLE,
        defaultSize = 140.dp,
        supportsDragging = true,
        autoHideDelay = 30000L,
        zIndexPriority = 4,
        category = BubbleCategory.PRODUCTIVITY
    ),

    REMINDER_BUBBLE(
        displayName = "Smart Reminders",
        description = "Create reminders from clipboard content with smart parsing",
        keyboardPolicy = KeyboardPolicy.MINIMIZE_WHEN_KEYBOARD_VISIBLE,
        defaultSize = 180.dp,
        supportsDragging = true,
        autoHideDelay = 0L,
        zIndexPriority = 6,
        category = BubbleCategory.PRODUCTIVITY
    ),

    // 🔧 System Integration Bubbles
    CLIPBOARD_SYNC(
        displayName = "Cloud Sync",
        description = "Sync clipboard across devices with cloud integration",
        keyboardPolicy = KeyboardPolicy.IGNORE_KEYBOARD,
        defaultSize = 160.dp,
        supportsDragging = true,
        autoHideDelay = 0L,
        zIndexPriority = 2,
        category = BubbleCategory.SYSTEM
    ),

    PERMISSION_MANAGER(
        displayName = "Permissions",
        description = "Manage app permissions and accessibility settings",
        keyboardPolicy = KeyboardPolicy.IGNORE_KEYBOARD,
        defaultSize = 200.dp,
        supportsDragging = false, // Settings should be stable
        autoHideDelay = 0L,
        zIndexPriority = 1,
        category = BubbleCategory.SYSTEM
    ),

    // 🎨 Creative & Media Bubbles
    COLOR_PICKER(
        displayName = "Color Tools",
        description = "Extract colors from clipboard images or pick custom colors",
        keyboardPolicy = KeyboardPolicy.SHOW_WHEN_KEYBOARD_VISIBLE,
        defaultSize = 220.dp,
        supportsDragging = true,
        autoHideDelay = 20000L,
        zIndexPriority = 5,
        category = BubbleCategory.CREATIVE
    ),

    IMAGE_TOOLS(
        displayName = "Image Tools",
        description = "Process clipboard images: resize, filter, convert format",
        keyboardPolicy = KeyboardPolicy.MINIMIZE_WHEN_KEYBOARD_VISIBLE,
        defaultSize = 240.dp,
        supportsDragging = true,
        autoHideDelay = 30000L,
        zIndexPriority = 6,
        category = BubbleCategory.CREATIVE
    ),

    // 🎤 Voice & Speech Bubbles
    VOICE_BUBBLE(
        displayName = "Voice Assistant",
        description = "Text-to-speech playback and voice recognition for creating/appending transcribed content",
        keyboardPolicy = KeyboardPolicy.REPOSITION_WHEN_KEYBOARD_VISIBLE,
        defaultSize = 100.dp,
        supportsDragging = true,
        autoHideDelay = 0L, // Persistent for accessibility
        zIndexPriority = 9,
        category = BubbleCategory.VOICE
    ),

    // 🔍 Pattern Matching & Collection Bubbles
    REGEX_ACCUMULATOR(
        displayName = "Regex Collector",
        description = "Accumulates clipboard content matching regex patterns with visual growth",
        keyboardPolicy = KeyboardPolicy.REPOSITION_WHEN_KEYBOARD_VISIBLE,
        defaultSize = 200.dp,
        supportsDragging = true,
        autoHideDelay = 0L, // Persistent collection
        zIndexPriority = 7,
        category = BubbleCategory.COLLECTION
    ),

    // 🤝 Collaboration & Sharing Bubbles
    COLLABORATION(
        displayName = "Collaboration Hub",
        description = "Real-time collaborative editing and sharing of clipboard content with multiple users",
        keyboardPolicy = KeyboardPolicy.REPOSITION_WHEN_KEYBOARD_VISIBLE,
        defaultSize = 300.dp,
        supportsDragging = false, // Fixed for collaboration stability
        autoHideDelay = 0L, // Persistent collaboration
        zIndexPriority = 9,
        category = BubbleCategory.COLLABORATION
    ),

    // 🎤 Voice & Speech Bubbles
    VOICE_BUBBLE(
        displayName = "Voice Assistant",
        description = "Text-to-speech playback and voice recognition for creating/appending transcribed content",
        keyboardPolicy = KeyboardPolicy.REPOSITION_WHEN_KEYBOARD_VISIBLE,
        defaultSize = 100.dp,
        supportsDragging = true,
        autoHideDelay = 0L, // Persistent for accessibility
        zIndexPriority = 9,
        category = BubbleCategory.VOICE
    ),

    // 🤝 Collaboration & Sharing Bubbles
    COLLABORATION(
        displayName = "Collaboration Hub",
        description = "Real-time collaborative editing and sharing of clipboard content with multiple users",
        keyboardPolicy = KeyboardPolicy.REPOSITION_WHEN_KEYBOARD_VISIBLE,
        defaultSize = 300.dp,
        supportsDragging = false, // Fixed for collaboration stability
        autoHideDelay = 0L, // Persistent collaboration
        zIndexPriority = 9,
        category = BubbleCategory.COLLABORATION
    );

    /**
     * Whether this bubble should be visible based on content context.
     */
    fun isRelevantForContent(content: String, contentType: ContentType): Boolean {
        return when (this) {
            CALCULATOR_BUBBLE -> contentType == ContentType.NUMBER || content.contains(Regex("[0-9+\\-*/=]"))
            TRANSLATION_BUBBLE -> contentType == ContentType.TEXT && content.length > 10
            FORMAT_BUBBLE -> contentType in listOf(ContentType.JSON, ContentType.XML, ContentType.CODE)
            URL_ACTIONS -> contentType == ContentType.URL
            CODE_ACTIONS -> contentType == ContentType.CODE
            COLOR_PICKER -> contentType == ContentType.IMAGE
            IMAGE_TOOLS -> contentType == ContentType.IMAGE
            SOCIAL_BUBBLE -> contentType in listOf(ContentType.TEXT, ContentType.URL, ContentType.IMAGE)
            else -> true // Most bubbles are generally useful
        }
    }

    /**
     * Get contextual priority based on content and user state.
     */
    fun getContextualPriority(content: String, contentType: ContentType, keyboardVisible: Boolean): Int {
        var priority = zIndexPriority

        // Boost priority for highly relevant content
        if (isRelevantForContent(content, contentType)) {
            priority += 2
        }

        // Adjust for keyboard state
        if (keyboardVisible && keyboardPolicy == KeyboardPolicy.SHOW_WHEN_KEYBOARD_VISIBLE) {
            priority += 1
        }

        return priority.coerceIn(0, 15)
    }

    companion object {
        /**
         * Get recommended bubbles for specific content.
         */
        fun getRelevantBubbles(content: String, contentType: ContentType, keyboardVisible: Boolean): List<AdvancedBubbleType> {
            return values()
                .filter { it.isRelevantForContent(content, contentType) }
                .sortedByDescending { it.getContextualPriority(content, contentType, keyboardVisible) }
                .take(5) // Top 5 most relevant
        }

        /**
         * Get bubbles by category.
         */
        fun getBubblesByCategory(category: BubbleCategory): List<AdvancedBubbleType> {
            return values().filter { it.category == category }
        }
    }
}

/**
 * Categories for organizing bubble types.
 */
enum class BubbleCategory {
    SEARCH,        // Search and discovery
    CONTENT,       // Content creation and templates
    PROCESSING,    // Text processing and transformation
    SHARING,       // Sharing and communication
    HISTORY,       // History browsing and management
    ORGANIZATION,  // Organization and favorites
    CONTEXT,       // Context-aware actions
    PRODUCTIVITY,  // Productivity tools and notes
    SYSTEM,        // System integration and settings
    CREATIVE,      // Creative tools and media
    COLLECTION,    // Pattern matching and data collection
    VOICE,         // Voice and speech interaction
    COLLABORATION  // Real-time collaborative editing
}

/**
 * Content types for contextual bubble recommendations.
 */
enum class ContentType {
    TEXT, URL, EMAIL, PHONE_NUMBER, JSON, XML, CODE, NUMBER, IMAGE, FILE_PATH, UNKNOWN
}

/**
 * Advanced bubble specification with contextual awareness.
 */
sealed class AdvancedBubbleSpec : BubbleSpec() {

    abstract val relevanceScore: Float
    abstract val contextualActions: List<String>

    /**
     * Search bubble for finding clipboard content.
     */
    data class SearchBubble(
        override val id: String = generateId(),
        override val type: BubbleType = AdvancedBubbleType.SEARCH_BUBBLE,
        override val position: Offset = Offset.Zero,
        override val size: Dp = type.defaultSize,
        override val isVisible: Boolean = true,
        override val isMinimized: Boolean = false,
        override val lastInteractionTime: Long = System.currentTimeMillis(),
        val searchQuery: String = "",
        val searchFilters: Set<SearchFilter> = emptySet(),
        val recentSearches: List<String> = emptyList(),
        override val relevanceScore: Float = 0.8f,
        override val contextualActions: List<String> = listOf("search", "filter", "clear")
    ) : AdvancedBubbleSpec() {

        override val content: @Composable (BubbleSpec) -> Unit = { spec ->
            val searchSpec = spec as SearchBubble
            SearchBubbleContent(searchSpec)
        }

        override fun withKeyboardState(isKeyboardVisible: Boolean): SearchBubble =
            copy(isVisible = isKeyboardVisible)

        override fun withPosition(newPosition: Offset): SearchBubble = copy(position = newPosition)
        override fun withMinimized(isMinimized: Boolean): SearchBubble = copy(isMinimized = isMinimized)
        override fun withSize(newSize: Dp): SearchBubble = copy(size = newSize)
        override fun withInteraction(): SearchBubble = copy(lastInteractionTime = System.currentTimeMillis())
    }

    /**
     * Template bubble for inserting predefined content.
     */
    data class TemplateBubble(
        override val id: String = generateId(),
        override val type: BubbleType = AdvancedBubbleType.TEMPLATE_BUBBLE,
        override val position: Offset = Offset.Zero,
        override val size: Dp = type.defaultSize,
        override val isVisible: Boolean = true,
        override val isMinimized: Boolean = false,
        override val lastInteractionTime: Long = System.currentTimeMillis(),
        val templates: List<TextTemplate> = emptyList(),
        val categories: List<String> = emptyList(),
        val selectedCategory: String? = null,
        override val relevanceScore: Float = 0.9f,
        override val contextualActions: List<String> = listOf("insert", "edit", "create")
    ) : AdvancedBubbleSpec() {

        override val content: @Composable (BubbleSpec) -> Unit = { spec ->
            val templateSpec = spec as TemplateBubble
            TemplateBubbleContent(templateSpec)
        }

        override fun withKeyboardState(isKeyboardVisible: Boolean): TemplateBubble =
            copy(isMinimized = isKeyboardVisible)

        override fun withPosition(newPosition: Offset): TemplateBubble = copy(position = newPosition)
        override fun withMinimized(isMinimized: Boolean): TemplateBubble = copy(isMinimized = isMinimized)
        override fun withSize(newSize: Dp): TemplateBubble = copy(size = newSize)
        override fun withInteraction(): TemplateBubble = copy(lastInteractionTime = System.currentTimeMillis())
    }

    /**
     * Regex accumulator bubble that collects clipboard content matching patterns.
     */
    data class RegexAccumulator(
        override val id: String = generateId(),
        override val type: BubbleType = AdvancedBubbleType.REGEX_ACCUMULATOR,
        override val position: Offset = Offset.Zero,
        override val size: Dp = type.defaultSize,
        override val isVisible: Boolean = true,
        override val isMinimized: Boolean = false,
        override val lastInteractionTime: Long = System.currentTimeMillis(),
        val pattern: RegexPattern,
        val accumulatedItems: List<AccumulatedItem> = emptyList(),
        val isCollecting: Boolean = true,
        val showDuplicates: Boolean = false,
        override val relevanceScore: Float = 0.8f,
        override val contextualActions: List<String> = listOf("collect", "export", "clear", "configure")
    ) : AdvancedBubbleSpec() {

        override val content: @Composable (BubbleSpec) -> Unit = { spec ->
            val regexSpec = spec as RegexAccumulator
            RegexAccumulatorContent(regexSpec)
        }

        override fun withKeyboardState(isKeyboardVisible: Boolean): RegexAccumulator =
            copy() // Repositioning handled by base class

        override fun withPosition(newPosition: Offset): RegexAccumulator = copy(position = newPosition)
        override fun withMinimized(isMinimized: Boolean): RegexAccumulator = copy(isMinimized = isMinimized)
        override fun withSize(newSize: Dp): RegexAccumulator = copy(size = newSize)
        override fun withInteraction(): RegexAccumulator = copy(lastInteractionTime = System.currentTimeMillis())

        /**
         * Attempts to add content if it matches the regex pattern.
         */
        fun tryAccumulate(content: String, source: String? = null): RegexAccumulator {
            return try {
                val regex = Regex(pattern.pattern)
                val matches = regex.findAll(content)

                val newItems = matches.map { match ->
                    AccumulatedItem(
                        content = match.value,
                        matchedAt = System.currentTimeMillis(),
                        source = source
                    )
                }.toList()

                if (newItems.isNotEmpty()) {
                    val updatedItems = if (showDuplicates) {
                        accumulatedItems + newItems
                    } else {
                        // Remove duplicates based on content
                        val existingContent = accumulatedItems.map { it.content }.toSet()
                        val filteredNewItems = newItems.filter { it.content !in existingContent }
                        accumulatedItems + filteredNewItems
                    }

                    // Limit to max items
                    val limitedItems = updatedItems.takeLast(pattern.maxItems)

                    copy(
                        accumulatedItems = limitedItems,
                        lastInteractionTime = System.currentTimeMillis()
                    )
                } else {
                    this
                }
            } catch (e: Exception) {
                // Invalid regex pattern, return unchanged
                this
            }
        }

        /**
         * Clears all accumulated items.
         */
        fun clearAccumulated(): RegexAccumulator = copy(
            accumulatedItems = emptyList(),
            lastInteractionTime = System.currentTimeMillis()
        )

        /**
         * Exports accumulated items as formatted string.
         */
        fun exportAccumulated(): String {
            return accumulatedItems.joinToString(pattern.delimiter.getSeparator()) { it.content }
        }

        /**
         * Gets the display size based on accumulated items count.
         */
        fun getDynamicSize(baseSize: Dp): Dp {
            val itemCount = accumulatedItems.size
            val growthFactor = when {
                itemCount <= 5 -> 1.0f
                itemCount <= 10 -> 1.2f
                itemCount <= 20 -> 1.4f
                itemCount <= 50 -> 1.6f
                else -> 1.8f
            }
            return (baseSize.value * growthFactor).dp
        }

        /**
         * Checks if the bubble should visually indicate new content.
         */
        fun hasNewContent(since: Long): Boolean {
            return accumulatedItems.any { it.matchedAt > since }
        }
    }

    companion object {
        private fun generateId(): String = "advanced_${System.currentTimeMillis()}_${(0..999).random()}"

        /**
         * Factory method to create appropriate advanced bubble spec based on type.
         */
        fun create(
            type: AdvancedBubbleType,
            id: String = generateId(),
            position: Offset = Offset.Zero,
            content: Any? = null
        ): AdvancedBubbleSpec {
            return when (type) {
                AdvancedBubbleType.SEARCH_BUBBLE -> {
                    val searchData = content as? Map<String, Any> ?: emptyMap()
                    SearchBubble(
                        id = id,
                        position = position,
                        searchQuery = searchData["query"] as? String ?: "",
                        searchFilters = searchData["filters"] as? Set<SearchFilter> ?: emptySet()
                    )
                }
                AdvancedBubbleType.TEMPLATE_BUBBLE -> {
                    val templateData = content as? List<*> ?: emptyList()
                    @Suppress("UNCHECKED_CAST")
                    TemplateBubble(
                        id = id,
                        position = position,
                        templates = templateData as List<TextTemplate>
                    )
                }
                AdvancedBubbleType.REGEX_ACCUMULATOR -> {
                    val regexData = content as? Map<String, Any> ?: emptyMap()
                    val pattern = regexData["pattern"] as? RegexPattern ?: RegexPattern(
                        id = "default_pattern",
                        name = "Default Pattern",
                        pattern = ".*",
                        description = "Matches all content"
                    )
                    RegexAccumulator(
                        id = id,
                        position = position,
                        pattern = pattern
                    )
                }
                AdvancedBubbleType.VOICE_BUBBLE -> {
                    val voiceData = content as? Map<String, Any> ?: emptyMap()
                    val textContent = voiceData["text"] as? String ?: ""
                    val isTTSEnabled = voiceData["ttsEnabled"] as? Boolean ?: true
                    val isVoiceEnabled = voiceData["voiceEnabled"] as? Boolean ?: true

                    VoiceBubble(
                        id = id,
                        position = position,
                        textContent = textContent,
                        isTTSEnabled = isTTSEnabled,
                        isVoiceRecognitionEnabled = isVoiceEnabled
                    )
                }
                AdvancedBubbleType.COLLABORATION -> {
                    val collabData = content as? Map<String, Any> ?: emptyMap()
                    val initialText = collabData["text"] as? String ?: ""
                    val isHost = collabData["isHost"] as? Boolean ?: true

                    CollaborationBubble(
                        id = id,
                        position = position,
                        isHost = isHost,
                        content = CollaborativeContent(text = initialText)
                    )
                }
                else -> {
                    // Fallback for other types
                    SearchBubble(id = id, position = position)
                }
            }
        }
    }
}

/**
 * Search filters for the search bubble.
 */
enum class SearchFilter {
    TEXT_ONLY, URLS_ONLY, IMAGES_ONLY, CODE_ONLY, RECENT_ONLY, FAVORITES_ONLY, BY_DATE, BY_APP
}

    /**
     * Text template for the template bubble.
     */
    data class TextTemplate(
        val id: String,
        val name: String,
        val content: String,
        val category: String,
        val tags: List<String> = emptyList(),
        val usageCount: Int = 0,
        val lastUsed: Long = System.currentTimeMillis()
    )

    /**
     * Regex pattern for the regex accumulator bubble.
     */
    data class RegexPattern(
        val id: String,
        val name: String,
        val pattern: String,
        val description: String,
        val delimiter: Delimiter = Delimiter.NEWLINE,
        val isEnabled: Boolean = true,
        val maxItems: Int = 50,
        val createdAt: Long = System.currentTimeMillis()
    ) {
        enum class Delimiter {
            NEWLINE, SPACE, COMMA, SEMICOLON, CUSTOM;

            fun getSeparator(): String = when (this) {
                NEWLINE -> "\n"
                SPACE -> " "
                COMMA -> ", "
                SEMICOLON -> "; "
                CUSTOM -> "" // Custom delimiter would be specified separately
            }
        }
    }

    /**
     * Accumulated item for the regex collector.
     */
    data class AccumulatedItem(
        val content: String,
        val matchedAt: Long = System.currentTimeMillis(),
        val source: String? = null // Optional source context
    )

