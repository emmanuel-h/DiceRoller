// app/src/main/java/fr/mandarine/diceroller/presentation/DiceRollerViewModel.kt
package fr.mandarine.diceroller.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import fr.mandarine.diceroller.data.DataStoreDiceColorStore
import fr.mandarine.diceroller.data.DataStoreRollHistoryStore
import fr.mandarine.diceroller.domain.Dice
import fr.mandarine.diceroller.domain.DicePool
import fr.mandarine.diceroller.domain.DiceRoller
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
 * Holds the current [DiceRollerUiState] and exposes actions for adjusting the dice pool,
 * choosing a color, rolling, and browsing the roll log. The color and the log are both restored
 * from storage on creation and written back as the user changes them. The log is append-only —
 * there is no clear action, by design. The pool itself is not persisted — every new instance
 * starts empty, per the multi-dice-roll PRD.
 *
 * @param diceRoller rolls the pool; injected with a seeded [kotlin.random.Random] in tests
 * @param colorStore persists the chosen color variant
 * @param historyStore persists past rolls
 * @param clock reads the current epoch time, injected so timestamps are deterministic in tests
 */
class DiceRollerViewModel(
    private val diceRoller: DiceRoller = DiceRoller(),
    private val colorStore: DiceColorStore = InMemoryDiceColorStore(),
    private val historyStore: RollHistoryStore = InMemoryRollHistoryStore(),
    private val clock: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DiceRollerUiState(nowMillis = clock()))

    /** Observable UI state for the dice roller screen. */
    val uiState: StateFlow<DiceRollerUiState> = _uiState.asStateFlow()

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
    }

    /**
     * Increments the pool count for [dice] by one, clamped to [DicePool.MAX_DICE_PER_TYPE].
     *
     * Clears the previous roll result, since the pool that would be rolled has changed. If the
     * count is already at the cap, this is a no-op and the state is left untouched.
     */
    fun incrementCount(dice: Dice) {
        updateCount(dice) { count -> (count + 1).coerceAtMost(DicePool.MAX_DICE_PER_TYPE) }
    }

    /**
     * Decrements the pool count for [dice] by one, clamped to a floor of 0.
     *
     * Clears the previous roll result, since the pool that would be rolled has changed. If the
     * count is already at 0, this is a no-op and the state is left untouched.
     */
    fun decrementCount(dice: Dice) {
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
    private fun updateCount(dice: Dice, transform: (Int) -> Int) {
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

    /**
     * Rolls every die currently in the pool, updates the result, and appends the roll to the log.
     *
     * A no-op when the pool is empty ([DiceRollerUiState.canRoll] is false), matching
     * [DiceRoller.rollPool]'s own defensive no-op for an empty pool — nothing is recorded either.
     *
     * The new state is published synchronously and the log write is dispatched after it, so the
     * result never waits on storage.
     */
    fun rollDice() {
        val state = _uiState.value
        if (!state.canRoll) return

        val result = diceRoller.rollPool(DicePool(state.pool))
        val rolledAtMillis = clock()
        _uiState.update { current -> current.copy(result = result, nowMillis = rolledAtMillis) }
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

    companion object {
        /**
         * Builds a factory that wires the ViewModel to the DataStore-backed color and history
         * stores, so both the chosen color and the roll log survive process death.
         */
        fun factory(context: Context): ViewModelProvider.Factory {
            val appContext = context.applicationContext
            return viewModelFactory {
                initializer {
                    DiceRollerViewModel(
                        colorStore = DataStoreDiceColorStore(appContext),
                        historyStore = DataStoreRollHistoryStore(appContext),
                    )
                }
            }
        }
    }
}
