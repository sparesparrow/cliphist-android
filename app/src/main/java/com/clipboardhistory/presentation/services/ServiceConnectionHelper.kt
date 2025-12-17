package com.clipboardhistory.presentation.services

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Helper class for managing service connections.
 *
 * This class simplifies binding and unbinding to services,
 * providing reactive state management for connection status.
 */
class ServiceConnectionHelper<T>(
    private val context: Context,
    private val serviceClass: Class<T>
) {

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _boundService = MutableStateFlow<T?>(null)
    val boundService: StateFlow<T?> = _boundService.asStateFlow()

    private var serviceConnection: ServiceConnection? = null

    /**
     * Binds to the service.
     *
     * @return true if binding was initiated successfully
     */
    fun bind(): Boolean {
        if (_connectionState.value == ConnectionState.CONNECTED) return true

        _connectionState.value = ConnectionState.CONNECTING

        serviceConnection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                when (service) {
                    is ClipboardMonitorService.ClipboardMonitorBinder -> {
                        @Suppress("UNCHECKED_CAST")
                        _boundService.value = service.getService() as T
                        _connectionState.value = ConnectionState.CONNECTED
                    }
                    else -> {
                        _connectionState.value = ConnectionState.ERROR
                    }
                }
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                _boundService.value = null
                _connectionState.value = ConnectionState.DISCONNECTED
            }
        }

        val intent = Intent(context, serviceClass)
        return context.bindService(intent, serviceConnection!!, Context.BIND_AUTO_CREATE)
    }

    /**
     * Unbinds from the service.
     */
    fun unbind() {
        serviceConnection?.let { connection ->
            try {
                context.unbindService(connection)
            } catch (e: IllegalArgumentException) {
                // Service might already be unbound
            }
        }

        serviceConnection = null
        _boundService.value = null
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    /**
     * Gets the currently bound service instance.
     *
     * @return The bound service or null if not connected
     */
    fun getService(): T? = _boundService.value

    /**
     * Checks if the service is currently bound.
     *
     * @return true if service is bound and connected
     */
    fun isConnected(): Boolean = _connectionState.value == ConnectionState.CONNECTED

    enum class ConnectionState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        ERROR
    }
}