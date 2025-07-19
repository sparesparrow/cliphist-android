package com.clipboardhistory.presentation

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.clipboardhistory.presentation.services.ClipboardService
import com.clipboardhistory.presentation.services.FloatingBubbleService
import com.clipboardhistory.presentation.ui.screens.MainScreen
import com.clipboardhistory.presentation.ui.theme.ClipboardHistoryTheme
import com.clipboardhistory.presentation.viewmodels.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main Activity for the Clipboard History application.
 * 
 * This activity serves as the entry point for the application and manages
 * the overall UI and service lifecycle.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (Settings.canDrawOverlays(this)) {
            startServices()
        }
    }
    
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            requestOverlayPermission()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            ClipboardHistoryTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val viewModel: MainViewModel = hiltViewModel()
                    val context = LocalContext.current
                    
                    MainScreen(
                        viewModel = viewModel,
                        onStartServices = {
                            requestPermissions()
                        },
                        onStopServices = {
                            stopServices()
                        }
                    )
                }
            }
        }
    }
    
    /**
     * Requests necessary permissions for the application.
     */
    private fun requestPermissions() {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) != android.content.pm.PackageManager.PERMISSION_GRANTED
                ) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    requestOverlayPermission()
                }
            }
            else -> {
                requestOverlayPermission()
            }
        }
    }
    
    /**
     * Requests overlay permission for floating bubbles.
     */
    private fun requestOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            overlayPermissionLauncher.launch(intent)
        } else {
            startServices()
        }
    }
    
    /**
     * Starts the clipboard and floating bubble services.
     */
    private fun startServices() {
        val clipboardIntent = Intent(this, ClipboardService::class.java)
        val bubbleIntent = Intent(this, FloatingBubbleService::class.java)
        
        startForegroundService(clipboardIntent)
        startForegroundService(bubbleIntent)
    }
    
    /**
     * Stops the clipboard and floating bubble services.
     */
    private fun stopServices() {
        val clipboardIntent = Intent(this, ClipboardService::class.java)
        val bubbleIntent = Intent(this, FloatingBubbleService::class.java)
        
        stopService(clipboardIntent)
        stopService(bubbleIntent)
    }
}