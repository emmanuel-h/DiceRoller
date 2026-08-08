// app/src/main/java/fr/mandarine/diceroller/presentation/DiceColorStore.kt
package fr.mandarine.diceroller.presentation

import fr.mandarine.diceroller.presentation.model.DiceColor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Persists the user's chosen dice color across app launches.
 *
 * Declared as an interface so [DiceRollerViewModel] can be unit-tested with
 * [InMemoryDiceColorStore] instead of a real DataStore — the same constructor
 * injection approach the project uses for [fr.mandarine.diceroller.domain.DiceRoller].
 */
interface DiceColorStore {

    /** Emits the stored color, falling back to [DiceColor.Default] when none is set. */
    val selectedColor: Flow<DiceColor>

    /** Stores [color] as the user's choice. */
    suspend fun setSelectedColor(color: DiceColor)
}

/**
 * Non-persistent [DiceColorStore] used by previews and unit tests.
 *
 * @param initial the color the store starts with
 */
class InMemoryDiceColorStore(
    initial: DiceColor = DiceColor.Default,
) : DiceColorStore {

    private val state = MutableStateFlow(initial)

    override val selectedColor: Flow<DiceColor> = state.asStateFlow()

    override suspend fun setSelectedColor(color: DiceColor) {
        state.value = color
    }
}
