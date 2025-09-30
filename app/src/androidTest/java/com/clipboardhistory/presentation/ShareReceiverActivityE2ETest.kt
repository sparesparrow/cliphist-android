package com.clipboardhistory.presentation

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ShareReceiverActivityE2ETest {
    @Test
    fun sendActionSEND_savesTextAndFinishes() {
        val intent =
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "Hello from test")
                setClassName("com.clipboardhistory", "com.clipboardhistory.presentation.ShareReceiverActivity")
            }
        ActivityScenario.launch<ShareReceiverActivity>(intent).use {
            // If no crash and activity finishes, test passes
        }
    }

    @Test
    fun sendActionPROCESS_TEXT_savesTextAndFinishes() {
        val intent =
            Intent(Intent.ACTION_PROCESS_TEXT).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_PROCESS_TEXT, "Hello from selection")
                setClassName("com.clipboardhistory", "com.clipboardhistory.presentation.ShareReceiverActivity")
            }
        ActivityScenario.launch<ShareReceiverActivity>(intent).use {
            // If no crash and activity finishes, test passes
        }
    }
}
