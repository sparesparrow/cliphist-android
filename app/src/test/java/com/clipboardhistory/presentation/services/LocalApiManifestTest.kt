package com.clipboardhistory.presentation.services

import org.junit.Assert.assertEquals
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

    @Test
    fun localApiListensOnIpv4Loopback() {
        // Clients (the MCP server, curl, agents) all use http://127.0.0.1:8765.
        assertEquals("127.0.0.1", LocalApiService.BIND_ADDRESS.hostAddress)
        assertTrue(LocalApiService.BIND_ADDRESS is java.net.Inet4Address)
    }
}
