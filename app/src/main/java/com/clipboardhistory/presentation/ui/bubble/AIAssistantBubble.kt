package com.clipboardhistory.presentation.ui.bubble

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import com.clipboardhistory.utils.ContentAnalysisResult
import com.clipboardhistory.utils.ContentAnalyzer
import com.clipboardhistory.utils.SuggestedAction

/**
 * AI Assistant Bubble that provides intelligent content analysis, insights, and automated actions.
 * Uses on-device content analysis to understand clipboard content and suggest relevant operations.
 */
data class AIAssistantBubble(
    override val id: String,
    override val type: BubbleType = AdvancedBubbleType.AI_ASSISTANT,
    override val position: Offset = Offset.Zero,
    override val size: Dp = type.defaultSize,
    override val isVisible: Boolean = true,
    override val isMinimized: Boolean = false,
    override val lastInteractionTime: Long = System.currentTimeMillis(),
    val analysisResult: ContentAnalysisResult? = null,
    val isAnalyzing: Boolean = false,
    val autoAnalyze: Boolean = true,
    val analysisHistory: List<AnalysisRecord> = emptyList(),
    val enabledFeatures: Set<AIFeature> = AIFeature.values().toSet(),
    override val relevanceScore: Float = 0.95f,
    override val contextualActions: List<String> = listOf("analyze", "suggest", "automate", "learn")
) : AdvancedBubbleSpec() {

    override val content: @Composable (BubbleSpec) -> Unit = { spec ->
        val aiSpec = spec as AIAssistantBubble
        AIAssistantBubbleContent(aiSpec)
    }

    override fun withKeyboardState(isKeyboardVisible: Boolean): AIAssistantBubble =
        copy(isMinimized = isKeyboardVisible)

    override fun withPosition(newPosition: Offset): AIAssistantBubble = copy(position = newPosition)
    override fun withMinimized(isMinimized: Boolean): AIAssistantBubble = copy(isMinimized = isMinimized)
    override fun withSize(newSize: Dp): AIAssistantBubble = copy(size = newSize)
    override fun withInteraction(): AIAssistantBubble = copy(lastInteractionTime = System.currentTimeMillis())

    /**
     * Starts analysis of the given content.
     */
    fun startAnalysis(content: String, analyzer: ContentAnalyzer): AIAssistantBubble {
        // Mark as analyzing
        val analyzing = copy(isAnalyzing = true)

        // Perform analysis asynchronously
        analyzer.analyzeContentAsync(content) { result ->
            // This would be handled by the ViewModel in real implementation
            // For now, we'll simulate completion
            completeAnalysis(result)
        }

        return analyzing
    }

    /**
     * Completes the analysis with results.
     */
    fun completeAnalysis(result: ContentAnalysisResult): AIAssistantBubble {
        val newRecord = AnalysisRecord(
            content = result.originalContent.take(100) + if (result.originalContent.length > 100) "..." else "",
            contentType = result.contentType,
            confidence = result.confidence,
            insights = result.insights,
            actions = result.suggestedActions,
            timestamp = System.currentTimeMillis()
        )

        return copy(
            analysisResult = result,
            isAnalyzing = false,
            analysisHistory = (listOf(newRecord) + analysisHistory).take(10) // Keep last 10
        )
    }

    /**
     * Gets the most relevant suggested actions based on current analysis.
     */
    fun getTopSuggestedActions(limit: Int = 5): List<SuggestedAction> {
        return analysisResult?.suggestedActions
            ?.sortedByDescending { it.priority.ordinal }
            ?.take(limit)
            ?: emptyList()
    }

    /**
     * Checks if a specific AI feature is enabled.
     */
    fun isFeatureEnabled(feature: AIFeature): Boolean = feature in enabledFeatures

    /**
     * Enables or disables an AI feature.
     */
    fun withFeatureEnabled(feature: AIFeature, enabled: Boolean): AIAssistantBubble {
        val newFeatures = if (enabled) {
            enabledFeatures + feature
        } else {
            enabledFeatures - feature
        }
        return copy(enabledFeatures = newFeatures)
    }

    /**
     * Gets analysis statistics.
     */
    fun getAnalysisStats(): AnalysisStats {
        val totalAnalyses = analysisHistory.size
        val averageConfidence = if (totalAnalyses > 0) {
            analysisHistory.map { it.confidence }.average().toFloat()
        } else 0f

        val contentTypeDistribution = analysisHistory
            .groupBy { it.contentType }
            .mapValues { it.value.size }

        val mostUsedActions = analysisHistory
            .flatMap { it.actions }
            .groupBy { it.id }
            .mapValues { it.value.size }
            .entries
            .sortedByDescending { it.value }
            .take(5)
            .associate { it.key to it.value }

        return AnalysisStats(
            totalAnalyses = totalAnalyses,
            averageConfidence = averageConfidence,
            contentTypeDistribution = contentTypeDistribution,
            mostUsedActions = mostUsedActions,
            analysisHistorySize = analysisHistory.size
        )
    }

    /**
     * Clears analysis history.
     */
    fun clearAnalysisHistory(): AIAssistantBubble =
        copy(analysisHistory = emptyList())

    /**
     * Gets the current analysis status for UI display.
     */
    fun getAnalysisStatus(): AnalysisStatus {
        return when {
            isAnalyzing -> AnalysisStatus.ANALYZING
            analysisResult != null -> AnalysisStatus.COMPLETED
            analysisHistory.isNotEmpty() -> AnalysisStatus.HAS_HISTORY
            else -> AnalysisStatus.IDLE
        }
    }

    companion object {
        /**
         * Creates an AI Assistant bubble with default settings.
         */
        fun createDefault(): AIAssistantBubble {
            return AIAssistantBubble(
                id = "ai_assistant_${System.currentTimeMillis()}",
                autoAnalyze = true,
                enabledFeatures = setOf(
                    AIFeature.CONTENT_ANALYSIS,
                    AIFeature.ACTION_SUGGESTIONS,
                    AIFeature.INSIGHT_GENERATION,
                    AIFeature.AUTOMATED_ACTIONS
                )
            )
        }

        /**
         * Creates an AI Assistant bubble optimized for content analysis only.
         */
        fun createAnalyzer(): AIAssistantBubble {
            return AIAssistantBubble(
                id = "content_analyzer_${System.currentTimeMillis()}",
                enabledFeatures = setOf(AIFeature.CONTENT_ANALYSIS, AIFeature.INSIGHT_GENERATION)
            )
        }

        /**
         * Creates an AI Assistant bubble focused on automation.
         */
        fun createAutomator(): AIAssistantBubble {
            return AIAssistantBubble(
                id = "automation_assistant_${System.currentTimeMillis()}",
                enabledFeatures = setOf(AIFeature.AUTOMATED_ACTIONS, AIFeature.ACTION_SUGGESTIONS)
            )
        }
    }
}

