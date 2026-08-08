// app/src/main/java/fr/mandarine/diceroller/presentation/component/DiceImageSize.kt
package fr.mandarine.diceroller.presentation.component

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Size variants for the [DiceImage] composable.
 *
 * @property sizeDp the side length of the square box the artwork is fitted into
 */
enum class DiceImageSize(val sizeDp: Dp) {

    /** Small variant used in the dice selector row. */
    Small(sizeDp = 56.dp),

    /** Large variant used in the result display area. */
    Large(sizeDp = 160.dp),
}
