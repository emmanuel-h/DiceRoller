// app/src/main/java/fr/mandarine/diceroller/data/DataStoreAppLanguageStore.kt
package fr.mandarine.diceroller.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import fr.mandarine.diceroller.presentation.AppLanguage
import fr.mandarine.diceroller.presentation.AppLanguageStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Key under which the chosen language's BCP-47 tag is stored. */
private val LANGUAGE_TAG_KEY = stringPreferencesKey("app_language_tag")

/**
 * [AppLanguageStore] backed by DataStore Preferences.
 *
 * Stored as the BCP-47 **tag** rather than the enum name, so the file stays readable and stays
 * valid if the enum is renamed or reordered — the same reasoning that has
 * [DataStoreDiceColorStore] persist a colour's `name` rather than its ordinal, applied to the
 * value that is genuinely stable here.
 *
 * An absent key — every fresh install, and every install from before the "System default" option
 * was dropped — reads back as the device's language, or English when the app does not ship it
 * ([deviceLanguage]). So does a tag naming a language this build no longer ships.
 *
 * @param context any context; the application context is retained internally
 */
class DataStoreAppLanguageStore(context: Context) : AppLanguageStore {

    private val dataStore = context.applicationContext.diceDataStore

    private val appContext = context.applicationContext

    override val language: Flow<AppLanguage> = dataStore.data.map { preferences ->
        AppLanguage.ofTag(preferences[LANGUAGE_TAG_KEY]) ?: appContext.deviceLanguage()
    }

    override suspend fun setLanguage(language: AppLanguage) {
        dataStore.edit { preferences -> preferences[LANGUAGE_TAG_KEY] = language.tag }
    }
}
