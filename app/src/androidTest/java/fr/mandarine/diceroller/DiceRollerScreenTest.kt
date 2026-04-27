// app/src/androidTest/java/fr/mandarine/diceroller/DiceRollerScreenTest.kt
package fr.mandarine.diceroller

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertIsNotSelected
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
 * Compose UI tests for the main DiceRollerScreen.
 *
 * Die selector chips are now image-only (no text label) — nodes are located
 * via contentDescription ("Select D4", "Select D6", etc.) rather than text.
 */
@RunWith(AndroidJUnit4::class)
class DiceRollerScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun launchScreen(uiState: DiceRollerUiState = DiceRollerUiState()) {
        composeTestRule.setContent {
            DiceRollerTheme {
                DiceRollerScreen(
                    uiState = uiState,
                    onSelectDice = {},
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
                    onSelectDice = viewModel::selectDice,
                    onRollDice = viewModel::rollDice,
                )
            }
        }
        return viewModel
    }

    // --- Initial state ---

    @Test
    fun givenAppLaunch_whenScreenIsDisplayed_thenD6ChipIsSelected() {
        launchScreen()

        composeTestRule.onNodeWithContentDescription("Select D6").assertIsSelected()
    }

    @Test
    fun givenAppLaunch_whenScreenIsDisplayed_thenD4ChipIsNotSelected() {
        launchScreen()

        composeTestRule.onNodeWithContentDescription("Select D4").assertIsNotSelected()
    }

    @Test
    fun givenAppLaunch_whenScreenIsDisplayed_thenAllFiveDiceChipsAreVisible() {
        launchScreen()

        Dice.entries.forEach { dice ->
            composeTestRule
                .onNodeWithContentDescription("Select ${dice.name}")
                .assertIsDisplayed()
        }
    }

    @Test
    fun givenNoRollPerformed_whenScreenIsDisplayed_thenPlaceholderDashIsShown() {
        launchScreen()

        composeTestRule.onNodeWithText("–").assertIsDisplayed()
    }

    @Test
    fun givenNoRollPerformed_whenScreenIsDisplayed_thenRollButtonShowsD6() {
        launchScreen()

        composeTestRule.onNodeWithText("Roll D6").assertIsDisplayed()
    }

    // --- Chip selection changes state ---

    @Test
    fun givenD6SelectedByDefault_whenD20ChipIsTapped_thenD20ChipBecomesSelected() {
        launchWithViewModel()

        composeTestRule.onNodeWithContentDescription("Select D20").performClick()

        composeTestRule.onNodeWithContentDescription("Select D20").assertIsSelected()
    }

    @Test
    fun givenD6SelectedByDefault_whenD20ChipIsTapped_thenD6ChipBecomesDeselected() {
        launchWithViewModel()

        composeTestRule.onNodeWithContentDescription("Select D20").performClick()

        composeTestRule.onNodeWithContentDescription("Select D6").assertIsNotSelected()
    }

    @Test
    fun givenD6SelectedByDefault_whenD4ChipIsTapped_thenRollButtonUpdatesToD4() {
        launchWithViewModel()

        composeTestRule.onNodeWithContentDescription("Select D4").performClick()

        composeTestRule.onNodeWithText("Roll D4").assertIsDisplayed()
    }

    @Test
    fun givenD6SelectedByDefault_whenD12ChipIsTapped_thenRollButtonUpdatesToD12() {
        launchWithViewModel()

        composeTestRule.onNodeWithContentDescription("Select D12").performClick()

        composeTestRule.onNodeWithText("Roll D12").assertIsDisplayed()
    }

    // --- Roll button produces result ---

    @Test
    fun givenPlaceholderShown_whenRollButtonIsTapped_thenANumberIsDisplayed() {
        launchWithViewModel()

        composeTestRule.onNodeWithText("Roll D6").performClick()

        composeTestRule.onNodeWithText("–").assertDoesNotExist()
    }

    @Test
    fun givenRollResultDisplayed_whenRollButtonIsTappedAgain_thenPreviousResultIsReplaced() {
        // seed=1 gives two different consecutive D6 values
        val vm = launchWithViewModel(seed = 1)

        composeTestRule.onNodeWithText("Roll D6").performClick()
        val firstResult = vm.uiState.value.result!!.toString()

        composeTestRule.onNodeWithText("Roll D6").performClick()
        val secondResult = vm.uiState.value.result!!.toString()

        composeTestRule.onNodeWithText(secondResult).assertIsDisplayed()
        composeTestRule.onNodeWithText(firstResult).assertDoesNotExist()
    }

    // --- Result state passed via uiState parameter ---

    @Test
    fun givenUiStateWithResult_whenScreenIsDisplayed_thenResultNumberIsShown() {
        launchScreen(uiState = DiceRollerUiState(selectedDice = Dice.D6, result = 5))

        composeTestRule.onNodeWithText("5").assertIsDisplayed()
    }

    @Test
    fun givenUiStateWithResult_whenScreenIsDisplayed_thenPlaceholderDashIsNotShown() {
        launchScreen(uiState = DiceRollerUiState(selectedDice = Dice.D6, result = 5))

        composeTestRule.onNodeWithText("–").assertDoesNotExist()
    }

    @Test
    fun givenUiStateWithD20Selected_whenScreenIsDisplayed_thenRollButtonShowsD20() {
        launchScreen(uiState = DiceRollerUiState(selectedDice = Dice.D20))

        composeTestRule.onNodeWithText("Roll D20").assertIsDisplayed()
    }
}
