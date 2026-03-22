package com.clipboardhistory.domain.usecase

import com.clipboardhistory.data.ai.ClaudeApiClient
import com.clipboardhistory.data.ai.ClaudeResult
import javax.inject.Inject

/**
 * Use case for analyzing clipboard content using the Claude API.
 *
 * Wraps [ClaudeApiClient] with the domain layer contract and provides
 * pre-built prompt templates for common clipboard operations.
 */
class AnalyzeWithClaudeUseCase @Inject constructor(
    private val client: ClaudeApiClient,
) {
    /**
     * Analyze clipboard content: detects type, summarizes, suggests actions.
     */
    suspend fun analyze(content: String, apiKey: String): ClaudeResult =
        client.analyzeClipboardContent(content, apiKey)

    /**
     * Improve grammar and style of the given text.
     */
    suspend fun improveText(content: String, apiKey: String): ClaudeResult =
        client.transformText(content, "Improve the grammar and style of this text. Return only the improved text, no explanations", apiKey)

    /**
     * Summarize the given content.
     */
    suspend fun summarize(content: String, apiKey: String): ClaudeResult =
        client.transformText(content, "Summarize this content in 2-3 sentences", apiKey)

    /**
     * Translate text to the specified language.
     */
    suspend fun translate(content: String, targetLanguage: String, apiKey: String): ClaudeResult =
        client.transformText(content, "Translate this to $targetLanguage. Return only the translation", apiKey)

    /**
     * Explain what the given code does.
     */
    suspend fun explainCode(content: String, apiKey: String): ClaudeResult =
        client.transformText(content, "Explain what this code does in plain English", apiKey)

    /**
     * Extract key data points (dates, names, numbers, etc.) from text.
     */
    suspend fun extractData(content: String, apiKey: String): ClaudeResult =
        client.transformText(
            content,
            "Extract all key data points (names, dates, numbers, URLs, emails, addresses) from this text. Format as a bullet list",
            apiKey,
        )

    /**
     * Ask a free-form question about the clipboard content.
     */
    suspend fun askAbout(content: String, question: String, apiKey: String): ClaudeResult =
        client.complete(
            prompt = "Content:\n$content\n\nQuestion: $question",
            apiKey = apiKey,
            maxTokens = 1024,
        )
}
