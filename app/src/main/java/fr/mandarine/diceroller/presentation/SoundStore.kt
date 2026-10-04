// app/src/main/java/fr/mandarine/diceroller/presentation/SoundStore.kt
package fr.mandarine.diceroller.presentation

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Persists whether a roll plays its dice sound (issue #5).
 *
 * On unless the user turns it off, like [ShakeToRollStore]: the sound plays on the media stream, so
 * a phone with its media volume down is already silent, and the switch is for the times it is not —
 * a quiet table where the volume is up for something else. Declared as an interface for the same
 * reason as [DiceColorStore].
 */
interface SoundStore {

    /** Emits the stored choice, falling back to true when none is set. */
    val isEnabled: Flow<Boolean>

    /** Stores [enabled] as the user's choice. */
    suspend fun setEnabled(enabled: Boolean)
}

/**
 * Non-persistent [SoundStore] used by previews and unit tests.
 *
 * @param initial the choice the store starts with
 */
class InMemorySoundStore(initial: Boolean = true) : SoundStore {

    private val state = MutableStateFlow(initial)

    override val isEnabled: Flow<Boolean> = state.asStateFlow()

    override suspend fun setEnabled(enabled: Boolean) {
        state.value = enabled
    }
}
