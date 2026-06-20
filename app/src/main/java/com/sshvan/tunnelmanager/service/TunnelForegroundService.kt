package com.sshvan.tunnelmanager.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.sshvan.tunnelmanager.R
import com.sshvan.tunnelmanager.domain.model.ConnectionProfile
import com.sshvan.tunnelmanager.domain.model.TunnelStatus
import com.sshvan.tunnelmanager.presentation.ui.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Foreground Service that keeps the SSH tunnel alive in the background.
 * Uses a persistent notification and WakeLock to prevent Android
 * from killing the connection.
 */
@AndroidEntryPoint
class TunnelForegroundService : Service() {

    companion object {
        private const val TAG = "TunnelService"
        const val CHANNEL_ID = "ssh_tunnel_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_CONNECT = "com.sshvan.tunnelmanager.ACTION_CONNECT"
        const val ACTION_DISCONNECT = "com.sshvan.tunnelmanager.ACTION_DISCONNECT"
        const val EXTRA_PROFILE_ID = "extra_profile_id"

        private const val WAKELOCK_TAG = "SSHTunnelManager::TunnelWakeLock"

        fun createConnectIntent(context: Context, profileId: Long): Intent {
            return Intent(context, TunnelForegroundService::class.java).apply {
                action = ACTION_CONNECT
                putExtra(EXTRA_PROFILE_ID, profileId)
            }
        }

        fun createDisconnectIntent(context: Context): Intent {
            return Intent(context, TunnelForegroundService::class.java).apply {
                action = ACTION_DISCONNECT
            }
        }
    }

    @Inject
    lateinit var sshManager: SshManager

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        observeTunnelState()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CONNECT -> {
                // Start as foreground immediately to avoid ANR
                androidx.core.app.ServiceCompat.startForeground(
                    this, 
                    NOTIFICATION_ID, 
                    createNotification("Connecting...", null),
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q)
                        android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
                    else 0
                )
                acquireWakeLock()
            }
            ACTION_DISCONNECT -> {
                serviceScope.launch {
                    sshManager.disconnect()
                }
                releaseWakeLock()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            else -> {
                // Service restarted by system, show idle notification
                androidx.core.app.ServiceCompat.startForeground(
                    this,
                    NOTIFICATION_ID,
                    createNotification("SSH Tunnel Service Ready", null),
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q)
                        android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
                    else 0
                )
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        releaseWakeLock()
        serviceScope.cancel()
        Log.d(TAG, "Service destroyed")
    }

    /**
     * Observe tunnel state changes and update the notification accordingly.
     */
    private fun observeTunnelState() {
        serviceScope.launch {
            sshManager.tunnelState.collect { state ->
                val (title, text) = when (state.status) {
                    TunnelStatus.DISCONNECTED -> {
                        "Disconnected" to "No active tunnel"
                    }
                    TunnelStatus.CONNECTING -> {
                        "Connecting..." to (state.activeProfile?.sshDescription() ?: "")
                    }
                    TunnelStatus.CONNECTED -> {
                        val profile = state.activeProfile
                        "Connected" to (profile?.tunnelDescription() ?: "Tunnel active")
                    }
                    TunnelStatus.RECONNECTING -> {
                        "Reconnecting..." to (state.errorMessage ?: "Attempting to reconnect")
                    }
                    TunnelStatus.ERROR -> {
                        "Connection Error" to (state.errorMessage ?: "Unknown error")
                    }
                }

                val notification = createNotification(title, text)
                val notificationManager = getSystemService(NotificationManager::class.java)
                notificationManager.notify(NOTIFICATION_ID, notification)

                // Note: We don't auto-stop the service here on DISCONNECTED.
                // The service is explicitly stopped via ACTION_DISCONNECT intent.
                // Stopping it here causes a race condition when reconnecting (StateFlow emits initial DISCONNECTED state).
            }
        }
    }

    /**
     * Create the notification channel required for Android 8+.
     */
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "SSH Tunnel Status",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows the status of active SSH tunnels"
            setShowBadge(false)
        }
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    /**
     * Create or update the foreground notification.
     */
    private fun createNotification(title: String, text: String?): Notification {
        // Intent to open the app when notification is tapped
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Disconnect action button
        val disconnectIntent = createDisconnectIntent(this)
        val disconnectPendingIntent = PendingIntent.getService(
            this, 1, disconnectIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_tunnel)
            .setOngoing(true)
            .setContentIntent(openAppPendingIntent)
            .addAction(
                R.drawable.ic_disconnect,
                "Disconnect",
                disconnectPendingIntent
            )
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    /**
     * Acquire a partial WakeLock to keep the CPU running while tunnel is active.
     */
    private fun acquireWakeLock() {
        if (wakeLock == null) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                WAKELOCK_TAG
            ).apply {
                acquire(8 * 60 * 60 * 1000L) // Max 8 hours as safety timeout
            }
            Log.d(TAG, "WakeLock acquired")
        }
    }

    /**
     * Release the WakeLock.
     */
    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
                Log.d(TAG, "WakeLock released")
            }
        }
        wakeLock = null
    }
}
