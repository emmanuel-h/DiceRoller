// app/src/main/java/fr/mandarine/diceroller/presentation/component/AboutSheet.kt
package fr.mandarine.diceroller.presentation.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import fr.mandarine.diceroller.BuildConfig
import fr.mandarine.diceroller.R
import fr.mandarine.diceroller.ui.theme.DiceRollerTheme

/**
 * Credit required by the CC BY 4.0 license covering the dice artwork.
 *
 * Lived in the main screen's bottom bar until issue #66 moved it in here; the string itself is
 * unchanged, because it is the exact wording `docs/licenses/third-party-assets.md` records as
 * required. Public so the instrumented tests can assert on it rather than retyping it.
 */
const val ART_ATTRIBUTION: String = "Dice art by Aeynit · CC BY 4.0"

/** Test tag of the About sheet's content root. */
const val ABOUT_SHEET_TAG: String = "about-sheet"

/** Test tag of the icon button that opens the sheet. */
const val ABOUT_BUTTON_TAG: String = "about-button"

/** Content description of that icon button, and the sheet's own title. */
private const val ABOUT_LABEL = "About"

/** The app's own license, shown alongside the artwork's so neither is mistaken for the other. */
private const val APP_COPYRIGHT = "© 2026 Mandarine Tech"
private const val APP_LICENSE_LABEL = "Apache License 2.0"
private const val APP_LICENSE_URL = "https://www.apache.org/licenses/LICENSE-2.0"

/** The license the artwork is used under — the tail of [ART_ATTRIBUTION], and its link. */
private const val ART_LICENSE_LABEL = "CC BY 4.0"
private const val ART_LICENSE_URL = "https://creativecommons.org/licenses/by/4.0/"

/** Where to write about the app. Opened as a `mailto:` so it lands in the user's mail app. */
private const val CONTACT_EMAIL = "mandarinetech.dev@gmail.com"

/** Inset of the sheet's content, wider than the screen's so the sheet reads as its own surface. */
private val SHEET_HORIZONTAL_PADDING = 24.dp

/** Padding under the last row, keeping it clear of the gesture bar. */
private val SHEET_BOTTOM_PADDING = 32.dp

/** Gap between the sheet's sections, and the tighter one between a section's own rows. */
private val SECTION_SPACING = 20.dp
private val ROW_SPACING = 4.dp

/** Minimum height of a tappable link row, so a one-line link is still a comfortable target. */
private val LINK_MIN_HEIGHT = 40.dp

/**
 * The icon button that opens [AboutSheet], pinned at the trailing end of the color swatch row.
 *
 * Sits *outside* [DiceColorSwatchRow]'s horizontal scroll rather than inside it: the row already
 * scrolls its twelve swatches sideways, and a credit the license requires to be discoverable
 * cannot live somewhere the user has to scroll to find. It costs no vertical space either — the
 * swatch row's touch targets are already 44dp tall, which is what made this the placement that
 * pays for issue #66's freed footer rather than spending it again.
 *
 * @param onClick invoked when the button is tapped
 * @param modifier optional [Modifier] applied to the button
 */
@Composable
fun AboutIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.testTag(ABOUT_BUTTON_TAG),
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_info_outline),
            contentDescription = ABOUT_LABEL,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * The modal bottom sheet holding everything the app has to say about itself: its own license, the
 * artwork's CC BY credit, and a contact address.
 *
 * This is where the license-required credit line lives since issue #66 took it out of the main
 * screen's bottom bar. CC BY 4.0 asks for attribution that is visible to users, not attribution
 * that is permanently on screen, so one tap from the main screen satisfies it while giving the
 * roll button back the band it was competing with.
 *
 * A sheet rather than a dialog because the content is three sections and a handful of links: it
 * scrolls internally, so a short viewport shortens the sheet instead of clipping the contact row
 * off the bottom of a fixed box.
 *
 * @param onDismiss invoked when the sheet is swiped away or its scrim tapped
 * @param modifier optional [Modifier] applied to the sheet
 * @param versionName the app version shown under the title; the real one unless a test overrides it
 * @param onOpenLink opens a link; defaults to the platform handler, overridable so instrumented
 *   tests can assert *which* URI a row opens without leaving the app
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    versionName: String = BuildConfig.VERSION_NAME,
    onOpenLink: (String) -> Unit = LocalUriHandler.current::openUri,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        sheetState = rememberModalBottomSheetState(),
    ) {
        AboutSheetContent(versionName = versionName, onOpenLink = onOpenLink)
    }
}

