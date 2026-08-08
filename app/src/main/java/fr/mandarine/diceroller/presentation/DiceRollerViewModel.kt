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
 * Holds the current [DiceRollerUiState] and exposes actions for selecting a
 * die, choosing a color and rolling. The color is restored from [colorStore]
 * on creation and written back whenever the user picks a new one.
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
     * Selects the given [dice] type.
     *
     * When the die type changes, the previous roll result is cleared to null
     * so the result display shows the empty state for the newly selected die.
     * If the same die type is re-selected, the state is unchanged.
     */
    fun selectDice(dice: Dice) {
        _uiState.update { state ->
            if (state.selectedDice == dice) {
                state
            } else {
                state.copy(selectedDice = dice, result = null)
            }
        }
    }

    /**
     * Selects the given [color] variant and persists it.
     *
     * Unlike [selectDice] this deliberately preserves [DiceRollerUiState.result]
     * — recoloring the dice set is a cosmetic change, not a new roll.
     */
    fun selectColor(color: DiceColor) {
        _uiState.update { state -> state.copy(selectedColor = color) }
        viewModelScope.launch {
            colorStore.setSelectedColor(color)
        }
    }

    /**
     * Rolls the currently selected die and updates the result.
     */
    fun rollDice() {
        _uiState.update { state ->
            state.copy(result = diceRoller.roll(state.selectedDice))
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
