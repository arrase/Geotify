package dev.arrase.geotify.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.arrase.geotify.data.GeotifyDatabase
import dev.arrase.geotify.data.dao.LocationDao
import dev.arrase.geotify.data.dao.ReminderDao
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @Singleton
    @MainDispatcher
    fun provideMainDispatcher(): CoroutineDispatcher = Dispatchers.Main

    @Provides
    @Singleton
    @DefaultDispatcher
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): GeotifyDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            GeotifyDatabase::class.java,
            "geotify.db"
        )
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }

    @Provides
    fun provideLocationDao(database: GeotifyDatabase): LocationDao = database.locationDao()

    @Provides
    fun provideReminderDao(database: GeotifyDatabase): ReminderDao = database.reminderDao()
}
