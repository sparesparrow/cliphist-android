package com.clipboardhistory.data.ai

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Lightweight client for the Anthropic Claude API.
 *
 * Uses HttpURLConnection (no extra dependencies).
 * Configure via [ClaudeConfig]:
 *   - apiKey: your Anthropic API key (stored encrypted via EncryptedSharedPreferences)
 *   - model: defaults to claude-sonnet-4-6
 *   - maxTokens: response length limit
 */
@Singleton
class ClaudeApiClient @Inject constructor() {

    companion object {
        private const val TAG = "ClaudeApiClient"
        private const val API_URL = "https://api.anthropic.com/v1/messages"
        private const val API_VERSION = "2023-06-01"
        private const val DEFAULT_MODEL = "claude-sonnet-4-6"
        private const val DEFAULT_MAX_TOKENS = 1024
        private const val CONNECT_TIMEOUT_MS = 10_000
        private const val READ_TIMEOUT_MS = 30_000
    }

    /**
     * Sends a prompt to Claude and returns the text response.
     *
     * @param prompt The user message to send
     * @param apiKey Anthropic API key
     * @param systemPrompt Optional system prompt
     * @param model Claude model ID (default: claude-sonnet-4-6)
     * @param maxTokens Maximum tokens in response
     */
    suspend fun complete(
        prompt: String,
        apiKey: String,
        systemPrompt: String? = null,
        model: String = DEFAULT_MODEL,
        maxTokens: Int = DEFAULT_MAX_TOKENS,
    ): ClaudeResult = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext ClaudeResult.Error("No API key configured. Set your Anthropic API key in ClipHist Settings.")
        }

        try {
            val requestBody = buildRequestJson(prompt, model, maxTokens, systemPrompt)
            val responseJson = postJson(API_URL, requestBody, apiKey)
            val text = parseTextFromResponse(responseJson)
            ClaudeResult.Success(text)
        } catch (e: Exception) {
            Log.e(TAG, "Claude API call failed", e)
            ClaudeResult.Error(e.message ?: "Unknown error")
        }
    }

    /**
     * Analyze clipboard content: returns a summary, detected type, and suggested actions.
     */
    suspend fun analyzeClipboardContent(
        content: String,
        apiKey: String,
    ): ClaudeResult {
        val systemPrompt = """
            You are a clipboard analysis assistant. When given text content, respond with a concise JSON object:
            {
              "summary": "<1-2 sentence summary of the content>",
              "detectedType": "<one of: url, email, phone, code, address, json, plain_text, other>",
              "language": "<programming language if code, otherwise null>",
              "suggestedActions": ["<action1>", "<action2>"],
              "insights": ["<insight1>", "<insight2>"]
            }
            Keep responses brief and actionable.
        """.trimIndent()

        val prompt = "Analyze this clipboard content:\n\n$content"
        return complete(prompt, apiKey, systemPrompt, maxTokens = 512)
    }

    /**
     * Improve or transform text: rewrite, summarize, translate, fix grammar, etc.
     */
    suspend fun transformText(
        content: String,
        instruction: String,
        apiKey: String,
    ): ClaudeResult {
        val prompt = "$instruction:\n\n$content"
        return complete(prompt, apiKey, maxTokens = 2048)
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun buildRequestJson(
        prompt: String,
        model: String,
        maxTokens: Int,
        systemPrompt: String?,
    ): String {
        val escapedPrompt = prompt.escapeJson()
        val messagesBlock = """[{"role":"user","content":"$escapedPrompt"}]"""
        val systemBlock = if (systemPrompt != null) {
            ""","system":"${systemPrompt.escapeJson()}""""
        } else ""
        return """{"model":"$model","max_tokens":$maxTokens$systemBlock,"messages":$messagesBlock}"""
    }

    private fun postJson(url: String, body: String, apiKey: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("x-api-key", apiKey)
            connection.setRequestProperty("anthropic-version", API_VERSION)
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = READ_TIMEOUT_MS
            connection.doOutput = true

            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(body)
            }

            val statusCode = connection.responseCode
            val inputStream = if (statusCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }

            val responseText = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
                .use { it.readText() }

            if (statusCode !in 200..299) {
                throw ClaudeApiException(statusCode, responseText)
            }

            responseText
        } finally {
            connection.disconnect()
        }
    }

    /**
     * Extracts the first text content block from the Claude API response JSON.
     * Claude response structure: {"content":[{"type":"text","text":"..."}],...}
     */
    private fun parseTextFromResponse(json: String): String {
        val textRegex = Regex(""""text"\s*:\s*"((?:[^"\\]|\\.)*)"""")
        val match = textRegex.find(json)
            ?: throw IllegalStateException("No text content in API response: $json")
        return match.groupValues[1]
            .replace("\\n", "\n")
            .replace("\\\"", "\"")
            .replace("\\\\", "\\")
            .replace("\\/", "/")
    }

    private fun String.escapeJson(): String = this
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t")
}

sealed class ClaudeResult {
    data class Success(val text: String) : ClaudeResult()
    data class Error(val message: String) : ClaudeResult()
}

class ClaudeApiException(val statusCode: Int, val body: String) :
    Exception("Claude API returned $statusCode: $body")
