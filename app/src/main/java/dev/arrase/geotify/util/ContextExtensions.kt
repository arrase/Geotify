package dev.arrase.geotify.util

import android.content.BroadcastReceiver
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration.Companion.seconds

/**
 * Calls [goAsync] and launches a coroutine to perform background work within the
 * BroadcastReceiver's lifecycle. Ensures [PendingResult.finish] is always called,
 * even if the coroutine fails or times out.
 *
 * Android kills BroadcastReceivers after ~10 seconds, so a 9-second timeout is enforced.
 */
fun BroadcastReceiver.goAsyncCoroutine(block: suspend CoroutineScope.() -> Unit) {
    val pendingResult = goAsync()
    CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
        try {
            withTimeout(9.seconds) {
                block()
            }
        } catch (e: Exception) {
            Log.e("GoAsyncCoroutine", "Error in BroadcastReceiver coroutine", e)
        } finally {
            pendingResult.finish()
        }
    }
}
