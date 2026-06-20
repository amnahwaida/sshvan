package com.sshvan.tunnelmanager.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.widget.Toast

/**
 * Utility object for network-related operations.
 * Handles hotspot IP detection, link generation, and clipboard operations.
 */
object NetworkUtils {

    /**
     * Get the Hotspot IP address of the Android device.
     * When hotspot is active, the device usually uses a tethering interface (e.g. ap0, wlan0, swlan0).
     * The most common default is 192.168.43.1.
     */
    fun getHotspotIpAddress(): String? {
        try {
            val interfaces = java.net.NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                // Check for tethering-related interface names
                val name = networkInterface.name.lowercase()
                val isTethering = name.startsWith("ap") ||
                        name.startsWith("swlan") ||
                        name.startsWith("wlan") ||
                        name.startsWith("rndis") ||
                        name.startsWith("usb")

                if (!networkInterface.isLoopback && networkInterface.isUp) {
                    val addresses = networkInterface.inetAddresses
                    while (addresses.hasMoreElements()) {
                        val address = addresses.nextElement()
                        if (!address.isLoopbackAddress && address is java.net.Inet4Address) {
                            val ip = address.hostAddress
                            // Tethering interfaces often have these IP ranges
                            if (ip != null && (isTethering || ip.startsWith("192.168.43.") || ip.startsWith("192.168.49."))) {
                                return ip
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore, return null
        }
        return null
    }

    /**
     * Check if the device has an active network connection.
     */
    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    /**
     * Generate a local link for accessing the tunneled service from this device.
     */
    fun generateLocalLink(localPort: Int): String =
        "http://127.0.0.1:$localPort"

    /**
     * Generate a hotspot link for accessing the tunneled service from another device
     * connected to this device's hotspot.
     */
    fun generateHotspotLink(localPort: Int): String? {
        val hotspotIp = getHotspotIpAddress()
        return hotspotIp?.let { "http://$it:$localPort" }
    }

    /**
     * Copy text to clipboard and show a toast notification.
     */
    fun copyToClipboard(context: Context, label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Copied: $text", Toast.LENGTH_SHORT).show()
    }
}
