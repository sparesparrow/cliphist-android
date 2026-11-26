package com.clipboardhistory.presentation.services

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.PersistableBundle
import androidx.core.content.ContextCompat
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.SecureRandom

/**
 * Turns the local API for agents ([LocalApiService], 127.0.0.1:8765) on and off.
 *
 * Off by default. Turning it on generates a random Bearer token, keeps it in
 * EncryptedSharedPreferences and starts the service with it; [BootReceiver] starts it again after a
 * reboot while it stays on. Turning it off stops the service and forgets the token, so the next
 * enable issues a new one. The token is never logged.
 */
object LocalApiControl {
    private const val PREFS = "local_api_prefs"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_TOKEN = "token"
    private const val TOKEN_BYTES = 32

    /** ClipDescription extra (API 33+ constant, honoured as a plain extra before) for sensitive clips. */
    const val EXTRA_IS_SENSITIVE = "android.content.extra.IS_SENSITIVE"

    /** 32 random bytes as lowercase hex: 64 characters, safe in a header and a shell file. */
    internal fun newToken(random: SecureRandom = SecureRandom()): String =
        ByteArray(TOKEN_BYTES).also(random::nextBytes).joinToString("") { "%02x".format(it) }

    private fun prefs(context: Context): SharedPreferences {
        val app = context.applicationContext
        val key = MasterKey.Builder(app).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        return EncryptedSharedPreferences.create(
            app,
            PREFS,
            key,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    fun isEnabled(context: Context): Boolean = prefs(context).getBoolean(KEY_ENABLED, false)

    /** The current token, or null while the API is off. */
    fun token(context: Context): String? =
        prefs(context).takeIf { it.getBoolean(KEY_ENABLED, false) }?.getString(KEY_TOKEN, null)

    /** Turns the API on (keeping an existing token) and returns the token. */
    fun enable(context: Context): String {
        val p = prefs(context)
        val token = p.getString(KEY_TOKEN, null)?.takeIf { it.isNotBlank() } ?: newToken()
        p.edit().putBoolean(KEY_ENABLED, true).putString(KEY_TOKEN, token).apply()
        start(context, token)
        return token
    }

    fun disable(context: Context) {
        prefs(context).edit().putBoolean(KEY_ENABLED, false).remove(KEY_TOKEN).apply()
        context.stopService(Intent(context, LocalApiService::class.java))
    }

    /** Called after boot: starts the service only when the API was left on. */
    fun startIfEnabled(context: Context) {
        val token = try { token(context) } catch (e: Exception) { null } ?: return
        start(context, token)
    }

    private fun start(context: Context, token: String) {
        ContextCompat.startForegroundService(context, LocalApiService.buildStartIntent(context, token))
    }

    /**
     * Copies the token so it can be pasted into the agent's token file. The clip is marked
     * sensitive: the system hides its preview, and [ClipboardService] does not store it.
     */
    fun copyToken(context: Context, token: String) {
        val clip = ClipData.newPlainText("cliphist API token", token)
        clip.description.extras = PersistableBundle().apply { putBoolean(EXTRA_IS_SENSITIVE, true) }
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(clip)
    }

    /** True when a clip was marked sensitive by the app that copied it (password managers, us). */
    fun isSensitive(description: ClipDescription?): Boolean {
        return description?.extras?.getBoolean(EXTRA_IS_SENSITIVE, false) == true
    }
}
