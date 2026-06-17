package com.sshvan.tunnelmanager.domain.repository

import com.sshvan.tunnelmanager.domain.model.ConnectionProfile
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing connection profiles.
 * Follows Clean Architecture principle: domain layer defines the interface,
 * data layer provides the implementation.
 */
interface ProfileRepository {

    /**
     * Get all saved connection profiles as a reactive Flow.
     */
    fun getAllProfiles(): Flow<List<ConnectionProfile>>

    /**
     * Get a single profile by its ID.
     */
    suspend fun getProfileById(id: Long): ConnectionProfile?

    /**
     * Insert or update a connection profile.
     * Returns the ID of the inserted/updated profile.
     */
    suspend fun saveProfile(profile: ConnectionProfile): Long

    /**
     * Delete a connection profile by its ID.
     */
    suspend fun deleteProfile(id: Long)

    /**
     * Update the lastUsed timestamp for a profile.
     */
    suspend fun updateLastUsed(id: Long, timestamp: Long = System.currentTimeMillis())
}
