import com.zerotier.sockets.*

fun test() {
    val ztSocket = ZeroTierSocket(ZeroTierNative.ZTS_AF_INET, ZeroTierNative.ZTS_SOCK_STREAM, 0)
    ztSocket.connect(java.net.InetSocketAddress("10.0.0.1", 22))
}
