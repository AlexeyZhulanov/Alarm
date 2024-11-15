package com.example.alarm.di

import android.app.AlarmManager
import android.content.Context
import androidx.room.Room
import com.example.alarm.model.AlarmService
import com.example.alarm.room.AlarmDao
import com.example.alarm.room.AppDatabase
import com.example.alarm.room.SettingsDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AlarmModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext appContext: Context): AppDatabase {
        return Room.databaseBuilder(
            appContext,
            AppDatabase::class.java,
            "database.db"
        ).createFromAsset("init_db.db").build()
    }

    @Provides
    @Singleton
    fun provideAlarmDao(appDatabase: AppDatabase): AlarmDao {
        return appDatabase.getAlarmDao()
    }

    @Provides
    @Singleton
    fun provideSettingsDao(appDatabase: AppDatabase): SettingsDao {
        return appDatabase.getSettingsDao()
    }

    @Provides
    @Singleton
    fun provideAlarmService(alarmDao: AlarmDao, settingsDao: SettingsDao): AlarmService {
        return AlarmService(alarmDao, settingsDao)
    }
    @Provides
    fun provideAlarmManager(@ApplicationContext context: Context): AlarmManager {
        return context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    }
}