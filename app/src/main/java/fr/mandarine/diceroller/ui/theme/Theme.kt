package fr.mandarine.diceroller.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Parchment and ink. Every role is set, not only the three the template overrode, so that the
 * pieces Material draws on its own — the settings sheet, the custom-die dialog, the snackbar,
 * text fields — come out on parchment too instead of falling back to the baseline purple.
 */
private val ParchmentColorScheme = lightColorScheme(
    primary = ParchmentInkStamp,
    onPrimary = ParchmentGoldLeaf,
    primaryContainer = ParchmentWash,
    onPrimaryContainer = ParchmentInk,
    inversePrimary = LeatherGold,
    secondary = ParchmentGold,
    onSecondary = ParchmentPageLowest,
    secondaryContainer = ParchmentGoldWash,
    onSecondaryContainer = ParchmentInk,
    tertiary = ParchmentOxblood,
    onTertiary = ParchmentPageLowest,
    tertiaryContainer = ParchmentOxbloodWash,
    onTertiaryContainer = ParchmentInk,
    background = ParchmentPage,
    onBackground = ParchmentInk,
    surface = ParchmentPage,
    onSurface = ParchmentInk,
    surfaceVariant = ParchmentPageHigh,
    onSurfaceVariant = ParchmentInkSoft,
    surfaceTint = ParchmentInkStamp,
    inverseSurface = ParchmentInk,
    inverseOnSurface = ParchmentPage,
    error = ParchmentError,
    onError = ParchmentPageLowest,
    errorContainer = ParchmentErrorWash,
    onErrorContainer = ParchmentInk,
    outline = ParchmentRule,
    outlineVariant = ParchmentRuleFaint,
    surfaceBright = ParchmentPageLowest,
    surfaceDim = ParchmentPageHighest,
    surfaceContainerLowest = ParchmentPageLowest,
    surfaceContainerLow = ParchmentPageLow,
    surfaceContainer = ParchmentPageMid,
    surfaceContainerHigh = ParchmentPageHigh,
    surfaceContainerHighest = ParchmentPageHighest,
)

/** Aged leather and gold: the same roles as [ParchmentColorScheme], read by lamplight. */
private val LeatherColorScheme = darkColorScheme(
    primary = LeatherGold,
    onPrimary = LeatherShadow,
    primaryContainer = LeatherTooled,
    onPrimaryContainer = LeatherTooledPale,
    inversePrimary = ParchmentInkStamp,
    secondary = LeatherGoldPale,
    onSecondary = LeatherShadow,
    secondaryContainer = LeatherTooledSoft,
    onSecondaryContainer = LeatherCream,
    tertiary = LeatherRust,
    onTertiary = LeatherShadow,
    tertiaryContainer = LeatherRustDeep,
    onTertiaryContainer = LeatherCream,
    background = LeatherHide,
    onBackground = LeatherCream,
    surface = LeatherHide,
    onSurface = LeatherCream,
    surfaceVariant = LeatherHideHigh,
    onSurfaceVariant = LeatherCreamSoft,
    surfaceTint = LeatherGold,
    inverseSurface = LeatherCream,
    inverseOnSurface = LeatherHideLow,
    error = LeatherError,
    onError = LeatherErrorDeep,
    errorContainer = LeatherErrorWash,
    onErrorContainer = LeatherCream,
    outline = LeatherStitch,
    outlineVariant = LeatherStitchFaint,
    surfaceBright = LeatherHideHighest,
    surfaceDim = LeatherHideLowest,
    surfaceContainerLowest = LeatherHideLowest,
    surfaceContainerLow = LeatherHideLow,
    surfaceContainer = LeatherHideMid,
    surfaceContainerHigh = LeatherHideHigh,
    surfaceContainerHighest = LeatherHideHighest,
)

/**
 * Tighter corners than Material's defaults, which are what make a screen look like every other
 * Compose app: a card cut from a page has a nicked corner, not a pill's.
 */
private val DiceRollerShapes = Shapes(
    extraSmall = RoundedCornerShape(2.dp),
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(10.dp),
    extraLarge = RoundedCornerShape(14.dp),
)

/** The page's colour at its top and at its foot, which [ParchmentBackground] reads. */
@Immutable
private data class Lamplight(val top: Color, val bottom: Color)

private val LocalLamplight = staticCompositionLocalOf { Lamplight(ParchmentLamplight, ParchmentShade) }

/**
 * The app's theme: [ParchmentColorScheme] by day, [LeatherColorScheme] by night (issue #72).
 *
 * There is deliberately no dynamic-colour switch. Material You took the wallpaper's colours, which
 * is exactly what made the app look like a stock one; the palette is now the app's own identity
 * and does not change with the phone.
 */
@Composable
fun DiceRollerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val lamplight = if (darkTheme) {
        Lamplight(top = LeatherLamplight, bottom = LeatherShade)
    } else {
        Lamplight(top = ParchmentLamplight, bottom = ParchmentShade)
    }
    CompositionLocalProvider(LocalLamplight provides lamplight) {
        MaterialTheme(
            colorScheme = if (darkTheme) LeatherColorScheme else ParchmentColorScheme,
            typography = Typography,
            shapes = DiceRollerShapes,
            content = content,
        )
    }
}

/**
 * The page behind the screen: the theme's background, lit from above. It is lightest at the top,
 * passes through the plain background colour, and deepens slightly towards the Roll bar, as a
 * page does under a lamp hung over the table.
 *
 * This replaced a radial vignette. Compose sizes a radial gradient's default radius to half the
 * shorter side, so on a portrait screen it drew a visible disc in the middle of the page. A
 * vertical wash has no shape to show at any aspect ratio, and it is still one draw call with no
 * asset.
 */
@Composable
fun ParchmentBackground(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val page = MaterialTheme.colorScheme.background
    val lamplight = LocalLamplight.current
    Box(
        modifier = modifier.background(
            Brush.verticalGradient(
                0f to lamplight.top,
                LAMPLIGHT_PLAIN_STOP to page,
                1f to lamplight.bottom,
            ),
        ),
    ) {
        content()
    }
}

/** How far down the page, as a fraction of its height, it reaches its plain background colour. */
private const val LAMPLIGHT_PLAIN_STOP = 0.4f
