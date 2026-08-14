// app/src/main/java/fr/mandarine/diceroller/presentation/component/DiceImage.kt
package fr.mandarine.diceroller.presentation.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import fr.mandarine.diceroller.domain.CustomDie
import fr.mandarine.diceroller.domain.Dice
import fr.mandarine.diceroller.domain.DieType
import fr.mandarine.diceroller.presentation.model.DiceColor
import fr.mandarine.diceroller.ui.theme.DiceRollerTheme

/**
 * Renders [dice] in the given [color] at [sizeVariant] — whichever kind of die it is.
 *
 * This is the one place that decides how a die is drawn, so no caller has to care that only the
 * six presets have artwork:
 * - a [Dice] preset renders its Fantasy Dices Pack drawable, via
 *   [DiceColor.drawableFor] — still the single `(color, die)` → resource mapping
 * - a [CustomDie] has no drawable to render, so it falls through to [CustomDieBadge], a
 *   swatch-coloured pill carrying its face count (issue #4)
 *
 * The artwork is a fixed render with a numeral already painted on its faces, so it is decorative
 * only — the rolled value is never drawn on the die. Callers display the result separately (see
 * [DiceResultDisplay]).
 *
 * @param dice the die whose art or badge to render
 * @param color the color variant to render
 * @param sizeVariant controls the box the artwork is fitted into, and the badge's height
 * @param modifier optional [Modifier] applied to the image
 * @param alpha opacity applied to the artwork, used to dim the empty state
 * @param contentDescription accessibility label; defaults to `"D20, amethyst"` form. Callers
 *        that wrap the image in their own labelled control should pass a distinct label so the
 *        two do not collide in the semantics tree, or `null` to mark the artwork decorative when
 *        an adjacent element already conveys everything the image would — e.g. a "D6" text label
 *        or a stepper button's own description (stepper chips), or an ancestor node such as a
 *        merged row that already carries the accessible description (result face-ladder rows).
 */
@Composable
fun DiceImage(
    dice: DieType,
    color: DiceColor,
    sizeVariant: DiceImageSize,
    modifier: Modifier = Modifier,
    alpha: Float = 1f,
    contentDescription: String? = "${dice.label}, ${color.label}",
) {
    when (dice) {
        is Dice -> Image(
            painter = painterResource(id = color.drawableFor(dice)),
            contentDescription = contentDescription,
            modifier = modifier.size(sizeVariant.sizeDp),
            contentScale = ContentScale.Fit,
            alpha = alpha,
        )

        is CustomDie -> CustomDieBadge(
            die = dice,
            color = color,
            sizeVariant = sizeVariant,
            modifier = modifier,
            alpha = alpha,
            contentDescription = contentDescription,
        )
    }
}

// -- Previews -----------------------------------------------------------------

@Preview(name = "D20 amethyst - Inline", showBackground = true)
@Composable
private fun DiceImageD20InlinePreview() {
    DiceRollerTheme(dynamicColor = false) {
        DiceImage(
            dice = Dice.D20,
            color = DiceColor.Amethyst,
            sizeVariant = DiceImageSize.Inline,
        )
    }
}

@Preview(name = "D10 jade - Small", showBackground = true)
@Composable
private fun DiceImageD10SmallPreview() {
    DiceRollerTheme(dynamicColor = false) {
        DiceImage(
            dice = Dice.D10,
            color = DiceColor.Jade,
            sizeVariant = DiceImageSize.Small,
        )
    }
}

/** The custom-die fall-through: same call site, badge instead of artwork. */
@Preview(name = "D7 ruby - Small", showBackground = true)
@Composable
private fun DiceImageCustomSmallPreview() {
    DiceRollerTheme(dynamicColor = false) {
        DiceImage(
            dice = CustomDie(7),
            color = DiceColor.Ruby,
            sizeVariant = DiceImageSize.Small,
        )
    }
}
