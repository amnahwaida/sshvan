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
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.zerotier.sockets.ZeroTierNode
import com.zerotier.sockets.ZeroTierSocket
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
import java.util.concurrent.Executors
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
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext

@Singleton
class SshManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val TAG = "SshManager"
        private const val CONNECT_TIMEOUT_MS = 15_000
        private const val KEEP_ALIVE_INTERVAL_MS = 30_000
        private const val MAX_RECONNECT_ATTEMPTS = 5
        private const val BASE_RECONNECT_DELAY_MS = 2_000L
        private const val MAX_RECONNECT_DELAY_MS = 60_000L
        private const val HEALTH_CHECK_INTERVAL_MS = 10_000L
    }

    // Use a dedicated cached thread pool for SSH and ZeroTier connections.
    // ZeroTier's native socket connection can block uninterruptibly for up to 60s if the host is down.
    // If a user cancels and reconnects repeatedly, this would exhaust the standard Dispatchers.IO pool.
    // A cached thread pool can grow as needed and threads will die after being idle.
    private val sshDispatcher = Executors.newCachedThreadPool().asCoroutineDispatcher()
    private val scope = CoroutineScope(SupervisorJob() + sshDispatcher)
    private var session: Session? = null
    private var healthCheckJob: Job? = null
    private var reconnectJob: Job? = null
    private var reconnectAttempts = 0
    private var activeZeroTierNetworkId: Long? = null
    private var activeConnectionJob: Job? = null
    
    @Volatile
    private var connectingSession: Session? = null
    @Volatile
    private var connectingZtSocket: Socket? = null

    private val _tunnelState = MutableStateFlow(TunnelState())
    val tunnelState: StateFlow<TunnelState> = _tunnelState.asStateFlow()

    private var ztNode: ZeroTierNode? = null
    @Volatile
    private var isZtStarted = false
    private val ztLock = Any()

    init {
        scope.launch {
            try {
                synchronized(ztLock) {
                    if (!isZtStarted) {
                        val ztDir = File(context.filesDir, "zerotier")
                        ztDir.mkdirs()
                        File(ztDir, "zerotier-one.pid").delete()
                        File(ztDir, "zerotier-one.port").delete()

                        val newNode = ZeroTierNode()
                        newNode.initFromStorage(ztDir.absolutePath)
                        newNode.start()
                        ztNode = newNode
                        isZtStarted = true
                        Log.d(TAG, "ZeroTier pre-initialized on startup, node ID: ${String.format("%010x", newNode.id)}")
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to pre-initialize ZeroTier: ${e.message}")
            }
        }
    }

    fun getZeroTierNodeId(): String? {
        synchronized(ztLock) {
            val node = ztNode
            if (node != null && isZtStarted) {
                try {
                    val id = node.id
                    if (id != 0L) {
                        return String.format("%010x", id)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error getting ZT Node ID from running node: ${e.message}")
                }
            }
        }
        // Fallback: read from storage
        try {
            val ztDir = File(context.filesDir, "zerotier")
            val identityFile = File(ztDir, "identity.public")
            if (identityFile.exists()) {
                val content = identityFile.readText().trim()
                val parts = content.split(":")
                if (parts.isNotEmpty() && parts[0].length == 10) {
                    return parts[0]
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not read identity.public: ${e.message}")
        }
        return null
    }

    private suspend fun prepareZeroTier(networkIdStr: String) {
        val networkId = try {
            java.lang.Long.parseUnsignedLong(networkIdStr, 16)
        } catch (e: Exception) {
            throw Exception("Invalid ZeroTier Network ID format")
        }

        val node: ZeroTierNode
        synchronized(ztLock) {
            if (!isZtStarted || ztNode == null) {
                val ztDir = File(context.filesDir, "zerotier")
                ztDir.mkdirs()

                // Delete stale PID/port files to prevent native crash on restart
                File(ztDir, "zerotier-one.pid").delete()
                File(ztDir, "zerotier-one.port").delete()

                val newNode = ZeroTierNode()
                newNode.initFromStorage(ztDir.absolutePath)
                newNode.start()
                ztNode = newNode
                isZtStarted = true
                Log.d(TAG, "ZeroTier node started, waiting for it to come online...")
            }
            node = ztNode!!
        }

        // Wait for the native node to fully come online before calling join()
        // This prevents SIGSEGV from accessing uninitialized native mutex in join()
        var onlineAttempts = 0
        while (!node.isOnline && onlineAttempts < 60) {
            delay(500)
            onlineAttempts++
        }

        if (!node.isOnline) {
            throw Exception("Timeout waiting for ZeroTier node to come online")
        }
        Log.d(TAG, "ZeroTier node is online after ${onlineAttempts * 500}ms")

        // Leave any previously joined network if it is different from the target network
        val currentActive = activeZeroTierNetworkId
        if (currentActive != null && currentActive != networkId) {
            Log.d(TAG, "Leaving previous ZeroTier network: ${currentActive.toString(16)} to join: $networkIdStr")
            try {
                node.leave(currentActive)
            } catch (e: Exception) {
                Log.w(TAG, "Error leaving ZeroTier network: ${e.message}")
            }
            activeZeroTierNetworkId = null
        }

        // If we are already joined to this network and transport is ready, skip join and wait
        if (activeZeroTierNetworkId == networkId && node.isNetworkTransportReady(networkId)) {
            Log.d(TAG, "ZeroTier network $networkIdStr is already joined and transport is ready.")
            return
        }

        Log.d(TAG, "Joining ZeroTier network: $networkIdStr")
        node.join(networkId)
        activeZeroTierNetworkId = networkId

        var attempts = 0
        while (!node.isNetworkTransportReady(networkId) && attempts < 30) {
            delay(500)
            attempts++
        }

        if (!node.isNetworkTransportReady(networkId)) {
            throw Exception("Timeout waiting for ZeroTier network transport. Is the node authorized?")
        }
        Log.d(TAG, "ZeroTier network ready: $networkIdStr")
    }

    /**
     * Leave the currently joined ZeroTier network.
     * NOTE: We intentionally NEVER call ztNode.stop() because libzt's
     * zts_node_stop() tries to DetachCurrentThread on the calling JVM thread,
     * which causes a fatal SIGABRT. The node stays alive as a singleton
     * and is reused across connections for faster rejoining.
     */
    private fun leaveZeroTierNetwork() {
        activeZeroTierNetworkId?.let { networkId ->
            try {
                ztNode?.leave(networkId)
                Log.d(TAG, "Left ZeroTier network: ${networkId.toString(16)}")
            } catch (e: Exception) {
                Log.w(TAG, "Error leaving ZeroTier network: ${e.message}")
            }
            activeZeroTierNetworkId = null
        }
    }

    private inner class ZTSocketFactory : com.jcraft.jsch.SocketFactory {
        override fun createSocket(host: String, port: Int): Socket {
            val wrapper = ZeroTierSocketWrapper()
            connectingZtSocket = wrapper
            
            val executor = Executors.newSingleThreadExecutor()
            val future = executor.submit {
                wrapper.ztConnect(host, port)
            }
            try {
                future.get(CONNECT_TIMEOUT_MS.toLong(), java.util.concurrent.TimeUnit.MILLISECONDS)
            } catch (e: java.util.concurrent.TimeoutException) {
                wrapper.close()
                future.cancel(true)
                throw java.io.IOException("ZeroTier connect timed out after $CONNECT_TIMEOUT_MS ms")
            } catch (e: Exception) {
                wrapper.close()
                throw java.io.IOException("ZeroTier connect failed: ${e.message}", e)
            } finally {
                if (connectingZtSocket == wrapper) {
                    connectingZtSocket = null
                }
                executor.shutdown()
            }
            return wrapper
        }
        override fun getInputStream(socket: Socket): InputStream = socket.getInputStream()
        override fun getOutputStream(socket: Socket): OutputStream = socket.getOutputStream()
    }

    /**
     * Connect to the SSH server and establish port forwarding.
     */
    suspend fun connect(profile: ConnectionProfile): Result<Unit> {
        return withContext(sshDispatcher) {
            activeConnectionJob?.cancel()
            activeConnectionJob = coroutineContext[Job]
            var newSession: Session? = null
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

                newSession = jsch.getSession(
                    profile.username,
                    profile.sshHost,
                    profile.sshPort
                )
                connectingSession = newSession

                // Set password if using password auth
                if (profile.authType == AuthType.PASSWORD && profile.password != null) {
                    newSession.setPassword(profile.password)
                }

                // If ZeroTier Network ID is provided, join and route through ZT Socket
                if (!profile.zeroTierNetworkId.isNullOrBlank()) {
                    _tunnelState.value = _tunnelState.value.copy(
                        status = TunnelStatus.CONNECTING,
                        // We can add a custom message or just rely on CONNECTING status
                    )
                    prepareZeroTier(profile.zeroTierNetworkId)
                    newSession.setSocketFactory(ZTSocketFactory())
                } else {
                    // Leave ZeroTier network if the new profile does not use ZeroTier
                    leaveZeroTierNetwork()
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

                if (!isActive) {
                    throw CancellationException("Cancelled before connection")
                }

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
            } catch (e: Throwable) {
                if (!isActive || activeConnectionJob != coroutineContext[Job]) {
                    Log.d(TAG, "Connection failed but job was cancelled, ignoring error")
                    throw CancellationException("Connection cancelled", e)
                }
                val errorMessage = mapSshError(Exception(e))
                Log.e(TAG, "Connection failed: $errorMessage", e)

                _tunnelState.value = TunnelState(
                    status = TunnelStatus.ERROR,
                    activeProfile = profile,
                    errorMessage = errorMessage
                )

                Result.failure(SshException(errorMessage))
            } finally {
                if (connectingSession == newSession) {
                    connectingSession = null
                }
                if (activeConnectionJob == coroutineContext[Job]) {
                    activeConnectionJob = null
                }
            }
        }
    }

    /**
     * Disconnect the active SSH tunnel.
     */
    fun disconnect() {
        reconnectJob?.cancel()
        reconnectJob = null
        activeConnectionJob?.cancel()
        activeConnectionJob = null
        
        // Update state immediately so UI responds without hanging
        _tunnelState.value = TunnelState(status = TunnelStatus.DISCONNECTED)
        reconnectAttempts = 0
        
        // Run the actual disconnection in a background coroutine.
        // This prevents the UI from hanging if JSch's disconnect() blocks.
        scope.launch {
            disconnectInternal()
            Log.i(TAG, "Disconnected")
        }
    }

    /**
     * Test connection without keeping it open.
     */
    suspend fun testConnection(profile: ConnectionProfile): Result<String> {
        return withContext(sshDispatcher) {
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

                val usedZeroTier = !profile.zeroTierNetworkId.isNullOrBlank()
                try {
                    if (usedZeroTier) {
                        prepareZeroTier(profile.zeroTierNetworkId!!)
                        testSession.setSocketFactory(ZTSocketFactory())
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
                } finally {
                    // Leave ZeroTier network after test only if it's not the active tunnel's network
                    if (usedZeroTier) {
                        val activeProfile = _tunnelState.value.activeProfile
                        val activeNetworkIdStr = activeProfile?.zeroTierNetworkId
                        val isActiveTunnelConnected = _tunnelState.value.status == TunnelStatus.CONNECTED ||
                                _tunnelState.value.status == TunnelStatus.CONNECTING ||
                                _tunnelState.value.status == TunnelStatus.RECONNECTING
                        
                        if (!isActiveTunnelConnected || activeNetworkIdStr != profile.zeroTierNetworkId) {
                            leaveZeroTierNetwork()
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                Result.failure(SshException(mapSshError(Exception(e))))
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

        val cSession = connectingSession
        val cSocket = connectingZtSocket
        
        connectingSession = null
        connectingZtSocket = null

        // Run closures in a separate background coroutine so we don't block
        // the current thread. JSch connect() holds a synchronized lock that 
        // disconnect() waits for, which causes deadlocks.
        scope.launch(Dispatchers.IO) {
            try {
                cSocket?.close()
            } catch (_: Exception) {}
            try {
                cSession?.disconnect()
            } catch (_: Exception) {}
        }

        val aSession = session
        session = null

        try {
            aSession?.let { s ->
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

            message.contains("Error while connecting to remote host (-1)", ignoreCase = true) ->
                "ZeroTier connection error: Unable to reach the remote SSH host. Please verify that the host is online, has joined the same network, and that both this device and the host are authorized in your ZeroTier console."

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
