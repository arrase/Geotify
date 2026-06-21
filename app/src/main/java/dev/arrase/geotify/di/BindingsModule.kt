package dev.arrase.geotify.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.arrase.geotify.geofence.AndroidGeofenceManager
import dev.arrase.geotify.geofence.GeofenceManager
import dev.arrase.geotify.location.DefaultLocationProvider
import dev.arrase.geotify.location.LocationProvider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class BindingsModule {

    @Binds
    @Singleton
    abstract fun bindGeofenceManager(impl: AndroidGeofenceManager): GeofenceManager

    @Binds
    @Singleton
    abstract fun bindLocationProvider(impl: DefaultLocationProvider): LocationProvider
}
