package com.sshvan.tunnelmanager.data.local

import com.sshvan.tunnelmanager.data.local.entity.ConnectionProfileEntity
import com.sshvan.tunnelmanager.domain.model.AuthType
import com.sshvan.tunnelmanager.domain.model.ConnectionProfile

/**
 * Mapper functions to convert between domain models and Room entities.
 * Keeps the domain layer clean from any database-specific annotations.
 */

fun ConnectionProfileEntity.toDomain(): ConnectionProfile =
    ConnectionProfile(
        id = id,
        name = name,
        sshHost = sshHost,
        sshPort = sshPort,
        username = username,
        authType = try {
            AuthType.valueOf(authType)
        } catch (e: IllegalArgumentException) {
            AuthType.PASSWORD
        },
        password = password,
        privateKeyPath = privateKeyPath,
        localPort = localPort,
        remoteHost = remoteHost,
        remotePort = remotePort,
        createdAt = createdAt,
        lastUsed = lastUsed,
        isLocked = isLocked
    )

fun ConnectionProfile.toEntity(): ConnectionProfileEntity =
    ConnectionProfileEntity(
        id = id,
        name = name,
        sshHost = sshHost,
        sshPort = sshPort,
        username = username,
        authType = authType.name,
        password = password,
        privateKeyPath = privateKeyPath,
        localPort = localPort,
        remoteHost = remoteHost,
        remotePort = remotePort,
        createdAt = createdAt,
        lastUsed = lastUsed,
        isLocked = isLocked
    )
