// app/src/main/java/fr/mandarine/diceroller/presentation/component/SettingsSheet.kt
package fr.mandarine.diceroller.presentation.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
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
import fr.mandarine.diceroller.presentation.AppLanguage
import fr.mandarine.diceroller.ui.theme.DiceRollerTheme

/** Test tag of the settings sheet's content root. */
const val SETTINGS_SHEET_TAG: String = "settings-sheet"

/** Test tag of the gear button that opens the sheet. */
const val SETTINGS_BUTTON_TAG: String = "settings-button"

/** Test tag of one language option in the picker, e.g. `language-option-fr`. */
fun languageOptionTestTag(language: AppLanguage): String = "language-option-${language.tag ?: "system"}"

/** Destination of the app's own license link; the label beside it is `about_app_license`. */
private const val APP_LICENSE_URL = "https://www.apache.org/licenses/LICENSE-2.0"

/**
 * Destination of the artwork's license link.
 *
 * The label is [R.string.about_art_license], which is also the suffix
 * [R.string.about_art_attribution] must end in — see the note on that string, and
 * [AboutSheetContent] for what splits them.
 */
private const val ART_LICENSE_URL = "https://creativecommons.org/licenses/by/4.0/"

/** Where to write about the app. Opened as a `mailto:` so it lands in the user's mail app. */
private const val CONTACT_MAILTO_SCHEME = "mailto:"

/** Inset of the sheet's content, wider than the screen's so the sheet reads as its own surface. */
private val SHEET_HORIZONTAL_PADDING = 24.dp

/** Padding under the last row, keeping it clear of the gesture bar. */
private val SHEET_BOTTOM_PADDING = 32.dp

/** Gap between the sheet's sections, and the tighter one between a section's own rows. */
private val SECTION_SPACING = 20.dp
private val ROW_SPACING = 4.dp

/** Minimum height of a tappable link row, so a one-line link is still a comfortable target. */
private val LINK_MIN_HEIGHT = 40.dp

/** Height and internal gap of one language option, sized as a comfortable radio target. */
private val LANGUAGE_OPTION_MIN_HEIGHT = 48.dp
private val LANGUAGE_OPTION_SPACING = 8.dp

/**
 * The gear button that opens [SettingsSheet], pinned at the trailing end of the color swatch row.
 *
 * Sits *outside* [DiceColorSwatchRow]'s horizontal scroll rather than inside it: the row already
 * scrolls its twelve swatches sideways, and a credit the license requires to be discoverable
 * cannot live somewhere the user has to scroll to find. It costs no vertical space either — the
 * swatch row's touch targets are already 44dp tall, which is what made this the placement that
 * pays for issue #66's freed footer rather than spending it again.
 *
 * It was an ⓘ until the sheet behind it gained the language control: once there is something to
 * *change* in there and not merely something to read, a gear is the affordance users go looking
 * for and an info glyph is one they do not.
 *
 * @param onClick invoked when the button is tapped
 * @param modifier optional [Modifier] applied to the button
 */
