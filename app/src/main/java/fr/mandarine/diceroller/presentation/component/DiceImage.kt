// app/src/main/java/fr/mandarine/diceroller/presentation/component/DiceImage.kt
package fr.mandarine.diceroller.presentation.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import fr.mandarine.diceroller.domain.Dice
import fr.mandarine.diceroller.presentation.model.DiceColor
import fr.mandarine.diceroller.ui.theme.DiceRollerTheme

/**
 * Renders the Fantasy Dices Pack artwork for the given [dice] in the given [color].
 *
 * The artwork is a fixed render with a numeral already painted on its faces, so
 * it is decorative only — the rolled value is never drawn on the die. Callers
 * display the result separately (see [DiceResultDisplay]).
 *
 * @param dice the die type whose artwork to render
 * @param color the color variant to render
 * @param sizeVariant controls the box the artwork is fitted into
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
    dice: Dice,
    color: DiceColor,
    sizeVariant: DiceImageSize,
    modifier: Modifier = Modifier,
    alpha: Float = 1f,
    contentDescription: String? = "${dice.name}, ${color.label}",
) {
    Image(
        painter = painterResource(id = color.drawableFor(dice)),
        contentDescription = contentDescription,
        modifier = modifier.size(sizeVariant.sizeDp),
        contentScale = ContentScale.Fit,
        alpha = alpha,
    )
}

// -- Previews -----------------------------------------------------------------

@Preview(name = "D20 amethyst - Large", showBackground = true)
@Composable
private fun DiceImageD20LargePreview() {
    DiceRollerTheme(dynamicColor = false) {
        DiceImage(
            dice = Dice.D20,
            color = DiceColor.Amethyst,
            sizeVariant = DiceImageSize.Large,
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
