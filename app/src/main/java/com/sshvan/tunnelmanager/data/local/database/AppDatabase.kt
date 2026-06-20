package com.sshvan.tunnelmanager.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.sshvan.tunnelmanager.data.local.dao.ConnectionProfileDao
import com.sshvan.tunnelmanager.data.local.entity.ConnectionProfileEntity

/**
 * Room database for SSH Tunnel Manager.
 */
@Database(
    entities = [ConnectionProfileEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun connectionProfileDao(): ConnectionProfileDao

    companion object {
        const val DATABASE_NAME = "ssh_tunnel_manager.db"
    }
}
