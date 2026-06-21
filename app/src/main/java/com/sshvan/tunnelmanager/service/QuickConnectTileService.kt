package com.sshvan.tunnelmanager.service

import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.sshvan.tunnelmanager.domain.model.TunnelStatus
import com.sshvan.tunnelmanager.domain.usecase.GetProfilesUseCase
import com.sshvan.tunnelmanager.domain.usecase.UpdateLastUsedUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class QuickConnectTileService : TileService() {

    @Inject
    lateinit var sshManager: SshManager

    @Inject
    lateinit var getProfilesUseCase: GetProfilesUseCase

    @Inject
    lateinit var updateLastUsedUseCase: UpdateLastUsedUseCase

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        val tile = qsTile ?: return

        serviceScope.launch {
            val status = sshManager.tunnelState.value.status
            if (status == TunnelStatus.CONNECTED || status == TunnelStatus.CONNECTING) {
                // Disconnect
                sshManager.disconnect()
                val stopIntent = TunnelForegroundService.createDisconnectIntent(applicationContext)
                startService(stopIntent)
            } else {
                // Connect to the most recently used profile
                val profiles = getProfilesUseCase().first()
                val lastUsedProfile = profiles.maxByOrNull { it.lastUsed ?: it.createdAt }
                
                if (lastUsedProfile != null) {
                    val serviceIntent = TunnelForegroundService.createConnectIntent(
                        applicationContext, lastUsedProfile.id
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(serviceIntent)
                    } else {
                        startService(serviceIntent)
                    }

                    val result = sshManager.connect(lastUsedProfile)
                    if (result.isSuccess) {
                        updateLastUsedUseCase(lastUsedProfile.id)
                    } else {
                        val stopIntent = TunnelForegroundService.createDisconnectIntent(applicationContext)
                        startService(stopIntent)
                    }
                }
            }
            updateTile()
        }
    }

    private fun updateTile() {
        val tile = qsTile ?: return
        val status = sshManager.tunnelState.value.status

        when (status) {
            TunnelStatus.CONNECTED -> {
                tile.state = Tile.STATE_ACTIVE
                tile.label = "Netvan (Connected)"
            }
            TunnelStatus.CONNECTING, TunnelStatus.RECONNECTING -> {
                tile.state = Tile.STATE_ACTIVE
                tile.label = "Netvan (Connecting)"
            }
            else -> {
                tile.state = Tile.STATE_INACTIVE
                tile.label = "Netvan"
            }
        }
        tile.updateTile()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
