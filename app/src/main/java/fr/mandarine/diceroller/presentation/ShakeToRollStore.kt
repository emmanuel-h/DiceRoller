// app/src/main/java/fr/mandarine/diceroller/presentation/ShakeToRollStore.kt
package fr.mandarine.diceroller.presentation

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Persists whether shaking the phone rolls the dice (issue #1).
 *
 * On unless the user turns it off: shaking is the gesture a dice app invites, and the switch exists
 * for the times it is not wanted — a bumpy train, a phone in a pocket — rather than as something to
 * discover first. Declared as an interface for the same reason as [DiceColorStore].
 */
interface ShakeToRollStore {

    /** Emits the stored choice, falling back to true when none is set. */
    val isEnabled: Flow<Boolean>

    /** Stores [enabled] as the user's choice. */
    suspend fun setEnabled(enabled: Boolean)
}

/**
 * Non-persistent [ShakeToRollStore] used by previews and unit tests.
 *
 * @param initial the choice the store starts with
 */
class InMemoryShakeToRollStore(initial: Boolean = true) : ShakeToRollStore {

    private val state = MutableStateFlow(initial)

    override val isEnabled: Flow<Boolean> = state.asStateFlow()

    override suspend fun setEnabled(enabled: Boolean) {
        state.value = enabled
    }
}
