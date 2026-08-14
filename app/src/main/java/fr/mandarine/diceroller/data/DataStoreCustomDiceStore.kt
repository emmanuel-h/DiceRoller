// app/src/main/java/fr/mandarine/diceroller/data/DataStoreCustomDiceStore.kt
package fr.mandarine.diceroller.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import fr.mandarine.diceroller.domain.CustomDie
import fr.mandarine.diceroller.presentation.CustomDiceStore
import fr.mandarine.diceroller.presentation.normalizeCustomDice
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Key under which the encoded custom die definitions are stored. */
private val CUSTOM_DICE_KEY = stringPreferencesKey("custom_dice")

/**
 * [CustomDiceStore] backed by DataStore Preferences, sharing the [diceDataStore] file with
 * [DataStoreDiceColorStore] and [DataStoreRollHistoryStore] — it must, since a second
 * `preferencesDataStore` delegate for the same file name throws at runtime.
 *
 * Definitions are held as a [CustomDiceCodec]-encoded list of face counts. A stored value that
 * fails to decode degrades to whatever definitions survive rather than throwing.
 *
 * @param context any context; the application context is retained internally
 */
class DataStoreCustomDiceStore(context: Context) : CustomDiceStore {

    private val dataStore = context.applicationContext.diceDataStore

    override val customDice: Flow<List<CustomDie>> = dataStore.data.map { preferences ->
        CustomDiceCodec.decode(preferences[CUSTOM_DICE_KEY])
    }

    override suspend fun setCustomDice(dice: List<CustomDie>) {
        dataStore.edit { preferences ->
            preferences[CUSTOM_DICE_KEY] = CustomDiceCodec.encode(normalizeCustomDice(dice))
        }
    }
}
