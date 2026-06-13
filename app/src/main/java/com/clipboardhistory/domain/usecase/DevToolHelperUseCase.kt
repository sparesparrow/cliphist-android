package com.clipboardhistory.domain.usecase

class DevToolHelperUseCase {

    fun detectLanguage(content: String): CodeLanguage {
        return when {
            content.contains("fun ") || content.contains("val ") -> CodeLanguage.KOTLIN
            content.contains("def ") && !content.contains("{") -> CodeLanguage.PYTHON
            content.contains("public class") || content.contains("void ") -> CodeLanguage.JAVA
            content.contains("function ") || content.contains("const ") -> CodeLanguage.JAVASCRIPT
            content.contains("#include") || content.contains("std::") -> CodeLanguage.CPP
            else -> CodeLanguage.UNKNOWN
        }
    }

    fun generateHelper(content: String, language: CodeLanguage, type: HelperType): String {
        return when (type) {
            HelperType.DOCSTRING -> generateDocstring(content, language)
            HelperType.TEST_STUB -> generateTestStub(content, language)
            HelperType.README_SNIPPET -> generateReadmeSnippet(content, language)
        }
    }

    private fun generateDocstring(content: String, language: CodeLanguage): String {
        val funcName = extractFunctionName(content, language) ?: "function"
        return when (language) {
            CodeLanguage.KOTLIN -> "/**\n * TODO: Describe $funcName.\n *\n * @param TODO describe parameters\n * @return TODO describe return value\n */"
            CodeLanguage.JAVA -> "/**\n * TODO: Describe $funcName.\n *\n * @param TODO describe parameters\n * @return TODO describe return value\n */"
            CodeLanguage.PYTHON -> "\"\"\"\nTODO: Describe $funcName.\n\nArgs:\n    TODO: describe parameters\n\nReturns:\n    TODO: describe return value\n\"\"\""
            CodeLanguage.JAVASCRIPT -> "/**\n * TODO: Describe $funcName.\n *\n * @param {*} TODO describe parameters\n * @returns {*} TODO describe return value\n */"
            CodeLanguage.CPP -> "/**\n * @brief TODO: Describe $funcName.\n *\n * @param TODO describe parameters\n * @return TODO describe return value\n */"
            CodeLanguage.UNKNOWN -> "// TODO: Describe this code block"
        }
    }

    private fun generateTestStub(content: String, language: CodeLanguage): String {
        val funcName = extractFunctionName(content, language) ?: "function"
        val capitalizedName = funcName.replaceFirstChar { it.uppercaseChar() }
        return when (language) {
            CodeLanguage.KOTLIN -> "@Test\nfun test$capitalizedName() {\n    TODO(\"Implement test for $funcName\")\n}"
            CodeLanguage.JAVA -> "@Test\npublic void test$capitalizedName() {\n    // TODO: Implement test for $funcName\n    throw new UnsupportedOperationException(\"Not implemented\");\n}"
            CodeLanguage.PYTHON -> "def test_$funcName():\n    # TODO: Implement test for $funcName\n    raise NotImplementedError"
            CodeLanguage.JAVASCRIPT -> "test('$funcName', () => {\n    // TODO: Implement test for $funcName\n    throw new Error('Not implemented');\n});"
            CodeLanguage.CPP -> "TEST($capitalizedName, Basic) {\n    // TODO: Implement test for $funcName\n    FAIL() << \"Not implemented\";\n}"
            CodeLanguage.UNKNOWN -> "// TODO: Write a test for this code block"
        }
    }

    private fun generateReadmeSnippet(content: String, language: CodeLanguage): String {
        val tag = when (language) {
            CodeLanguage.KOTLIN -> "kotlin"
            CodeLanguage.JAVA -> "java"
            CodeLanguage.PYTHON -> "python"
            CodeLanguage.JAVASCRIPT -> "javascript"
            CodeLanguage.CPP -> "cpp"
            CodeLanguage.UNKNOWN -> ""
        }
        return "```$tag\n$content\n```"
    }

    private fun extractFunctionName(content: String, language: CodeLanguage): String? {
        val pattern = when (language) {
            CodeLanguage.KOTLIN -> Regex("fun\\s+(\\w+)")
            CodeLanguage.JAVA -> Regex("(?:public|private|protected)?\\s*\\w+\\s+(\\w+)\\s*\\(")
            CodeLanguage.PYTHON -> Regex("def\\s+(\\w+)")
            CodeLanguage.JAVASCRIPT -> Regex("function\\s+(\\w+)|const\\s+(\\w+)\\s*=")
            CodeLanguage.CPP -> Regex("\\w+\\s+(\\w+)\\s*\\(")
            CodeLanguage.UNKNOWN -> return null
        }
        val match = pattern.find(content) ?: return null
        return match.groupValues.drop(1).firstOrNull { it.isNotBlank() }
    }
}

enum class CodeLanguage { KOTLIN, PYTHON, JAVA, JAVASCRIPT, CPP, UNKNOWN }

enum class HelperType { DOCSTRING, TEST_STUB, README_SNIPPET }
