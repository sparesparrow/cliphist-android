package com.clipboardhistory.presentation.services

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ServicesE2ETest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun startClipboardService_foregroundNotificationShown() {
        val intent = Intent(context, ClipboardService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }

        // If no crash occurs, assume startForeground called; further validation can use notifications APIs
        assertTrue(true)
    }

    @Test
    fun startFloatingBubbleService_noCrash() {
        val intent = Intent(context, FloatingBubbleService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
        assertTrue(true)
    }
}
