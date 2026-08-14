// app/src/main/java/fr/mandarine/diceroller/data/DiceDataStore.kt
package fr.mandarine.diceroller.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

/** Name of the on-disk preferences file holding every dice setting. */
private const val PREFERENCES_NAME = "dice_settings"

/**
 * The app's single DataStore instance, shared by every store in this package.
 *
 * It lives here rather than beside one of its users because `preferencesDataStore` throws at
 * runtime if two delegates are created for the same file name — so there must be exactly one
 * declaration, and each store ([DataStoreDiceColorStore], [DataStoreRollHistoryStore]) owns only
 * its own keys within it.
 */
internal val Context.diceDataStore: DataStore<Preferences> by preferencesDataStore(
    name = PREFERENCES_NAME,
)
