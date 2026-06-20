package com.sshvan.tunnelmanager.service

import android.util.Log
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import com.sshvan.tunnelmanager.domain.model.AuthType
import com.sshvan.tunnelmanager.domain.model.ConnectionProfile
import com.sshvan.tunnelmanager.domain.model.TunnelState
import com.sshvan.tunnelmanager.domain.model.TunnelStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Core SSH Manager responsible for creating and managing SSH tunnels.
 * Uses JSch library for SSH connections and port forwarding.
 *
 * Features:
 * - SSH Local Port Forwarding
 * - Password and Private Key authentication
 * - Auto-reconnect with exponential backoff
 * - Connection status monitoring
 */
@Singleton
class SshManager @Inject constructor() {

    companion object {
        private const val TAG = "SshManager"
        private const val CONNECT_TIMEOUT_MS = 15_000
        private const val KEEP_ALIVE_INTERVAL_MS = 30_000
        private const val MAX_RECONNECT_ATTEMPTS = 5
        private const val BASE_RECONNECT_DELAY_MS = 2_000L
        private const val MAX_RECONNECT_DELAY_MS = 60_000L
        private const val HEALTH_CHECK_INTERVAL_MS = 10_000L
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var session: Session? = null
    private var healthCheckJob: Job? = null
    private var reconnectJob: Job? = null
    private var reconnectAttempts = 0

    private val _tunnelState = MutableStateFlow(TunnelState())
    val tunnelState: StateFlow<TunnelState> = _tunnelState.asStateFlow()

    /**
     * Connect to the SSH server and establish port forwarding.
     */
    suspend fun connect(profile: ConnectionProfile): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                // Disconnect any existing session first
                disconnectInternal()

                _tunnelState.value = TunnelState(
                    status = TunnelStatus.CONNECTING,
                    activeProfile = profile
                )

                Log.d(TAG, "Connecting to ${profile.sshDescription()}")

                // Validate configuration before connecting
                validateProfile(profile).getOrThrow()

                // Create and configure JSch session
                val jsch = JSch()

                // Add private key if using key-based auth
                if (profile.authType == AuthType.PRIVATE_KEY && profile.privateKeyPath != null) {
                    val keyFile = File(profile.privateKeyPath)
                    if (!keyFile.exists()) {
                        throw SshException("Private key file not found: ${profile.privateKeyPath}")
                    }
                    jsch.addIdentity(profile.privateKeyPath)
                }

                val newSession = jsch.getSession(
                    profile.username,
                    profile.sshHost,
                    profile.sshPort
                )

                // Set password if using password auth
                if (profile.authType == AuthType.PASSWORD && profile.password != null) {
                    newSession.setPassword(profile.password)
                }

                // Configure session properties
                val config = java.util.Properties()
                config["StrictHostKeyChecking"] = "no"
                config["TCPKeepAlive"] = "yes"
                config["ServerAliveInterval"] = (KEEP_ALIVE_INTERVAL_MS / 1000).toString()
                config["ServerAliveCountMax"] = "3"
                config["PreferredAuthentications"] = when (profile.authType) {
                    AuthType.PASSWORD -> "password,keyboard-interactive"
                    AuthType.PRIVATE_KEY -> "publickey"
                }
                newSession.setConfig(config)
                newSession.timeout = CONNECT_TIMEOUT_MS

                // Connect
                newSession.connect(CONNECT_TIMEOUT_MS)

                // Setup port forwarding: -L localPort:remoteHost:remotePort
                try {
                    // Use "0.0.0.0" as bind_address so it can be accessed from LAN/Hotspot IP
                    newSession.setPortForwardingL(
                        "0.0.0.0",
                        profile.localPort,
                        profile.remoteHost,
                        profile.remotePort
                    )
                } catch (e: Exception) {
                    newSession.disconnect()
                    val errorMsg = when {
                        e.message?.contains("Address already in use") == true ->
                            "Local port ${profile.localPort} is already in use. Please choose a different port."
                        e.message?.contains("Permission denied") == true ->
                            "Permission denied for port ${profile.localPort}. Try using a port above 1024."
                        else -> "Failed to setup port forwarding: ${e.message}"
                    }
                    throw SshException(errorMsg)
                }

                session = newSession
                reconnectAttempts = 0

                _tunnelState.value = TunnelState(
                    status = TunnelStatus.CONNECTED,
                    activeProfile = profile,
                    connectedAt = System.currentTimeMillis()
                )

                Log.i(TAG, "Connected successfully: ${profile.tunnelDescription()}")

                // Start health monitoring
                startHealthCheck(profile)

                Result.success(Unit)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val errorMessage = mapSshError(e)
                Log.e(TAG, "Connection failed: $errorMessage", e)

                _tunnelState.value = TunnelState(
                    status = TunnelStatus.ERROR,
                    activeProfile = profile,
                    errorMessage = errorMessage
                )

                Result.failure(SshException(errorMessage))
            }
        }
    }

    /**
     * Disconnect the active SSH tunnel.
     */
    suspend fun disconnect() {
        withContext(Dispatchers.IO) {
            reconnectJob?.cancel()
            reconnectJob = null
            disconnectInternal()
            reconnectAttempts = 0
            _tunnelState.value = TunnelState(status = TunnelStatus.DISCONNECTED)
            Log.i(TAG, "Disconnected")
        }
    }

    /**
     * Test connection without keeping it open.
     */
    suspend fun testConnection(profile: ConnectionProfile): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                validateProfile(profile).getOrThrow()

                val jsch = JSch()

                if (profile.authType == AuthType.PRIVATE_KEY && profile.privateKeyPath != null) {
                    jsch.addIdentity(profile.privateKeyPath)
                }

                val testSession = jsch.getSession(
                    profile.username,
                    profile.sshHost,
                    profile.sshPort
                )

                if (profile.authType == AuthType.PASSWORD && profile.password != null) {
                    testSession.setPassword(profile.password)
                }

                val config = java.util.Properties()
                config["StrictHostKeyChecking"] = "no"
                config["PreferredAuthentications"] = when (profile.authType) {
                    AuthType.PASSWORD -> "password,keyboard-interactive"
                    AuthType.PRIVATE_KEY -> "publickey"
                }
                testSession.setConfig(config)
                testSession.timeout = CONNECT_TIMEOUT_MS

                testSession.connect(CONNECT_TIMEOUT_MS)
                val serverVersion = testSession.serverVersion ?: "Unknown"
                testSession.disconnect()

                Result.success("Connection successful! Server: $serverVersion")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(SshException(mapSshError(e)))
            }
        }
    }

    /**
     * Check if there is an active connection.
     */
    fun isConnected(): Boolean = session?.isConnected == true

    /**
     * Internal disconnect without state reset.
     */
    private fun disconnectInternal() {
        healthCheckJob?.cancel()
        healthCheckJob = null

        try {
            session?.let { s ->
                if (s.isConnected) {
                    // Remove port forwarding first
                    try {
                        val forwardedPorts = s.portForwardingL
                        forwardedPorts?.forEach { portInfo ->
                            try {
                                val port = portInfo.split(":")[0].toInt()
                                s.delPortForwardingL(port)
                            } catch (_: Exception) {
                                // Ignore cleanup errors
                            }
                        }
                    } catch (_: Exception) {
                        // Ignore cleanup errors
                    }
                    s.disconnect()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error during disconnect: ${e.message}")
        } finally {
            session = null
        }
    }

    /**
     * Start a coroutine that periodically checks connection health
     * and triggers reconnection if needed.
     */
    private fun startHealthCheck(profile: ConnectionProfile) {
        healthCheckJob?.cancel()
        healthCheckJob = scope.launch {
            while (isActive) {
                delay(HEALTH_CHECK_INTERVAL_MS)

                val currentSession = session
                if (currentSession == null || !currentSession.isConnected) {
                    Log.w(TAG, "Connection lost, attempting reconnection...")
                    attemptReconnect(profile)
                    return@launch
                }

                // Send keep-alive to verify connection is truly alive
                try {
                    currentSession.sendKeepAliveMsg()
                } catch (e: Exception) {
                    Log.w(TAG, "Keep-alive failed: ${e.message}")
                    attemptReconnect(profile)
                    return@launch
                }
            }
        }
    }

    /**
     * Attempt to reconnect with exponential backoff.
     */
    private fun attemptReconnect(profile: ConnectionProfile) {
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            while (isActive && reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
                reconnectAttempts++

                val delayMs = (BASE_RECONNECT_DELAY_MS * (1L shl (reconnectAttempts - 1).coerceAtMost(5)))
                    .coerceAtMost(MAX_RECONNECT_DELAY_MS)

                Log.i(TAG, "Reconnect attempt $reconnectAttempts/$MAX_RECONNECT_ATTEMPTS in ${delayMs}ms")

                _tunnelState.value = TunnelState(
                    status = TunnelStatus.RECONNECTING,
                    activeProfile = profile,
                    errorMessage = "Reconnecting... (attempt $reconnectAttempts/$MAX_RECONNECT_ATTEMPTS)"
                )

                delay(delayMs)

                // Try to reconnect
                disconnectInternal()

                val result = connect(profile)
                if (result.isSuccess) {
                    Log.i(TAG, "Reconnected successfully after $reconnectAttempts attempts")
                    reconnectAttempts = 0
                    return@launch
                }
            }

            if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
                Log.e(TAG, "Max reconnect attempts reached, giving up")
                _tunnelState.value = TunnelState(
                    status = TunnelStatus.ERROR,
                    activeProfile = profile,
                    errorMessage = "Connection lost. Failed to reconnect after $MAX_RECONNECT_ATTEMPTS attempts."
                )
                reconnectAttempts = 0
            }
        }
    }

    /**
     * Validate profile configuration before attempting connection.
     */
    private fun validateProfile(profile: ConnectionProfile): Result<Unit> {
        val errors = mutableListOf<String>()

        if (profile.sshHost.isBlank()) {
            errors.add("SSH host is required")
        }
        if (profile.username.isBlank()) {
            errors.add("Username is required")
        }
        if (profile.sshPort !in 1..65535) {
            errors.add("SSH port must be between 1 and 65535")
        }
        if (profile.localPort !in 1..65535) {
            errors.add("Local port must be between 1 and 65535")
        }
        if (profile.remotePort !in 1..65535) {
            errors.add("Remote port must be between 1 and 65535")
        }
        if (profile.remoteHost.isBlank()) {
            errors.add("Remote host is required")
        }
        if (profile.authType == AuthType.PASSWORD && profile.password.isNullOrEmpty()) {
            errors.add("Password is required for password authentication")
        }
        if (profile.authType == AuthType.PRIVATE_KEY && profile.privateKeyPath.isNullOrEmpty()) {
            errors.add("Private key file is required for key-based authentication")
        }

        return if (errors.isEmpty()) {
            Result.success(Unit)
        } else {
            Result.failure(SshException(errors.joinToString("; ")))
        }
    }

    /**
     * Map JSch exceptions to user-friendly error messages.
     */
    private fun mapSshError(e: Exception): String {
        val message = e.message ?: return "Unknown SSH error"
        return when {
            message.contains("Auth fail", ignoreCase = true) ||
            message.contains("auth failed", ignoreCase = true) ||
            message.contains("Authentication failed", ignoreCase = true) ->
                "Authentication failed. Please check your username and password/key."

            message.contains("Connection refused", ignoreCase = true) ->
                "Connection refused. Please check the SSH host and port."

            message.contains("timeout", ignoreCase = true) ||
            message.contains("timed out", ignoreCase = true) ->
                "Connection timed out. Please check the host address and your network connection."

            message.contains("UnknownHostException", ignoreCase = true) ||
            message.contains("No route to host", ignoreCase = true) ->
                "Cannot reach the host. Please verify the SSH host address."

            message.contains("Network is unreachable", ignoreCase = true) ->
                "Network is unreachable. Please check your internet connection."

            message.contains("Address already in use", ignoreCase = true) ->
                "Local port is already in use. Please choose a different local port."

            message.contains("USERAUTH fail", ignoreCase = true) ->
                "Authentication method not supported by server."

            message.contains("invalid privatekey", ignoreCase = true) ->
                "Invalid private key format. Please ensure the key file is in a supported format (OpenSSH/PEM)."

            e is SshException -> message

            else -> "SSH error: $message"
        }
    }
}

/**
 * Custom exception for SSH-related errors with user-friendly messages.
 */
class SshException(message: String) : Exception(message)
