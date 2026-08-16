// app/src/androidTest/java/fr/mandarine/diceroller/CustomDiceUiTest.kt
package fr.mandarine.diceroller

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.mandarine.diceroller.domain.CustomDie
import fr.mandarine.diceroller.domain.Dice
import fr.mandarine.diceroller.domain.DiceRoller
import fr.mandarine.diceroller.domain.DieType
import fr.mandarine.diceroller.presentation.DiceRollerViewModel
import fr.mandarine.diceroller.presentation.InMemoryCustomDiceStore
import fr.mandarine.diceroller.presentation.MAX_CUSTOM_DICE
import fr.mandarine.diceroller.presentation.component.ADD_DICE_CHIP_TAG
import fr.mandarine.diceroller.presentation.component.CLEAR_POOL_BUTTON_TAG
import fr.mandarine.diceroller.presentation.component.CUSTOM_DIE_ADD_BUTTON_TAG
import fr.mandarine.diceroller.presentation.component.CUSTOM_FACES_FIELD_TAG
import fr.mandarine.diceroller.presentation.component.CUSTOM_FACES_MESSAGE_TAG
import fr.mandarine.diceroller.presentation.component.chipCountTestTag
import fr.mandarine.diceroller.presentation.component.chipRemoveTestTag
import fr.mandarine.diceroller.ui.theme.DiceRollerTheme
import kotlin.random.Random
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Custom dice (issue #4) end to end, driven through a real [DiceRollerViewModel] rather than
 * hand-fed [fr.mandarine.diceroller.presentation.DiceRollerUiState]s — the feature's whole point is
 * that a die the user *creates* then behaves like a preset, so the creation has to be real for the
 * assertions after it to mean anything.
 */
@RunWith(AndroidJUnit4::class)
class CustomDiceUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun launch(
        seed: Long = 42,
        initialDice: List<CustomDie> = emptyList(),
    ): DiceRollerViewModel {
        val viewModel = DiceRollerViewModel(
            diceRoller = DiceRoller(random = Random(seed)),
            customDiceStore = InMemoryCustomDiceStore(initialDice),
        )
        composeTestRule.setContent {
            DiceRollerTheme {
                val uiState by viewModel.uiState.collectAsState()
                DiceRollerScreen(
                    uiState = uiState,
                    onIncrementCount = viewModel::incrementCount,
                    onDecrementCount = viewModel::decrementCount,
                    onSelectColor = viewModel::selectColor,
                    onRollDice = viewModel::rollDice,
                    onToggleHistory = viewModel::toggleHistoryExpanded,
                    onShowCustomDieCreator = viewModel::showCustomDieCreator,
                    onDismissCustomDieCreator = viewModel::dismissCustomDieCreator,
                    onAddCustomDie = viewModel::addCustomDie,
                    onRemoveCustomDie = viewModel::removeCustomDie,
                    onUndoRemoveCustomDie = viewModel::undoRemoveCustomDie,
                    onDismissRemovedCustomDie = viewModel::dismissRemovedCustomDie,
                    onClearPool = viewModel::clearPool,
                )
            }
        }
        return viewModel
    }

    private fun addChip() = composeTestRule.onNodeWithTag(ADD_DICE_CHIP_TAG)
    private fun facesField() = composeTestRule.onNodeWithTag(CUSTOM_FACES_FIELD_TAG)
    private fun addButton() = composeTestRule.onNodeWithTag(CUSTOM_DIE_ADD_BUTTON_TAG)
    private fun message() =
        composeTestRule.onNodeWithTag(CUSTOM_FACES_MESSAGE_TAG, useUnmergedTree = true)

    private fun countText(dice: CustomDie) =
        composeTestRule.onNodeWithTag(chipCountTestTag(dice), useUnmergedTree = true)

    private fun increaseButton(dice: DieType) = composeTestRule
        .onNodeWithContentDescription(str(R.string.chip_increase_description, dieNotation(dice)))

    private fun decreaseButton(dice: DieType) = composeTestRule
        .onNodeWithContentDescription(str(R.string.chip_decrease_description, dieNotation(dice)))

    /** Creates a custom die of [faces] the way a user would: add chip, type, Add. */
    private fun createDie(faces: Int) {
        addChip().performClick()
        facesField().performTextInput("$faces")
        addButton().performClick()
    }

    // --- The add chip ---

    @Test
    fun givenAppLaunch_whenScreenIsDisplayed_thenTheAddChipIsPresent() {
        launch()

        addChip().assertIsDisplayed()
    }

    @Test
    fun givenTheAddChip_whenTapped_thenTheCreatorOpens() {
        launch()

        addChip().performClick()

        facesField().assertIsDisplayed()
    }

    /** Hidden rather than disabled at the cap, which is what holds the grid to three rows. */
    @Test
    fun givenTheCapIsReached_whenScreenIsDisplayed_thenTheAddChipIsGone() {
        val full = (1..MAX_CUSTOM_DICE).map { CustomDie(it + 100) }
        launch(initialDice = full)

        addChip().assertDoesNotExist()
    }

    // --- Creating a die ---

    @Test
    fun givenTheCreator_whenAValidFaceCountIsTyped_thenAddIsEnabled() {
        launch()
        addChip().performClick()

        facesField().performTextInput("7")

        addButton().assertIsEnabled()
    }

    @Test
    fun givenTheCreator_whenNothingIsTyped_thenAddIsDisabled() {
        launch()

        addChip().performClick()

        addButton().assertIsNotEnabled()
    }

    @Test
    fun givenTheCreator_whenAPresetsFaceCountIsTyped_thenItIsRefusedWithAReason() {
        launch()
        addChip().performClick()

        facesField().performTextInput("6")

        addButton().assertIsNotEnabled()
        message().assertTextEquals(str(R.string.custom_faces_is_preset, dieNotation(Dice.D6)))
    }

    @Test
    fun givenADieAlreadyDefined_whenItIsTypedAgain_thenItIsRefusedAsADuplicate() {
        launch(initialDice = listOf(CustomDie(7)))
        addChip().performClick()

        facesField().performTextInput("7")

        addButton().assertIsNotEnabled()
        message().assertTextEquals(str(R.string.custom_faces_duplicate, dieNotation(CustomDie(7))))
    }

    @Test
    fun givenTheCreator_whenAValidDieIsAdded_thenItsChipAppearsAndTheCreatorCloses() {
        launch()

        createDie(7)

        countText(CustomDie(7)).assertTextEquals(str(R.string.number, 0))
        facesField().assertDoesNotExist()
    }

    @Test
    fun givenTheCreator_whenCancelled_thenNoDieIsCreated() {
        launch()
        addChip().performClick()
        facesField().performTextInput("7")

        composeTestRule.onNodeWithText(str(R.string.action_cancel)).performClick()

        countText(CustomDie(7)).assertDoesNotExist()
    }

    /** The badge is the custom die's stand-in for artwork, so it must carry the face count. */
    @Test
    fun givenACustomDie_whenItsChipIsDisplayed_thenTheChipIsLabelledWithItsFaceCount() {
        launch(initialDice = listOf(CustomDie(7)))

        increaseButton(CustomDie(7)).assertIsDisplayed()
        decreaseButton(CustomDie(7)).assertExists()
    }

    // --- A custom die behaves like a preset ---

    @Test
    fun givenACustomDie_whenItsChipIsIncremented_thenTheCountAndTheRollLabelFollow() {
        launch(initialDice = listOf(CustomDie(7)))

        increaseButton(CustomDie(7)).performClick()

        countText(CustomDie(7)).assertTextEquals(str(R.string.number, 1))
        composeTestRule.onNodeWithText(rollLabel("1D7")).assertIsDisplayed()
    }

    /** Notation and the result ladder order by face count, so a D7 reads between the D6 and D8. */
    @Test
    fun givenAPoolMixingAPresetAndACustomDie_whenLabelled_thenTheD7SitsBetweenThem() {
        launch(initialDice = listOf(CustomDie(7)))

        increaseButton(Dice.D8).performClick()
        increaseButton(Dice.D6).performClick()
        increaseButton(CustomDie(7)).performClick()

        composeTestRule.onNodeWithText(rollLabel("1D6 + 1D7 + 1D8")).assertIsDisplayed()
    }

    @Test
    fun givenACustomDieInThePool_whenRolled_thenItsGroupAppearsInTheLadder() {
        launch(initialDice = listOf(CustomDie(7)))
        increaseButton(CustomDie(7)).performClick()

        composeTestRule.onNodeWithText(rollLabel("1D7")).performClick()

        composeTestRule.onNodeWithText(groupHeader(1, dieNotation(CustomDie(7))))
            .assertIsDisplayed()
    }

    /**
     * Clearing the pool (issue #67) zeroes a custom die's count like any other, and stops there:
     * the definition is only ever undone by the chip's own `×` badge, which is the control that
     * offers an undo.
     */
    @Test
    fun givenACustomDieInThePool_whenThePoolIsCleared_thenItsChipStaysAtZero() {
        launch(initialDice = listOf(CustomDie(7)))
        increaseButton(CustomDie(7)).performClick()
        increaseButton(Dice.D6).performClick()

        composeTestRule.onNodeWithTag(CLEAR_POOL_BUTTON_TAG).performClick()

        countText(CustomDie(7)).assertTextEquals(str(R.string.number, 0))
        increaseButton(CustomDie(7)).assertIsDisplayed()
        composeTestRule.onNodeWithText(str(R.string.roll_button_empty)).assertIsDisplayed()
    }

    // --- Removing a die, and undoing that ---

    @Test
    fun givenACustomDie_whenItsRemoveBadgeIsTapped_thenTheChipGoes() {
        launch(initialDice = listOf(CustomDie(7)))

        composeTestRule.onNodeWithTag(chipRemoveTestTag(CustomDie(7))).performClick()

        countText(CustomDie(7)).assertDoesNotExist()
    }

    /** Presets are not deletable, so they must not carry the badge at all. */
    @Test
    fun givenAPresetChip_whenDisplayed_thenItHasNoRemoveBadge() {
        launch()

        Dice.entries.forEach { dice ->
            composeTestRule.onNodeWithTag(chipRemoveTestTag(dice)).assertDoesNotExist()
        }
    }

    @Test
    fun givenACustomDieIsRemoved_whenTheSnackbarAppears_thenUndoBringsItBack() {
        launch(initialDice = listOf(CustomDie(7)))
        increaseButton(CustomDie(7)).performClick()

        composeTestRule.onNodeWithTag(chipRemoveTestTag(CustomDie(7))).performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText(str(R.string.action_undo))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeTestRule.onNodeWithText(str(R.string.action_undo)).performClick()

        // The count it was carrying comes back with it, not just the definition.
        countText(CustomDie(7)).assertTextEquals(str(R.string.number, 1))
    }

    @Test
    fun givenTheCapIsReached_whenOneIsRemoved_thenTheAddChipReturns() {
        val full = (1..MAX_CUSTOM_DICE).map { CustomDie(it + 100) }
        launch(initialDice = full)

        composeTestRule.onNodeWithTag(chipRemoveTestTag(full.first())).performClick()

        addChip().assertIsDisplayed()
    }

    // --- The cap, reached through the UI ---

    @Test
    fun givenDiceCreatedUpToTheCap_whenTheLastOneIsAdded_thenTheAddChipDisappears() {
        launch()

        repeat(MAX_CUSTOM_DICE) { index -> createDie(index + 101) }

        addChip().assertDoesNotExist()
        (1..MAX_CUSTOM_DICE).forEach { index ->
            countText(CustomDie(index + 100)).assertTextEquals(str(R.string.number, 0))
        }
    }

    /** Retyping into the field re-validates, so a refused entry is not a dead end. */
    @Test
    fun givenARefusedFaceCount_whenReplacedWithAValidOne_thenAddBecomesEnabled() {
        launch()
        addChip().performClick()
        facesField().performTextInput("6")

        facesField().performTextReplacement("7")

        addButton().assertIsEnabled()
        message().assertTextEquals(
            str(R.string.custom_die_valid_message, dieNotation(CustomDie(7))),
        )
    }
}
