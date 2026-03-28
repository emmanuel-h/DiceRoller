// app/src/main/java/fr/mandarine/diceroller/presentation/DiceRollerUiState.kt
package fr.mandarine.diceroller.presentation

import fr.mandarine.diceroller.domain.Dice

/**
 * UI state for the dice roller screen.
 *
 * @property selectedDice the currently selected die type
 * @property result the result of the last roll, or null if no roll has been performed yet
 */
data class DiceRollerUiState(
    val selectedDice: Dice = Dice.D6,
    val result: Int? = null,
)