/**
 * AI features that can be enabled/disabled.
 */
enum class AIFeature {
    CONTENT_ANALYSIS,      // Analyze content type and structure
    INSIGHT_GENERATION,    // Generate insights about content
    ACTION_SUGGESTIONS,    // Suggest relevant actions
    AUTOMATED_ACTIONS,     // Automatically perform actions
    LEARNING_ADAPTATION,   // Learn from user behavior
    CONTEXT_AWARENESS      // Consider app context and history
}

/**
 * Analysis record for tracking analysis history.
 */
data class AnalysisRecord(
    val content: String,
    val contentType: com.clipboardhistory.utils.ContentType,
    val confidence: Float,
    val insights: List<com.clipboardhistory.utils.ContentInsight>,
    val actions: List<SuggestedAction>,
    val timestamp: Long
)

/**
 * Analysis statistics.
 */
data class AnalysisStats(
    val totalAnalyses: Int,
    val averageConfidence: Float,
    val contentTypeDistribution: Map<com.clipboardhistory.utils.ContentType, Int>,
    val mostUsedActions: Map<String, Int>,
    val analysisHistorySize: Int
)

/**
 * Current analysis status for UI state management.
 */
enum class AnalysisStatus {
    IDLE,           // No analysis in progress, no results
    ANALYZING,      // Currently analyzing content
    COMPLETED,      // Has current analysis results
    HAS_HISTORY     // Has analysis history but no current results
}