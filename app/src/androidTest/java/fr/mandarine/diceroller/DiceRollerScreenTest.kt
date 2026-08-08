// app/src/androidTest/java/fr/mandarine/diceroller/DiceRollerScreenTest.kt
package fr.mandarine.diceroller

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.mandarine.diceroller.domain.Dice
import fr.mandarine.diceroller.domain.DiceRoller
import fr.mandarine.diceroller.presentation.DiceRollerUiState
import fr.mandarine.diceroller.presentation.DiceRollerViewModel
import fr.mandarine.diceroller.ui.theme.DiceRollerTheme
import kotlin.random.Random
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Screen-level behaviour: the dice-pool stepper row, the Roll button, and the result
 * face-ladder acting together.
 *
 * Per-component behaviour is covered standalone by `DiceStepperChipTest` and
 * `DiceResultDisplayTest`; color-picker/artwork specifics live in [FantasyDiceArtUiTest].
 */
@RunWith(AndroidJUnit4::class)
class DiceRollerScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun decreaseButton(dice: Dice) =
        composeTestRule.onNodeWithContentDescription("Decrease ${dice.name} count")

    private fun increaseButton(dice: Dice) =
        composeTestRule.onNodeWithContentDescription("Increase ${dice.name} count")

    private fun launchScreen(uiState: DiceRollerUiState = DiceRollerUiState()) {
        composeTestRule.setContent {
            DiceRollerTheme {
                DiceRollerScreen(
                    uiState = uiState,
                    onIncrementCount = {},
                    onDecrementCount = {},
                    onSelectColor = {},
                    onRollDice = {},
                )
            }
        }
    }

    private fun launchWithViewModel(seed: Long = 42): DiceRollerViewModel {
        val viewModel = DiceRollerViewModel(diceRoller = DiceRoller(random = Random(seed)))
        composeTestRule.setContent {
            DiceRollerTheme {
                val uiState by viewModel.uiState.collectAsState()
                DiceRollerScreen(
                    uiState = uiState,
                    onIncrementCount = viewModel::incrementCount,
                    onDecrementCount = viewModel::decrementCount,
                    onSelectColor = viewModel::selectColor,
                    onRollDice = viewModel::rollDice,
                )
            }
        }
        return viewModel
    }

    // --- Initial state ---

    @Test
    fun givenAppLaunch_whenScreenIsDisplayed_thenAllSixStepperChipsAreVisible() {
        launchScreen()

        Dice.entries.forEach { dice -> increaseButton(dice).assertIsDisplayed() }
    }

    @Test
    fun givenAppLaunch_whenScreenIsDisplayed_thenEveryDecreaseControlIsDisabled() {
        launchScreen()

        Dice.entries.forEach { dice -> decreaseButton(dice).assertIsNotEnabled() }
    }

    @Test
    fun givenEmptyPool_whenScreenIsDisplayed_thenRollButtonIsDisabledWithThePlaceholderLabel() {
        launchScreen()

        composeTestRule.onNodeWithText("Add dice to roll").assertIsDisplayed().assertIsNotEnabled()
    }

    @Test
    fun givenEmptyPool_whenScreenIsDisplayed_thenTheEmptyPoolCaptionIsShown() {
        launchScreen()

        composeTestRule.onNodeWithText("Add dice above to build your pool.").assertIsDisplayed()
    }

    // --- Stepper changes drive the Roll button ---

    @Test
    fun givenEmptyPool_whenD6IsIncremented_thenRollButtonShowsRoll1D6AndIsEnabled() {
        launchWithViewModel()

        increaseButton(Dice.D6).performClick()

        composeTestRule.onNodeWithText("Roll 1D6").assertIsDisplayed().assertIsEnabled()
    }

    @Test
    fun givenAMixedPool_whenBothCountsAreIncremented_thenRollButtonListsThemSmallestToLargest() {
        launchWithViewModel()

        increaseButton(Dice.D8).performClick()
        increaseButton(Dice.D6).performClick()

        composeTestRule.onNodeWithText("Roll 1D6 + 1D8").assertIsDisplayed()
    }

    @Test
    fun givenANonEmptyPool_whenTheDieIsDecrementedBackToZero_thenRollButtonReturnsToThePlaceholder() {
        launchWithViewModel()
        increaseButton(Dice.D6).performClick()

        decreaseButton(Dice.D6).performClick()

        composeTestRule.onNodeWithText("Add dice to roll").assertIsDisplayed().assertIsNotEnabled()
    }

    // --- Rolling produces a result ---

    @Test
    fun givenANonEmptyPool_whenRollButtonIsTapped_thenTheNotRolledCaptionIsGone() {
        launchWithViewModel(seed = 1)
        increaseButton(Dice.D6).performClick()

        composeTestRule.onNodeWithText("Roll 1D6").performClick()

        composeTestRule.onNodeWithText("Tap Roll to see results.").assertDoesNotExist()
    }

    @Test
    fun givenANonEmptyPool_whenRollButtonIsTapped_thenAGroupHeaderAndTotalLineAppear() {
        val viewModel = launchWithViewModel(seed = 1)
        increaseButton(Dice.D6).performClick()

        composeTestRule.onNodeWithText("Roll 1D6").performClick()

        composeTestRule.onNodeWithText("1×D6").assertIsDisplayed()
        val total = viewModel.uiState.value.result!!.total
        composeTestRule.onNodeWithText("Total $total").assertIsDisplayed()
    }

    @Test
    fun givenARollResult_whenTheCountIsChangedAgain_thenTheResultIsCleared() {
        val viewModel = launchWithViewModel(seed = 1)
        increaseButton(Dice.D6).performClick()
        composeTestRule.onNodeWithText("Roll 1D6").performClick()
        assert(viewModel.uiState.value.result != null)

        increaseButton(Dice.D6).performClick()

        composeTestRule.onNodeWithText("Tap Roll to see results.").assertIsDisplayed()
    }
}
