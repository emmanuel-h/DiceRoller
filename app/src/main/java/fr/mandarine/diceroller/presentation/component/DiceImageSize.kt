// app/src/main/java/fr/mandarine/diceroller/presentation/component/DiceImageSize.kt
package fr.mandarine.diceroller.presentation.component

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Size variants for the [DiceImage] composable.
 *
 * Both variants exist to make the artwork the dominant element of whatever hosts it: the die art
 * carries the app's identity, so the wording around it is deliberately smaller (issues #62, #63).
 *
 * @property sizeDp the side length of the square box the artwork is fitted into
 */
enum class DiceImageSize(val sizeDp: Dp) {

    /** Inline variant used inside a result face-ladder entry, beside its value and `×N` count. */
    Inline(sizeDp = 36.dp),

    /** Chip variant: the artwork that fills a [DiceStepperChip] in the pool selector. */
    Small(sizeDp = 56.dp),
}
