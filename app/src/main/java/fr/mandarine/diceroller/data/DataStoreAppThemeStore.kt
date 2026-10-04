// app/src/main/java/fr/mandarine/diceroller/data/DataStoreAppThemeStore.kt
package fr.mandarine.diceroller.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import fr.mandarine.diceroller.presentation.AppTheme
import fr.mandarine.diceroller.presentation.AppThemeStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Key under which the chosen theme's enum name is stored. */
private val THEME_KEY = stringPreferencesKey("app_theme")

/**
 * [AppThemeStore] backed by DataStore Preferences, sharing the app's one [diceDataStore].
 *
 * Stored by enum `name`, as [DataStoreDiceColorStore] stores a colour, so reordering the enum
 * cannot change what a stored value means. An absent key — a user who never picked — or an
 * unrecognised one reads back as whichever matches the device's dark mode ([deviceTheme]).
 *
 * @param context any context; the application context is retained internally
 */
class DataStoreAppThemeStore(context: Context) : AppThemeStore {

    private val dataStore = context.applicationContext.diceDataStore

    private val appContext = context.applicationContext

    override val theme: Flow<AppTheme> = dataStore.data.map { preferences ->
        AppTheme.ofName(preferences[THEME_KEY]) ?: appContext.deviceTheme()
    }

    override suspend fun setTheme(theme: AppTheme) {
        dataStore.edit { preferences -> preferences[THEME_KEY] = theme.name }
    }
}
