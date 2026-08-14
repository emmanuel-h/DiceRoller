// app/src/main/java/fr/mandarine/diceroller/presentation/component/AddDiceChip.kt
package fr.mandarine.diceroller.presentation.component

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import fr.mandarine.diceroller.presentation.MAX_CUSTOM_DICE
import fr.mandarine.diceroller.ui.theme.DiceRollerTheme

/** Test tag of the chip that opens the custom-die creator. */
const val ADD_DICE_CHIP_TAG: String = "add-dice-chip"

/** Caption under the `+`, naming what gets added rather than repeating the glyph. */
private const val ADD_CHIP_LABEL = "Custom"

/** Opacity of the `+`, matching the quieter-than-artwork weight of the stepper chips' glyphs. */
private const val ADD_GLYPH_ALPHA = 0.75f

/**
 * The pool selector's last cell: a chip that opens the custom-die creator (issue #4).
 *
 * Deliberately built from [DiceStepperChip]'s own geometry — [CHIP_SHAPE], [CHIP_MIN_WIDTH], the
 * same vertical padding and label spacing, and a `+` occupying exactly the
 * [DiceImageSize.Small] box the artwork would — so it measures to the same height as its
 * neighbours by construction rather than by a hand-tuned constant. That is what keeps the grid a
 * grid when this cell shares a row with real dice.
 *
 * Always drawn in the "excluded" outline style: it holds no dice, so the filled treatment that
 * means "this die is in the pool" would be a lie. Callers hide it entirely once
 * [MAX_CUSTOM_DICE] dice exist rather than disabling it — see that constant for why hiding is what
 * keeps the grid at three rows.
 *
 * @param onClick invoked when the chip is tapped
 * @param modifier optional [Modifier] applied to the chip container
 */
@Composable
fun AddDiceChip(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .defaultMinSize(minWidth = CHIP_MIN_WIDTH)
            .clip(CHIP_SHAPE)
            .border(
                width = CHIP_BORDER_WIDTH,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = CHIP_SHAPE,
            )
            .clickable(role = Role.Button, onClick = onClick)
            .testTag(ADD_DICE_CHIP_TAG)
            .semantics(mergeDescendants = true) {
                contentDescription = "Add a custom die"
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.padding(vertical = CHIP_VERTICAL_PADDING),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(CHIP_LABEL_SPACING),
        ) {
            // Sized to the artwork box rather than to the glyph, so this cell is exactly as tall
            // as a die chip without either one knowing the other's total height.
            Box(
                modifier = Modifier.size(DiceImageSize.Small.sizeDp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "+",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                        alpha = ADD_GLYPH_ALPHA,
                    ),
                )
            }
            Text(
                text = ADD_CHIP_LABEL,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// -- Previews -----------------------------------------------------------------

@Preview(name = "Add chip", showBackground = true)
@Composable
private fun AddDiceChipPreview() {
    DiceRollerTheme(dynamicColor = false) {
        AddDiceChip(onClick = {}, modifier = Modifier.padding(4.dp))
    }
}
