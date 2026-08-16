// app/src/main/java/fr/mandarine/diceroller/presentation/DiceRollerUiState.kt
package fr.mandarine.diceroller.presentation

import fr.mandarine.diceroller.domain.CustomDie
import fr.mandarine.diceroller.domain.Dice
import fr.mandarine.diceroller.domain.DicePoolResult
import fr.mandarine.diceroller.domain.DieType
import fr.mandarine.diceroller.domain.RollRecord
import fr.mandarine.diceroller.presentation.model.DiceColor

/**
 * UI state for the dice roller screen.
 *
 * @property pool how many dice of each [DieType] are currently queued to be rolled together.
 *   Every [Dice] preset is always present as a key; a count of 0 means that die type is excluded
 *   from the next roll. Each of [customDice] is a key too, added when the die is defined and
 *   removed with it.
 * @property customDice the user's own die definitions (issue #4), ascending by face count, at most
 *   [MAX_CUSTOM_DICE] of them. Restored from storage on creation, like [selectedColor].
 * @property selectedColor the currently selected dice color variant
 * @property result the tallied outcome of the last roll, or null if no roll has been performed
 *   yet, or the pool has changed since the last roll
 * @property history past rolls, newest first, at most [MAX_HISTORY_RECORDS] of them. Restored
 *   from storage on creation, so it is generally non-empty from the first frame.
 * @property isHistoryExpanded whether the history band is showing its list rather than just its
 *   header. Session state, deliberately not persisted: a log worth keeping is not the same as a
 *   drawer worth reopening.
 * @property isCustomDieCreatorVisible whether the "add a custom die" dialog is open. Session
 *   state for the same reason.
 * @property language the language the app is written in. [AppLanguage.System] — the value every
 *   install starts at — means "whatever the device is set to", which Android's own resource
 *   resolution turns into English for any device language this app does not ship.
 * @property isSettingsVisible whether the settings sheet is open — the one that carries the
 *   language row, and the license-required artwork credit since issue #66 took it off the main
 *   screen. Session state too: a setting is something to be able to reach, not something to
 *   reopen on every launch.
 * @property removedCustomDie the die the last [DiceRollerViewModel.removeCustomDie] took away, or
 *   null once that removal has been undone or its snackbar acknowledged. Present so the screen can
 *   offer an undo: the remove control is a small badge on the edge of the increment half, so a
 *   mistap is likely enough that the definition should not vanish irrecoverably.
 * @property nowMillis the reference time the history's relative timestamps are rendered against,
 *   refreshed on each roll and each expand rather than ticking continuously
 */
data class DiceRollerUiState(
    val pool: Map<DieType, Int> = Dice.entries.associateWith { 0 },
    val customDice: List<CustomDie> = emptyList(),
    val selectedColor: DiceColor = DiceColor.Default,
    val language: AppLanguage = AppLanguage.System,
    val result: DicePoolResult? = null,
    val history: List<RollRecord> = emptyList(),
    val isHistoryExpanded: Boolean = false,
    val isCustomDieCreatorVisible: Boolean = false,
    val isSettingsVisible: Boolean = false,
    val removedCustomDie: CustomDie? = null,
    val nowMillis: Long = 0L,
) {

    /**
     * Every die type the pool selector shows a chip for: the six presets in their declared order,
     * then the custom dice ascending by face count.
     *
     * Custom dice are appended rather than interleaved by face count, so defining a D7 does not
     * shuffle the presets a user already knows the position of. Results and notation *do*
     * interleave them — that ordering belongs to
     * [fr.mandarine.diceroller.domain.DicePool.entries], not to the selector.
     */
    val dieTypes: List<DieType> get() = Dice.entries + customDice

    /**
     * True while there is room for another custom die. When false the selector's add chip is
     * hidden rather than disabled, which is also what keeps the chip grid at exactly three rows —
     * see [MAX_CUSTOM_DICE].
     */
    val canAddCustomDie: Boolean get() = customDice.size < MAX_CUSTOM_DICE

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
