package com.clipboardhistory.presentation.services

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * LocalApiService opens a ServerSocket on 127.0.0.1. Without INTERNET, Android refuses any socket
 * (even loopback) with "Permission denied", so the API never answers. That shipped once unnoticed.
 */
class LocalApiManifestTest {

    @Test
    fun manifestRequestsInternetForTheLocalApi() {
        val manifest = listOf("src/main/AndroidManifest.xml", "app/src/main/AndroidManifest.xml")
            .map(::File).first { it.exists() }.readText()
        assertTrue(
            "AndroidManifest.xml must request android.permission.INTERNET for LocalApiService",
            Regex("""<uses-permission\s+android:name="android\.permission\.INTERNET"\s*/>""").containsMatchIn(manifest),
        )
    }
}
