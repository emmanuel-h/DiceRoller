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
import fr.mandarine.diceroller.str
import fr.mandarine.diceroller.ui.theme.DiceRollerTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The sheet behind the gear: the language picker it gained, and the About content it used to *be*.
 *
 * The sheet is a pure function of `selectedLanguage` and reports picks through `onSelectLanguage`;
 * what a pick then *does* belongs to the ViewModel and to `ProvideAppLanguage`, and is covered by
 * `DiceRollerViewModelTest` and by the French run of the whole suite.
 */
@RunWith(AndroidJUnit4::class)
class SettingsSheetTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val selected = mutableListOf<AppLanguage>()
    private val shakeToggles = mutableListOf<Boolean>()
    private val sensitivities = mutableListOf<Int>()

    private fun launchSheet(
        selectedLanguage: AppLanguage = AppLanguage.System,
        isShakeToRollEnabled: Boolean = true,
    ) {
        selected.clear()
        shakeToggles.clear()
        sensitivities.clear()
        composeTestRule.setContent {
            DiceRollerTheme {
                SettingsSheet(
                    onDismiss = {},
                    versionName = "1.0",
                    onOpenLink = {},
                    selectedLanguage = selectedLanguage,
                    onSelectLanguage = { selected += it },
                    isShakeToRollEnabled = isShakeToRollEnabled,
                    onSetShakeToRollEnabled = { shakeToggles += it },
                    onSetShakeSensitivity = { sensitivities += it },
                )
            }
        }
    }

    private fun option(language: AppLanguage) =
        composeTestRule.onNodeWithTag(languageOptionTestTag(language))

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

    /**
     * About was the sheet's headline until the picker arrived; it is a section among the others
     * now. Both strings therefore have to be on screen, in that relationship.
     */
    @Test
    fun givenTheSheet_whenOpened_thenAboutIsASectionBelowTheTitle() {
        launchSheet()

        val titleTop = topOfText(str(R.string.settings_title))
        val aboutSectionTop = topOfText(str(R.string.settings_section_about).uppercase())

        assertTrue(
            "Expected the About section ($aboutSectionTop) below the title ($titleTop)",
            titleTop < aboutSectionTop,
        )
    }

    @Test
    fun givenTheSheet_whenOpened_thenTheVersionIsStillShown() {
        launchSheet()

        composeTestRule
            .onNodeWithText(str(R.string.about_version, str(R.string.app_name), "1.0"))
            .assertIsDisplayed()
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

    /** First, because it is the only thing on the sheet that does anything. */
    @Test
    fun givenTheSheet_whenOpened_thenThePickerIsAboveTheAboutSection() {
        launchSheet()

        val pickerTop = topOfTag(languageOptionTestTag(AppLanguage.System))
        val aboutSectionTop = topOfText(str(R.string.settings_section_about).uppercase())

        assertTrue(
            "Expected the picker ($pickerTop) above About ($aboutSectionTop)",
            pickerTop < aboutSectionTop,
        )
    }

    // --- Choosing one ---

    @Test
    fun givenTheDefault_whenFrenchIsPicked_thenFrenchIsRequested() {
        launchSheet(selectedLanguage = AppLanguage.System)

        option(AppLanguage.French).performClick()

        assertEquals(listOf(AppLanguage.French), selected)
    }

    /** The whole feature in one test: an app following an English device, put into French. */
    @Test
    fun givenTheAppFollowingTheDevice_whenAnotherLanguageIsPicked_thenItOverridesTheDevice() {
        launchSheet(selectedLanguage = AppLanguage.System)

        option(AppLanguage.System).assertIsSelected()
        option(AppLanguage.French).assertIsNotSelected()

        option(AppLanguage.French).performClick()

        assertEquals(AppLanguage.French, selected.single())
        assertEquals("fr", selected.single().tag)
    }

    /** And the way back: [AppLanguage.System] is what drops the override rather than pinning one. */
    @Test
    fun givenAnOverriddenLanguage_whenSystemDefaultIsPicked_thenTheOverrideIsDropped() {
        launchSheet(selectedLanguage = AppLanguage.French)

        option(AppLanguage.System).performClick()

        assertEquals(AppLanguage.System, selected.single())
        assertEquals(null, selected.single().tag)
    }

    @Test
    fun givenAnOverriddenLanguage_whenTheSheetIsOpened_thenThatLanguageIsTheSelectedOne() {
        launchSheet(selectedLanguage = AppLanguage.French)

        option(AppLanguage.French).assertIsSelected()
        option(AppLanguage.English).assertIsNotSelected()
        option(AppLanguage.System).assertIsNotSelected()
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

    // --- The credit is still reachable, which is a licence requirement ---

    @Test
    fun givenTheSheet_whenOpened_thenTheRequiredCreditIsStillThere() {
        launchSheet()

        composeTestRule.onNodeWithText(str(R.string.about_art_attribution)).assertIsDisplayed()
    }
}
