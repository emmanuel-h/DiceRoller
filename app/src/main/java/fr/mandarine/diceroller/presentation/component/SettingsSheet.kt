// app/src/main/java/fr/mandarine/diceroller/presentation/component/SettingsSheet.kt
package fr.mandarine.diceroller.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import fr.mandarine.diceroller.R
import fr.mandarine.diceroller.presentation.AppLanguage
import fr.mandarine.diceroller.presentation.AppTheme
import fr.mandarine.diceroller.presentation.ShakeDetector
import kotlin.math.roundToInt
import fr.mandarine.diceroller.ui.theme.DiceRollerTheme
import fr.mandarine.diceroller.ui.theme.displayStyle

/** Test tag of the settings sheet's content root. */
const val SETTINGS_SHEET_TAG: String = "settings-sheet"

/** Test tag of the gear button that opens the sheet. */
const val SETTINGS_BUTTON_TAG: String = "settings-button"

/** Test tag on the shake-to-roll row, which is the switch's whole touch target. */
const val SHAKE_TO_ROLL_TOGGLE_TAG: String = "shake-to-roll-toggle"

/** Test tag on the shake sensitivity slider. */
const val SHAKE_SENSITIVITY_SLIDER_TAG: String = "shake-sensitivity-slider"

/** Test tag of one language option in the picker, e.g. `language-option-fr`. */
fun languageOptionTestTag(language: AppLanguage): String = "language-option-${language.tag}"

/** Test tag of one theme option in the picker, e.g. `theme-option-dark`. */
fun themeOptionTestTag(theme: AppTheme): String = "theme-option-${theme.name.lowercase()}"

/**
 * Destination of the artwork's license link.
 *
 * The label is [R.string.about_art_license], which is also the suffix
 * [R.string.about_art_attribution] must end in — see the note on that string, and
 * [SettingsSheetContent] for what splits them.
 */
private const val ART_LICENSE_URL = "https://creativecommons.org/licenses/by/4.0/"

/** Inset of the sheet's content, wider than the screen's so the sheet reads as its own surface. */
private val SHEET_HORIZONTAL_PADDING = 24.dp

/** Padding under the last row, keeping it clear of the gesture bar. */
private val SHEET_BOTTOM_PADDING = 32.dp

/** Gap between the sheet's sections, and the tighter one between a section's own rows. */
private val SECTION_SPACING = 20.dp
private val ROW_SPACING = 4.dp

/** Height and internal gap of one option row, sized as a comfortable radio or switch target. */
private val OPTION_MIN_HEIGHT = 48.dp
private val OPTION_SPACING = 8.dp

/** Gap between two radio options sharing a line, wide enough that they read as two choices. */
private val INLINE_OPTION_SPACING = 24.dp

/** Above this luminance the sheet's surface is light, so its bar icons must be dark. */
private const val LIGHT_SURFACE_LUMINANCE = 0.5f

