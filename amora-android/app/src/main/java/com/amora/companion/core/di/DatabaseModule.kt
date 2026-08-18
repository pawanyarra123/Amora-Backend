package com.amora.companion.core.di

import android.content.Context
import androidx.room.Room
import com.amora.companion.core.data.local.db.AmoraDatabase
import com.amora.companion.core.data.local.db.dao.AlarmDao
import com.amora.companion.core.data.local.db.dao.AmoraDao
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
    fun provideDatabase(@ApplicationContext context: Context): AmoraDatabase {
        return Room.databaseBuilder(
            context,
            AmoraDatabase::class.java,
            "amora_db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideAmoraDao(database: AmoraDatabase): AmoraDao {
        return database.amoraDao()
    }

    @Provides
    fun provideAlarmDao(database: AmoraDatabase): AlarmDao {
        return database.alarmDao()
    }
}
