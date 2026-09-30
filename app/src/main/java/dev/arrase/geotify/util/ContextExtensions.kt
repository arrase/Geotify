package dev.arrase.geotify.util

import android.content.BroadcastReceiver
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration.Companion.seconds

/**
 * Runs [block] with the BroadcastReceiver kept alive via [goAsync], guaranteeing that
 * `PendingResult.finish()` is always called.
 *
 * Android only grants receivers about 10 seconds, so the work is bounded by a slightly shorter
 * timeout. On timeout the coroutine is cancelled and the block unwinds, so any cleanup it still
 * needs must happen in a `finally` block.
 */
fun BroadcastReceiver.goAsyncCoroutine(block: suspend () -> Unit) {
    val pendingResult = goAsync()
    CoroutineScope(Dispatchers.IO).launch {
        try {
            withTimeout(TIMEOUT) { block() }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "BroadcastReceiver coroutine timed out after $TIMEOUT", e)
        } catch (e: Exception) {
            Log.e(TAG, "BroadcastReceiver coroutine failed", e)
        } finally {
            pendingResult.finish()
        }
    }
}

private const val TAG = "GoAsyncCoroutine"
private val TIMEOUT = 9.seconds
