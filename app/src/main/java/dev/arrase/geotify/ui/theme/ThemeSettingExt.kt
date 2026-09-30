package dev.arrase.geotify.ui.theme

import dev.arrase.geotify.data.ThemeSetting

/** Resolves a [ThemeSetting] into a concrete boolean for a feature that only has light/dark. */
fun ThemeSetting.resolve(systemIsDark: Boolean): Boolean = when (this) {
    ThemeSetting.SYSTEM -> systemIsDark
    ThemeSetting.LIGHT -> false
    ThemeSetting.DARK -> true
}