/** Material's opacity for disabled content, applied to the slider's labels while shaking is off. */
private const val DISABLED_ALPHA = 0.38f

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
 * The modal bottom sheet holding the app's settings — language, theme, shake to roll — and the
 * artwork's CC BY credit, the one read-only line the licence requires the app to show somewhere.
 *
 * This is still where the license-required credit line lives since issue #66 took it out of the
 * main screen's bottom bar. CC BY 4.0 asks for attribution that is visible to users, not
 * attribution that is permanently on screen, so one tap from the main screen satisfies it while
 * giving the roll button back the band it was competing with — and a gear is at least as findable
 * as the ⓘ it replaced.
 *
 * A sheet rather than a dialog because the content is several sections: it scrolls internally, so
 * a short viewport shortens the sheet instead of clipping the credit off the bottom of a fixed box.
 *
 * @param onDismiss invoked when the sheet is swiped away or its scrim tapped
 * @param modifier optional [Modifier] applied to the sheet
 * @param onOpenLink opens a link; defaults to the platform handler, overridable so instrumented
 *   tests can assert *which* URI a row opens without leaving the app
 * @param selectedLanguage the language the app is currently written in
 * @param onSelectLanguage invoked with the language the user picked
 * @param selectedTheme whether the app is currently drawn light or dark
 * @param onSelectTheme invoked with the theme the user picked
 * @param isShakeToRollEnabled whether shaking the phone currently rolls the dice
 * @param onSetShakeToRollEnabled invoked with the new value when the shake switch is flipped
 * @param shakeSensitivity the sensitivity slider's current step, 0 being the least sensitive
 * @param onSetShakeSensitivity invoked with the step the slider moved to
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenLink: (String) -> Unit = LocalUriHandler.current::openUri,
    selectedLanguage: AppLanguage = AppLanguage.English,
    onSelectLanguage: (AppLanguage) -> Unit = {},
    selectedTheme: AppTheme = AppTheme.Light,
    onSelectTheme: (AppTheme) -> Unit = {},
    isShakeToRollEnabled: Boolean = true,
    onSetShakeToRollEnabled: (Boolean) -> Unit = {},
    shakeSensitivity: Int = ShakeDetector.DEFAULT_SENSITIVITY,
    onSetShakeSensitivity: (Int) -> Unit = {},
) {
    val isLightTheme = MaterialTheme.colorScheme.surface.luminance() > LIGHT_SURFACE_LUMINANCE
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        // Fully open from the start: half-open, the Rolling section (issue #1) pushed the artwork
        // credit below the fold, and the licence needs it one tap away, not one tap and a drag.
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        properties = ModalBottomSheetProperties(
            // The sheet is its own window, whose bar icons otherwise follow the device's dark mode
            // rather than the app's theme — and dark icons on leather are invisible.
            isAppearanceLightStatusBars = isLightTheme,
            isAppearanceLightNavigationBars = isLightTheme,
        ),
    ) {
        SettingsSheetContent(
            onOpenLink = onOpenLink,
            selectedLanguage = selectedLanguage,
            onSelectLanguage = onSelectLanguage,
            selectedTheme = selectedTheme,
            onSelectTheme = onSelectTheme,
            isShakeToRollEnabled = isShakeToRollEnabled,
            onSetShakeToRollEnabled = onSetShakeToRollEnabled,
            shakeSensitivity = shakeSensitivity,
            onSetShakeSensitivity = onSetShakeSensitivity,
        )
    }
}

/**
 * Everything inside the sheet, split out from [SettingsSheet] so it can be previewed: a
 * [ModalBottomSheet] renders into its own window and shows up empty in the preview pane.
 */
@Composable
private fun SettingsSheetContent(
    onOpenLink: (String) -> Unit,
    selectedLanguage: AppLanguage = AppLanguage.English,
    onSelectLanguage: (AppLanguage) -> Unit = {},
    selectedTheme: AppTheme = AppTheme.Light,
    onSelectTheme: (AppTheme) -> Unit = {},
    isShakeToRollEnabled: Boolean = true,
    onSetShakeToRollEnabled: (Boolean) -> Unit = {},
    shakeSensitivity: Int = ShakeDetector.DEFAULT_SENSITIVITY,
    onSetShakeSensitivity: (Int) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    // A device with no browser throws rather than returning false, and that is not worth crashing
    // the dice roller over.
    val openLink: (String) -> Unit = { uri -> runCatching { onOpenLink(uri) } }
    val artLicenseLabel = stringResource(R.string.about_art_license)

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
            style = displayStyle(MaterialTheme.typography.headlineSmall),
        )

        // The settings come first, because they are the only things here that *do* anything; the
        // credit below them is something to read.
        SettingsSection(title = stringResource(R.string.settings_section_language)) {
            InlineRadioGroup(
                options = AppLanguage.entries,
                selected = selectedLanguage,
                onSelect = onSelectLanguage,
                labelRes = AppLanguage::labelRes,
                testTag = ::languageOptionTestTag,
            )
        }

        SettingsSection(title = stringResource(R.string.settings_section_theme)) {
            InlineRadioGroup(
                options = AppTheme.entries,
                selected = selectedTheme,
                onSelect = onSelectTheme,
                labelRes = AppTheme::labelRes,
                testTag = ::themeOptionTestTag,
            )
        }

        SettingsSection(title = stringResource(R.string.settings_section_rolling)) {
            ShakeToRollToggle(
                isEnabled = isShakeToRollEnabled,
                onSetEnabled = onSetShakeToRollEnabled,
            )
            ShakeSensitivitySlider(
                level = shakeSensitivity,
                enabled = isShakeToRollEnabled,
                onSetLevel = onSetShakeSensitivity,
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

    }
}

/**
 * A two- or three-way choice laid out on one line under its section header: a radio and a label
 * per option, side by side. Used by the language and the theme, which each have exactly two
 * options — a column of radios would spend a row each to say less than one line does.
 *
 * It is a real [selectableGroup] so a screen reader announces "2 of 2" and the options behave as
 * one control. Each whole option, not just its radio, is the tap target.
 */
