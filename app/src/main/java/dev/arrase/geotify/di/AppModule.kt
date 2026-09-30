package dev.arrase.geotify.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.arrase.geotify.data.GeotifyDatabase
import dev.arrase.geotify.data.Migrations
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
    fun provideDatabase(@ApplicationContext context: Context): GeotifyDatabase =
        Room.databaseBuilder(
            context.applicationContext,
            GeotifyDatabase::class.java,
            GeotifyDatabase.NAME
        )
            // Every schema step is declared explicitly, so user data is never dropped on upgrade.
            .addMigrations(*Migrations.all())
            .build()

    @Provides
    fun provideLocationDao(database: GeotifyDatabase): LocationDao = database.locationDao()

    @Provides
    fun provideReminderDao(database: GeotifyDatabase): ReminderDao = database.reminderDao()
}
