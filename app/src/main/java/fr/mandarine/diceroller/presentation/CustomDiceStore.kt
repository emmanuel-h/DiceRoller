// app/src/main/java/fr/mandarine/diceroller/presentation/CustomDiceStore.kt
package fr.mandarine.diceroller.presentation

import fr.mandarine.diceroller.domain.CustomDie
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * How many custom dice the user may define at once.
 *
 * This is a layout budget, not a technical limit. The pool selector shows six presets plus the
 * custom dice plus an "add" chip, three to a row: at this cap the grid is *always* exactly three
 * rows — 7, 8 or 9 chips with the add chip, and 9 without it once the cap is reached and the add
 * chip is hidden. So issue #64's scroll-free screen loses one chip row to this feature and never
 * a second one, whatever the user does.
 */
const val MAX_CUSTOM_DICE: Int = 3

/**
 * Persists the user's custom die definitions across app launches (issue #4).
 *
 * Only the definitions live here — how many of each are queued is pool state, which is
 * deliberately not persisted at all (see [DiceRollerViewModel]). Deleting a definition therefore
 * removes a die the user built, not a roll they made: past rolls of it stay in the log, because
 * [fr.mandarine.diceroller.data.RollHistoryCodec] records face counts rather than references.
 *
 * Declared as an interface for the same reason as [DiceColorStore] and [RollHistoryStore]: it
 * lets [DiceRollerViewModel] be unit-tested against [InMemoryCustomDiceStore] with no DataStore
 * and no Android runtime, using constructor injection rather than a mocking framework.
 *
 * Implementations own [normalizeCustomDice], so no caller has to remember to dedupe, sort or cap.
 */
interface CustomDiceStore {

    /** Emits the defined dice, ascending by face count, at most [MAX_CUSTOM_DICE] of them. */
    val customDice: Flow<List<CustomDie>>

    /** Replaces the stored set with [dice], normalized via [normalizeCustomDice]. */
    suspend fun setCustomDice(dice: List<CustomDie>)
}

/**
 * Puts a custom-dice list into the one shape every layer expects to read: de-duplicated, ascending
 * by face count, and no longer than [MAX_CUSTOM_DICE].
 *
 * Ascending order matters beyond tidiness — it is the order the chips appear in the selector, and
 * it matches the smallest-to-largest ordering [fr.mandarine.diceroller.domain.DicePool.entries]
 * imposes on results, so a die sits in the same relative place in both.
 */
fun normalizeCustomDice(dice: List<CustomDie>): List<CustomDie> =
    dice.distinct().sortedBy { it.faces }.take(MAX_CUSTOM_DICE)

/**
 * Non-persistent [CustomDiceStore] used by previews and unit tests.
 *
 * @param initial the dice the store starts with; normalized, so tests may pass any order
 */
class InMemoryCustomDiceStore(
    initial: List<CustomDie> = emptyList(),
) : CustomDiceStore {

    private val state = MutableStateFlow(normalizeCustomDice(initial))

    override val customDice: Flow<List<CustomDie>> = state.asStateFlow()

    override suspend fun setCustomDice(dice: List<CustomDie>) {
        state.value = normalizeCustomDice(dice)
    }
}
