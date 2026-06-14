package dev.arrase.geotify.util

import android.content.BroadcastReceiver
import android.content.Context
import dev.arrase.geotify.GeotifyApplication
import dev.arrase.geotify.data.GeotifyRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

val Context.geotifyRepository: GeotifyRepository
    get() = (applicationContext as GeotifyApplication).repository

fun BroadcastReceiver.goAsyncCoroutine(block: suspend CoroutineScope.() -> Unit) {
    val pendingResult = goAsync()
    CoroutineScope(Dispatchers.IO).launch {
        try {
            block()
        } finally {
            pendingResult.finish()
        }
    }
}
