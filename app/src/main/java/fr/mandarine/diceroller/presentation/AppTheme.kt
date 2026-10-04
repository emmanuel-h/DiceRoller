// app/src/main/java/fr/mandarine/diceroller/presentation/AppTheme.kt
package fr.mandarine.diceroller.presentation

import androidx.annotation.StringRes
import fr.mandarine.diceroller.R

/**
 * Whether the app draws in parchment ([Light]) or leather ([Dark]), independently of the device.
 *
 * Like [AppLanguage] there is no "follow the system" entry: an install that has never picked starts
 * in whichever matches the device's dark mode — see [forDevice].
 *
 * @property isDark whether this theme uses the dark colour scheme
 * @property labelRes what the option is called in the settings sheet
 */
enum class AppTheme(val isDark: Boolean, @param:StringRes val labelRes: Int) {

    Light(isDark = false, labelRes = R.string.theme_light),

    Dark(isDark = true, labelRes = R.string.theme_dark);

    companion object {

        /** The entry stored under [name], or null when it is null or unrecognised. */
        fun ofName(name: String?): AppTheme? = entries.firstOrNull { it.name == name }

        /** The theme matching a device that is, or is not, in dark mode. */
        fun forDevice(isDeviceDark: Boolean): AppTheme = if (isDeviceDark) Dark else Light
    }
}
