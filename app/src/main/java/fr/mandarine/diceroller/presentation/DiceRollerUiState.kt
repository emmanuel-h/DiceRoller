// app/src/main/java/fr/mandarine/diceroller/presentation/DiceRollerUiState.kt
package fr.mandarine.diceroller.presentation

import fr.mandarine.diceroller.domain.Dice
import fr.mandarine.diceroller.domain.DicePoolResult
import fr.mandarine.diceroller.domain.RollRecord
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
 * @property history past rolls, newest first, at most [MAX_HISTORY_RECORDS] of them. Restored
 *   from storage on creation, so it is generally non-empty from the first frame.
 * @property isHistoryExpanded whether the history band is showing its list rather than just its
 *   header. Session state, deliberately not persisted: a log worth keeping is not the same as a
 *   drawer worth reopening.
 * @property nowMillis the reference time the history's relative timestamps are rendered against,
 *   refreshed on each roll and each expand rather than ticking continuously
 */
data class DiceRollerUiState(
    val pool: Map<Dice, Int> = Dice.entries.associateWith { 0 },
    val selectedColor: DiceColor = DiceColor.Default,
    val result: DicePoolResult? = null,
    val history: List<RollRecord> = emptyList(),
    val isHistoryExpanded: Boolean = false,
    val nowMillis: Long = 0L,
) {

    /**
     * True when at least one die type in [pool] has a non-zero count, i.e. a roll would produce
     * a non-empty result.
     */
    val canRoll: Boolean get() = pool.values.any { it > 0 }

    /**
     * True when there is at least one past roll to show. The history band is absent entirely
     * until then, so a first launch looks exactly like it did before the log existed.
     */
    val hasHistory: Boolean get() = history.isNotEmpty()
}
