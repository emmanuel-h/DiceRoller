// app/src/main/java/fr/mandarine/diceroller/presentation/component/DicePolygonSize.kt
package fr.mandarine.diceroller.presentation.component

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Size variants for the [DicePolygon] composable.
 *
 * @property sizeDp the side length of the square bounding box
 * @property strokeWidthDp the polygon outline stroke width
 */
enum class DicePolygonSize(val sizeDp: Dp, val strokeWidthDp: Dp) {

    /** Small variant used in the dice selector row. */
    Small(sizeDp = 48.dp, strokeWidthDp = 2.dp),

    /** Large variant used in the result display area. */
    Large(sizeDp = 160.dp, strokeWidthDp = 3.dp),
}
