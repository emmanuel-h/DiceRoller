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

/** The colour the page darkens towards at its edges, which [ParchmentBackground] reads. */
@Immutable
private data class Vignette(val edge: Color)

private val LocalVignette = staticCompositionLocalOf { Vignette(ParchmentVignette) }

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
    val vignette = Vignette(edge = if (darkTheme) LeatherVignette else ParchmentVignette)
    CompositionLocalProvider(LocalVignette provides vignette) {
        MaterialTheme(
            colorScheme = if (darkTheme) LeatherColorScheme else ParchmentColorScheme,
            typography = Typography,
            shapes = DiceRollerShapes,
            content = content,
        )
    }
}

/**
 * The page behind the screen: the theme's background, darkening towards its edges as an old
 * sheet does. A flat fill was the other half of the stock look; this costs one draw call and no
 * asset.
 */
@Composable
fun ParchmentBackground(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val page = MaterialTheme.colorScheme.background
    val edge = LocalVignette.current.edge
    Box(
        modifier = modifier
            .background(page)
            .background(
                Brush.radialGradient(
                    VIGNETTE_CLEAR_STOP to Color.Transparent,
                    1f to edge.copy(alpha = VIGNETTE_EDGE_ALPHA),
                ),
            ),
    ) {
        content()
    }
}

/** How far out from the centre the page stays its plain colour before it starts to darken. */
private const val VIGNETTE_CLEAR_STOP = 0.55f

/** How dark the very edge gets: enough to read as aged, never enough to compete with the dice. */
private const val VIGNETTE_EDGE_ALPHA = 0.55f
