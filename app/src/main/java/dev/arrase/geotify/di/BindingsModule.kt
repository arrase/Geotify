package dev.arrase.geotify.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.arrase.geotify.geofence.AndroidGeofenceManager
import dev.arrase.geotify.geofence.GeofenceManager
import dev.arrase.geotify.location.DefaultLocationProvider
import dev.arrase.geotify.location.LocationProvider

@Module
@InstallIn(SingletonComponent::class)
@Suppress("unused")
interface BindingsModule {

    @Binds
    fun bindGeofenceManager(impl: AndroidGeofenceManager): GeofenceManager

    @Binds
    fun bindLocationProvider(impl: DefaultLocationProvider): LocationProvider
}
