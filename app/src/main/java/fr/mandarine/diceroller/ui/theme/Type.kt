package fr.mandarine.diceroller.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import fr.mandarine.diceroller.R

/**
 * Cinzel, the display face (issue #72): Roman inscriptional capitals, which is what gives Roll,
 * the total and the headings their engraved look. It is used with restraint — only on short
 * labels, never on running text or numerals that have to be read at a glance on a die.
 *
 * Bundled as two static weights rather than the variable font, because variation axes need API
 * 26 and the app supports 24. SIL Open Font License; see `docs/licenses/third-party-assets.md`.
 */
val DisplayFontFamily = FontFamily(
    Font(R.font.cinzel_semibold, FontWeight.SemiBold),
    Font(R.font.cinzel_bold, FontWeight.Bold),
)

/** A [TextStyle] set in [DisplayFontFamily], keeping the size and colour of the [base] it dresses. */
fun displayStyle(base: TextStyle, weight: FontWeight = FontWeight.Bold): TextStyle =
    base.copy(fontFamily = DisplayFontFamily, fontWeight = weight, letterSpacing = DISPLAY_TRACKING)

/** Inscriptional capitals want air between them; Material's label tracking is set for Roboto. */
private val DISPLAY_TRACKING = 0.06.em

// Material's baseline type scale, except bodyLarge as the template set it.
val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
)
