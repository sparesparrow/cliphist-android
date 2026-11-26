package com.clipboardhistory.presentation.services

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalApiControlTest {
    @Test
    fun newToken_is64LowercaseHexCharacters() {
        val t = LocalApiControl.newToken()
        assertEquals(64, t.length)
        assertTrue(t.all { it in '0'..'9' || it in 'a'..'f' })
    }

    @Test
    fun newToken_differsEachTime() {
        val tokens = (1..20).map { LocalApiControl.newToken() }.toSet()
        assertEquals(20, tokens.size)
        assertNotEquals(LocalApiControl.newToken(), LocalApiControl.newToken())
    }

    @Test
    fun noDescription_isNotSensitive() {
        assertFalse(LocalApiControl.isSensitive(null))
    }
}
