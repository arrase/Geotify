package dev.arrase.geotify.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.arrase.geotify.data.GeotifyDatabase
import dev.arrase.geotify.data.dao.LocationDao
import dev.arrase.geotify.data.dao.ReminderDao
import dev.arrase.geotify.geofence.AndroidGeofenceManager
import dev.arrase.geotify.geofence.GeofenceManager
import dev.arrase.geotify.location.DefaultLocationProvider
import dev.arrase.geotify.location.LocationProvider
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
        return GeotifyDatabase.getInstance(context)
    }

    @Provides
    fun provideLocationDao(database: GeotifyDatabase): LocationDao = database.locationDao()

    @Provides
    fun provideReminderDao(database: GeotifyDatabase): ReminderDao = database.reminderDao()

    @Provides
    @Singleton
    fun provideGeofenceManager(impl: AndroidGeofenceManager): GeofenceManager {
        return impl
    }

    @Provides
    @Singleton
    fun provideLocationProvider(impl: DefaultLocationProvider): LocationProvider {
        return impl
    }
}
