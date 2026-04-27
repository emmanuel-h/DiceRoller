// app/src/androidTest/java/fr/mandarine/diceroller/VisualDiceUiTest.kt
package fr.mandarine.diceroller

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
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
 * Compose UI tests for the visual-dice feature.
 *
 * Covers the die-shape selector row interaction states, the result display
 * area (including D6 pip display and the empty state), and the behavior
 * of switching die types.
 *
 * Tests run in both light and dark theme variants via [launchScreenLight] and
 * [launchScreenDark] / [launchWithViewModelDark].
 */
@RunWith(AndroidJUnit4::class)
class VisualDiceUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun launchScreenLight(uiState: DiceRollerUiState = DiceRollerUiState()) {
        composeTestRule.setContent {
            DiceRollerTheme(darkTheme = false, dynamicColor = false) {
                DiceRollerScreen(
                    uiState = uiState,
                    onSelectDice = {},
                    onRollDice = {},
                )
            }
        }
    }

    private fun launchScreenDark(uiState: DiceRollerUiState = DiceRollerUiState()) {
        composeTestRule.setContent {
            DiceRollerTheme(darkTheme = true, dynamicColor = false) {
                DiceRollerScreen(
                    uiState = uiState,
                    onSelectDice = {},
                    onRollDice = {},
                )
            }
        }
    }

    private fun launchWithViewModelLight(seed: Long = 42): DiceRollerViewModel {
        val viewModel = DiceRollerViewModel(diceRoller = DiceRoller(random = Random(seed)))
        composeTestRule.setContent {
            DiceRollerTheme(darkTheme = false, dynamicColor = false) {
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

    private fun launchWithViewModelDark(seed: Long = 42): DiceRollerViewModel {
        val viewModel = DiceRollerViewModel(diceRoller = DiceRoller(random = Random(seed)))
        composeTestRule.setContent {
            DiceRollerTheme(darkTheme = true, dynamicColor = false) {
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


    // -------------------------------------------------------------------------
    // Die selector: RadioButton role and initial selection
    // -------------------------------------------------------------------------

    @Test
    fun givenDefaultState_whenSelectorRowIsRendered_thenAllFiveDieSelectorNodesAreDisplayed() {
        launchScreenLight()

        // Verify all five chip content descriptions are present and displayed
        listOf("D4", "D6", "D8", "D12", "D20").forEach { diceName ->
            composeTestRule
                .onNodeWithContentDescription("Select $diceName")
                .assertIsDisplayed()
        }
    }

    @Test
    fun givenDefaultState_whenSelectorRowIsRendered_thenD6SelectorIsMarkedSelected() {
        launchScreenLight()

        composeTestRule
            .onNodeWithContentDescription("Select D6")
            .assertIsSelected()
    }

    @Test
    fun givenDefaultState_whenSelectorRowIsRendered_thenD4SelectorIsMarkedNotSelected() {
        launchScreenLight()

        composeTestRule
            .onNodeWithContentDescription("Select D4")
            .assertIsNotSelected()
    }

    @Test
    fun givenDefaultState_whenSelectorRowIsRendered_thenD8SelectorIsMarkedNotSelected() {
        launchScreenLight()

        composeTestRule
            .onNodeWithContentDescription("Select D8")
            .assertIsNotSelected()
    }

    @Test
    fun givenDefaultState_whenSelectorRowIsRendered_thenD12SelectorIsMarkedNotSelected() {
        launchScreenLight()

        composeTestRule
            .onNodeWithContentDescription("Select D12")
            .assertIsNotSelected()
    }

    @Test
    fun givenDefaultState_whenSelectorRowIsRendered_thenD20SelectorIsMarkedNotSelected() {
        launchScreenLight()

        composeTestRule
            .onNodeWithContentDescription("Select D20")
            .assertIsNotSelected()
    }

    // -------------------------------------------------------------------------
    // Die selector: RadioButton role and initial selection — dark theme
    // -------------------------------------------------------------------------

    @Test
    fun givenDefaultStateDarkTheme_whenSelectorRowIsRendered_thenD6SelectorIsMarkedSelected() {
        launchScreenDark()

        composeTestRule
            .onNodeWithContentDescription("Select D6")
            .assertIsSelected()
    }

    @Test
    fun givenDefaultStateDarkTheme_whenSelectorRowIsRendered_thenNonD6SelectorsAreMarkedNotSelected() {
        launchScreenDark()

        listOf("D4", "D8", "D12", "D20").forEach { diceName ->
            composeTestRule
                .onNodeWithContentDescription("Select $diceName")
                .assertIsNotSelected()
        }
    }

    // -------------------------------------------------------------------------
    // Die selector: tapping a selector changes the selected die
    // -------------------------------------------------------------------------

    @Test
    fun givenD6SelectedByDefault_whenD4SelectorIsTapped_thenD4SelectorBecomesSelected() {
        launchWithViewModelLight()

        composeTestRule.onNodeWithContentDescription("Select D4").performClick()

        composeTestRule
            .onNodeWithContentDescription("Select D4")
            .assertIsSelected()
    }

    @Test
    fun givenD6SelectedByDefault_whenD4SelectorIsTapped_thenD6SelectorBecomesNotSelected() {
        launchWithViewModelLight()

        composeTestRule.onNodeWithContentDescription("Select D4").performClick()

        composeTestRule
            .onNodeWithContentDescription("Select D6")
            .assertIsNotSelected()
    }

    @Test
    fun givenD6SelectedByDefault_whenD8SelectorIsTapped_thenD8SelectorBecomesSelected() {
        launchWithViewModelLight()

        composeTestRule.onNodeWithContentDescription("Select D8").performClick()

        composeTestRule
            .onNodeWithContentDescription("Select D8")
            .assertIsSelected()
    }

    @Test
    fun givenD6SelectedByDefault_whenD12SelectorIsTapped_thenD12SelectorBecomesSelected() {
        launchWithViewModelLight()

        composeTestRule.onNodeWithContentDescription("Select D12").performClick()

        composeTestRule
            .onNodeWithContentDescription("Select D12")
            .assertIsSelected()
    }

    @Test
    fun givenD6SelectedByDefault_whenD20SelectorIsTapped_thenD20SelectorBecomesSelected() {
        launchWithViewModelLight()

        composeTestRule.onNodeWithContentDescription("Select D20").performClick()

        composeTestRule
            .onNodeWithContentDescription("Select D20")
            .assertIsSelected()
    }

    @Test
    fun givenD20Selected_whenD4SelectorIsTapped_thenD4SelectorBecomesSelectedAndD20BecomesNotSelected() {
        launchWithViewModelLight()
        composeTestRule.onNodeWithContentDescription("Select D20").performClick()

        composeTestRule.onNodeWithContentDescription("Select D4").performClick()

        composeTestRule.onNodeWithContentDescription("Select D4").assertIsSelected()
        composeTestRule.onNodeWithContentDescription("Select D20").assertIsNotSelected()
    }

    // -------------------------------------------------------------------------
    // Die selector: tapping a selector changes the selected die — dark theme
    // -------------------------------------------------------------------------

    @Test
    fun givenD6SelectedByDefaultDarkTheme_whenD20SelectorIsTapped_thenD20SelectorBecomesSelected() {
        launchWithViewModelDark()

        composeTestRule.onNodeWithContentDescription("Select D20").performClick()

        composeTestRule
            .onNodeWithContentDescription("Select D20")
            .assertIsSelected()
    }

    // -------------------------------------------------------------------------
    // Result display: empty state (no roll yet)
    // -------------------------------------------------------------------------

    @Test
    fun givenNoRollPerformed_whenScreenIsDisplayed_thenResultAreaShowsDashPlaceholder() {
        launchScreenLight()

        composeTestRule.onNodeWithText("–").assertIsDisplayed()
    }

    @Test
    fun givenNoRollPerformedDarkTheme_whenScreenIsDisplayed_thenResultAreaShowsDashPlaceholder() {
        launchScreenDark()

        composeTestRule.onNodeWithText("–").assertIsDisplayed()
    }

    @Test
    fun givenUiStateWithNullResult_whenScreenIsDisplayed_thenDashPlaceholderIsShown() {
        launchScreenLight(uiState = DiceRollerUiState(selectedDice = Dice.D12, result = null))

        composeTestRule.onNodeWithText("–").assertIsDisplayed()
    }

    // -------------------------------------------------------------------------
    // Result display: non-D6 roll shows numeric Text
    // -------------------------------------------------------------------------

    @Test
    fun givenD4Selected_whenRolled_thenResultAreaShowsNumericText() {
        val viewModel = launchWithViewModelLight(seed = 0)
        composeTestRule.onNodeWithContentDescription("Select D4").performClick()

        composeTestRule.onNodeWithText("Roll D4").performClick()

        val result = viewModel.uiState.value.result!!
        composeTestRule.onNodeWithText(result.toString()).assertIsDisplayed()
    }

    @Test
    fun givenD8Selected_whenRolled_thenResultAreaShowsNumericText() {
        val viewModel = launchWithViewModelLight(seed = 7)
        composeTestRule.onNodeWithContentDescription("Select D8").performClick()

        composeTestRule.onNodeWithText("Roll D8").performClick()

        val result = viewModel.uiState.value.result!!
        composeTestRule.onNodeWithText(result.toString()).assertIsDisplayed()
    }

    @Test
    fun givenD12Selected_whenRolled_thenResultAreaShowsNumericText() {
        val viewModel = launchWithViewModelLight(seed = 3)
        composeTestRule.onNodeWithContentDescription("Select D12").performClick()

        composeTestRule.onNodeWithText("Roll D12").performClick()

        val result = viewModel.uiState.value.result!!
        composeTestRule.onNodeWithText(result.toString()).assertIsDisplayed()
    }

    @Test
    fun givenD20Selected_whenRolled_thenResultAreaShowsNumericText() {
        val viewModel = launchWithViewModelLight(seed = 5)
        composeTestRule.onNodeWithContentDescription("Select D20").performClick()

        composeTestRule.onNodeWithText("Roll D20").performClick()

        val result = viewModel.uiState.value.result!!
        composeTestRule.onNodeWithText(result.toString()).assertIsDisplayed()
    }

    @Test
    fun givenNonD6ResultViaUiState_whenScreenIsDisplayed_thenResultNumberIsShown() {
        launchScreenLight(uiState = DiceRollerUiState(selectedDice = Dice.D20, result = 17))

        composeTestRule.onNodeWithText("17").assertIsDisplayed()
    }

    @Test
    fun givenNonD6ResultViaUiState_whenScreenIsDisplayed_thenDashPlaceholderIsNotShown() {
        launchScreenLight(uiState = DiceRollerUiState(selectedDice = Dice.D20, result = 17))

        composeTestRule.onNodeWithText("–").assertDoesNotExist()
    }

    @Test
    fun givenNonD6ResultViaUiStateDarkTheme_whenScreenIsDisplayed_thenResultNumberIsShown() {
        launchScreenDark(uiState = DiceRollerUiState(selectedDice = Dice.D12, result = 11))

        composeTestRule.onNodeWithText("11").assertIsDisplayed()
    }

    // -------------------------------------------------------------------------
    // Result display: D6 roll shows D6Pips (no numeric Text in result area)
    // -------------------------------------------------------------------------

    @Test
    fun givenD6ResultViaUiState_whenScreenIsDisplayed_thenDashPlaceholderIsNotShown() {
        launchScreenLight(uiState = DiceRollerUiState(selectedDice = Dice.D6, result = 3))

        composeTestRule.onNodeWithText("–").assertDoesNotExist()
    }

    @Test
    fun givenD6Result1ViaUiState_whenScreenIsDisplayed_thenNumericTextForResultIsAbsent() {
        launchScreenLight(uiState = DiceRollerUiState(selectedDice = Dice.D6, result = 1))

        composeTestRule.onNodeWithText("1").assertDoesNotExist()
    }

    @Test
    fun givenD6Result2ViaUiState_whenScreenIsDisplayed_thenNumericTextForResultIsAbsent() {
        launchScreenLight(uiState = DiceRollerUiState(selectedDice = Dice.D6, result = 2))

        composeTestRule.onNodeWithText("2").assertDoesNotExist()
    }

    @Test
    fun givenD6Result3ViaUiState_whenScreenIsDisplayed_thenNumericTextForResultIsAbsent() {
        launchScreenLight(uiState = DiceRollerUiState(selectedDice = Dice.D6, result = 3))

        composeTestRule.onNodeWithText("3").assertDoesNotExist()
    }

    @Test
    fun givenD6Result4ViaUiState_whenScreenIsDisplayed_thenNumericTextForResultIsAbsent() {
        launchScreenLight(uiState = DiceRollerUiState(selectedDice = Dice.D6, result = 4))

        composeTestRule.onNodeWithText("4").assertDoesNotExist()
    }

    @Test
    fun givenD6Result5ViaUiState_whenScreenIsDisplayed_thenNumericTextForResultIsAbsent() {
        launchScreenLight(uiState = DiceRollerUiState(selectedDice = Dice.D6, result = 5))

        composeTestRule.onNodeWithText("5").assertDoesNotExist()
    }

    @Test
    fun givenD6Result6ViaUiState_whenScreenIsDisplayed_thenNumericTextForResultIsAbsent() {
        launchScreenLight(uiState = DiceRollerUiState(selectedDice = Dice.D6, result = 6))

        composeTestRule.onNodeWithText("6").assertDoesNotExist()
    }

    @Test
    fun givenD6ResultDarkTheme_whenScreenIsDisplayed_thenDashPlaceholderIsNotShown() {
        launchScreenDark(uiState = DiceRollerUiState(selectedDice = Dice.D6, result = 4))

        composeTestRule.onNodeWithText("–").assertDoesNotExist()
    }

    @Test
    fun givenD6ResultDarkTheme_whenScreenIsDisplayed_thenNumericTextForResultIsAbsent() {
        launchScreenDark(uiState = DiceRollerUiState(selectedDice = Dice.D6, result = 4))

        composeTestRule.onNodeWithText("4").assertDoesNotExist()
    }

    // -------------------------------------------------------------------------
    // Switching die type clears the previous result (shows dash again)
    // -------------------------------------------------------------------------

    @Test
    fun givenD6RolledWithResult_whenD20SelectorIsTapped_thenResultIsCleared() {
        launchWithViewModelLight(seed = 42)
        composeTestRule.onNodeWithText("Roll D6").performClick()
        composeTestRule.onNodeWithText("–").assertDoesNotExist()

        composeTestRule.onNodeWithContentDescription("Select D20").performClick()

        composeTestRule.onNodeWithText("–").assertIsDisplayed()
    }

    @Test
    fun givenD4RolledWithResult_whenD8SelectorIsTapped_thenResultIsCleared() {
        launchWithViewModelLight(seed = 11)
        composeTestRule.onNodeWithContentDescription("Select D4").performClick()
        composeTestRule.onNodeWithText("Roll D4").performClick()
        composeTestRule.onNodeWithText("–").assertDoesNotExist()

        composeTestRule.onNodeWithContentDescription("Select D8").performClick()

        composeTestRule.onNodeWithText("–").assertIsDisplayed()
    }

    @Test
    fun givenD12RolledWithResult_whenD4SelectorIsTapped_thenD4IsSelectedAndDashIsShown() {
        launchWithViewModelLight(seed = 7)
        composeTestRule.onNodeWithContentDescription("Select D12").performClick()
        composeTestRule.onNodeWithText("Roll D12").performClick()

        composeTestRule.onNodeWithContentDescription("Select D4").performClick()

        composeTestRule.onNodeWithContentDescription("Select D4").assertIsSelected()
        composeTestRule.onNodeWithText("–").assertIsDisplayed()
    }

    @Test
    fun givenResultDisplayed_whenSameDieIsTappedAgain_thenResultIsNotCleared() {
        val viewModel = launchWithViewModelLight(seed = 42)
        composeTestRule.onNodeWithText("Roll D6").performClick()
        val firstResult = viewModel.uiState.value.result!!

        composeTestRule.onNodeWithContentDescription("Select D6").performClick()

        val stateAfter = viewModel.uiState.value.result
        assert(stateAfter == firstResult) {
            "Expected result to be unchanged ($firstResult) but got $stateAfter"
        }
    }

    @Test
    fun givenD20RolledWithResultDarkTheme_whenD6SelectorIsTapped_thenDashIsShown() {
        launchWithViewModelDark(seed = 3)
        composeTestRule.onNodeWithContentDescription("Select D20").performClick()
        composeTestRule.onNodeWithText("Roll D20").performClick()
        composeTestRule.onNodeWithText("–").assertDoesNotExist()

        composeTestRule.onNodeWithContentDescription("Select D6").performClick()

        composeTestRule.onNodeWithText("–").assertIsDisplayed()
    }

    // -------------------------------------------------------------------------
    // Accessibility: liveRegion on result area
    // -------------------------------------------------------------------------

    @Test
    fun givenResultArea_whenScreenIsDisplayed_thenResultNodeHasLiveRegionSemantics() {
        launchScreenLight()

        composeTestRule
            .onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.LiveRegion))
            .assertIsDisplayed()
    }

    @Test
    fun givenResultAreaDarkTheme_whenScreenIsDisplayed_thenResultNodeHasLiveRegionSemantics() {
        launchScreenDark()

        composeTestRule
            .onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.LiveRegion))
            .assertIsDisplayed()
    }

    // -------------------------------------------------------------------------
    // Roll button label tracks selected die
    // -------------------------------------------------------------------------

    @Test
    fun givenD4Selected_whenScreenIsDisplayed_thenRollButtonLabelShowsD4() {
        launchScreenLight(uiState = DiceRollerUiState(selectedDice = Dice.D4))

        composeTestRule.onNodeWithText("Roll D4").assertIsDisplayed()
    }

    @Test
    fun givenD8Selected_whenScreenIsDisplayed_thenRollButtonLabelShowsD8() {
        launchScreenLight(uiState = DiceRollerUiState(selectedDice = Dice.D8))

        composeTestRule.onNodeWithText("Roll D8").assertIsDisplayed()
    }

    @Test
    fun givenD12Selected_whenScreenIsDisplayed_thenRollButtonLabelShowsD12() {
        launchScreenLight(uiState = DiceRollerUiState(selectedDice = Dice.D12))

        composeTestRule.onNodeWithText("Roll D12").assertIsDisplayed()
    }

    @Test
    fun givenD20Selected_whenScreenIsDisplayed_thenRollButtonLabelShowsD20() {
        launchScreenLight(uiState = DiceRollerUiState(selectedDice = Dice.D20))

        composeTestRule.onNodeWithText("Roll D20").assertIsDisplayed()
    }

    @Test
    fun givenD6SelectedByDefault_whenD20Tapped_thenRollButtonLabelUpdatesToD20() {
        launchWithViewModelLight()

        composeTestRule.onNodeWithContentDescription("Select D20").performClick()

        composeTestRule.onNodeWithText("Roll D20").assertIsDisplayed()
    }
}
