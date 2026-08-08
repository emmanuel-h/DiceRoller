// app/src/main/java/fr/mandarine/diceroller/presentation/DiceRollerUiState.kt
package fr.mandarine.diceroller.presentation

import fr.mandarine.diceroller.domain.Dice
import fr.mandarine.diceroller.presentation.model.DiceColor

/**
 * UI state for the dice roller screen.
 *
 * @property selectedDice the currently selected die type
 * @property selectedColor the currently selected dice color variant
 * @property result the result of the last roll, or null if no roll has been performed yet
 */
data class DiceRollerUiState(
    val selectedDice: Dice = Dice.D6,
    val selectedColor: DiceColor = DiceColor.Default,
    val result: Int? = null,
)
