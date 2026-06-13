package com.clipboardhistory.domain.usecase

import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DevToolHelperUseCaseTest {

    private lateinit var useCase: DevToolHelperUseCase

    @Before
    fun setup() {
        useCase = DevToolHelperUseCase()
    }

    // --- detectLanguage ---

    @Test
    fun `detectLanguage returns KOTLIN for fun keyword`() {
        assertEquals(CodeLanguage.KOTLIN, useCase.detectLanguage("fun main() {}"))
    }

    @Test
    fun `detectLanguage returns KOTLIN for val keyword`() {
        assertEquals(CodeLanguage.KOTLIN, useCase.detectLanguage("val x = 5"))
    }

    @Test
    fun `detectLanguage returns PYTHON for def keyword without braces`() {
        assertEquals(CodeLanguage.PYTHON, useCase.detectLanguage("def greet(name):\n    return name"))
    }

    @Test
    fun `detectLanguage returns JAVA for public class`() {
        assertEquals(CodeLanguage.JAVA, useCase.detectLanguage("public class Foo {}"))
    }

    @Test
    fun `detectLanguage returns JAVA for void keyword`() {
        assertEquals(CodeLanguage.JAVA, useCase.detectLanguage("void doSomething() {}"))
    }

    @Test
    fun `detectLanguage returns JAVASCRIPT for function keyword`() {
        assertEquals(CodeLanguage.JAVASCRIPT, useCase.detectLanguage("function hello() {}"))
    }

    @Test
    fun `detectLanguage returns JAVASCRIPT for const keyword`() {
        assertEquals(CodeLanguage.JAVASCRIPT, useCase.detectLanguage("const x = 42;"))
    }

    @Test
    fun `detectLanguage returns CPP for include directive`() {
        assertEquals(CodeLanguage.CPP, useCase.detectLanguage("#include <iostream>"))
    }

    @Test
    fun `detectLanguage returns CPP for std namespace`() {
        assertEquals(CodeLanguage.CPP, useCase.detectLanguage("std::cout << \"hello\";"))
    }

    @Test
    fun `detectLanguage returns UNKNOWN for plain text`() {
        assertEquals(CodeLanguage.UNKNOWN, useCase.detectLanguage("hello world"))
    }

    // --- generateHelper: DOCSTRING ---

    @Test
    fun `generateHelper DOCSTRING for Kotlin contains KDoc markers`() {
        val result = useCase.generateHelper("fun calculate(x: Int): Int { return x }", CodeLanguage.KOTLIN, HelperType.DOCSTRING)
        assertTrue(result.contains("/**") && result.contains("*/"))
        assertTrue(result.contains("@param") && result.contains("@return"))
    }

    @Test
    fun `generateHelper DOCSTRING for Python contains triple quotes`() {
        val result = useCase.generateHelper("def add(a, b):\n    return a + b", CodeLanguage.PYTHON, HelperType.DOCSTRING)
        assertTrue(result.contains("\"\"\""))
        assertTrue(result.contains("Args:") && result.contains("Returns:"))
    }

    @Test
    fun `generateHelper DOCSTRING for Java contains Javadoc markers`() {
        val result = useCase.generateHelper("public int add(int a) { return a; }", CodeLanguage.JAVA, HelperType.DOCSTRING)
        assertTrue(result.contains("/**") && result.contains("*/"))
    }

    @Test
    fun `generateHelper DOCSTRING for JavaScript contains JSDoc markers`() {
        val result = useCase.generateHelper("function greet(name) { return name; }", CodeLanguage.JAVASCRIPT, HelperType.DOCSTRING)
        assertTrue(result.contains("/**") && result.contains("@param") && result.contains("@returns"))
    }

    @Test
    fun `generateHelper DOCSTRING for CPP contains brief tag`() {
        val result = useCase.generateHelper("int add(int a, int b) { return a + b; }", CodeLanguage.CPP, HelperType.DOCSTRING)
        assertTrue(result.contains("@brief"))
    }

    @Test
    fun `generateHelper DOCSTRING for UNKNOWN returns single line comment`() {
        val result = useCase.generateHelper("some code", CodeLanguage.UNKNOWN, HelperType.DOCSTRING)
        assertTrue(result.startsWith("//"))
    }

    // --- generateHelper: TEST_STUB ---

    @Test
    fun `generateHelper TEST_STUB for Kotlin contains @Test and fun test`() {
        val result = useCase.generateHelper("fun calculate(): Int = 0", CodeLanguage.KOTLIN, HelperType.TEST_STUB)
        assertTrue(result.contains("@Test"))
        assertTrue(result.contains("fun test"))
    }

    @Test
    fun `generateHelper TEST_STUB for Python contains def test_`() {
        val result = useCase.generateHelper("def add(a, b):\n    return a + b", CodeLanguage.PYTHON, HelperType.TEST_STUB)
        assertTrue(result.contains("def test_"))
    }

    @Test
    fun `generateHelper TEST_STUB for Java contains @Test annotation`() {
        val result = useCase.generateHelper("public int add(int a) {}", CodeLanguage.JAVA, HelperType.TEST_STUB)
        assertTrue(result.contains("@Test"))
        assertTrue(result.contains("public void test"))
    }

    @Test
    fun `generateHelper TEST_STUB for JavaScript uses test() function`() {
        val result = useCase.generateHelper("function greet() {}", CodeLanguage.JAVASCRIPT, HelperType.TEST_STUB)
        assertTrue(result.contains("test("))
    }

    @Test
    fun `generateHelper TEST_STUB for CPP uses TEST macro`() {
        val result = useCase.generateHelper("int add(int a, int b) {}", CodeLanguage.CPP, HelperType.TEST_STUB)
        assertTrue(result.contains("TEST("))
    }

    // --- generateHelper: README_SNIPPET ---

    @Test
    fun `generateHelper README_SNIPPET for Kotlin wraps in kotlin code fence`() {
        val code = "fun hello() {}"
        val result = useCase.generateHelper(code, CodeLanguage.KOTLIN, HelperType.README_SNIPPET)
        assertTrue(result.startsWith("```kotlin"))
        assertTrue(result.endsWith("```"))
        assertTrue(result.contains(code))
    }

    @Test
    fun `generateHelper README_SNIPPET for Python wraps in python code fence`() {
        val code = "def hello(): pass"
        val result = useCase.generateHelper(code, CodeLanguage.PYTHON, HelperType.README_SNIPPET)
        assertTrue(result.startsWith("```python"))
    }

    @Test
    fun `generateHelper README_SNIPPET for UNKNOWN wraps in generic code fence`() {
        val code = "some text"
        val result = useCase.generateHelper(code, CodeLanguage.UNKNOWN, HelperType.README_SNIPPET)
        assertTrue(result.startsWith("```\n"))
    }

    // --- edge cases ---

    @Test
    fun `generateHelper handles empty content without crashing`() {
        val result = useCase.generateHelper("", CodeLanguage.KOTLIN, HelperType.DOCSTRING)
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun `generateHelper uses fallback name when no function found`() {
        val result = useCase.generateHelper("val x = 5", CodeLanguage.KOTLIN, HelperType.DOCSTRING)
        assertTrue(result.contains("function") || result.contains("TODO"))
    }
}
