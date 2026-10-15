package com.clipboardhistory.presentation.services

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalApiAuthTest {
    @Test
    fun blankToken_neverAuthorises_evenWithoutOrWithMatchingHeader() {
        assertFalse(isAuthorized("", null))
        assertFalse(isAuthorized("", ""))
        assertFalse(isAuthorized("", "Bearer "))
        assertFalse(isAuthorized("   ", "Bearer    "))
    }

    @Test
    fun correctBearerToken_isAuthorised() {
        assertTrue(isAuthorized("s3cret-token", "Bearer s3cret-token"))
    }

    @Test
    fun wrongOrMissingHeader_isRejected() {
        assertFalse(isAuthorized("s3cret-token", null))
        assertFalse(isAuthorized("s3cret-token", ""))
        assertFalse(isAuthorized("s3cret-token", "Bearer other"))
        assertFalse(isAuthorized("s3cret-token", "s3cret-token"))
        assertFalse(isAuthorized("s3cret-token", "bearer s3cret-token"))
        assertFalse(isAuthorized("s3cret-token", "Bearer s3cret-token "))
    }
}
