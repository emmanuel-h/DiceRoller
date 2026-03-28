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
     * The previous roll result is preserved until the next roll,
     * as specified by the design spec.
     */
    fun selectDice(dice: Dice) {
        _uiState.update { it.copy(selectedDice = dice) }
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
