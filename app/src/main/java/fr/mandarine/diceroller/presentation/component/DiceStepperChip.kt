// app/src/main/java/fr/mandarine/diceroller/presentation/component/DiceStepperChip.kt
package fr.mandarine.diceroller.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
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

private val CHIP_SHAPE = RoundedCornerShape(16.dp)
private val CHIP_BORDER_WIDTH = 1.dp
private val CHIP_VERTICAL_PADDING = 8.dp
private val CHIP_LABEL_SPACING = 6.dp

/** Inset of the `−`/`+` glyphs from their half's outer edge. */
private val GLYPH_EDGE_PADDING = 8.dp

/**
 * Minimum size of each tappable half, per the Material accessibility guideline.
 *
 * The pool selector gives every chip an equal, generous share of the row, so both dimensions are
 * comfortably exceeded there. These floors matter when the chip is measured by its own content
 * instead — an unconstrained mount, or a very narrow container — where the 56dp artwork alone
 * would leave each half only ~28dp wide.
 */
private val HALF_MIN_SIZE = 48.dp

/** Chip width below which the two halves could not both reach [HALF_MIN_SIZE]. */
private val CHIP_MIN_WIDTH = HALF_MIN_SIZE * 2

/** Opacity of an actionable edge glyph — visible as an affordance, quieter than the artwork. */
private const val ACTIVE_GLYPH_ALPHA = 0.6f

/**
 * Opacity of the `−` glyph when the count is already 0. Dimmed rather than hidden so the chip
 * still reads as split in two, and never greyed out entirely: four of six chips sit at 0 at rest,
 * and greying their left edge would make the whole selector look disabled.
 */
private const val INERT_GLYPH_ALPHA = 0.22f

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
 * One die type's count control within the dice pool selector — the die artwork *is* the control.
 *
 * The chip is split down the middle into two tap targets: **tapping the left half decrements,
 * tapping the right half increments** (issue #62). Dropping the separate stepper row gives that
 * vertical space back to the artwork, which doubles from 28dp to [DiceImageSize.Small].
 *
 * The two halves are laid over the artwork rather than beside it, so the art can be centred in
 * the full chip while each half still spans the chip's whole height. At the pool selector's
 * three-per-row grid a half is ~52dp wide on a 360dp screen, clearing the 48dp target.
 *
 * All six die types render one of these, always visible (see [Dice.entries]); [count] of 0 is
 * the "excluded" style, `1..`[DicePool.MAX_DICE_PER_TYPE] the "included" style. At the bounds the
 * corresponding half is disabled — inert to taps and announced as such — rather than removed.
 *
 * @param dice the die type this chip controls
 * @param count the current pool count for [dice]; expected within `0..DicePool.MAX_DICE_PER_TYPE`
 * @param color the currently selected color variant, used to render the die art
 * @param onIncrement invoked when the right half is tapped
 * @param onDecrement invoked when the left half is tapped
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

    CompositionLocalProvider(LocalContentColor provides chipColors.content) {
        Box(
            modifier = modifier
                .defaultMinSize(minWidth = CHIP_MIN_WIDTH)
                .clip(CHIP_SHAPE)
                .background(chipColors.container)
                .border(width = CHIP_BORDER_WIDTH, color = chipColors.border, shape = CHIP_SHAPE),
            contentAlignment = Alignment.Center,
        ) {
            // Visual layer: artwork over its "D6 4" caption, centred in the whole chip so the
            // art is not squeezed into either half.
            Column(
                modifier = Modifier.padding(vertical = CHIP_VERTICAL_PADDING),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(CHIP_LABEL_SPACING),
            ) {
                DiceImage(
                    dice = dice,
                    color = color,
                    sizeVariant = DiceImageSize.Small,
                    // Decorative: the caption below and the two halves' descriptions already
                    // say "D6", so a third description would be redundant.
                    contentDescription = null,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(CHIP_LABEL_SPACING),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "D${dice.faces}",
                        style = MaterialTheme.typography.labelMedium,
                    )
                    Text(
                        text = "$count",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier
                            .testTag(chipCountTestTag(dice))
                            .semantics { stateDescription = "$count" },
                    )
                }
            }

            // Interactive layer: two transparent halves laid over the visual one, so a tap
            // anywhere on the chip — artwork included — resolves to a decrement or an increment.
            Row(modifier = Modifier.matchParentSize()) {
                StepperHalf(
                    glyph = DECREMENT_GLYPH,
                    description = "Decrease ${dice.name} count",
                    enabled = count > 0,
                    onClick = onDecrement,
                    glyphAlignment = Alignment.CenterStart,
                    modifier = Modifier.weight(1f),
                )
                StepperHalf(
                    glyph = INCREMENT_GLYPH,
                    description = "Increase ${dice.name} count",
                    enabled = count < DicePool.MAX_DICE_PER_TYPE,
                    onClick = onIncrement,
                    glyphAlignment = Alignment.CenterEnd,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/**
 * Test tag of the count text inside [dice]'s chip. Exposed so screen-level tests can read one
 * specific chip's count without depending on how the chip nests its nodes.
 */
fun chipCountTestTag(dice: Dice): String = "chip-count-${dice.name}"

/**
 * One half of a [DiceStepperChip]: a full-height transparent tap target carrying its `−`/`+`
 * glyph pinned to the chip's outer edge.
 *
 * The glyph is dimmed rather than recoloured when [enabled] is false, keeping the split
 * discoverable at a glance while still reporting the disabled state to accessibility services.
 */
@Composable
private fun StepperHalf(
    glyph: String,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit,
    glyphAlignment: Alignment,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .heightIn(min = HALF_MIN_SIZE)
            .clickable(
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            // Merged so the half is a single accessibility focus stop carrying its own label,
            // rather than a target plus a stray "−" text node beside it.
            .semantics(mergeDescendants = true) { contentDescription = description },
        contentAlignment = glyphAlignment,
    ) {
        Text(
            text = glyph,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .padding(horizontal = GLYPH_EDGE_PADDING)
                .alpha(if (enabled) ACTIVE_GLYPH_ALPHA else INERT_GLYPH_ALPHA),
        )
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
