// app/src/main/java/fr/mandarine/diceroller/presentation/component/RolledDieImage.kt
package fr.mandarine.diceroller.presentation.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import fr.mandarine.diceroller.R
import fr.mandarine.diceroller.domain.CustomDie
import fr.mandarine.diceroller.domain.Dice
import fr.mandarine.diceroller.domain.DieType
import fr.mandarine.diceroller.presentation.model.DiceColor
import fr.mandarine.diceroller.ui.theme.DiceRollerTheme

/** Colour of the numeral's fill; the outline beneath it is what makes it legible on any die. */
private val NUMERAL_FILL = Color.White

/** Colour of the numeral's outline: dark enough to separate white from even the palest variant. */
private val NUMERAL_OUTLINE = Color.Black.copy(alpha = 0.85f)

/**
 * [DiceImage] with the rolled [value] drawn straight over its centre (issue #69).
 *
 * The numeral is bold white over a dark outline rather than sitting in a disc: a disc large enough
 * to read hid most of the die, and the die is the point. The outline is what keeps the numeral
 * legible against any of the twelve colours and over the numerals already painted on the art,
 * which still show faintly around it. A [CustomDie]'s badge is drawn blank instead, since its face
 * count is solid text that the value would sit squarely on top of. Putting the value on the die is
 * what lets the `×N` beside it read as a count: when the value sat between the die and the `×2`,
 * `7 ×2` read as arithmetic.
 *
 * Shared by the live result and the roll history so the two never disagree on how a face looks.
 *
 * @param dice the die to render
 * @param color the color variant to render
 * @param sizeVariant the box the artwork is fitted into; also sizes the numeral and its outline
 * @param value the rolled face to show on the die
 * @param modifier optional [Modifier] applied to the outer box
 * @param imageModifier optional [Modifier] applied to the artwork itself, e.g. its test tag
 */
@Composable
fun RolledDieImage(
    dice: DieType,
    color: DiceColor,
    sizeVariant: DiceImageSize,
    value: Int,
    modifier: Modifier = Modifier,
    imageModifier: Modifier = Modifier,
) {
    val text = stringResource(R.string.number, value)
    val style = sizeVariant.valueTextStyle()
    val outlineWidth = with(LocalDensity.current) { sizeVariant.valueOutlineWidth().toPx() }
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        // Decorative either way: every host merges the entry and gives it one spoken description.
        when (dice) {
            is Dice -> DiceImage(
                dice = dice,
                color = color,
                sizeVariant = sizeVariant,
                contentDescription = null,
                modifier = imageModifier,
            )
            // A custom die's badge carries its face count as real text, not faint painted art, so
            // the value drawn over it was a second numeral on top of the first ("7" over "1000").
            // The pill goes blank instead; the group header and the history notation still name
            // the die.
            is CustomDie -> CustomDieBadge(
                die = dice,
                color = color,
                sizeVariant = sizeVariant,
                modifier = imageModifier,
                showFaceCount = false,
            )
        }
        // The outline is a second, stroked copy of the numeral under the filled one. It is cleared
        // from semantics so the value exists once in the tree, as the filled text.
        Text(
            text = text,
            style = style.copy(drawStyle = Stroke(width = outlineWidth, join = StrokeJoin.Round)),
            color = NUMERAL_OUTLINE,
            maxLines = 1,
            modifier = Modifier.clearAndSetSemantics {},
        )
        Text(
            text = text,
            style = style,
            color = NUMERAL_FILL,
            maxLines = 1,
        )
    }
}

@Composable
private fun DiceImageSize.valueTextStyle(): TextStyle = when (this) {
    DiceImageSize.Compact -> MaterialTheme.typography.labelMedium
    DiceImageSize.Inline -> MaterialTheme.typography.titleMedium
    DiceImageSize.Small -> MaterialTheme.typography.headlineSmall
}.copy(fontWeight = FontWeight.Bold)

private fun DiceImageSize.valueOutlineWidth(): Dp = when (this) {
    DiceImageSize.Compact -> 2.5.dp
    DiceImageSize.Inline -> 3.5.dp
    DiceImageSize.Small -> 4.dp
}

// -- Previews -----------------------------------------------------------------

@Preview(name = "D10 jade - Inline, rolled 9", showBackground = true)
@Composable
private fun RolledDieImageInlinePreview() {
    DiceRollerTheme {
        RolledDieImage(
            dice = Dice.D10,
            color = DiceColor.Jade,
            sizeVariant = DiceImageSize.Inline,
            value = 9,
            modifier = Modifier.padding(8.dp),
        )
    }
}

@Preview(name = "D20 amethyst - Compact, rolled 20", showBackground = true)
@Composable
private fun RolledDieImageCompactPreview() {
    DiceRollerTheme {
        RolledDieImage(
            dice = Dice.D20,
            color = DiceColor.Amethyst,
            sizeVariant = DiceImageSize.Compact,
            value = 20,
            modifier = Modifier.padding(8.dp),
        )
    }
}

@Preview(name = "D100 ruby - Inline, rolled 100", showBackground = true)
@Composable
private fun RolledDieImageCustomPreview() {
    DiceRollerTheme {
        RolledDieImage(
            dice = CustomDie(faces = 100),
            color = DiceColor.Ruby,
            sizeVariant = DiceImageSize.Inline,
            value = 100,
            modifier = Modifier.padding(8.dp),
        )
    }
}
