package com.sshvan.tunnelmanager.service

import com.zerotier.sockets.ZeroTierSocket
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
import java.net.SocketAddress
import com.zerotier.sockets.ZeroTierNative

class ZeroTierSocketWrapper : Socket() {
    private val ztSocket = ZeroTierSocket(ZeroTierNative.ZTS_AF_INET, ZeroTierNative.ZTS_SOCK_STREAM, 0)

    fun ztConnect(host: String, port: Int) {
        ztSocket.connect(java.net.InetSocketAddress(host, port))
    }

    override fun getInputStream(): InputStream = ztSocket.inputStream
    override fun getOutputStream(): OutputStream = ztSocket.outputStream
    
    override fun close() {
        try {
            ztSocket.close()
        } catch (_: Exception) {}
    }
    
    override fun connect(endpoint: SocketAddress?) {
        ztSocket.connect(endpoint)
    }
    
    override fun connect(endpoint: SocketAddress?, timeout: Int) {
        ztSocket.connect(endpoint)
    }
    
    override fun isConnected(): Boolean = true 
    override fun isClosed(): Boolean = false
}
