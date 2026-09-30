package dev.arrase.geotify.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn

/** How long a `stateIn` subscription stays alive after its last collector disappears. */
private const val SUBSCRIPTION_TIMEOUT_MS = 5_000L

/**
 * Base for the app's ViewModels. Adds a buffered one-shot message channel and helpers for
 * exposing settings as state and for running mutating work with uniform error reporting.
 */
abstract class BaseViewModel : ViewModel() {

    /**
     * One-shot user messages. A [Channel] is used rather than a `SharedFlow` because a shared
     * flow with no replay drops events emitted while nothing is collecting, which would silently
     * lose exactly the error messages the user needs to see.
     */
    protected val messages = Channel<UiText>(Channel.BUFFERED)
    val messagesFlow: Flow<UiText> = messages.receiveAsFlow()

    /** Exposes a flow as state, replaying [initialValue] until the first emission. */
    protected fun <T> Flow<T>.settingFlow(initialValue: T): StateFlow<T> =
        stateIn(viewModelScope, SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS), initialValue)

    /**
     * Runs [action], then [onSuccess]. Cancellation is never reported as a failure — only genuine
     * errors are turned into a user-visible message. [action] is the last parameter so callers can
     * pass it as a trailing lambda.
     */
    protected suspend fun mutate(
        errorFallback: String,
        onSuccess: suspend () -> Unit,
        action: suspend () -> Unit
    ) {
        try {
            action()
            onSuccess()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            messages.send(errorMessage(errorFallback, e))
        }
    }
}
