package com.clipboardhistory.presentation.services

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SecretRedactorTest {
    private val m = SecretRedactor.MARK

    // Fixtures are glued together at runtime so secret scanners don't flag this file; none of them is a real credential.
    private fun j(vararg parts: String) = parts.joinToString("")

    @Test
    fun oneTimeCodes_areRedacted() {
        assertEquals("Your verification code is $m", SecretRedactor.redact("Your verification code is 482913"))
        assertEquals("Váš ověřovací kód: $m", SecretRedactor.redact("Váš ověřovací kód: 4829"))
        assertEquals("$m is your Google code", SecretRedactor.redact("482913 is your Google code"))
        assertEquals(m, SecretRedactor.redact("123456"))
    }

    @Test
    fun tokensAndKeys_areRedacted() {
        assertEquals(m, SecretRedactor.redact(j("eyJ", "hbGciOiJIUzI1NiJ9", ".eyJ", "zdWIiOiIxMjM0NSJ9", ".abcDEF123_-xyz")))
        assertEquals("key $m here", SecretRedactor.redact(j("key sk", "-ant-api03-", "AbCdEf0123456789xyz here")))
        assertEquals(m, SecretRedactor.redact(j("gh", "p_", "0123456789abcdefABCDEF0123")))
        assertEquals("id $m.", SecretRedactor.redact(j("id AK", "IA", "IOSFODNN7EXAMPLE.")))
        assertEquals("Authorization: Bearer $m", SecretRedactor.redact(j("Authorization: Bear", "er ", "abcdefghijklmnop1234")))
        val pem = j("-----BEGIN PRIV", "ATE KEY-----\nMIIEvQIBADANBg\n-----END PRIV", "ATE KEY-----")
        assertEquals("before $m after", SecretRedactor.redact("before $pem after"))
    }

    @Test
    fun passwords_areRedacted() {
        assertEquals("password: $m", SecretRedactor.redact(j("pass", "word: ", "hunter2!x")))
        assertEquals("heslo=$m", SecretRedactor.redact("heslo=\"tajne heslo\""))
        assertEquals(m, SecretRedactor.redact("Tr0ub4dor&3xyz"))
    }

    @Test
    fun ordinaryText_isUnchanged() {
        listOf(
            "The quick brown fox",
            "Meet at 10:30 in room 204, budget 15000 CZK",
            "see PR #145 and issue 1234",
            "https://example.com/a?b=C1d!",
            "/sdcard/Download/Report-2026_v2!.pdf",
            "",
        ).forEach { assertEquals(it, SecretRedactor.redact(it)) }
    }

    @Test
    fun passwordShape_needsAllFourCharacterClasses_andNoUrlOrPath() {
        assertTrue(SecretRedactor.looksLikePassword("Tr0ub4dor&3xyz"))
        assertFalse(SecretRedactor.looksLikePassword("troubadorxyz3"))
        assertFalse(SecretRedactor.looksLikePassword("https://Ex4mple.com/!"))
        assertFalse(SecretRedactor.looksLikePassword("Short1!"))
        assertFalse(SecretRedactor.looksLikePassword("two Words1!x"))
    }
}
