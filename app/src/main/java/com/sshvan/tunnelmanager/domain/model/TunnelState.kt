package com.sshvan.tunnelmanager.domain.model

import androidx.compose.runtime.Immutable

/**
 * Represents the current status of an SSH tunnel connection.
 */
enum class TunnelStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    RECONNECTING,
    ERROR
}

/**
 * Holds the full state of the tunnel including status, error info, and active profile.
 */
@Immutable
data class TunnelState(
    val status: TunnelStatus = TunnelStatus.DISCONNECTED,
    val activeProfile: ConnectionProfile? = null,
    val errorMessage: String? = null,
    val connectedAt: Long? = null
) {
    val isActive: Boolean
        get() = status == TunnelStatus.CONNECTED || status == TunnelStatus.RECONNECTING

    val isConnecting: Boolean
        get() = status == TunnelStatus.CONNECTING || status == TunnelStatus.RECONNECTING
}
