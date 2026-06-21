package com.sshvan.tunnelmanager.service

import com.zerotier.sockets.ZeroTierSocket
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
import java.net.SocketAddress
import com.zerotier.sockets.ZeroTierNative

class ZeroTierSocketWrapper : Socket() {
    private val ztSocket = ZeroTierSocket(ZeroTierNative.ZTS_AF_INET, ZeroTierNative.ZTS_SOCK_STREAM, 0)

    @Volatile
    private var connected = false
    @Volatile
    private var closed = false

    fun ztConnect(host: String, port: Int) {
        ztSocket.connect(java.net.InetSocketAddress(host, port))
        connected = true
    }

    override fun getInputStream(): InputStream = ztSocket.inputStream
    override fun getOutputStream(): OutputStream = ztSocket.outputStream
    
    override fun close() {
        closed = true
        connected = false
        try {
            ztSocket.inputStream?.close()
        } catch (_: Exception) {}
        try {
            ztSocket.outputStream?.close()
        } catch (_: Exception) {}
        try {
            ztSocket.close()
        } catch (_: Exception) {}
    }
    
    override fun connect(endpoint: SocketAddress?) {
        ztSocket.connect(endpoint)
        connected = true
    }
    
    override fun connect(endpoint: SocketAddress?, timeout: Int) {
        ztSocket.connect(endpoint)
        connected = true
    }

    override fun setSoTimeout(timeout: Int) {
        ztSocket.soTimeout = timeout
    }

    override fun getSoTimeout(): Int {
        return ztSocket.soTimeout
    }
    
    override fun isConnected(): Boolean = connected
    override fun isClosed(): Boolean = closed
}
