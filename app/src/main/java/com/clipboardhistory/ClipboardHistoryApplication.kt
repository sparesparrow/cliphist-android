package com.clipboardhistory

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Main application class for the Clipboard History app.
 * 
 * This class serves as the entry point for the application and initializes
 * the Dagger Hilt dependency injection framework.
 */
@HiltAndroidApp
class ClipboardHistoryApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        // Initialize any global components here
    }
}