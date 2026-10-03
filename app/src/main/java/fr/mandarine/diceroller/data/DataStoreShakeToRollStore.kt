// app/src/main/java/fr/mandarine/diceroller/data/DataStoreShakeToRollStore.kt
package fr.mandarine.diceroller.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import fr.mandarine.diceroller.presentation.ShakeToRollStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Key under which the shake-to-roll switch is stored. */
private val SHAKE_TO_ROLL_KEY = booleanPreferencesKey("shake_to_roll_enabled")

/**
 * [ShakeToRollStore] backed by DataStore Preferences, sharing the app's one [diceDataStore].
 *
 * An absent key reads as enabled, so every install starts with shaking on without a write.
 *
 * @param context any context; the application context is retained internally
 */
class DataStoreShakeToRollStore(context: Context) : ShakeToRollStore {

    private val dataStore = context.applicationContext.diceDataStore

    override val isEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[SHAKE_TO_ROLL_KEY] ?: true
    }

    override suspend fun setEnabled(enabled: Boolean) {
        dataStore.edit { preferences -> preferences[SHAKE_TO_ROLL_KEY] = enabled }
    }
}
