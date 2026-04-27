// app/src/main/java/fr/mandarine/diceroller/presentation/component/DiceImage.kt
package fr.mandarine.diceroller.presentation.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import fr.mandarine.diceroller.R
import fr.mandarine.diceroller.domain.Dice
import fr.mandarine.diceroller.presentation.model.DiceAsset
import fr.mandarine.diceroller.ui.theme.DiceRollerTheme

/** Padding applied inside the bounding box so the asset stroke is never clipped. */
private val ASSET_INNER_PADDING: Dp = 4.dp

/**
 * Size variants for [DiceImage], matching the two contexts where dice art appears.
 *
 * @property sizeDp the square bounding box side length
 */
enum class DiceImageSize(val sizeDp: Dp) {
    /** Small variant used in the die selector chip row (48×48 dp). */
    Small(sizeDp = 48.dp),

    /** Large variant used in the roll result display (160×160 dp). */
    Large(sizeDp = 160.dp),
}

/**
 * Renders a static RPG-style die asset for the given [dice] type.
 *
 * The drawable asset is loaded via [DiceAsset.fromDice] and tinted at runtime
 * using the provided [tintColor], so it adapts automatically to the Material 3
 * color scheme in both light and dark mode.
 *
 * A [content] slot is centered inside the bounding box so callers can overlay
 * the result number or [D6Pips] on top of the die art.
 *
 * @param dice the die type whose asset to render
 * @param sizeVariant controls the physical dimensions of the component
 * @param modifier optional [Modifier] applied to the root container
 * @param tintColor the color used to tint the single-color drawable asset;
 *   defaults to [MaterialTheme.colorScheme.primary]
 * @param content composable lambda rendered centered inside the die asset
 */
@Composable
fun DiceImage(
    dice: Dice,
    sizeVariant: DiceImageSize,
    modifier: Modifier = Modifier,
    tintColor: Color = MaterialTheme.colorScheme.primary,
    content: @Composable () -> Unit = {},
) {
    Box(
        modifier = modifier.size(sizeVariant.sizeDp),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(id = DiceAsset.fromDice(dice)),
            contentDescription = contentDescriptionFor(dice),
            modifier = Modifier
                .fillMaxSize()
                .padding(ASSET_INNER_PADDING),
            contentScale = ContentScale.Fit,
            colorFilter = ColorFilter.tint(tintColor),
        )
        content()
    }
}

/**
 * Returns a localised content description for the given [dice] type.
 *
 * Kept as a private helper so the description string is derived from the
 * same die name used throughout the domain layer.
 */
@Composable
private fun contentDescriptionFor(dice: Dice): String =
    stringResource(R.string.dice_image_content_description, dice.name)

// -- Previews -----------------------------------------------------------------

@Preview(name = "D4 Small", showBackground = true)
@Composable
private fun DiceImageD4SmallPreview() {
    DiceRollerTheme(dynamicColor = false) {
        DiceImage(dice = Dice.D4, sizeVariant = DiceImageSize.Small)
    }
}

@Preview(name = "D6 Small — selected", showBackground = true)
@Composable
private fun DiceImageD6SmallSelectedPreview() {
    DiceRollerTheme(dynamicColor = false) {
        DiceImage(
            dice = Dice.D6,
            sizeVariant = DiceImageSize.Small,
            tintColor = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

@Preview(name = "D8 Large", showBackground = true)
@Composable
private fun DiceImageD8LargePreview() {
    DiceRollerTheme(dynamicColor = false) {
        DiceImage(dice = Dice.D8, sizeVariant = DiceImageSize.Large)
    }
}

@Preview(name = "D12 Large", showBackground = true)
@Composable
private fun DiceImageD12LargePreview() {
    DiceRollerTheme(dynamicColor = false) {
        DiceImage(dice = Dice.D12, sizeVariant = DiceImageSize.Large)
    }
}

@Preview(name = "D20 Large", showBackground = true)
@Composable
private fun DiceImageD20LargePreview() {
    DiceRollerTheme(dynamicColor = false) {
        DiceImage(dice = Dice.D20, sizeVariant = DiceImageSize.Large)
    }
}
