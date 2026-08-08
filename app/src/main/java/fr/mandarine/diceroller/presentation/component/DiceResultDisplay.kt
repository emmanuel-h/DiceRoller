// app/src/main/java/fr/mandarine/diceroller/presentation/component/DiceResultDisplay.kt
package fr.mandarine.diceroller.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import fr.mandarine.diceroller.domain.Dice
import fr.mandarine.diceroller.presentation.model.DiceColor
import fr.mandarine.diceroller.ui.theme.DiceRollerTheme

/** Opacity applied to the die artwork before the first roll. */
private const val EMPTY_STATE_ALPHA = 0.4f

/** Placeholder shown in place of the number before the first roll. */
private const val EMPTY_STATE_PLACEHOLDER = "–"

/**
 * Displays the selected die's artwork with the roll result underneath.
 *
 * The pack artwork carries its own painted numerals, so it cannot show the
 * rolled value — the die is decorative and the result is a large number below
 * it, rendered on the surface so it stays legible against every color variant
 * in both light and dark themes.
 *
 * - **Empty state** (result is null): artwork dimmed to [EMPTY_STATE_ALPHA],
 *   with an en-dash in place of the number.
 * - **Rolled**: artwork at full opacity, result in `displayLarge`.
 *
 * The result text carries `liveRegion = LiveRegionMode.Polite` semantics so
 * accessibility services announce each new roll.
 *
 * @param selectedDice the currently selected die type
 * @param selectedColor the currently selected color variant
 * @param result the roll result, or null when no roll has been performed yet
 * @param modifier optional [Modifier] applied to the root container
 */
@Composable
fun DiceResultDisplay(
    selectedDice: Dice,
    selectedColor: DiceColor,
    result: Int?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        DiceImage(
            dice = selectedDice,
            color = selectedColor,
            sizeVariant = DiceImageSize.Large,
            alpha = if (result == null) EMPTY_STATE_ALPHA else 1f,
        )
        Text(
            text = result?.toString() ?: EMPTY_STATE_PLACEHOLDER,
            style = MaterialTheme.typography.displayLarge,
            color = if (result == null) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
}

// -- Previews -----------------------------------------------------------------

@Preview(name = "Empty state - D6 amethyst", showBackground = true)
@Composable
private fun DiceResultDisplayEmptyPreview() {
    DiceRollerTheme(dynamicColor = false) {
        DiceResultDisplay(
            selectedDice = Dice.D6,
            selectedColor = DiceColor.Amethyst,
            result = null,
        )
    }
}

@Preview(name = "D20 ruby result = 17", showBackground = true)
@Composable
private fun DiceResultDisplayD20Preview() {
    DiceRollerTheme(dynamicColor = false) {
        DiceResultDisplay(
            selectedDice = Dice.D20,
            selectedColor = DiceColor.Ruby,
            result = 17,
        )
    }
}

@Preview(name = "D10 jade result = 7", showBackground = true)
@Composable
private fun DiceResultDisplayD10Preview() {
    DiceRollerTheme(dynamicColor = false) {
        DiceResultDisplay(
            selectedDice = Dice.D10,
            selectedColor = DiceColor.Jade,
            result = 7,
        )
    }
}
