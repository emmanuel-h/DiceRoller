// app/src/main/java/fr/mandarine/diceroller/presentation/component/DiceImageSize.kt
package fr.mandarine.diceroller.presentation.component

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Size variants for the [DiceImage] composable.
 *
 * The variants exist to make the artwork the dominant element of whatever hosts it: the die art
 * carries the app's identity, so the wording around it is deliberately smaller (issues #62, #63).
 * The exception is [Compact], which is deliberately quiet — it renders rolls that are over.
 *
 * @property sizeDp the side length of the square box the artwork is fitted into
 */
enum class DiceImageSize(val sizeDp: Dp) {

    /**
     * History variant: past rolls in the roll-history band, sized to read as a record of a roll
     * rather than to compete with the live result above it (issue #3).
     */
    Compact(sizeDp = 24.dp),

    /** Inline variant used inside a result face-ladder entry, beside its value and `×N` count. */
    Inline(sizeDp = 36.dp),

    /** Chip variant: the artwork that fills a [DiceStepperChip] in the pool selector. */
    Small(sizeDp = 56.dp),
}
