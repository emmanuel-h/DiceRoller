// app/src/main/java/fr/mandarine/diceroller/data/DataStoreDiceColorStore.kt
package fr.mandarine.diceroller.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import fr.mandarine.diceroller.presentation.DiceColorStore
import fr.mandarine.diceroller.presentation.model.DiceColor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Name of the on-disk preferences file holding the dice settings. */
private const val PREFERENCES_NAME = "dice_settings"

private val Context.diceDataStore: DataStore<Preferences> by preferencesDataStore(
    name = PREFERENCES_NAME,
)

/** Key under which the selected color's enum name is stored. */
private val SELECTED_COLOR_KEY = stringPreferencesKey("selected_color")

/**
 * [DiceColorStore] backed by DataStore Preferences.
 *
 * The color is stored by its enum [DiceColor.name] rather than its ordinal, so
 * reordering the enum does not silently change a user's saved choice. An
 * unknown or missing name falls back to [DiceColor.Default].
 *
 * @param context any context; the application context is retained internally
 */
class DataStoreDiceColorStore(context: Context) : DiceColorStore {

    private val dataStore = context.applicationContext.diceDataStore

    override val selectedColor: Flow<DiceColor> = dataStore.data.map { preferences ->
        DiceColor.fromNameOrDefault(preferences[SELECTED_COLOR_KEY])
    }

    override suspend fun setSelectedColor(color: DiceColor) {
        dataStore.edit { preferences ->
            preferences[SELECTED_COLOR_KEY] = color.name
        }
    }
}
