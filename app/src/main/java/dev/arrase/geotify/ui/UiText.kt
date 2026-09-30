package dev.arrase.geotify.ui

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

/**
 * A user-facing message that may either be a literal string or a string resource resolved at
 * composition time, so it honours the user's locale and configuration changes.
 */
sealed interface UiText {
    data class DynamicString(val value: String) : UiText

    data class StringResource(
        @param:StringRes val resId: Int,
        val args: List<Any> = emptyList()
    ) : UiText {
        constructor(@StringRes resId: Int, vararg formatArgs: Any) : this(resId, formatArgs.toList())
    }

    @Composable
    fun asString(): String = when (this) {
        is DynamicString -> value
        is StringResource -> stringResource(resId, *args.toTypedArray())
    }

    fun resolve(context: Context): String = when (this) {
        is DynamicString -> value
        is StringResource -> if (args.isEmpty()) {
            context.getString(resId)
        } else {
            context.getString(resId, *args.toTypedArray())
        }
    }
}

/**
 * Wraps a failure so the user always sees something meaningful: the cause's message when it has
 * one, otherwise the supplied fallback.
 */
fun errorMessage(fallback: String, throwable: Throwable): UiText.DynamicString {
    val detail = throwable.localizedMessage
    return UiText.DynamicString(if (detail.isNullOrBlank()) fallback else "$fallback: $detail")
}
