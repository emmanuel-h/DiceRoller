// app/src/main/java/fr/mandarine/diceroller/presentation/component/DiceColorSwatchRow.kt
package fr.mandarine.diceroller.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import fr.mandarine.diceroller.R
import fr.mandarine.diceroller.presentation.model.DiceColor
import fr.mandarine.diceroller.ui.theme.DiceRollerTheme

/** Diameter of the filled color dot. */
private val SWATCH_SIZE = 32.dp

/** Diameter of the tappable area around the dot, which also hosts the selection ring. */
private val SWATCH_TOUCH_SIZE = 44.dp

/** Thickness of the ring drawn around the selected swatch. */
private val SELECTION_RING_WIDTH = 2.dp

/**
 * Horizontally scrollable row of the twelve [DiceColor] swatches.
 *
 * Each swatch is a filled circle in the variant's dominant color; the selected
 * one is ringed in `primary`. Selecting a color recolors both the selector and
 * the result die without clearing the current roll.
 *
 * @param selectedColor the currently selected color variant
 * @param onSelectColor callback invoked with the tapped color
 * @param modifier optional [Modifier] applied to the row
 */
@Composable
fun DiceColorSwatchRow(
    selectedColor: DiceColor,
    onSelectColor: (DiceColor) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DiceColor.entries.forEach { color ->
            DiceColorSwatch(
                color = color,
                isSelected = color == selectedColor,
                onClick = { onSelectColor(color) },
            )
        }
    }
}

/**
 * A single color dot, ringed when [isSelected].
 *
 * The ring sits on the outer touch target rather than on the dot itself so the
 * dot's own color is never overdrawn.
 */
@Composable
private fun DiceColorSwatch(
    color: DiceColor,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(R.string.swatch_description, stringResource(color.labelRes))
    Box(
        modifier = modifier
            .size(SWATCH_TOUCH_SIZE)
            .clip(CircleShape)
            .selectable(
                selected = isSelected,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .then(
                if (isSelected) {
                    Modifier.border(
                        width = SELECTION_RING_WIDTH,
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape,
                    )
                } else {
                    Modifier
                },
            )
            .padding(SELECTION_RING_WIDTH)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(SWATCH_SIZE)
                .clip(CircleShape)
                .background(color.swatch)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = CircleShape,
                ),
        )
    }
}

// -- Previews -----------------------------------------------------------------

@Preview(name = "Swatch row", showBackground = true)
@Composable
private fun DiceColorSwatchRowPreview() {
    DiceRollerTheme(dynamicColor = false) {
        DiceColorSwatchRow(
            selectedColor = DiceColor.Sapphire,
            onSelectColor = {},
        )
    }
}
