// app/src/main/java/fr/mandarine/diceroller/presentation/component/DiceResultDisplay.kt
package fr.mandarine.diceroller.presentation.component

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import fr.mandarine.diceroller.domain.Dice
import fr.mandarine.diceroller.ui.theme.DiceRollerTheme

/**
 * Displays the dice roll result inside a large [DiceImage].
 *
 * - **Empty state** (result is null): die asset tinted in `outlineVariant`
 *   with an en-dash placeholder centered inside.
 * - **D6 with result 1–6**: die asset tinted in `primary` with [D6Pips]
 *   rendered on top.
 * - **All other dice with a result**: die asset tinted in `primary` with
 *   the result number in `displayLarge` typography centered on top.
 *
 * The composable carries `liveRegion = LiveRegionMode.Polite` semantics so
 * accessibility services announce result changes.
 *
 * @param selectedDice the currently selected die type whose asset to display
 * @param result the roll result, or null when no roll has been performed yet
 * @param modifier optional [Modifier] applied to the root container
 */
@Composable
fun DiceResultDisplay(
    selectedDice: Dice,
    result: Int?,
    modifier: Modifier = Modifier,
) {
    val tintColor = if (result == null) {
        MaterialTheme.colorScheme.outlineVariant
    } else {
        MaterialTheme.colorScheme.primary
    }

    DiceImage(
        dice = selectedDice,
        sizeVariant = DiceImageSize.Large,
        modifier = modifier.semantics { liveRegion = LiveRegionMode.Polite },
        tintColor = tintColor,
    ) {
        when {
            result == null -> {
                Text(
                    text = "–",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            selectedDice == Dice.D6 && result in 1..6 -> {
                D6Pips(
                    result = result,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                )
            }
            else -> {
                Text(
                    text = result.toString(),
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

// -- Previews -----------------------------------------------------------------

@Preview(name = "Empty state - D6", showBackground = true)
@Composable
private fun DiceResultDisplayEmptyPreview() {
    DiceRollerTheme(dynamicColor = false) {
        DiceResultDisplay(selectedDice = Dice.D6, result = null)
    }
}

@Preview(name = "D6 result = 5", showBackground = true)
@Composable
private fun DiceResultDisplayD6Preview() {
    DiceRollerTheme(dynamicColor = false) {
        DiceResultDisplay(selectedDice = Dice.D6, result = 5)
    }
}

@Preview(name = "D4 result = 3", showBackground = true)
@Composable
private fun DiceResultDisplayD4Preview() {
    DiceRollerTheme(dynamicColor = false) {
        DiceResultDisplay(selectedDice = Dice.D4, result = 3)
    }
}

@Preview(name = "D20 result = 17", showBackground = true)
@Composable
private fun DiceResultDisplayD20Preview() {
    DiceRollerTheme(dynamicColor = false) {
        DiceResultDisplay(selectedDice = Dice.D20, result = 17)
    }
}
