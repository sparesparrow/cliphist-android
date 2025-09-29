package com.clipboardhistory.domain.model

/**
 * Minimal content analyzer to provide smart actions based on simple heuristics.
 */
object ContentAnalyzer {
    enum class Type { URL, PHONE, EMAIL, MAPS, TEXT }

    fun analyzeContentType(content: String): Type {
        val trimmed = content.trim()
        return when {
            trimmed.startsWith("http://") || trimmed.startsWith("https://") -> Type.URL
            Regex("^\\+?[0-9 -]{6,}").matches(trimmed) -> Type.PHONE
            Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$").matches(trimmed) -> Type.EMAIL
            trimmed.length in 3..100 && trimmed.contains(" ") -> Type.MAPS
            else -> Type.TEXT
        }
    }

    fun getSmartActions(type: Type, content: String): List<SmartAction> {
        return when (type) {
            Type.URL -> listOf(SmartAction("Open Link", BubbleState.REPLACE))
            Type.PHONE -> listOf(SmartAction("Call Number", BubbleState.REPLACE))
            Type.EMAIL -> listOf(SmartAction("Send Email", BubbleState.REPLACE))
            Type.MAPS -> listOf(SmartAction("Open Maps", BubbleState.REPLACE))
            Type.TEXT -> listOf(SmartAction("Search Text", BubbleState.REPLACE))
        }
    }
}

