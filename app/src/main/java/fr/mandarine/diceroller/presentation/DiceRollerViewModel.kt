// app/src/main/java/fr/mandarine/diceroller/presentation/DiceRollerViewModel.kt
package fr.mandarine.diceroller.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import fr.mandarine.diceroller.data.DataStoreCustomDiceStore
import fr.mandarine.diceroller.data.DataStoreDiceColorStore
import fr.mandarine.diceroller.data.DataStoreRollHistoryStore
import fr.mandarine.diceroller.domain.CustomDie
import fr.mandarine.diceroller.domain.Dice
import fr.mandarine.diceroller.domain.DicePool
import fr.mandarine.diceroller.domain.DiceRoller
import fr.mandarine.diceroller.domain.DieType
import fr.mandarine.diceroller.domain.RollRecord
import fr.mandarine.diceroller.presentation.model.DiceColor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for the dice roller screen.
 *
 * Holds the current [DiceRollerUiState] and exposes actions for adjusting the dice pool, choosing
 * a color, defining custom dice, rolling, and browsing the roll log. The color, the custom dice
 * and the log are all restored from storage on creation and written back as the user changes them.
 * The log is append-only — there is no clear action, by design. The pool itself is not persisted —
 * every new instance starts empty, per the multi-dice-roll PRD.
 *
 * @param diceRoller rolls the pool; injected with a seeded [kotlin.random.Random] in tests
 * @param colorStore persists the chosen color variant
 * @param historyStore persists past rolls
 * @param customDiceStore persists the user's custom die definitions
 * @param clock reads the current epoch time, injected so timestamps are deterministic in tests
 */
