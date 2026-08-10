// app/src/androidTest/java/fr/mandarine/diceroller/DiceRollerScreenTest.kt
package fr.mandarine.diceroller

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.mandarine.diceroller.domain.Dice
import fr.mandarine.diceroller.domain.DiceGroupResult
import fr.mandarine.diceroller.domain.DicePool
import fr.mandarine.diceroller.domain.DicePoolResult
import fr.mandarine.diceroller.domain.DiceRoller
import fr.mandarine.diceroller.domain.ValueTally
import fr.mandarine.diceroller.presentation.component.chipCountTestTag
import fr.mandarine.diceroller.presentation.DiceRollerUiState
import fr.mandarine.diceroller.presentation.DiceRollerViewModel
import fr.mandarine.diceroller.presentation.model.DiceColor
import fr.mandarine.diceroller.ui.theme.DiceRollerTheme
import kotlin.random.Random
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Screen-level behaviour: the dice-pool chip grid, the Roll button, and the result
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

    /**
     * The chip's count text, disambiguated from the other five chips (which may show the same
     * digit) by its per-die test tag. The chip lays its count out in a separate layer from its
     * two tap halves, so sibling-based matching no longer identifies it.
     */
    private fun countText(dice: Dice) =
        composeTestRule.onNodeWithTag(chipCountTestTag(dice), useUnmergedTree = true)

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

    /**
     * Mounts the screen inside a viewport of a fixed dp size, so the fit-on-screen assertions
     * describe a phone rather than whichever device happens to run the suite.
     */
    private fun launchScreenInViewport(
        uiState: DiceRollerUiState,
        width: Dp,
        height: Dp,
    ) {
        composeTestRule.setContent {
            DiceRollerTheme {
                Box(modifier = Modifier.size(width = width, height = height)) {
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
    fun givenAMixedPoolWithMultipleCountsPerType_whenScreenIsDisplayed_thenRollButtonLabelMatchesPoolCompositionAndOrdering() {
        val pool = Dice.entries.associateWith { dice ->
            when (dice) {
                Dice.D6 -> 4
                Dice.D8 -> 2
                else -> 0
            }
        }
        launchScreen(uiState = DiceRollerUiState(pool = pool))

        composeTestRule.onNodeWithText("Roll 4D6 + 2D8").assertIsDisplayed().assertIsEnabled()
    }

    @Test
    fun givenANonEmptyPool_whenTheDieIsDecrementedBackToZero_thenRollButtonReturnsToThePlaceholder() {
        launchWithViewModel()
        increaseButton(Dice.D6).performClick()

        decreaseButton(Dice.D6).performClick()

        composeTestRule.onNodeWithText("Add dice to roll").assertIsDisplayed().assertIsNotEnabled()
    }

    // --- Stepper count display ---

    @Test
    fun givenAZeroCountChip_whenIncrementIsTappedOnce_thenTheDisplayedCountBecomesOne() {
        launchWithViewModel()

        increaseButton(Dice.D6).performClick()

        countText(Dice.D6).assertTextEquals("1")
    }

    @Test
    fun givenAOneCountChip_whenIncrementIsTappedAgain_thenTheDisplayedCountBecomesTwo() {
        launchWithViewModel()
        increaseButton(Dice.D6).performClick()

        increaseButton(Dice.D6).performClick()

        countText(Dice.D6).assertTextEquals("2")
    }

    @Test
    fun givenATwoCountChip_whenDecrementIsTapped_thenTheDisplayedCountBecomesOne() {
        launchWithViewModel()
        increaseButton(Dice.D6).performClick()
        increaseButton(Dice.D6).performClick()

        decreaseButton(Dice.D6).performClick()

        countText(Dice.D6).assertTextEquals("1")
    }

    @Test
    fun givenIncrementingOneChip_whenAnotherChipIsUntouched_thenItsDisplayedCountStaysZero() {
        launchWithViewModel()

        increaseButton(Dice.D6).performClick()

        countText(Dice.D8).assertTextEquals("0")
    }

    // --- Stepper bounds ---

    @Test
    fun givenAChipAtMaxCount_whenScreenIsDisplayed_thenTheIncreaseControlIsDisabled() {
        val pool = Dice.entries.associateWith { dice ->
            if (dice == Dice.D6) DicePool.MAX_DICE_PER_TYPE else 0
        }
        launchScreen(uiState = DiceRollerUiState(pool = pool))

        increaseButton(Dice.D6).assertIsNotEnabled()
    }

    @Test
    fun givenAChipOneBelowMaxCount_whenScreenIsDisplayed_thenTheIncreaseControlIsStillEnabled() {
        val pool = Dice.entries.associateWith { dice ->
            if (dice == Dice.D6) DicePool.MAX_DICE_PER_TYPE - 1 else 0
        }
        launchScreen(uiState = DiceRollerUiState(pool = pool))

        increaseButton(Dice.D6).assertIsEnabled()
    }

    @Test
    fun givenANonZeroCountChip_whenScreenIsDisplayed_thenTheDecreaseControlIsEnabled() {
        val pool = Dice.entries.associateWith { dice -> if (dice == Dice.D6) 1 else 0 }
        launchScreen(uiState = DiceRollerUiState(pool = pool))

        decreaseButton(Dice.D6).assertIsEnabled()
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

    // --- Whole-screen fit (issue #64) ---

    @Test
    fun givenTheRealisticPoolRolledOnACompactPhone_whenScreenIsDisplayed_thenEveryBandIsVisibleAtOnce() {
        launchScreenInViewport(
            uiState = realisticRolledState(),
            width = COMPACT_PHONE_WIDTH,
            height = COMPACT_PHONE_HEIGHT,
        )

        // Top band: the color picker. Only its leading swatch is asserted — the row holds twelve
        // 44dp targets and has always scrolled sideways; issue #64 is about vertical fit.
        composeTestRule
            .onNodeWithContentDescription("${DiceColor.entries.first().label} dice")
            .assertIsDisplayed()
        // Selector band: all six die types, still reachable without scrolling.
        Dice.entries.forEach { dice -> increaseButton(dice).assertIsDisplayed() }
        // Result band: every group and the total. Were the layout overflowing, the results would
        // scroll and the last group and total would fall below the fold.
        composeTestRule.onNodeWithText("4×D6").assertIsDisplayed()
        composeTestRule.onNodeWithText("4×D8").assertIsDisplayed()
        composeTestRule.onNodeWithText("4×D20").assertIsDisplayed()
        composeTestRule.onNodeWithText("Total $REALISTIC_POOL_TOTAL").assertIsDisplayed()
        // Bottom band: the Roll button and the license-required credit.
        composeTestRule.onNodeWithText("Roll 4D6 + 4D8 + 4D20").assertIsDisplayed()
        composeTestRule.onNodeWithText("Dice art by Aeynit · CC BY 4.0").assertIsDisplayed()
    }

    @Test
    fun givenTheRealisticPoolRolledOnACompactPhone_whenScreenIsDisplayed_thenTheTotalSitsAboveTheRollButton() {
        launchScreenInViewport(
            uiState = realisticRolledState(),
            width = COMPACT_PHONE_WIDTH,
            height = COMPACT_PHONE_HEIGHT,
        )

        val totalBottom = composeTestRule
            .onNodeWithText("Total $REALISTIC_POOL_TOTAL")
            .fetchSemanticsNode()
            .boundsInRoot
            .bottom
        val buttonTop = composeTestRule
            .onNodeWithText("Roll 4D6 + 4D8 + 4D20")
            .fetchSemanticsNode()
            .boundsInRoot
            .top

        assertTrue(
            "Results overlap the Roll button: total ends at $totalBottom, button starts at $buttonTop",
            totalBottom <= buttonTop,
        )
    }

    private companion object {

        /** Deliberately smaller than the emulator's own screen, to bound the fit assertions. */
        val COMPACT_PHONE_WIDTH = 360.dp
        val COMPACT_PHONE_HEIGHT = 640.dp

        /** Sum of [realisticRolledState]'s tallies. */
        const val REALISTIC_POOL_TOTAL = 80

        /**
         * Issue #64's acceptance pool: three die types, four dice each, each landing on three
         * distinct values — the densest arrangement the design still calls typical.
         */
        fun realisticRolledState(): DiceRollerUiState {
            val counts = mapOf(Dice.D6 to 4, Dice.D8 to 4, Dice.D20 to 4)
            return DiceRollerUiState(
                pool = Dice.entries.associateWith { dice -> counts[dice] ?: 0 },
                result = DicePoolResult(
                    groups = listOf(
                        DiceGroupResult(
                            dice = Dice.D6,
                            poolCount = 4,
                            tallies = listOf(
                                ValueTally(value = 6, count = 1),
                                ValueTally(value = 4, count = 2),
                                ValueTally(value = 3, count = 1),
                            ),
                        ),
                        DiceGroupResult(
                            dice = Dice.D8,
                            poolCount = 4,
                            tallies = listOf(
                                ValueTally(value = 7, count = 1),
                                ValueTally(value = 5, count = 2),
                                ValueTally(value = 2, count = 1),
                            ),
                        ),
                        DiceGroupResult(
                            dice = Dice.D20,
                            poolCount = 4,
                            tallies = listOf(
                                ValueTally(value = 18, count = 1),
                                ValueTally(value = 11, count = 2),
                                ValueTally(value = 4, count = 1),
                            ),
                        ),
                    ),
                    total = REALISTIC_POOL_TOTAL,
                ),
            )
        }
    }
}
