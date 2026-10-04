// app/src/androidTest/java/fr/mandarine/diceroller/presentation/component/SettingsSheetTest.kt
package fr.mandarine.diceroller.presentation.component

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.mandarine.diceroller.R
import fr.mandarine.diceroller.presentation.AppLanguage
import fr.mandarine.diceroller.presentation.AppTheme
import fr.mandarine.diceroller.str
import fr.mandarine.diceroller.ui.theme.DiceRollerTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The sheet behind the gear: the inline language and theme pickers, shake to roll, and the
 * licence-required artwork credit — and none of the About, licence or contact rows it used to carry.
 *
 * The sheet is a pure function of its selections and reports picks through callbacks; what a pick
 * then *does* belongs to the ViewModel and to `MainActivity`, and is covered by
 * `DiceRollerViewModelTest` and by the French run of the whole suite.
 */
@RunWith(AndroidJUnit4::class)
class SettingsSheetTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val selected = mutableListOf<AppLanguage>()
    private val selectedThemes = mutableListOf<AppTheme>()
    private val shakeToggles = mutableListOf<Boolean>()
    private val sensitivities = mutableListOf<Int>()

    private fun launchSheet(
        selectedLanguage: AppLanguage = AppLanguage.English,
        selectedTheme: AppTheme = AppTheme.Light,
        isShakeToRollEnabled: Boolean = true,
    ) {
        selected.clear()
        selectedThemes.clear()
        shakeToggles.clear()
        sensitivities.clear()
        composeTestRule.setContent {
            DiceRollerTheme {
                SettingsSheet(
                    onDismiss = {},
                    onOpenLink = {},
                    selectedLanguage = selectedLanguage,
                    onSelectLanguage = { selected += it },
                    selectedTheme = selectedTheme,
                    onSelectTheme = { selectedThemes += it },
                    isShakeToRollEnabled = isShakeToRollEnabled,
                    onSetShakeToRollEnabled = { shakeToggles += it },
                    onSetShakeSensitivity = { sensitivities += it },
                )
            }
        }
    }

    private fun option(language: AppLanguage) =
        composeTestRule.onNodeWithTag(languageOptionTestTag(language))

    private fun option(theme: AppTheme) =
        composeTestRule.onNodeWithTag(themeOptionTestTag(theme))

    private fun topOfTag(tag: String): Float = composeTestRule
        .onNodeWithTag(tag)
        .fetchSemanticsNode()
        .boundsInRoot
        .top

    private fun topOfText(text: String): Float = composeTestRule
        .onNodeWithText(text)
        .fetchSemanticsNode()
        .boundsInRoot
        .top

    // --- Shake to roll (issue #1) ---

    @Test
    fun givenShakeToRollOn_whenTheSheetOpens_thenTheSwitchShowsOn() {
        launchSheet(isShakeToRollEnabled = true)

        composeTestRule.onNodeWithText(str(R.string.shake_to_roll_label)).assertIsDisplayed()
        composeTestRule.onNodeWithTag(SHAKE_TO_ROLL_TOGGLE_TAG).assertIsOn()
    }

    @Test
    fun givenShakeToRollOn_whenTheRowIsTapped_thenItAsksToSwitchItOff() {
        launchSheet(isShakeToRollEnabled = true)

        composeTestRule.onNodeWithTag(SHAKE_TO_ROLL_TOGGLE_TAG).performClick()

        assertEquals(listOf(false), shakeToggles)
    }

    @Test
    fun givenShakeToRollOff_whenTheRowIsTapped_thenItAsksToSwitchItOn() {
        launchSheet(isShakeToRollEnabled = false)

        composeTestRule.onNodeWithTag(SHAKE_TO_ROLL_TOGGLE_TAG).assertIsOff().performClick()

        assertEquals(listOf(true), shakeToggles)
    }

    @Test
    fun givenShakeToRollOn_whenTheSheetOpens_thenTheSensitivitySliderIsUsable() {
        launchSheet(isShakeToRollEnabled = true)

        composeTestRule.onNodeWithTag(SHAKE_SENSITIVITY_SLIDER_TAG).assertIsEnabled()
    }

    /** Greyed out rather than hidden, so turning shaking back on does not move the rows below. */
    @Test
    fun givenShakeToRollOff_whenTheSheetOpens_thenTheSensitivitySliderIsShownDisabled() {
        launchSheet(isShakeToRollEnabled = false)

        composeTestRule.onNodeWithTag(SHAKE_SENSITIVITY_SLIDER_TAG).assertIsDisplayed().assertIsNotEnabled()
    }

    @Test
    fun givenTheSlider_whenMovedToTheMostSensitiveEnd_thenThatStepIsRequested() {
        launchSheet()

        composeTestRule.onNodeWithTag(SHAKE_SENSITIVITY_SLIDER_TAG)
            .performSemanticsAction(SemanticsActions.SetProgress) { setProgress -> setProgress(4f) }

        assertEquals(4, sensitivities.last())
    }

    // --- It is a settings sheet now, not an About box ---

    @Test
    fun givenTheSheet_whenOpened_thenItIsTitledSettings() {
        launchSheet()

        composeTestRule.onNodeWithText(str(R.string.settings_title)).assertIsDisplayed()
    }

    /** About, the app's licence and the contact address were taken out of the sheet. */
    @Test
    fun givenTheSheet_whenOpened_thenThereIsNoAboutLicenseOrContact() {
        launchSheet()

        composeTestRule.onNodeWithText("ABOUT", ignoreCase = true).assertDoesNotExist()
        composeTestRule.onNodeWithText("Apache", substring = true).assertDoesNotExist()
        composeTestRule.onNodeWithText("@", substring = true).assertDoesNotExist()
    }

    // --- The picker offers every language the app ships ---

    @Test
    fun givenTheSheet_whenOpened_thenEveryShippedLanguageIsOffered() {
        launchSheet()

        AppLanguage.entries.forEach { language ->
            option(language).assertIsDisplayed()
            composeTestRule.onNodeWithText(str(language.labelRes)).assertIsDisplayed()
        }
    }

    /**
     * The named languages are listed in their own endonym, never translated: someone switching
     * *out* of a language they cannot read has to recognise the one they want.
     */
    @Test
    fun givenTheSheet_whenOpened_thenTheNamedLanguagesReadAsTheirOwnEndonyms() {
        launchSheet()

        assertEquals("English", str(AppLanguage.English.labelRes))
        assertEquals("Français", str(AppLanguage.French.labelRes))
    }

    /** Inline: both options of a picker share one line. */
    @Test
    fun givenTheSheet_whenOpened_thenEachPickersOptionsShareALine() {
        launchSheet()

        assertEquals(
            topOfTag(languageOptionTestTag(AppLanguage.English)),
            topOfTag(languageOptionTestTag(AppLanguage.French)),
        )
        assertEquals(topOfTag(themeOptionTestTag(AppTheme.Light)), topOfTag(themeOptionTestTag(AppTheme.Dark)))
    }

    /** Settings first, then the read-only credit. */
    @Test
    fun givenTheSheet_whenOpened_thenThePickersAreAboveTheCredit() {
        launchSheet()

        val languageTop = topOfTag(languageOptionTestTag(AppLanguage.English))
        val themeTop = topOfTag(themeOptionTestTag(AppTheme.Light))
        val creditTop = topOfText(str(R.string.about_section_artwork).uppercase())

        assertTrue("Expected language ($languageTop) above theme ($themeTop)", languageTop < themeTop)
        assertTrue("Expected theme ($themeTop) above the credit ($creditTop)", themeTop < creditTop)
    }

    // --- Choosing one ---

    @Test
    fun givenEnglish_whenFrenchIsPicked_thenFrenchIsRequested() {
        launchSheet(selectedLanguage = AppLanguage.English)

        option(AppLanguage.English).assertIsSelected()
        option(AppLanguage.French).assertIsNotSelected().performClick()

        assertEquals(listOf(AppLanguage.French), selected)
    }

    @Test
    fun givenAnOverriddenLanguage_whenTheSheetIsOpened_thenThatLanguageIsTheSelectedOne() {
        launchSheet(selectedLanguage = AppLanguage.French)

        option(AppLanguage.French).assertIsSelected()
        option(AppLanguage.English).assertIsNotSelected()
    }

    /**
     * Re-picking what is already in force would write the same value and recompose to an identical
     * screen, so the selected option is inert rather than a no-op round trip.
     */
    @Test
    fun givenTheLanguageInForce_whenItIsPickedAgain_thenNothingIsRequested() {
        launchSheet(selectedLanguage = AppLanguage.French)

        option(AppLanguage.French).performClick()

        assertTrue("Expected no request, got $selected", selected.isEmpty())
    }

    // --- Theme ---

    @Test
    fun givenTheSheet_whenOpened_thenOnlyLightAndDarkAreOffered() {
        launchSheet()

        AppTheme.entries.forEach { theme ->
            option(theme).assertIsDisplayed()
            composeTestRule.onNodeWithText(str(theme.labelRes)).assertIsDisplayed()
        }
        assertEquals(listOf(AppTheme.Light, AppTheme.Dark), AppTheme.entries)
    }

    @Test
    fun givenLight_whenDarkIsPicked_thenDarkIsRequested() {
        launchSheet(selectedTheme = AppTheme.Light)

        option(AppTheme.Light).assertIsSelected()
        option(AppTheme.Dark).assertIsNotSelected().performClick()

        assertEquals(listOf(AppTheme.Dark), selectedThemes)
    }

    @Test
    fun givenTheThemeInForce_whenItIsPickedAgain_thenNothingIsRequested() {
        launchSheet(selectedTheme = AppTheme.Dark)

        option(AppTheme.Dark).assertIsSelected().performClick()

        assertTrue("Expected no request, got $selectedThemes", selectedThemes.isEmpty())
    }

    // --- The credit is still reachable, which is a licence requirement ---

    @Test
    fun givenTheSheet_whenOpened_thenTheRequiredCreditIsStillThere() {
        launchSheet()

        composeTestRule.onNodeWithText(str(R.string.about_art_attribution)).assertIsDisplayed()
    }
}
