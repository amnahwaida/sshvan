import com.zerotier.sockets.ZeroTierNode
import com.zerotier.sockets.ZeroTierSocket
import java.net.Socket

class ZTSocketFactory : com.jcraft.jsch.SocketFactory {
    override fun createSocket(host: String, port: Int): Socket {
        return ZeroTierSocket(host, port)
    }
    override fun getInputStream(socket: Socket) = socket.getInputStream()
    override fun getOutputStream(socket: Socket) = socket.getOutputStream()
}

// init ZeroTier inside SshManager:
private val ztNode = ZeroTierNode()
private var isZtStarted = false

private suspend fun prepareZeroTier(networkIdStr: String) {
    val networkId = networkIdStr.toLong(16)
    
    if (!isZtStarted) {
        val ztPath = File(context.filesDir, "zerotier").absolutePath
        ztNode.initFromStorage(ztPath)
        ztNode.start()
        isZtStarted = true
    }
    
    Log.d(TAG, "Joining ZeroTier network: $networkIdStr")
    ztNode.join(networkId)
    
    var attempts = 0
    while (!ztNode.isNetworkTransportReady(networkId) && attempts < 20) {
        delay(500)
        attempts++
    }
    
    if (!ztNode.isNetworkTransportReady(networkId)) {
        throw Exception("Timeout waiting for ZeroTier network transport. Is the node authorized?")
    }
    Log.d(TAG, "ZeroTier network ready: $networkIdStr")
}
