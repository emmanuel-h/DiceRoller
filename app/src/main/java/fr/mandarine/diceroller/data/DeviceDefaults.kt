// app/src/main/java/fr/mandarine/diceroller/data/DeviceDefaults.kt
package fr.mandarine.diceroller.data

import android.content.Context
import android.content.res.Configuration
import fr.mandarine.diceroller.presentation.AppLanguage
import fr.mandarine.diceroller.presentation.AppTheme

/**
 * The language a user who never picked one reads the app in: the device's, or English when the
 * app does not ship it.
 *
 * Read from the application's configuration, which the app never changes (see `ProvideAppLanguage`
 * in `MainActivity`), so it is always the device's own locale.
 */
fun Context.deviceLanguage(): AppLanguage =
    AppLanguage.forDevice(resources.configuration.locales[0]?.language)

/** The theme a user who never picked one sees: whichever matches the device's dark mode. */
fun Context.deviceTheme(): AppTheme {
    val nightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
    return AppTheme.forDevice(isDeviceDark = nightMode == Configuration.UI_MODE_NIGHT_YES)
}
