package com.sshvan.tunnelmanager.presentation.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sshvan.tunnelmanager.domain.model.TunnelState
import com.sshvan.tunnelmanager.domain.model.TunnelStatus
import com.sshvan.tunnelmanager.presentation.ui.theme.StatusConnected
import com.sshvan.tunnelmanager.presentation.ui.theme.StatusConnecting
import com.sshvan.tunnelmanager.presentation.ui.theme.StatusDisconnected
import com.sshvan.tunnelmanager.presentation.ui.theme.StatusError

/**
 * Status card shown at the top of the HomeScreen.
 * Shows connection status, active tunnel info, and a status indicator.
 */
@Composable
fun TunnelStatusCard(
    tunnelState: TunnelState,
    modifier: Modifier = Modifier
) {
    val statusColor by animateColorAsState(
        targetValue = when (tunnelState.status) {
            TunnelStatus.CONNECTED -> StatusConnected
            TunnelStatus.CONNECTING, TunnelStatus.RECONNECTING -> StatusConnecting
            TunnelStatus.ERROR -> StatusError
            TunnelStatus.DISCONNECTED -> StatusDisconnected
        },
        label = "statusColor"
    )

    val statusIcon = when (tunnelState.status) {
        TunnelStatus.CONNECTED -> Icons.Filled.CloudDone
        TunnelStatus.CONNECTING, TunnelStatus.RECONNECTING -> Icons.Filled.CloudSync
        TunnelStatus.ERROR -> Icons.Filled.Error
        TunnelStatus.DISCONNECTED -> Icons.Filled.CloudOff
    }

    val statusText = when (tunnelState.status) {
        TunnelStatus.CONNECTED -> "Connected"
        TunnelStatus.CONNECTING -> "Connecting..."
        TunnelStatus.RECONNECTING -> "Reconnecting..."
        TunnelStatus.ERROR -> "Error"
        TunnelStatus.DISCONNECTED -> "Disconnected"
    }

    // Pulsing animation for connecting states
    val pulsingAlpha = if (tunnelState.isConnecting) {
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0.4f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(800),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulseAlpha"
        )
        alpha
    } else {
        1f
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status indicator dot
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .alpha(pulsingAlpha)
                    .clip(CircleShape)
                    .background(statusColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = statusIcon,
                    contentDescription = statusText,
                    tint = statusColor,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )

                if (tunnelState.activeProfile != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = tunnelState.activeProfile.tunnelDescription(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Text(
                        text = tunnelState.activeProfile.sshDescription(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }

                if (tunnelState.errorMessage != null && tunnelState.status == TunnelStatus.ERROR) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = tunnelState.errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = StatusError
                    )
                }
            }
        }
    }
}
