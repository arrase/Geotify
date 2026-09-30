package dev.arrase.geotify.di

import javax.inject.Qualifier

/** Marks the dispatcher used for disk and database work. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher
