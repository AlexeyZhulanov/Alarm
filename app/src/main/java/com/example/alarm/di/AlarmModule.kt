package com.example.alarm.di

import android.app.AlarmManager
import android.content.Context
import android.content.SharedPreferences
import androidx.room.Room
import com.example.alarm.APP_PREFERENCES
import com.example.alarm.model.AlarmService
import com.example.alarm.model.DefaultTimeProvider
import com.example.alarm.model.MyAlarmManager
import com.example.alarm.model.TimeProvider
import com.example.alarm.room.AlarmDao
import com.example.alarm.room.AppDatabase
import com.example.alarm.room.SettingsDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
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
    fun provideAlarmService(
        alarmDao: AlarmDao,
        settingsDao: SettingsDao,
        myAlarmManager: MyAlarmManager,
        @IoDispatcher dispatcher: CoroutineDispatcher
    ): AlarmService {
        return AlarmService(alarmDao, settingsDao, dispatcher, myAlarmManager)
    }
    @Provides
    fun provideAlarmManager(@ApplicationContext context: Context): AlarmManager {
        return context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    }

    @Provides
    fun provideTimeProvider(): TimeProvider {
        return DefaultTimeProvider()
    }

    @Provides
    @Singleton
    fun provideMyAlarmManager(
        context: Context,
        alarmManager: AlarmManager,
        timeProvider: TimeProvider,
        @DefaultDispatcher defaultDispatcher: CoroutineDispatcher
    ): MyAlarmManager {
        return MyAlarmManager(context, alarmManager, timeProvider, defaultDispatcher)
    }

    @Provides
    fun provideContext(@ApplicationContext context: Context): Context {
        return context
    }

    @Provides
    @Singleton
    fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        return context.getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE)
    }

    @Provides
    @DefaultDispatcher
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default

    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @MainDispatcher
    fun provideMainDispatcher(): CoroutineDispatcher = Dispatchers.Main

}