@Composable
fun SettingsIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.testTag(SETTINGS_BUTTON_TAG),
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_settings),
            contentDescription = stringResource(R.string.settings_title),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * The modal bottom sheet holding the app's one setting and everything it has to say about itself:
 * the language, then its own license, the artwork's CC BY credit, and a contact address.
 *
 * It was an About sheet until it gained the language row. That is why the whole thing is now
 * titled *Settings* with **About demoted to a section**: a sheet that only tells you things and a
 * sheet you change things in are different promises, and the title has to make the right one.
 *
 * This is still where the license-required credit line lives since issue #66 took it out of the
 * main screen's bottom bar. CC BY 4.0 asks for attribution that is visible to users, not
 * attribution that is permanently on screen, so one tap from the main screen satisfies it while
 * giving the roll button back the band it was competing with — and a gear is at least as findable
 * as the ⓘ it replaced.
 *
 * A sheet rather than a dialog because the content is several sections and a handful of links: it
 * scrolls internally, so a short viewport shortens the sheet instead of clipping the contact row
 * off the bottom of a fixed box.
 *
 * @param onDismiss invoked when the sheet is swiped away or its scrim tapped
 * @param modifier optional [Modifier] applied to the sheet
 * @param versionName the app version shown in the About section; the real one unless a test
 *   overrides it
 * @param onOpenLink opens a link; defaults to the platform handler, overridable so instrumented
 *   tests can assert *which* URI a row opens without leaving the app
 * @param selectedLanguage the language the app is currently written in
 * @param onSelectLanguage invoked with the language the user picked
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    versionName: String = BuildConfig.VERSION_NAME,
    onOpenLink: (String) -> Unit = LocalUriHandler.current::openUri,
    selectedLanguage: AppLanguage = AppLanguage.System,
    onSelectLanguage: (AppLanguage) -> Unit = {},
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        sheetState = rememberModalBottomSheetState(),
    ) {
        SettingsSheetContent(
            versionName = versionName,
            onOpenLink = onOpenLink,
            selectedLanguage = selectedLanguage,
            onSelectLanguage = onSelectLanguage,
        )
    }
}

/**
 * Everything inside the sheet, split out from [SettingsSheet] so it can be previewed: a
 * [ModalBottomSheet] renders into its own window and shows up empty in the preview pane.
 */