/**
 * Everything inside the sheet, split out from [AboutSheet] so it can be previewed: a
 * [ModalBottomSheet] renders into its own window and shows up empty in the preview pane.
 */
@Composable
private fun AboutSheetContent(
    versionName: String,
    onOpenLink: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    // A device with no browser or no mail app throws rather than returning false, and a missing
    // mail app is not worth crashing the dice roller over.
    val openLink: (String) -> Unit = { uri -> runCatching { onOpenLink(uri) } }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(
                start = SHEET_HORIZONTAL_PADDING,
                end = SHEET_HORIZONTAL_PADDING,
                bottom = SHEET_BOTTOM_PADDING,
            )
            .testTag(ABOUT_SHEET_TAG),
        verticalArrangement = Arrangement.spacedBy(SECTION_SPACING),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(ROW_SPACING)) {
            Text(
                text = ABOUT_LABEL,
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = "${stringResource(id = R.string.app_name)} $versionName",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        AboutSection(title = "License") {
            AboutLinkedLine(
                prefix = "$APP_COPYRIGHT · ",
                linkLabel = APP_LICENSE_LABEL,
                uri = APP_LICENSE_URL,
                onOpenLink = openLink,
            )
        }

        AboutSection(title = "Artwork") {
            // One line, and the only one the license actually mandates: the credit rendered whole,
            // with the license name it already ends in acting as the link to the deed. Naming the
            // pack, or linking its listing, cost rows that said nothing this line does not.
            AboutLinkedLine(
                prefix = ART_ATTRIBUTION.removeSuffix(ART_LICENSE_LABEL),
                linkLabel = ART_LICENSE_LABEL,
                uri = ART_LICENSE_URL,
                onOpenLink = openLink,
            )
        }

        AboutSection(title = "Contact") {
            AboutLink(
                label = CONTACT_EMAIL,
                uri = "mailto:$CONTACT_EMAIL",
                onOpenLink = openLink,
            )
        }
    }
}

/** A titled block of the sheet: a label in `primary`, then whatever rows the caller passes. */
@Composable
private fun AboutSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(ROW_SPACING),
    ) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        content()
    }
}

/**
 * A line of body text ending in a link: [prefix] in the normal color, then [linkLabel] underlined
 * and opening [uri].
 *
 * Exists so the credit line can carry its own license link. `Dice art by Aeynit · CC BY 4.0`
 * already names the license, so a separate `CC BY 4.0 ↗` row underneath was a row spent repeating
 * what the line above it said — the link belongs on the words that are already there.
 *
 * The tap target is the glyph box of [linkLabel] alone, which is why full-row links like the
 * contact address still use [AboutLink] instead.
 */
@Composable
private fun AboutLinkedLine(
    prefix: String,
    linkLabel: String,
    uri: String,
    onOpenLink: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val linkStyles = TextLinkStyles(
        style = SpanStyle(
            color = MaterialTheme.colorScheme.primary,
            textDecoration = TextDecoration.Underline,
        ),
    )
    // A listener replaces the default open-the-URL behaviour, which is what routes this through
    // the caller's handler — the same seam the other rows use.
    val link = LinkAnnotation.Url(
        url = uri,
        styles = linkStyles,
        linkInteractionListener = { onOpenLink(uri) },
    )

    Text(
        text = buildAnnotatedString {
            append(prefix)
            withLink(link) { append(linkLabel) }
        },
        style = MaterialTheme.typography.bodyMedium,
        modifier = modifier,
    )
}

/** One tappable row opening [uri], underlined so it reads as a link without an affordance icon. */
@Composable
private fun AboutLink(
    label: String,
    uri: String,
    onOpenLink: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Text(
        text = label,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.primary,
        textDecoration = TextDecoration.Underline,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = LINK_MIN_HEIGHT)
            .wrapContentHeight()
            .clickable(role = Role.Button) { onOpenLink(uri) }
            .semantics { contentDescription = "$label, opens externally" },
    )
}

// -- Previews -----------------------------------------------------------------

/** The sheet's content, previewed without the sheet window it normally lives in. */
@Preview(name = "About sheet content", showBackground = true, widthDp = 360)
@Composable
private fun AboutSheetContentPreview() {
    DiceRollerTheme(dynamicColor = false) {
        Surface {
            AboutSheetContent(versionName = "1.0", onOpenLink = {})
        }
    }
}

@Preview(name = "About button", showBackground = true)
@Composable
private fun AboutIconButtonPreview() {
    DiceRollerTheme(dynamicColor = false) {
        AboutIconButton(onClick = {})
    }
}
