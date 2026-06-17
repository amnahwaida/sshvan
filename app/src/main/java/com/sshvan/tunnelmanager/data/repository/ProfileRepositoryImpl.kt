package com.sshvan.tunnelmanager.data.repository

import com.sshvan.tunnelmanager.data.local.dao.ConnectionProfileDao
import com.sshvan.tunnelmanager.data.local.toDomain
import com.sshvan.tunnelmanager.data.local.toEntity
import com.sshvan.tunnelmanager.domain.model.ConnectionProfile
import com.sshvan.tunnelmanager.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of ProfileRepository using Room database.
 */
@Singleton
class ProfileRepositoryImpl @Inject constructor(
    private val dao: ConnectionProfileDao
) : ProfileRepository {

    override fun getAllProfiles(): Flow<List<ConnectionProfile>> =
        dao.getAllProfiles().map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun getProfileById(id: Long): ConnectionProfile? =
        dao.getProfileById(id)?.toDomain()

    override suspend fun saveProfile(profile: ConnectionProfile): Long {
        val entity = profile.toEntity()
        return if (profile.id == 0L) {
            dao.insertProfile(entity)
        } else {
            dao.updateProfile(entity)
            profile.id
        }
    }

    override suspend fun deleteProfile(id: Long) =
        dao.deleteProfile(id)

    override suspend fun updateLastUsed(id: Long, timestamp: Long) =
        dao.updateLastUsed(id, timestamp)
}
