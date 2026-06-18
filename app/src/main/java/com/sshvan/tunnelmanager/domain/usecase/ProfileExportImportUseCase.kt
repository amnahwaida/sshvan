package com.sshvan.tunnelmanager.domain.usecase

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.sshvan.tunnelmanager.domain.model.ConnectionProfile
import com.sshvan.tunnelmanager.domain.repository.ProfileRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ProfileExportImportUseCase @Inject constructor(
    private val repository: ProfileRepository,
    @ApplicationContext private val context: Context,
    private val gson: Gson
) {
    suspend fun exportProfiles(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val profiles = repository.getAllProfiles().first()
            val json = gson.toJson(profiles)
            
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(json.toByteArray())
            } ?: return@withContext Result.failure(Exception("Failed to open output stream"))
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun importProfiles(uri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val json = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.bufferedReader().use { it.readText() }
            } ?: return@withContext Result.failure(Exception("Failed to open input stream"))

            val listType = object : TypeToken<List<ConnectionProfile>>() {}.type
            val profiles: List<ConnectionProfile> = gson.fromJson(json, listType)
            
            var importedCount = 0
            for (profile in profiles) {
                val newProfile = profile.copy(id = 0)
                repository.saveProfile(newProfile)
                importedCount++
            }
            
            Result.success(importedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
