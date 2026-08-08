// app/src/main/java/fr/mandarine/diceroller/presentation/component/DiceStepperChip.kt
package fr.mandarine.diceroller.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import fr.mandarine.diceroller.domain.Dice
import fr.mandarine.diceroller.domain.DicePool
import fr.mandarine.diceroller.presentation.model.DiceColor
import fr.mandarine.diceroller.ui.theme.DiceRollerTheme

/** U+2212 MINUS SIGN — a plain-text glyph, avoiding the `material-icons-extended` dependency. */
private const val DECREMENT_GLYPH = "−"

/** Plain-text "+" glyph, matching [DECREMENT_GLYPH]'s avoidance of the extended icon set. */
private const val INCREMENT_GLYPH = "+"

/** Fixed width for the count text so the chip doesn't reflow width as digit count changes. */
private val COUNT_TEXT_WIDTH = 24.dp

private val CHIP_SHAPE = RoundedCornerShape(12.dp)
private val CHIP_BORDER_WIDTH = 1.dp
private val CHIP_PADDING = 8.dp
private val CHIP_ROW_SPACING = 4.dp

/** The container/border/content color triple for one of [DiceStepperChip]'s two visual states. */
private data class ChipColors(
    val container: Color,
    val border: Color,
    val content: Color,
)

/**
 * Resolves the single [ChipColors] triple for [isIncluded], replacing three independent
 * `if (isIncluded) X else Y` branches with one lookup so the "excluded"/"included" state
 * mapping lives in exactly one place — matching the table already documented on
 * [DiceStepperChip].
 */
@Composable
private fun chipColorsFor(isIncluded: Boolean): ChipColors = if (isIncluded) {
    ChipColors(
        container = MaterialTheme.colorScheme.primaryContainer,
        border = MaterialTheme.colorScheme.primary,
        content = MaterialTheme.colorScheme.onPrimaryContainer,
    )
} else {
    ChipColors(
        container = Color.Transparent,
        border = MaterialTheme.colorScheme.outlineVariant,
        content = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/**
 * One die type's `- count +` stepper control within the dice pool selector.
 *
 * All six die types render one of these, always visible (see [Dice.entries]); [count] of 0 is
 * the "excluded" style, `1..`[DicePool.MAX_DICE_PER_TYPE] the "included" style. The chip
 * container itself carries no click handling — only the two stepper [IconButton]s are
 * interactive, so it is not wrapped in `selectable`.
 *
 * @param dice the die type this chip controls
 * @param count the current pool count for [dice]; expected within `0..DicePool.MAX_DICE_PER_TYPE`
 * @param color the currently selected color variant, used to render the die art
 * @param onIncrement invoked when the `+` control is tapped
 * @param onDecrement invoked when the `-` control is tapped
 * @param modifier optional [Modifier] applied to the chip container
 */
@Composable
fun DiceStepperChip(
    dice: Dice,
    count: Int,
    color: DiceColor,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isIncluded = count > 0
    val chipColors = chipColorsFor(isIncluded)

    Box(
        modifier = modifier
            .clip(CHIP_SHAPE)
            .background(chipColors.container)
            .border(width = CHIP_BORDER_WIDTH, color = chipColors.border, shape = CHIP_SHAPE)
            .padding(CHIP_PADDING),
    ) {
        CompositionLocalProvider(LocalContentColor provides chipColors.content) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(CHIP_ROW_SPACING),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(CHIP_ROW_SPACING),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    DiceImage(
                        dice = dice,
                        color = color,
                        sizeVariant = DiceImageSize.Micro,
                        // Decorative: the label below and the two buttons' descriptions
                        // already say "D6", so a third description would be redundant.
                        contentDescription = null,
                    )
                    Text(
                        text = "D${dice.faces}",
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onDecrement,
                        enabled = count > 0,
                        modifier = Modifier.semantics {
                            contentDescription = "Decrease ${dice.name} count"
                        },
                    ) {
                        Text(text = DECREMENT_GLYPH, style = MaterialTheme.typography.titleMedium)
                    }
                    Text(
                        text = "$count",
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .width(COUNT_TEXT_WIDTH)
                            .semantics { stateDescription = "$count" },
                    )
                    IconButton(
                        onClick = onIncrement,
                        enabled = count < DicePool.MAX_DICE_PER_TYPE,
                        modifier = Modifier.semantics {
                            contentDescription = "Increase ${dice.name} count"
                        },
                    ) {
                        Text(text = INCREMENT_GLYPH, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}

// -- Previews -----------------------------------------------------------------

@Preview(name = "Excluded (count 0)", showBackground = true)
@Composable
private fun DiceStepperChipExcludedPreview() {
    DiceRollerTheme(dynamicColor = false) {
        DiceStepperChip(
            dice = Dice.D6,
            count = 0,
            color = DiceColor.Amethyst,
            onIncrement = {},
            onDecrement = {},
        )
    }
}

@Preview(name = "Included (count 4)", showBackground = true)
@Composable
private fun DiceStepperChipIncludedPreview() {
    DiceRollerTheme(dynamicColor = false) {
        DiceStepperChip(
            dice = Dice.D6,
            count = 4,
            color = DiceColor.Ruby,
            onIncrement = {},
            onDecrement = {},
        )
    }
}

@Preview(name = "At cap (count 20)", showBackground = true)
@Composable
private fun DiceStepperChipAtCapPreview() {
    DiceRollerTheme(dynamicColor = false) {
        DiceStepperChip(
            dice = Dice.D20,
            count = DicePool.MAX_DICE_PER_TYPE,
            color = DiceColor.Jade,
            onIncrement = {},
            onDecrement = {},
        )
    }
}
