// app/src/androidTest/java/fr/mandarine/diceroller/FantasyDiceArtUiTest.kt
package fr.mandarine.diceroller

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.mandarine.diceroller.domain.Dice
import fr.mandarine.diceroller.domain.DiceRoller
import fr.mandarine.diceroller.presentation.DiceRollerUiState
import fr.mandarine.diceroller.presentation.DiceRollerViewModel
import fr.mandarine.diceroller.presentation.model.DiceColor
import fr.mandarine.diceroller.ui.theme.DiceRollerTheme
import kotlin.random.Random
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Compose UI tests for the Fantasy Dices Pack artwork and the color picker.
 *
 * The die artwork is decorative — the rolled value is never painted on it — so
 * these tests assert on the artwork's content description (`"D20, amethyst"`)
 * to tell which variant is on screen, and on the separate result number below.
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
                    onSelectDice = {},
                    onSelectColor = {},
                    onRollDice = {},
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
                    onSelectDice = viewModel::selectDice,
                    onSelectColor = viewModel::selectColor,
                    onRollDice = viewModel::rollDice,
                )
            }
        }
        return viewModel
    }

    /** The result-area artwork for [dice] in [color], e.g. `"D20, ruby"`. */
    private fun resultArt(dice: Dice, color: DiceColor) =
        composeTestRule.onNodeWithContentDescription("${dice.name}, ${color.label}")

    private fun swatch(color: DiceColor) =
        composeTestRule.onNodeWithContentDescription("${color.label} dice")

    // -------------------------------------------------------------------------
    // Die selector: all six dice render as artwork
    // -------------------------------------------------------------------------

    @Test
    fun givenDefaultState_whenSelectorIsRendered_thenAllSixDiceAreShown() {
        launchScreen()

        Dice.entries.forEach { dice ->
            composeTestRule
                .onNodeWithContentDescription("Select ${dice.name}")
                .assertIsDisplayed()
        }
    }

    @Test
    fun givenDefaultState_whenSelectorIsRendered_thenExactlySixDiceChipsExist() {
        launchScreen()

        composeTestRule
            .onAllNodesWithContentDescription("Select ", substring = true)
            .assertCountEquals(6)
    }

    @Test
    fun givenDefaultState_whenSelectorIsRendered_thenD10IsSelectable() {
        launchWithViewModel()

        composeTestRule.onNodeWithContentDescription("Select D10").performClick()

        composeTestRule.onNodeWithContentDescription("Select D10").assertIsSelected()
        composeTestRule.onNodeWithContentDescription("Select D6").assertIsNotSelected()
    }

    // -------------------------------------------------------------------------
    // Artwork content descriptions
    // -------------------------------------------------------------------------

    @Test
    fun givenDefaultState_whenScreenIsDisplayed_thenResultArtIsDescribedAsDieAndColor() {
        launchScreen()

        resultArt(Dice.D6, DiceColor.Default).assertIsDisplayed()
    }

    @Test
    fun givenD20AndRuby_whenScreenIsDisplayed_thenResultArtDescriptionMatchesBoth() {
        launchScreen(
            uiState = DiceRollerUiState(
                selectedDice = Dice.D20,
                selectedColor = DiceColor.Ruby,
                result = 17,
            ),
        )

        resultArt(Dice.D20, DiceColor.Ruby).assertIsDisplayed()
    }

    @Test
    fun givenD10AndJade_whenScreenIsDisplayed_thenResultArtDescriptionMatchesBoth() {
        launchScreen(
            uiState = DiceRollerUiState(
                selectedDice = Dice.D10,
                selectedColor = DiceColor.Jade,
                result = 7,
            ),
        )

        resultArt(Dice.D10, DiceColor.Jade).assertIsDisplayed()
    }

    // -------------------------------------------------------------------------
    // Color picker
    // -------------------------------------------------------------------------

    @Test
    fun givenColorRow_whenScreenIsDisplayed_thenAllTwelveSwatchesExist() {
        launchScreen()

        DiceColor.entries.forEach { color ->
            swatch(color).assertExists()
        }
    }

    @Test
    fun givenColorRow_whenScreenIsDisplayed_thenTheSelectedSwatchIsMarkedSelected() {
        launchScreen(uiState = DiceRollerUiState(selectedColor = DiceColor.Sapphire))

        swatch(DiceColor.Sapphire).assertIsSelected()
        swatch(DiceColor.Amethyst).assertIsNotSelected()
    }

    @Test
    fun givenDefaultColor_whenJadeSwatchIsTapped_thenTheResultArtSwitchesToJade() {
        launchWithViewModel()
        resultArt(Dice.D6, DiceColor.Amethyst).assertIsDisplayed()

        swatch(DiceColor.Jade).performScrollTo().performClick()

        resultArt(Dice.D6, DiceColor.Jade).assertIsDisplayed()
        resultArt(Dice.D6, DiceColor.Amethyst).assertDoesNotExist()
    }

    @Test
    fun givenDefaultColor_whenIndigoSwatchIsTapped_thenIndigoBecomesSelected() {
        launchWithViewModel()

        swatch(DiceColor.Indigo).performScrollTo().performClick()

        swatch(DiceColor.Indigo).assertIsSelected()
        swatch(DiceColor.Amethyst).assertIsNotSelected()
    }

    @Test
    fun givenEveryColor_whenTapped_thenTheResultArtFollows() {
        launchWithViewModel()

        DiceColor.entries.forEach { color ->
            swatch(color).performScrollTo().performClick()
            resultArt(Dice.D6, color).assertIsDisplayed()
        }
    }

    @Test
    fun givenARollResult_whenTheColorIsChanged_thenTheResultIsKept() {
        val viewModel = launchWithViewModel()
        composeTestRule.onNodeWithText("Roll D6").performClick()
        val result = viewModel.uiState.value.result!!.toString()

        swatch(DiceColor.Bronze).performScrollTo().performClick()

        composeTestRule.onNodeWithText(result).assertIsDisplayed()
        composeTestRule.onNodeWithText("–").assertDoesNotExist()
    }

    @Test
    fun givenAColorChosen_whenADifferentDieIsSelected_thenTheColorSticks() {
        launchWithViewModel()
        swatch(DiceColor.Moss).performScrollTo().performClick()

        composeTestRule.onNodeWithContentDescription("Select D20").performClick()

        resultArt(Dice.D20, DiceColor.Moss).assertIsDisplayed()
        swatch(DiceColor.Moss).assertIsSelected()
    }

    // -------------------------------------------------------------------------
    // Result readout
    // -------------------------------------------------------------------------

    @Test
    fun givenNoRollYet_whenScreenIsDisplayed_thenTheDashPlaceholderIsShown() {
        launchScreen()

        composeTestRule.onNodeWithText("–").assertIsDisplayed()
    }

    @Test
    fun givenNoRollYetInDarkTheme_whenScreenIsDisplayed_thenTheDashPlaceholderIsShown() {
        launchScreen(darkTheme = true)

        composeTestRule.onNodeWithText("–").assertIsDisplayed()
    }

    @Test
    fun givenD6Rolled_whenScreenIsDisplayed_thenTheNumberIsShownAlongsideTheArt() {
        // The pack art has its numerals baked in, so the rolled value renders as
        // its own Text node below the die rather than on the face.
        launchScreen(
            uiState = DiceRollerUiState(
                selectedDice = Dice.D6,
                selectedColor = DiceColor.Gold,
                result = 3,
            ),
        )

        composeTestRule.onNodeWithText("3").assertIsDisplayed()
        resultArt(Dice.D6, DiceColor.Gold).assertIsDisplayed()
    }

    @Test
    fun givenAResultInDarkTheme_whenScreenIsDisplayed_thenTheNumberIsShown() {
        launchScreen(
            uiState = DiceRollerUiState(
                selectedDice = Dice.D12,
                selectedColor = DiceColor.Smoke,
                result = 11,
            ),
            darkTheme = true,
        )

        composeTestRule.onNodeWithText("11").assertIsDisplayed()
        resultArt(Dice.D12, DiceColor.Smoke).assertIsDisplayed()
    }

    @Test
    fun givenTheResultArea_whenScreenIsDisplayed_thenItCarriesLiveRegionSemantics() {
        launchScreen()

        composeTestRule
            .onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.LiveRegion))
            .assertIsDisplayed()
    }

    // -------------------------------------------------------------------------
    // Attribution required by the CC BY 4.0 license
    // -------------------------------------------------------------------------

    @Test
    fun givenTheScreen_whenDisplayed_thenTheArtworkAttributionIsVisible() {
        launchScreen()

        composeTestRule.onNodeWithText("Dice art by Aeynit · CC BY 4.0").assertIsDisplayed()
    }
}
