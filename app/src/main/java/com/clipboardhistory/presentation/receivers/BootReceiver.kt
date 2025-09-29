package com.clipboardhistory.presentation.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.clipboardhistory.presentation.services.ClipboardService
import com.clipboardhistory.presentation.services.FloatingBubbleService
import com.clipboardhistory.utils.PermissionUtils

/**
 * Receives BOOT_COMPLETED and starts core services if permissions are granted.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            if (PermissionUtils.hasAllRequiredPermissions(context)) {
                val clipboardIntent = Intent(context, ClipboardService::class.java)
                val bubbleIntent = Intent(context, FloatingBubbleService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(clipboardIntent)
                    context.startForegroundService(bubbleIntent)
                } else {
                    context.startService(clipboardIntent)
                    context.startService(bubbleIntent)
                }
            }
        }
    }
}
