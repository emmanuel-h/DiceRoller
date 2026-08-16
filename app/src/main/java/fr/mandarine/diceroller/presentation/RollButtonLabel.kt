// app/src/main/java/fr/mandarine/diceroller/presentation/RollButtonLabel.kt
package fr.mandarine.diceroller.presentation

import fr.mandarine.diceroller.R
import fr.mandarine.diceroller.domain.DicePool

/**
 * Formats the Roll button's label from [pool].
 *
 * A non-empty pool renders as the verb followed by its [poolNotation] — e.g. `"Roll 4D6 + 2D8"`
 * for a mixed pool, or `"Roll 4D6"` for a single die type (no `+`). Only the verb is translated;
 * the notation is a domain notation and goes in as an argument unchanged.
 *
 * An empty pool (all counts 0) renders as [R.string.roll_button_empty] instead, matching the Roll
 * button's disabled empty state.
 *
 * @param pool the dice pool to format a label for
 */
fun rollButtonLabel(pool: DicePool): UiText {
    if (pool.isEmpty) return UiText.Res(R.string.roll_button_empty)
    return UiText.Res(R.string.roll_button, listOf(poolNotation(pool.entries)))
}
