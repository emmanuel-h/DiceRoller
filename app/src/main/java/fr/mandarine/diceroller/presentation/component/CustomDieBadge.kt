// app/src/main/java/fr/mandarine/diceroller/presentation/component/CustomDieBadge.kt
package fr.mandarine.diceroller.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import fr.mandarine.diceroller.domain.CustomDie
import fr.mandarine.diceroller.presentation.model.DiceColor
import fr.mandarine.diceroller.ui.theme.DiceRollerTheme

/** Corner rounding as a fraction of the badge's height, keeping it a pill at every size. */
private val BADGE_SHAPE = RoundedCornerShape(percent = 30)

/** Inset of the face-count numeral from the badge's left and right edges. */
private val BADGE_HORIZONTAL_PADDING = 6.dp

/**
 * Luminance above which the numeral is drawn dark rather than light.
 *
 * Computed from the swatch instead of picked per colour: the twelve variants span a wide range,
 * and a hard-coded white would be unreadable on the lighter ones.
 */
private const val LIGHT_SWATCH_LUMINANCE = 0.5f

/**
 * The stand-in for a [CustomDie]'s missing artwork: a swatch-coloured pill carrying the die's face
 * count.
 *
 * The Fantasy Dices Pack ships renders for six shapes only, so a user-defined D7 or D100 has
 * nothing to draw (issue #4). Rather than borrow the nearest preset's art — which would imply a
 * shape the die does not have, and fight the numerals already painted on that render — a custom
 * die gets a deliberately different mark. It still answers to the colour picker, since the pill is
 * filled with [DiceColor.swatch], so choosing a colour recolours the whole pool as before.
 *
 * The pill is square at its smallest and **grows wider** for a longer numeral rather than shrinking
 * the text, so `"1000"` stays legible at the 24dp [DiceImageSize.Compact] size used in the roll
 * log. Height always matches [DiceImageSize.sizeDp] exactly, so a badge and a rendered die line up
 * on the same baseline wherever they sit side by side.
 *
 * @param die the custom die whose face count to show
 * @param color the selected colour variant, whose swatch fills the pill
 * @param sizeVariant the size to match the surrounding [DiceImage]s
 * @param modifier optional [Modifier] applied to the badge
 * @param alpha opacity applied to the badge, mirroring [DiceImage]'s parameter
 * @param contentDescription accessibility label; `null` marks the badge decorative, which is what
 *   every current caller does because an adjacent label or a merged ancestor already names the die
 */
@Composable
fun CustomDieBadge(
    die: CustomDie,
    color: DiceColor,
    sizeVariant: DiceImageSize,
    modifier: Modifier = Modifier,
    alpha: Float = 1f,
    contentDescription: String? = null,
) {
    val numeralColor = if (color.swatch.luminance() > LIGHT_SWATCH_LUMINANCE) {
        Color.Black
    } else {
        Color.White
    }
    Box(
        modifier = modifier
            .alpha(alpha)
            .height(sizeVariant.sizeDp)
            .widthIn(min = sizeVariant.sizeDp)
            .background(color = color.swatch, shape = BADGE_SHAPE)
            .labelledOrDecorative(contentDescription),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = die.faces.toString(),
            style = sizeVariant.badgeTextStyle(),
            color = numeralColor,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = BADGE_HORIZONTAL_PADDING),
        )
    }
}

/**
 * Labels the badge with [contentDescription], or leaves it alone when that is null.
 *
 * "Alone" rather than "cleared": unlike [DiceImage]'s bitmap, this badge's content is a text node,
 * and leaving it in the semantics tree is what lets tests find a custom die by its face count —
 * the same arrangement the roll-history faces rely on, where a merged ancestor supplies the spoken
 * description and the numerals stay findable underneath it.
 */
private fun Modifier.labelledOrDecorative(contentDescription: String?): Modifier =
    if (contentDescription == null) {
        this
    } else {
        semantics(mergeDescendants = true) { this.contentDescription = contentDescription }
    }

/**
 * The type scale that fills each badge size without crowding it.
 *
 * A separate mapping rather than a property on [DiceImageSize], which stays purely geometric —
 * only this badge draws text inside the box, the artwork variants draw a bitmap.
 */
@Composable
private fun DiceImageSize.badgeTextStyle(): TextStyle = when (this) {
    DiceImageSize.Compact -> MaterialTheme.typography.labelSmall
    DiceImageSize.Inline -> MaterialTheme.typography.labelLarge
    DiceImageSize.Small -> MaterialTheme.typography.titleLarge
}

// -- Previews -----------------------------------------------------------------

@Preview(name = "D7 amethyst - Small", showBackground = true)
@Composable
private fun CustomDieBadgeSmallPreview() {
    DiceRollerTheme(dynamicColor = false) {
        CustomDieBadge(
            die = CustomDie(7),
            color = DiceColor.Amethyst,
            sizeVariant = DiceImageSize.Small,
        )
    }
}

/** The widest numeral at the smallest size — the case the pill widens rather than shrinks for. */
@Preview(name = "D1000 moss - Compact", showBackground = true)
@Composable
private fun CustomDieBadgeCompactWidePreview() {
    DiceRollerTheme(dynamicColor = false) {
        CustomDieBadge(
            die = CustomDie(1000),
            color = DiceColor.Moss,
            sizeVariant = DiceImageSize.Compact,
        )
    }
}

@Preview(name = "D3 gold - Inline", showBackground = true)
@Composable
private fun CustomDieBadgeInlinePreview() {
    DiceRollerTheme(dynamicColor = false) {
        CustomDieBadge(
            die = CustomDie(3),
            color = DiceColor.Gold,
            sizeVariant = DiceImageSize.Inline,
        )
    }
}
