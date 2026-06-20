package com.sshvan.tunnelmanager.service

import com.zerotier.sockets.ZeroTierSocket
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
import java.net.SocketAddress

class ZeroTierSocketWrapper(host: String, port: Int) : Socket() {
    private val ztSocket = ZeroTierSocket(host, port)

    override fun getInputStream(): InputStream = ztSocket.inputStream
    override fun getOutputStream(): OutputStream = ztSocket.outputStream
    override fun close() = ztSocket.close()
    
    // Override other methods to prevent standard Socket implementations
    // that rely on native file descriptors.
    override fun connect(endpoint: SocketAddress?) {
        // ZeroTierSocket already connects in constructor
    }
    
    override fun connect(endpoint: SocketAddress?, timeout: Int) {
        // ZeroTierSocket already connects in constructor
    }
    
    override fun isConnected(): Boolean = true // Or query ztSocket if it has such method
    override fun isClosed(): Boolean = false
}