@Composable
private fun SettingsSheetContent(
    versionName: String,
    onOpenLink: (String) -> Unit,
    selectedLanguage: AppLanguage = AppLanguage.System,
    onSelectLanguage: (AppLanguage) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    // A device with no browser or no mail app throws rather than returning false, and a missing
    // mail app is not worth crashing the dice roller over.
    val openLink: (String) -> Unit = { uri -> runCatching { onOpenLink(uri) } }
    val artLicenseLabel = stringResource(R.string.about_art_license)
    val contactEmail = stringResource(R.string.about_contact_email)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(
                start = SHEET_HORIZONTAL_PADDING,
                end = SHEET_HORIZONTAL_PADDING,
                bottom = SHEET_BOTTOM_PADDING,
            )
            .testTag(SETTINGS_SHEET_TAG),
        verticalArrangement = Arrangement.spacedBy(SECTION_SPACING),
    ) {
        Text(
            text = stringResource(R.string.settings_title),
            style = MaterialTheme.typography.headlineSmall,
        )

        // First, because it is the only thing here that *does* anything; everything below it is
        // something to read.
        SettingsSection(title = stringResource(R.string.settings_section_language)) {
            LanguagePicker(
                selectedLanguage = selectedLanguage,
                onSelectLanguage = onSelectLanguage,
            )
        }

        SettingsSection(title = stringResource(R.string.settings_section_about)) {
            Text(
                text = stringResource(
                    R.string.about_version,
                    stringResource(R.string.app_name),
                    versionName,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        SettingsSection(title = stringResource(R.string.about_section_license)) {
            AboutLinkedLine(
                prefix = "${stringResource(R.string.about_app_copyright)} · ",
                linkLabel = stringResource(R.string.about_app_license),
                uri = APP_LICENSE_URL,
                onOpenLink = openLink,
            )
        }

        SettingsSection(title = stringResource(R.string.about_section_artwork)) {
            // One line, and the only one the license actually mandates: the credit rendered whole,
            // with the license name it already ends in acting as the link to the deed. Naming the
            // pack, or linking its listing, cost rows that said nothing this line does not.
            //
            // Splitting the suffix back off is why every translation of the credit has to keep
            // ending in "CC BY 4.0" — a translation that does not would render the licence name
            // twice rather than lose the link.
            AboutLinkedLine(
                prefix = stringResource(R.string.about_art_attribution)
                    .removeSuffix(artLicenseLabel),
                linkLabel = artLicenseLabel,
                uri = ART_LICENSE_URL,
                onOpenLink = openLink,
            )
        }

        SettingsSection(title = stringResource(R.string.about_section_contact)) {
            AboutLink(
                label = contactEmail,
                uri = "$CONTACT_MAILTO_SCHEME$contactEmail",
                onOpenLink = openLink,
            )
        }
    }
}

/**
 * The language options, as a radio group: the device's own language plus every language the app
 * ships strings for.
 *
 * A radio group rather than a dropdown or a link out to Settings, because the whole point is that
 * the choice is *visible*: three options fit in the space a "Language ›" row would have taken, and
 * the user can see what the app can be without opening anything. It is a real
 * [selectableGroup] so a screen reader announces "2 of 3" and the options behave as one control.
 *
 * Picking one is an ordinary state change: the ViewModel records it, `ProvideAppLanguage` swaps
 * the resources the composition reads, and this list recomposes with its new selection and its
 * own labels already translated. Nothing is torn down, which is the point — see
 * `docs/features/language-and-settings.md`.
 */
@Composable
private fun LanguagePicker(
    selectedLanguage: AppLanguage,
    onSelectLanguage: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.selectableGroup()) {
        AppLanguage.entries.forEach { language ->
            val isSelected = language == selectedLanguage
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = LANGUAGE_OPTION_MIN_HEIGHT)
                    .selectable(
                        selected = isSelected,
                        role = Role.RadioButton,
                        // Re-picking the language already in force would write the same value and
                        // recompose to an identical screen, so the selected row is inert.
                        enabled = !isSelected,
                        onClick = { onSelectLanguage(language) },
                    )
                    .testTag(languageOptionTestTag(language)),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(LANGUAGE_OPTION_SPACING),
            ) {
                // Null callback: the row above is the control, so the button must not be a second
                // focus stop announcing the same thing.
                RadioButton(selected = isSelected, onClick = null)
                Text(
                    text = stringResource(language.labelRes),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

/** A titled block of the sheet: a label in `primary`, then whatever rows the caller passes. */
@Composable
private fun SettingsSection(
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
    ExternalActionRow(label = label, onClick = { onOpenLink(uri) }, modifier = modifier)
}

/**
 * One tappable row that hands off to something outside this app, underlined so it reads as a link
 * without needing an affordance icon.
 *
 * Shared by the contact address and the language row, which look alike because they *are* alike:
 * both leave DiceRoller. That is also why both announce themselves as opening externally — the
 * language row navigates to a system screen, and a screen reader user deserves the same warning a
 * `mailto:` gets rather than being surprised by a context switch.
 */
@Composable
private fun ExternalActionRow(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(R.string.link_opens_externally, label)
    Text(
        text = label,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.primary,
        textDecoration = TextDecoration.Underline,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = LINK_MIN_HEIGHT)
            .wrapContentHeight()
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
    )
}

// -- Previews -----------------------------------------------------------------

/** The sheet's content, previewed without the sheet window it normally lives in. */
@Preview(name = "Settings sheet content", showBackground = true, widthDp = 360)
@Composable
private fun SettingsSheetContentPreview() {
    DiceRollerTheme(dynamicColor = false) {
        Surface {
            SettingsSheetContent(versionName = "1.0", onOpenLink = {})
        }
    }
}

/** The app put into French while the device stays in English — the whole point of the picker. */
@Preview(name = "Settings sheet content - French selected", showBackground = true, widthDp = 360)
@Composable
private fun SettingsSheetContentFrenchPreview() {
    DiceRollerTheme(dynamicColor = false) {
        Surface {
            SettingsSheetContent(
                versionName = "1.0",
                onOpenLink = {},
                selectedLanguage = AppLanguage.French,
            )
        }
    }
}

@Preview(name = "Settings button", showBackground = true)
@Composable
private fun SettingsIconButtonPreview() {
    DiceRollerTheme(dynamicColor = false) {
        SettingsIconButton(onClick = {})
    }
}
