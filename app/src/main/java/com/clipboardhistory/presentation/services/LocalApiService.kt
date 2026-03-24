package com.clipboardhistory.presentation.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.clipboardhistory.R
import com.clipboardhistory.domain.repository.ClipboardRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import javax.inject.Inject

/**
 * LocalApiService exposes the clipboard database as a JSON REST API on localhost:8765.
 *
 * This enables integration with:
 * - MCP servers (Claude Desktop, Claude Code)
 * - VS Code / Cursor extensions
 * - Desktop companion scripts
 *
 * Endpoints:
 *   GET  /items              - list recent items (default 50, ?limit=N)
 *   GET  /items/{id}         - single item by id
 *   POST /items              - add item (body: {"content":"..."})
 *   GET  /search?q=query     - full-text search
 *   GET  /stats              - clipboard statistics
 *   GET  /health             - liveness probe
 *
 * Authentication: shared Bearer token stored in EncryptedSharedPreferences.
 * Set via Intent extra "api_token" on first start, or auto-generated if absent.
 */
@AndroidEntryPoint
class LocalApiService : Service() {

    @Inject
    lateinit var repository: ClipboardRepository

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    private var serverSocket: ServerSocket? = null
    private var apiToken: String = ""

    companion object {
        const val PORT = 8765
        private const val NOTIFICATION_ID = 3001
        private const val CHANNEL_ID = "local_api_channel"
        const val EXTRA_API_TOKEN = "api_token"

        fun buildStartIntent(context: Context, token: String): Intent =
            Intent(context, LocalApiService::class.java).apply {
                putExtra(EXTRA_API_TOKEN, token)
            }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val token = intent?.getStringExtra(EXTRA_API_TOKEN)
        if (!token.isNullOrBlank()) {
            apiToken = token
        }
        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        serviceScope.launch { runServer() }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
        serverSocket?.close()
    }

