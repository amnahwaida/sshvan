package com.sshvan.tunnelmanager.domain.usecase

import com.sshvan.tunnelmanager.domain.model.ConnectionProfile
import com.sshvan.tunnelmanager.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case to get all saved connection profiles.
 */
class GetProfilesUseCase @Inject constructor(
    private val repository: ProfileRepository
) {
    operator fun invoke(): Flow<List<ConnectionProfile>> =
        repository.getAllProfiles()
}

/**
 * Use case to get a single profile by ID.
 */
class GetProfileByIdUseCase @Inject constructor(
    private val repository: ProfileRepository
) {
    suspend operator fun invoke(id: Long): ConnectionProfile? =
        repository.getProfileById(id)
}

/**
 * Use case to save (insert or update) a connection profile.
 */
class SaveProfileUseCase @Inject constructor(
    private val repository: ProfileRepository
) {
    suspend operator fun invoke(profile: ConnectionProfile): Long =
        repository.saveProfile(profile)
}

/**
 * Use case to delete a connection profile.
 */
class DeleteProfileUseCase @Inject constructor(
    private val repository: ProfileRepository
) {
    suspend operator fun invoke(id: Long) =
        repository.deleteProfile(id)
}

/**
 * Use case to update the lastUsed timestamp of a profile.
 */
class UpdateLastUsedUseCase @Inject constructor(
    private val repository: ProfileRepository
) {
    suspend operator fun invoke(id: Long) =
        repository.updateLastUsed(id)
}