@Composable
private fun <T> InlineRadioGroup(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    labelRes: (T) -> Int,
    testTag: (T) -> String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(INLINE_OPTION_SPACING),
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            Row(
                modifier = Modifier
                    .heightIn(min = OPTION_MIN_HEIGHT)
                    .selectable(
                        selected = isSelected,
                        role = Role.RadioButton,
                        // Re-picking the option already in force would write the same value and
                        // recompose to an identical screen, so the selected one is inert.
                        enabled = !isSelected,
                        onClick = { onSelect(option) },
                    )
                    .testTag(testTag(option)),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(OPTION_SPACING),
            ) {
                // Null callback: the option is the control, so the button must not be a second
                // focus stop announcing the same thing.
                RadioButton(selected = isSelected, onClick = null)
                Text(
                    text = stringResource(labelRes(option)),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

/**
 * The shake-to-roll switch (issue #1), as a row whose label explains the gesture.
 *
 * The whole row toggles, not just the switch, for the same reason the language options are whole
 * rows: the switch alone is a small target at the end of a line of text.
 */
@Composable
private fun ShakeToRollToggle(
    isEnabled: Boolean,
    onSetEnabled: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = OPTION_MIN_HEIGHT)
            .toggleable(value = isEnabled, role = Role.Switch, onValueChange = onSetEnabled)
            .testTag(SHAKE_TO_ROLL_TOGGLE_TAG),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(OPTION_SPACING),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.shake_to_roll_label),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = stringResource(R.string.shake_to_roll_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        // Null callback: the row is the control, as with the language options' radio buttons.
        Switch(checked = isEnabled, onCheckedChange = null)
    }
}

/**
 * How hard a shake must be, as a stepped slider from Less to More sensitive.
 *
 * Steps rather than a continuous range, because each one is a fixed jolt threshold
 * ([ShakeDetector.thresholdForSensitivity]); the ends are labelled in words rather than in g, which
 * would mean nothing to most people holding the phone. Greyed out while shaking is off, rather than
 * hidden, so switching it back on does not move the rows below.
 *
 * A screen reader announces the position as "3 of 5" instead of the slider's default percentage.
 */
@Composable
private fun ShakeSensitivitySlider(
    level: Int,
    enabled: Boolean,
    onSetLevel: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val lastLevel = ShakeDetector.SENSITIVITY_LEVELS - 1
    val state = stringResource(R.string.shake_sensitivity_state, level + 1, ShakeDetector.SENSITIVITY_LEVELS)
    val textColor = if (enabled) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = DISABLED_ALPHA)
    }
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.shake_sensitivity_label),
            style = MaterialTheme.typography.bodyMedium,
            color = textColor,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(OPTION_SPACING),
        ) {
            Text(
                text = stringResource(R.string.shake_sensitivity_less),
                style = MaterialTheme.typography.bodySmall,
                color = textColor,
            )
            Slider(
                value = level.toFloat(),
                // Fires on every movement of a drag; the ViewModel ignores repeats of a step.
                onValueChange = { value -> onSetLevel(value.roundToInt()) },
                enabled = enabled,
                valueRange = 0f..lastLevel.toFloat(),
                // `steps` counts the notches *between* the ends.
                steps = lastLevel - 1,
                modifier = Modifier
                    .weight(1f)
                    .semantics { stateDescription = state }
                    .testTag(SHAKE_SENSITIVITY_SLIDER_TAG),
            )
            Text(
                text = stringResource(R.string.shake_sensitivity_more),
                style = MaterialTheme.typography.bodySmall,
                color = textColor,
            )
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
 * The tap target is the glyph box of [linkLabel] alone.
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

// -- Previews -----------------------------------------------------------------

/** The sheet's content, previewed without the sheet window it normally lives in. */
@Preview(name = "Settings sheet content", showBackground = true, widthDp = 360)
@Composable
private fun SettingsSheetContentPreview() {
    DiceRollerTheme {
        Surface {
            SettingsSheetContent(onOpenLink = {})
        }
    }
}

/** The app put into French and dark — both inline pickers on their second option. */
@Preview(name = "Settings sheet content - French selected", showBackground = true, widthDp = 360)
@Composable
private fun SettingsSheetContentFrenchPreview() {
    DiceRollerTheme {
        Surface {
            SettingsSheetContent(
                onOpenLink = {},
                selectedLanguage = AppLanguage.French,
                selectedTheme = AppTheme.Dark,
            )
        }
    }
}

@Preview(name = "Settings button", showBackground = true)
@Composable
private fun SettingsIconButtonPreview() {
    DiceRollerTheme {
        SettingsIconButton(onClick = {})
    }
}
