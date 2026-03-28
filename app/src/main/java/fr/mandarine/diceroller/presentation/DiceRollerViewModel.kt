// app/src/main/java/fr/mandarine/diceroller/presentation/DiceRollerViewModel.kt
package fr.mandarine.diceroller.presentation

import androidx.lifecycle.ViewModel
import fr.mandarine.diceroller.domain.Dice
import fr.mandarine.diceroller.domain.DiceRoller
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * ViewModel for the dice roller screen.
 *
 * Holds the current [DiceRollerUiState] and exposes actions
 * for selecting a die and rolling it.
 */
class DiceRollerViewModel(
    private val diceRoller: DiceRoller = DiceRoller(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(DiceRollerUiState())

    /** Observable UI state for the dice roller screen. */
    val uiState: StateFlow<DiceRollerUiState> = _uiState.asStateFlow()

    /**
     * Selects the given [dice] type.
     *
     * When the die type changes, the previous roll result is cleared to null
     * so the result display shows the empty state for the newly selected shape.
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
     * Rolls the currently selected die and updates the result.
     */
    fun rollDice() {
        _uiState.update { state ->
            state.copy(result = diceRoller.roll(state.selectedDice))
        }
    }
}
