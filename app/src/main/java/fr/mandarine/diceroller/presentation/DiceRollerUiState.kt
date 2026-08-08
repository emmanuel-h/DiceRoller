// app/src/main/java/fr/mandarine/diceroller/presentation/DiceRollerUiState.kt
package fr.mandarine.diceroller.presentation

import fr.mandarine.diceroller.domain.Dice
import fr.mandarine.diceroller.domain.DicePoolResult
import fr.mandarine.diceroller.presentation.model.DiceColor

/**
 * UI state for the dice roller screen.
 *
 * @property pool how many dice of each [Dice] type are currently queued to be rolled together.
 *   Every [Dice] entry is always present as a key; a count of 0 means that die type is excluded
 *   from the next roll.
 * @property selectedColor the currently selected dice color variant
 * @property result the tallied outcome of the last roll, or null if no roll has been performed
 *   yet, or the pool has changed since the last roll
 */
data class DiceRollerUiState(
    val pool: Map<Dice, Int> = Dice.entries.associateWith { 0 },
    val selectedColor: DiceColor = DiceColor.Default,
    val result: DicePoolResult? = null,
) {

    /**
     * True when at least one die type in [pool] has a non-zero count, i.e. a roll would produce
     * a non-empty result.
     */
    val canRoll: Boolean get() = pool.values.any { it > 0 }
}
