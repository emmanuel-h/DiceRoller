// app/src/main/java/fr/mandarine/diceroller/data/DataStoreSoundStore.kt
package fr.mandarine.diceroller.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import fr.mandarine.diceroller.presentation.SoundStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Key under which the sound switch is stored. */
private val SOUND_ENABLED_KEY = booleanPreferencesKey("sound_enabled")

/**
 * [SoundStore] backed by DataStore Preferences, sharing the app's one [diceDataStore].
 *
 * An absent key reads as enabled, so every install starts with sound on without a write.
 *
 * @param context any context; the application context is retained internally
 */
class DataStoreSoundStore(context: Context) : SoundStore {

    private val dataStore = context.applicationContext.diceDataStore

    override val isEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[SOUND_ENABLED_KEY] ?: true
    }

    override suspend fun setEnabled(enabled: Boolean) {
        dataStore.edit { preferences -> preferences[SOUND_ENABLED_KEY] = enabled }
    }
}