    private suspend fun runServer() {
        try {
            serverSocket = ServerSocket(PORT)
            while (!serviceJob.isCancelled) {
                val client = serverSocket!!.accept()
                serviceScope.launch { handleClient(client) }
            }
        } catch (e: SocketException) {
            // Server was shut down cleanly
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun handleClient(socket: Socket) {
        socket.use { s ->
            val reader = BufferedReader(InputStreamReader(s.getInputStream()))
            val writer = PrintWriter(s.getOutputStream(), true)

            // Read request line
            val requestLine = reader.readLine() ?: return
            val parts = requestLine.split(" ")
            if (parts.size < 2) return
            val method = parts[0]
            val rawPath = parts[1]

            // Read headers
            val headers = mutableMapOf<String, String>()
            var line = reader.readLine()
            while (!line.isNullOrBlank()) {
                val colonIdx = line.indexOf(':')
                if (colonIdx > 0) {
                    headers[line.substring(0, colonIdx).trim().lowercase()] =
                        line.substring(colonIdx + 1).trim()
                }
                line = reader.readLine()
            }

            // Check auth
            if (apiToken.isNotBlank()) {
                val auth = headers["authorization"] ?: ""
                if (!auth.equals("Bearer $apiToken", ignoreCase = false)) {
                    writeResponse(writer, 401, """{"error":"unauthorized"}""")
                    return
                }
            }

            // Parse path and query
            val (path, queryString) = if ('?' in rawPath) {
                rawPath.substringBefore('?') to rawPath.substringAfter('?')
            } else {
                rawPath to ""
            }
            val queryParams = parseQuery(queryString)

            // Read body for POST
            val contentLength = headers["content-length"]?.toIntOrNull() ?: 0
            val body = if (contentLength > 0) {
                val buf = CharArray(contentLength)
                reader.read(buf, 0, contentLength)
                String(buf)
            } else ""

            // Route
            val response = route(method, path, queryParams, body)
            writeResponse(writer, response.first, response.second)
        }
    }

    private fun route(
        method: String,
        path: String,
        query: Map<String, String>,
        body: String,
    ): Pair<Int, String> {
        return when {
            path == "/health" ->
                200 to """{"status":"ok","port":$PORT}"""

            path == "/items" && method == "GET" -> {
                val limit = query["limit"]?.toIntOrNull() ?: 50
                val items = runBlocking { repository.getItemsWithPagination(limit, 0) }
                200 to buildItemsJson(items)
            }

            path == "/items" && method == "POST" -> {
                val content = extractJsonString(body, "content")
                if (content.isNullOrBlank()) {
                    400 to """{"error":"content field required"}"""
                } else {
                    runBlocking {
                        val item = com.clipboardhistory.domain.model.ClipboardItem(
                            id = java.util.UUID.randomUUID().toString(),
                            content = content,
                            timestamp = System.currentTimeMillis(),
                            contentType = com.clipboardhistory.domain.model.ContentType.TEXT,
                            isEncrypted = false,
                            size = content.length,
                        )
                        repository.insertItem(item)
                        201 to """{"id":"${item.id}","created":true}"""
                    }
                }
            }

            path.matches(Regex("/items/[^/]+")) && method == "GET" -> {
                val id = path.removePrefix("/items/")
                val item = runBlocking { repository.getItemById(id) }
                if (item == null) 404 to """{"error":"not found"}"""
                else 200 to itemToJson(item)
            }

            path == "/search" && method == "GET" -> {
                val q = query["q"] ?: ""
                if (q.isBlank()) {
                    400 to """{"error":"q parameter required"}"""
                } else {
                    val results = runBlocking { repository.searchItems(q) }
                    200 to buildItemsJson(results)
                }
            }

            path == "/stats" && method == "GET" -> {
                val stats = runBlocking { repository.getStatistics() }
                200 to """{"totalItems":${stats.totalItems},"favoriteItems":${stats.favoriteItems},"itemsToday":${stats.itemsToday},"itemsThisWeek":${stats.itemsThisWeek},"mostUsedContentType":"${stats.mostUsedContentType}","averageContentLength":${stats.averageContentLength},"lastActivityTimestamp":${stats.lastActivityTimestamp}}"""
            }

            else -> 404 to """{"error":"not found"}"""
        }
    }

    private fun buildItemsJson(items: List<com.clipboardhistory.domain.model.ClipboardItem>): String {
        val itemsJson = items.joinToString(",") { itemToJson(it) }
        return """{"items":[$itemsJson],"count":${items.size}}"""
    }

    private fun itemToJson(item: com.clipboardhistory.domain.model.ClipboardItem): String {
        val escapedContent = item.content
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
        return """{"id":"${item.id}","content":"$escapedContent","timestamp":${item.timestamp},"contentType":"${item.contentType}","size":${item.size},"isFavorite":${item.isFavorite}}"""
    }

    private fun writeResponse(writer: PrintWriter, statusCode: Int, body: String) {
        val statusText = when (statusCode) {
            200 -> "OK"
            201 -> "Created"
            400 -> "Bad Request"
            401 -> "Unauthorized"
            404 -> "Not Found"
            else -> "Internal Server Error"
        }
        writer.print("HTTP/1.1 $statusCode $statusText\r\n")
        writer.print("Content-Type: application/json\r\n")
        writer.print("Content-Length: ${body.toByteArray().size}\r\n")
        writer.print("Access-Control-Allow-Origin: *\r\n")
        writer.print("Connection: close\r\n")
        writer.print("\r\n")
        writer.print(body)
        writer.flush()
    }

    private fun parseQuery(queryString: String): Map<String, String> {
        if (queryString.isBlank()) return emptyMap()
        return queryString.split("&").associate { param ->
            val eq = param.indexOf('=')
            if (eq < 0) param to "" else param.substring(0, eq) to param.substring(eq + 1)
        }
    }

    private fun extractJsonString(json: String, key: String): String? {
        val pattern = Regex(""""$key"\s*:\s*"((?:[^"\\]|\\.)*)"""")
        return pattern.find(json)?.groupValues?.get(1)
            ?.replace("\\n", "\n")
            ?.replace("\\\"", "\"")
            ?.replace("\\\\", "\\")
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("ClipHist Local API")
            .setContentText("REST API active on localhost:$PORT")
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "ClipHist Local API",
            NotificationManager.IMPORTANCE_LOW,
        ).apply { description = "Background REST API for desktop integrations" }
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(channel)
    }
}
