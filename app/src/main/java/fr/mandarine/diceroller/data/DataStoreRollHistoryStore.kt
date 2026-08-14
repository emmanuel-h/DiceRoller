// app/src/main/java/fr/mandarine/diceroller/data/DataStoreRollHistoryStore.kt
package fr.mandarine.diceroller.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import fr.mandarine.diceroller.domain.RollRecord
import fr.mandarine.diceroller.presentation.MAX_HISTORY_RECORDS
import fr.mandarine.diceroller.presentation.RollHistoryStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Key under which the encoded roll log is stored. */
private val ROLL_HISTORY_KEY = stringPreferencesKey("roll_history")

/**
 * [RollHistoryStore] backed by DataStore Preferences, sharing the [diceDataStore] file with
 * [DataStoreDiceColorStore].
 *
 * Records are held newest-first as a single [RollHistoryCodec]-encoded string and trimmed to
 * [MAX_HISTORY_RECORDS] on every write, so the stored value is bounded no matter how long the
 * app is used — and, since the log is append-only, that cap is the only bound there is. A log
 * that fails to decode degrades to whatever records survive rather than throwing — see
 * [RollHistoryCodec].
 *
 * @param context any context; the application context is retained internally
 */
class DataStoreRollHistoryStore(context: Context) : RollHistoryStore {

    private val dataStore = context.applicationContext.diceDataStore

    override val history: Flow<List<RollRecord>> = dataStore.data.map { preferences ->
        RollHistoryCodec.decode(preferences[ROLL_HISTORY_KEY])
    }

    override suspend fun record(record: RollRecord) {
        dataStore.edit { preferences ->
            val current = RollHistoryCodec.decode(preferences[ROLL_HISTORY_KEY])
            val updated = (listOf(record) + current).take(MAX_HISTORY_RECORDS)
            preferences[ROLL_HISTORY_KEY] = RollHistoryCodec.encode(updated)
        }
    }
}
