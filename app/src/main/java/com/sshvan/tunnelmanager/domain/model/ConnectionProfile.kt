package com.sshvan.tunnelmanager.domain.model

/**
 * Domain model representing an SSH tunnel connection profile.
 */
data class ConnectionProfile(
    val id: Long = 0,
    val name: String,
    val sshHost: String,
    val sshPort: Int = 22,
    val username: String,
    val authType: AuthType,
    val password: String? = null,
    val privateKeyPath: String? = null,
    val localPort: Int = 8080,
    val remoteHost: String = "localhost",
    val remotePort: Int = 3000,
    val createdAt: Long = System.currentTimeMillis(),
    val lastUsed: Long? = null,
    val isLocked: Boolean = false
) {
    /**
     * Returns the tunnel description string, e.g. "localhost:8080 → remoteHost:3000"
     */
    fun tunnelDescription(): String =
        "localhost:$localPort → $remoteHost:$remotePort"

    /**
     * Returns the SSH connection description, e.g. "admin@192.168.1.50:22"
     */
    fun sshDescription(): String =
        "$username@$sshHost:$sshPort"
}

enum class AuthType {
    PASSWORD,
    PRIVATE_KEY
}
