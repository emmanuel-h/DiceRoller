// app/src/androidTest/java/fr/mandarine/diceroller/FantasyDiceArtUiTest.kt
package fr.mandarine.diceroller

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.mandarine.diceroller.domain.Dice
import fr.mandarine.diceroller.domain.DiceRoller
import fr.mandarine.diceroller.presentation.DiceRollerUiState
import fr.mandarine.diceroller.presentation.DiceRollerViewModel
import fr.mandarine.diceroller.presentation.component.ABOUT_BUTTON_TAG
import fr.mandarine.diceroller.presentation.component.ART_ATTRIBUTION
import fr.mandarine.diceroller.presentation.model.DiceColor
import fr.mandarine.diceroller.ui.theme.DiceRollerTheme
import kotlin.random.Random
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Compose UI tests for the Fantasy Dices Pack color picker as wired into [DiceRollerScreen].
 *
 * Per-die artwork rendering itself (content descriptions, decorative marking) is covered at the
 * component level by `DiceStepperChipTest` and `DiceResultDisplayTest`; this class covers the
 * color picker's integration with the rest of the screen — selection, persistence across pool
 * changes, and recoloring the result face-ladder without losing the roll.
 */
@RunWith(AndroidJUnit4::class)
class FantasyDiceArtUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun launchScreen(
        uiState: DiceRollerUiState = DiceRollerUiState(),
        darkTheme: Boolean = false,
    ) {
        composeTestRule.setContent {
            DiceRollerTheme(darkTheme = darkTheme, dynamicColor = false) {
                DiceRollerScreen(
                    uiState = uiState,
                    onIncrementCount = {},
                    onDecrementCount = {},
                    onSelectColor = {},
                    onRollDice = {},
                    onToggleHistory = {},
                )
            }
        }
    }

    private fun launchWithViewModel(
        seed: Long = 42,
        darkTheme: Boolean = false,
    ): DiceRollerViewModel {
        val viewModel = DiceRollerViewModel(diceRoller = DiceRoller(random = Random(seed)))
        composeTestRule.setContent {
            DiceRollerTheme(darkTheme = darkTheme, dynamicColor = false) {
                val uiState by viewModel.uiState.collectAsState()
                DiceRollerScreen(
                    uiState = uiState,
                    onIncrementCount = viewModel::incrementCount,
                    onDecrementCount = viewModel::decrementCount,
                    onSelectColor = viewModel::selectColor,
                    onRollDice = viewModel::rollDice,
                    onToggleHistory = viewModel::toggleHistoryExpanded,
                    onShowAbout = viewModel::showAbout,
                    onDismissAbout = viewModel::dismissAbout,
                )
            }
        }
        return viewModel
    }

    private fun increaseButton(dice: Dice) =
        composeTestRule.onNodeWithContentDescription("Increase ${dice.name} count")

    private fun swatch(color: DiceColor) =
        composeTestRule.onNodeWithContentDescription("${color.label} dice")

    /** The die art inside one result entry; unmerged, since an entry merges its own descendants. */
    private fun rowArt(dice: Dice, value: Int, color: DiceColor) = composeTestRule.onNode(
        hasTestTag("dice-row-art-${dice.name}-$value-${color.name}"),
        useUnmergedTree = true,
    )

    // -------------------------------------------------------------------------
    // Color picker
    // -------------------------------------------------------------------------

    @Test
    fun givenColorRow_whenScreenIsDisplayed_thenAllTwelveSwatchesExist() {
        launchScreen()

        DiceColor.entries.forEach { color -> swatch(color).assertExists() }
    }

    @Test
    fun givenColorRow_whenScreenIsDisplayed_thenTheSelectedSwatchIsMarkedSelected() {
        launchScreen(uiState = DiceRollerUiState(selectedColor = DiceColor.Sapphire))

        swatch(DiceColor.Sapphire).assertIsSelected()
        swatch(DiceColor.Amethyst).assertIsNotSelected()
    }

    @Test
    fun givenDefaultColor_whenIndigoSwatchIsTapped_thenIndigoBecomesSelected() {
        launchWithViewModel()

        swatch(DiceColor.Indigo).performScrollTo().performClick()

        swatch(DiceColor.Indigo).assertIsSelected()
        swatch(DiceColor.Amethyst).assertIsNotSelected()
    }

    // -------------------------------------------------------------------------
    // Recoloring a rolled result
    // -------------------------------------------------------------------------

    @Test
    fun givenARollResult_whenTheColorIsChanged_thenTheResultRowsSwitchToTheNewColor() {
        val viewModel = launchWithViewModel(seed = 7)
        increaseButton(Dice.D6).performClick()
        composeTestRule.onNodeWithText("Roll 1D6").performClick()
        val rolledValue = viewModel.uiState.value.result!!.groups.first().tallies.first().value

        swatch(DiceColor.Jade).performScrollTo().performClick()

        rowArt(Dice.D6, rolledValue, DiceColor.Jade).assertExists()
        rowArt(Dice.D6, rolledValue, DiceColor.Amethyst).assertDoesNotExist()
    }

    @Test
    fun givenARollResult_whenTheColorIsChanged_thenTheResultIsKept() {
        val viewModel = launchWithViewModel(seed = 7)
        increaseButton(Dice.D6).performClick()
        composeTestRule.onNodeWithText("Roll 1D6").performClick()
        val total = viewModel.uiState.value.result!!.total

        swatch(DiceColor.Bronze).performScrollTo().performClick()

        composeTestRule.onNodeWithText("Total $total").assertIsDisplayed()
    }

    @Test
    fun givenAColorChosen_whenADieCountIsIncremented_thenTheColorSticks() {
        launchWithViewModel()
        swatch(DiceColor.Moss).performScrollTo().performClick()

        increaseButton(Dice.D20).performClick()

        swatch(DiceColor.Moss).assertIsSelected()
    }

    // -------------------------------------------------------------------------
    // Attribution required by the CC BY 4.0 license
    // -------------------------------------------------------------------------

    /**
     * The credit left the bottom bar in issue #66, so what the license needs is no longer "it is
     * on screen" but "a user can get to it": one tap on a control that is visible at rest, with
     * the exact required wording behind it.
     */
    @Test
    fun givenTheScreen_whenTheAboutButtonIsTapped_thenTheArtworkAttributionIsVisible() {
        launchWithViewModel()

        composeTestRule.onNodeWithTag(ABOUT_BUTTON_TAG).assertIsDisplayed().performClick()

        composeTestRule.onNodeWithText(ART_ATTRIBUTION).assertIsDisplayed()
    }

    /**
     * The credit sits next to the app's own license so the two cannot be read as one, and it is
     * the *whole* of the artwork section: the license link lives inside the line rather than on a
     * row of its own, so there is deliberately no standalone `CC BY 4.0` node to find.
     */
    @Test
    fun givenTheAboutSheet_whenOpened_thenTheCreditIsOneLineBesideTheAppsOwnLicense() {
        launchWithViewModel()

        composeTestRule.onNodeWithTag(ABOUT_BUTTON_TAG).performClick()

        composeTestRule.onNodeWithText("© 2026 Mandarine Tech · Apache License 2.0")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("CC BY 4.0").assertDoesNotExist()
        composeTestRule.onNodeWithText("Fantasy Dices Pack by Aeynit").assertDoesNotExist()
    }
}
