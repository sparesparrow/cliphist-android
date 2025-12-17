package com.clipboardhistory.presentation.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.view.WindowManager
import com.clipboardhistory.presentation.ui.toolbelt.OverlayToolBeltWidget

class ToolBeltService : Service() {
    private lateinit var toolBeltWidget: OverlayToolBeltWidget
    private lateinit var windowManager: WindowManager

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        toolBeltWidget = OverlayToolBeltWidget(this, windowManager)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when(intent?.action) {
            ACTION_SHOW_TOOLBELT -> toolBeltWidget.show()
            ACTION_HIDE_TOOLBELT -> toolBeltWidget.hide()
            ACTION_TOGGLE_TOOLBELT -> toolBeltWidget.toggle()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        toolBeltWidget.destroy()
        super.onDestroy()
    }

    companion object {
        const val ACTION_SHOW_TOOLBELT = "com.clipboardhistory.SHOW_TOOLBELT"
        const val ACTION_HIDE_TOOLBELT = "com.clipboardhistory.HIDE_TOOLBELT"
        const val ACTION_TOGGLE_TOOLBELT = "com.clipboardhistory.TOGGLE_TOOLBELT"
    }
}