class DiceRollerViewModel(
    private val diceRoller: DiceRoller = DiceRoller(),
    private val colorStore: DiceColorStore = InMemoryDiceColorStore(),
    private val historyStore: RollHistoryStore = InMemoryRollHistoryStore(),
    private val customDiceStore: CustomDiceStore = InMemoryCustomDiceStore(),
    private val clock: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DiceRollerUiState(nowMillis = clock()))

    /** Observable UI state for the dice roller screen. */
    val uiState: StateFlow<DiceRollerUiState> = _uiState.asStateFlow()

    /**
     * The pool count [removedCustomDie]'s die had when it was removed, so [undoRemoveCustomDie]
     * restores the pool the user had rather than just the definition.
     *
     * Kept beside the state instead of in it: it is bookkeeping for one pending undo, and nothing
     * on screen renders it.
     */
    private var removedCustomDieCount: Int = 0

    init {
        viewModelScope.launch {
            val storedColor = colorStore.selectedColor.first()
            _uiState.update { state -> state.copy(selectedColor = storedColor) }
        }
        // Collected rather than read once: the store is the single source of truth for the log,
        // so a recorded roll lands here instead of being applied to the state twice.
        viewModelScope.launch {
            historyStore.history.collect { records ->
                _uiState.update { state -> state.copy(history = records) }
            }
        }
        // Likewise the single source of truth for the definitions, so add/remove/undo each write
        // once and read their result back here. The pool gains a zero entry for every restored
        // die, keeping "every visible chip has a pool key" true from the first frame.
        viewModelScope.launch {
            customDiceStore.customDice.collect { dice ->
                _uiState.update { state -> state.withCustomDice(dice) }
            }
        }
    }

    /**
     * Increments the pool count for [dice] by one, clamped to [DicePool.MAX_DICE_PER_TYPE].
     *
     * Clears the previous roll result, since the pool that would be rolled has changed. If the
     * count is already at the cap, this is a no-op and the state is left untouched.
     */
    fun incrementCount(dice: DieType) {
        updateCount(dice) { count -> (count + 1).coerceAtMost(DicePool.MAX_DICE_PER_TYPE) }
    }

    /**
     * Decrements the pool count for [dice] by one, clamped to a floor of 0.
     *
     * Clears the previous roll result, since the pool that would be rolled has changed. If the
     * count is already at 0, this is a no-op and the state is left untouched.
     */
    fun decrementCount(dice: DieType) {
        updateCount(dice) { count -> (count - 1).coerceAtLeast(0) }
    }

    /**
     * Applies [transform] to the current count for [dice] and stores the result, clearing the
     * roll result if the count actually changed. Leaves the state untouched when [transform]
     * yields the same count, mirroring the previous "re-select same die = no-op" pattern so the
     * "count change clears result" rule stays exact.
     *
     * Only the *current* result is cleared: the history log is a record of what was rolled and
     * is never rewritten by anything the user does to the pool afterwards.
     */
    private fun updateCount(dice: DieType, transform: (Int) -> Int) {
        _uiState.update { state ->
            val currentCount = state.pool[dice] ?: 0
            val newCount = transform(currentCount)
            if (newCount == currentCount) {
                state
            } else {
                state.copy(pool = state.pool + (dice to newCount), result = null)
            }
        }
    }

    /**
     * Puts every die type's count back to 0 in one action (issue #67), clearing the roll result
     * with them.
     *
     * The result must go: emptying the pool makes [DiceRollerUiState.canRoll] false, so a
     * face-ladder left on screen would describe a pool that no longer exists — the same rule
     * [updateCount] applies one count at a time.
     *
     * Every key is kept and zeroed rather than the map being rebuilt from [Dice.entries], so the
     * custom dice keep their chips and the selector's "every visible chip has a pool entry"
     * invariant holds. Clearing an already-empty pool leaves the state untouched, and the log is
     * left alone entirely: it records what *was* rolled, which emptying the pool does not change.
     */
    fun clearPool() {
        _uiState.update { state ->
            if (!state.canRoll) {
                state
            } else {
                state.copy(pool = state.pool.mapValues { 0 }, result = null)
            }
        }
    }

    /**
     * Selects the given [color] variant and persists it.
     *
     * Unlike [incrementCount]/[decrementCount] this deliberately preserves
     * [DiceRollerUiState.result] — recoloring the dice set is a cosmetic change, not a new roll.
     * For the same reason it recolors the history entries too, which store no color of their own.
     */
    fun selectColor(color: DiceColor) {
        _uiState.update { state -> state.copy(selectedColor = color) }
        viewModelScope.launch {
            colorStore.setSelectedColor(color)
        }
    }

    /** Opens the custom-die creator dialog. */
    fun showCustomDieCreator() {
        _uiState.update { state -> state.copy(isCustomDieCreatorVisible = true) }
    }

    /** Closes the custom-die creator dialog, discarding whatever was typed into it. */
    fun dismissCustomDieCreator() {
        _uiState.update { state -> state.copy(isCustomDieCreatorVisible = false) }
    }

    /**
     * Defines [die] as a custom die, persists it, and closes the creator.
     *
     * Deliberately does *not* clear [DiceRollerUiState.result]: the new die enters the pool at a
     * count of 0, so what the Roll button would roll is unchanged, and the "the pool changed"
     * rule in [updateCount] is about counts rather than about which chips exist.
     *
     * A die that is already defined, or one too many, is dropped rather than rejected loudly —
     * the creator has already run [validateCustomFaces] and cannot offer either, and
     * [normalizeCustomDice] would discard it on the way to storage regardless.
     */
    fun addCustomDie(die: CustomDie) {
        val current = _uiState.value.customDice
        _uiState.update { state -> state.copy(isCustomDieCreatorVisible = false) }
        if (die in current || current.size >= MAX_CUSTOM_DICE) return
        viewModelScope.launch {
            customDiceStore.setCustomDice(current + die)
        }
    }

    /**
     * Removes the custom die [die], dropping it out of the pool along with its count, and arms
     * [undoRemoveCustomDie].
     *
     * Clears the roll result only when the die had dice queued, since that is exactly when the
     * pool that would be rolled changed — the same rule [updateCount] follows. Past rolls of the
     * die stay in the log untouched: they record face counts, not definitions.
     */
    fun removeCustomDie(die: CustomDie) {
        val state = _uiState.value
        if (die !in state.customDice) return

        removedCustomDieCount = state.pool[die] ?: 0
        _uiState.update { current ->
            current.copy(
                pool = current.pool - die,
                result = if (removedCustomDieCount > 0) null else current.result,
                removedCustomDie = die,
            )
        }
        viewModelScope.launch {
            customDiceStore.setCustomDice(state.customDice - die)
        }
    }

    /**
     * Puts the last removed custom die back, with the pool count it had.
     *
     * The roll result stays cleared even though the pool is now identical to what it was: a
     * result is only ever produced by pressing Roll, and resurrecting one here would show the
     * user an outcome that no longer corresponds to anything they did.
     */
    fun undoRemoveCustomDie() {
        val die = _uiState.value.removedCustomDie ?: return
        val count = removedCustomDieCount
        val restored = _uiState.value.customDice + die

        _uiState.update { state ->
            state.copy(pool = state.pool + (die to count), removedCustomDie = null)
        }
        removedCustomDieCount = 0
        viewModelScope.launch {
            customDiceStore.setCustomDice(restored)
        }
    }

    /**
     * Acknowledges the pending removal, so its undo snackbar is not offered again.
     *
     * Called when the snackbar goes away on its own. Left separate from [undoRemoveCustomDie] so
     * dismissing is never mistaken for undoing.
     */
    fun dismissRemovedCustomDie() {
        _uiState.update { state -> state.copy(removedCustomDie = null) }
        removedCustomDieCount = 0
    }

    /**
     * Rolls every die currently in the pool, updates the result, empties the pool, and appends the
     * roll to the log.
     *
     * A no-op when the pool is empty ([DiceRollerUiState.canRoll] is false), matching
     * [DiceRoller.rollPool]'s own defensive no-op for an empty pool — nothing is recorded either.
     *
     * The pool is zeroed on the way out so a roll ends a run: the next one starts from a clean
     * selector rather than from whatever the last one happened to leave behind. The trade is
     * deliberate — rolling the same pool twice means queueing it again, since there is no longer a
     * pool for a second tap of Roll to act on.
     *
     * This is the one place a pool change does *not* clear the result: the result **is** what that
     * change produced. It survives until the user starts building the next pool, at which point
     * [updateCount]'s usual rule takes it away. Zeroing every key rather than rebuilding the map
     * keeps the custom dice's chips — the same reasoning as [clearPool], which this deliberately
     * does not call, because that one clears the result and this one must not.
     *
     * The new state is published synchronously and the log write is dispatched after it, so the
     * result never waits on storage.
     */
    fun rollDice() {
        val state = _uiState.value
        if (!state.canRoll) return

        val result = diceRoller.rollPool(DicePool(state.pool))
        val rolledAtMillis = clock()
        _uiState.update { current ->
            current.copy(
                pool = current.pool.mapValues { 0 },
                result = result,
                nowMillis = rolledAtMillis,
            )
        }
        viewModelScope.launch {
            historyStore.record(RollRecord(result = result, rolledAtMillis = rolledAtMillis))
        }
    }

    /**
     * Opens or closes the roll history band.
     *
     * Re-reads the clock on the way, so the relative timestamps are accurate whenever the list
     * actually becomes visible instead of drifting for as long as the app has been open.
     */
    fun toggleHistoryExpanded() {
        _uiState.update { state ->
            state.copy(isHistoryExpanded = !state.isHistoryExpanded, nowMillis = clock())
        }
    }

    /**
     * Opens the About sheet, which carries the artwork's license-required credit (issue #66).
     *
     * Like the creator dialog, this touches visibility only: it leaves the pool, the result and
     * the log exactly as they were, so reading the credit never costs the user a roll.
     */
    fun showAbout() {
        _uiState.update { state -> state.copy(isAboutVisible = true) }
    }

    /** Closes the About sheet. */
    fun dismissAbout() {
        _uiState.update { state -> state.copy(isAboutVisible = false) }
    }

    companion object {
        /**
         * Builds a factory that wires the ViewModel to the DataStore-backed color, history and
         * custom-dice stores, so the chosen color, the roll log and the user's own dice all
         * survive process death.
         */
        fun factory(context: Context): ViewModelProvider.Factory {
            val appContext = context.applicationContext
            return viewModelFactory {
                initializer {
                    DiceRollerViewModel(
                        colorStore = DataStoreDiceColorStore(appContext),
                        historyStore = DataStoreRollHistoryStore(appContext),
                        customDiceStore = DataStoreCustomDiceStore(appContext),
                    )
                }
            }
        }
    }
}

/**
 * This state with [dice] as its custom dice and a pool that matches: a zero count for every
 * newly-defined die, and no key at all for one that is gone.
 *
 * Reconciling the pool here rather than at each call site is what keeps the selector's invariant —
 * every chip on screen has a pool entry — true no matter how the definitions changed, including
 * the restore-from-storage case where they went from empty to three at once. Existing counts are
 * preserved, so an undo that re-adds a die does not lose the count
 * [DiceRollerViewModel.undoRemoveCustomDie] is about to put back.
 */
private fun DiceRollerUiState.withCustomDice(dice: List<CustomDie>): DiceRollerUiState {
    val keep: Set<DieType> = Dice.entries.toSet() + dice
    val reconciled = pool.filterKeys { it in keep } +
        dice.associateWith { die -> pool[die] ?: 0 }
    return copy(customDice = dice, pool = reconciled)
}
