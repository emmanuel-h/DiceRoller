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
 * [AppLanguage.System] is stored as the *absence* of the key rather than as a sentinel string:
 * following the device is the state every install starts in, so it is the one that needs no
 * record. A tag naming a language this build no longer ships reads back as
 * [AppLanguage.System] — see [AppLanguage.ofTag].
 *
 * @param context any context; the application context is retained internally
 */
class DataStoreAppLanguageStore(context: Context) : AppLanguageStore {

    private val dataStore = context.applicationContext.diceDataStore

    override val language: Flow<AppLanguage> = dataStore.data.map { preferences ->
        AppLanguage.ofTag(preferences[LANGUAGE_TAG_KEY])
    }

    override suspend fun setLanguage(language: AppLanguage) {
        dataStore.edit { preferences ->
            val tag = language.tag
            if (tag == null) preferences.remove(LANGUAGE_TAG_KEY) else preferences[LANGUAGE_TAG_KEY] = tag
        }
    }
}
