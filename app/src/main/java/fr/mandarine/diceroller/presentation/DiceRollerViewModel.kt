// app/src/main/java/fr/mandarine/diceroller/presentation/DiceRollerViewModel.kt
package fr.mandarine.diceroller.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import fr.mandarine.diceroller.data.DataStoreDiceColorStore
import fr.mandarine.diceroller.domain.Dice
import fr.mandarine.diceroller.domain.DicePool
import fr.mandarine.diceroller.domain.DiceRoller
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
 * choosing a color and rolling. The color is restored from [colorStore] on creation and written
 * back whenever the user picks a new one. The pool itself is not persisted — every new instance
 * starts empty, per the multi-dice-roll PRD.
 */
class DiceRollerViewModel(
    private val diceRoller: DiceRoller = DiceRoller(),
    private val colorStore: DiceColorStore = InMemoryDiceColorStore(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(DiceRollerUiState())

    /** Observable UI state for the dice roller screen. */
    val uiState: StateFlow<DiceRollerUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val storedColor = colorStore.selectedColor.first()
            _uiState.update { state -> state.copy(selectedColor = storedColor) }
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
     */
    fun selectColor(color: DiceColor) {
        _uiState.update { state -> state.copy(selectedColor = color) }
        viewModelScope.launch {
            colorStore.setSelectedColor(color)
        }
    }

    /**
     * Rolls every die currently in the pool and updates the result with the tallied outcome.
     *
     * A no-op when the pool is empty ([DiceRollerUiState.canRoll] is false), matching
     * [DiceRoller.rollPool]'s own defensive no-op for an empty pool.
     */
    fun rollDice() {
        _uiState.update { state ->
            if (!state.canRoll) {
                state
            } else {
                state.copy(result = diceRoller.rollPool(DicePool(state.pool)))
            }
        }
    }

    companion object {
        /**
         * Builds a factory that wires the ViewModel to the DataStore-backed
         * color store, so the chosen color survives process death.
         */
        fun factory(context: Context): ViewModelProvider.Factory {
            val appContext = context.applicationContext
            return viewModelFactory {
                initializer {
                    DiceRollerViewModel(colorStore = DataStoreDiceColorStore(appContext))
                }
            }
        }
    }
}
