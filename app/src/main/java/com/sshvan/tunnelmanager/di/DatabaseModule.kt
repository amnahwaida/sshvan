package com.sshvan.tunnelmanager.di

import android.content.Context
import androidx.room.Room
import com.sshvan.tunnelmanager.data.local.dao.ConnectionProfileDao
import com.sshvan.tunnelmanager.data.local.database.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideConnectionProfileDao(database: AppDatabase): ConnectionProfileDao {
        return database.connectionProfileDao()
    }
}
