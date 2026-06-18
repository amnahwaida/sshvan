package com.sshvan.tunnelmanager.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing a saved SSH tunnel connection profile.
 */
@Entity(tableName = "connection_profiles")
data class ConnectionProfileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val sshHost: String,
    val sshPort: Int = 22,
    val username: String,
    val authType: String, // "PASSWORD" or "PRIVATE_KEY"
    val password: String? = null,
    val privateKeyPath: String? = null,
    val localPort: Int = 8080,
    val remoteHost: String = "localhost",
    val remotePort: Int = 3000,
    val createdAt: Long = System.currentTimeMillis(),
    val lastUsed: Long? = null,
    val isLocked: Boolean = false
)
