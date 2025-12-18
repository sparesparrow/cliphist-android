package com.clipboardhistory.presentation.ui.bubble

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
    CREATIVE       // Creative tools and media
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
