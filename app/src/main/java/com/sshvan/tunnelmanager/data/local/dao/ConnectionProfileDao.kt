package com.sshvan.tunnelmanager.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.sshvan.tunnelmanager.data.local.entity.ConnectionProfileEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for ConnectionProfile database operations.
 */
@Dao
interface ConnectionProfileDao {

    @Query("SELECT * FROM connection_profiles ORDER BY lastUsed DESC, createdAt DESC")
    fun getAllProfiles(): Flow<List<ConnectionProfileEntity>>

    @Query("SELECT * FROM connection_profiles WHERE id = :id")
    suspend fun getProfileById(id: Long): ConnectionProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: ConnectionProfileEntity): Long

    @Update
    suspend fun updateProfile(profile: ConnectionProfileEntity)

    @Query("DELETE FROM connection_profiles WHERE id = :id")
    suspend fun deleteProfile(id: Long)

    @Query("UPDATE connection_profiles SET lastUsed = :timestamp WHERE id = :id")
    suspend fun updateLastUsed(id: Long, timestamp: Long)
}
