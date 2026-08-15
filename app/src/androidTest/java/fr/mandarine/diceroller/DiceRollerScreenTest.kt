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
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.mandarine.diceroller.domain.Dice
import fr.mandarine.diceroller.domain.DiceGroupResult
import fr.mandarine.diceroller.domain.DicePool
import fr.mandarine.diceroller.domain.DicePoolResult
import fr.mandarine.diceroller.domain.DiceRoller
import fr.mandarine.diceroller.domain.DieType
import fr.mandarine.diceroller.domain.RollRecord
import fr.mandarine.diceroller.domain.ValueTally
import fr.mandarine.diceroller.presentation.DiceRollerUiState
import fr.mandarine.diceroller.presentation.DiceRollerViewModel
import fr.mandarine.diceroller.presentation.component.ABOUT_BUTTON_TAG
import fr.mandarine.diceroller.presentation.component.ABOUT_SHEET_TAG
import fr.mandarine.diceroller.presentation.component.ART_ATTRIBUTION
import fr.mandarine.diceroller.presentation.component.CLEAR_POOL_BUTTON_TAG
import fr.mandarine.diceroller.presentation.component.ROLL_HISTORY_HEADER_TAG
import fr.mandarine.diceroller.presentation.component.ROLL_HISTORY_LIST_TAG
import fr.mandarine.diceroller.presentation.component.chipCountTestTag
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
                    onToggleHistory = {},
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
                        onToggleHistory = {},
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
                    onToggleHistory = viewModel::toggleHistoryExpanded,
                    onClearPool = viewModel::clearPool,
                    onShowAbout = viewModel::showAbout,
                    onDismissAbout = viewModel::dismissAbout,
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
        val pool: Map<DieType, Int> = Dice.entries.associateWith { dice ->
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
        val pool: Map<DieType, Int> = Dice.entries.associateWith { dice ->
            if (dice == Dice.D6) DicePool.MAX_DICE_PER_TYPE else 0
        }
        launchScreen(uiState = DiceRollerUiState(pool = pool))

        increaseButton(Dice.D6).assertIsNotEnabled()
    }

    @Test
    fun givenAChipOneBelowMaxCount_whenScreenIsDisplayed_thenTheIncreaseControlIsStillEnabled() {
        val pool: Map<DieType, Int> = Dice.entries.associateWith { dice ->
            if (dice == Dice.D6) DicePool.MAX_DICE_PER_TYPE - 1 else 0
        }
        launchScreen(uiState = DiceRollerUiState(pool = pool))

        increaseButton(Dice.D6).assertIsEnabled()
    }

    @Test
    fun givenANonZeroCountChip_whenScreenIsDisplayed_thenTheDecreaseControlIsEnabled() {
        val pool: Map<DieType, Int> = Dice.entries.associateWith { dice -> if (dice == Dice.D6) 1 else 0 }
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

    /**
     * Issue #64's fit, as it stands after custom dice (issue #4) took a third chip row.
     *
     * Measured, not assumed: a chip row is 98dp and the third one costs 106dp with its gap, against
     * the ~16dp of slack this viewport had. So at [COMPACT_PHONE_HEIGHT] the densest typical pool no
     * longer shows its last group and total outright — the result band scrolls them into reach. Every
     * *control* still fits, which is the guarantee that must never regress, and the thresholds where
     * the total fits outright again are pinned by the two tests below.
     */
    @Test
    fun givenTheRealisticPoolRolledOnACompactPhone_whenScreenIsDisplayed_thenEveryControlFitsAndTheResultScrollsIntoReach() {
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
        // Result band: the first group is on screen, and the rest is one scroll away rather than
        // gone. performScrollTo passes untouched for a node already visible, so this is the same
        // assertion for whichever of them still fits.
        composeTestRule.onNodeWithText("4×D6").assertIsDisplayed()
        composeTestRule.onNodeWithText("4×D8").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("4×D20").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Total $REALISTIC_POOL_TOTAL")
            .performScrollTo()
            .assertIsDisplayed()
        // Bottom band: the Roll button, now alone in it (issue #66). The credit it used to carry
        // is behind the About button in the top band, which is asserted alongside it.
        composeTestRule.onNodeWithText("Roll 4D6 + 4D8 + 4D20").assertIsDisplayed()
        composeTestRule.onNodeWithTag(ABOUT_BUTTON_TAG).assertIsDisplayed()
    }

    /** The measured height at which the total fits outright again with no history band. */
    @Test
    fun givenNoHistoryAtTheTotalFitHeight_whenScreenIsDisplayed_thenTheTotalNeedsNoScrolling() {
        launchScreenInViewport(
            uiState = realisticRolledState(withHistory = false),
            width = COMPACT_PHONE_WIDTH,
            height = TOTAL_FIT_HEIGHT_NO_HISTORY,
        )

        composeTestRule.onNodeWithText("4×D20").assertIsDisplayed()
        composeTestRule.onNodeWithText("Total $REALISTIC_POOL_TOTAL").assertIsDisplayed()
    }

    /** The same threshold once a collapsed history band is also charging its ~45dp. */
    @Test
    fun givenACollapsedHistoryBandAtTheTotalFitHeight_whenScreenIsDisplayed_thenTheTotalNeedsNoScrolling() {
        launchScreenInViewport(
            uiState = realisticRolledState(withHistory = true),
            width = COMPACT_PHONE_WIDTH,
            height = TOTAL_FIT_HEIGHT_WITH_HISTORY,
        )

        composeTestRule.onNodeWithText("4×D20").assertIsDisplayed()
        composeTestRule.onNodeWithText("Total $REALISTIC_POOL_TOTAL").assertIsDisplayed()
        composeTestRule.onNodeWithText("Recent (1)").assertIsDisplayed()
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

    // --- Roll history band (issue #3) ---

    @Test
    fun givenNoRollsYet_whenScreenIsDisplayed_thenTheHistoryBandIsAbsentEntirely() {
        launchScreen()

        composeTestRule.onNodeWithTag(ROLL_HISTORY_HEADER_TAG).assertDoesNotExist()
    }

    @Test
    fun givenARollIsMade_whenTheScreenUpdates_thenTheHistoryBandAppearsCollapsed() {
        launchWithViewModel()
        increaseButton(Dice.D6).performClick()

        composeTestRule.onNodeWithText("Roll 1D6").performClick()

        composeTestRule.onNodeWithText("Recent (1)").assertIsDisplayed()
        composeTestRule.onNodeWithTag(ROLL_HISTORY_LIST_TAG).assertDoesNotExist()
    }

    @Test
    fun givenTwoRollsAreMade_whenTheScreenUpdates_thenTheHistoryCountGrows() {
        launchWithViewModel()
        increaseButton(Dice.D6).performClick()
        composeTestRule.onNodeWithText("Roll 1D6").performClick()

        // A roll empties the pool, so the second run has to be queued like the first.
        increaseButton(Dice.D6).performClick()
        composeTestRule.onNodeWithText("Roll 1D6").performClick()

        composeTestRule.onNodeWithText("Recent (2)").assertIsDisplayed()
    }

    @Test
    fun givenACollapsedHistoryBand_whenTheHeaderIsTapped_thenTheEntriesAppear() {
        launchWithViewModel()
        increaseButton(Dice.D6).performClick()
        composeTestRule.onNodeWithText("Roll 1D6").performClick()

        composeTestRule.onNodeWithTag(ROLL_HISTORY_HEADER_TAG).performClick()

        composeTestRule.onNodeWithTag(ROLL_HISTORY_LIST_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithText("just now").assertIsDisplayed()
    }

    /**
     * The log is append-only. Once expanded, the screen offers no control that erases it — the
     * header toggles and nothing else — so a roll cannot be lost to a stray tap.
     */
    @Test
    fun givenAnExpandedHistoryBand_whenLookingForAWayToEraseIt_thenThereIsNone() {
        launchWithViewModel()
        increaseButton(Dice.D6).performClick()
        composeTestRule.onNodeWithText("Roll 1D6").performClick()

        composeTestRule.onNodeWithTag(ROLL_HISTORY_HEADER_TAG).performClick()

        composeTestRule.onNodeWithText("Clear").assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription("Clear roll history").assertDoesNotExist()
    }

    @Test
    fun givenAnExpandedHistoryBand_whenTheHeaderIsTappedAgain_thenItCollapsesWithoutLosingEntries() {
        launchWithViewModel()
        increaseButton(Dice.D6).performClick()
        composeTestRule.onNodeWithText("Roll 1D6").performClick()
        composeTestRule.onNodeWithTag(ROLL_HISTORY_HEADER_TAG).performClick()

        composeTestRule.onNodeWithTag(ROLL_HISTORY_HEADER_TAG).performClick()

        composeTestRule.onNodeWithTag(ROLL_HISTORY_LIST_TAG).assertDoesNotExist()
        composeTestRule.onNodeWithText("Recent (1)").assertIsDisplayed()
    }

    // --- Clearing the whole pool from the roll bar (issue #67) ---

    /** Hidden rather than disabled, so it never offers an action that would do nothing. */
    @Test
    fun givenEmptyPool_whenScreenIsDisplayed_thenTheClearButtonIsAbsent() {
        launchScreen()

        composeTestRule.onNodeWithTag(CLEAR_POOL_BUTTON_TAG).assertDoesNotExist()
    }

    @Test
    fun givenANonEmptyPool_whenScreenIsDisplayed_thenTheClearButtonIsVisibleBesideRoll() {
        launchScreen(uiState = DiceRollerUiState(pool = mapOf(Dice.D6 to 2)))

        composeTestRule.onNodeWithTag(CLEAR_POOL_BUTTON_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithText("Roll 2D6").assertIsDisplayed().assertIsEnabled()
    }

    @Test
    fun givenAMixedPool_whenTheClearButtonIsTapped_thenEveryChipReturnsToZero() {
        launchWithViewModel()
        repeat(3) { increaseButton(Dice.D6).performClick() }
        increaseButton(Dice.D8).performClick()
        increaseButton(Dice.D20).performClick()

        composeTestRule.onNodeWithTag(CLEAR_POOL_BUTTON_TAG).performClick()

        Dice.entries.forEach { dice -> countText(dice).assertTextEquals("0") }
    }

    /**
     * The state the ✕ actually exists for now that a roll empties the pool itself: a pool built up
     * and abandoned before rolling it.
     */
    @Test
    fun givenAnUnrolledPool_whenTheClearButtonIsTapped_thenTheEmptyStateAndTheButtonItselfAreGone() {
        launchWithViewModel(seed = 1)
        repeat(2) { increaseButton(Dice.D6).performClick() }
        increaseButton(Dice.D20).performClick()

        composeTestRule.onNodeWithTag(CLEAR_POOL_BUTTON_TAG).performClick()

        composeTestRule.onNodeWithText("Add dice above to build your pool.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Add dice to roll").assertIsDisplayed().assertIsNotEnabled()
        composeTestRule.onNodeWithTag(CLEAR_POOL_BUTTON_TAG).assertDoesNotExist()
    }

    /** A roll leaves nothing to clear, so the bar it returns to is the empty-pool one. */
    @Test
    fun givenAPool_whenItIsRolled_thenTheCountsResetAndTheClearButtonGoesWithThem() {
        launchWithViewModel(seed = 1)
        repeat(3) { increaseButton(Dice.D6).performClick() }
        increaseButton(Dice.D8).performClick()

        composeTestRule.onNodeWithText("Roll 3D6 + 1D8").performClick()

        Dice.entries.forEach { dice -> countText(dice).assertTextEquals("0") }
        composeTestRule.onNodeWithText("Add dice to roll").assertIsDisplayed().assertIsNotEnabled()
        composeTestRule.onNodeWithTag(CLEAR_POOL_BUTTON_TAG).assertDoesNotExist()
        // The ladder of the roll just made stays up, even though its pool is gone.
        composeTestRule.onNodeWithText("3×D6").assertIsDisplayed()
    }

    /** Cheap enough to redo by hand, which is the argument for not confirming it: no dialog. */
    @Test
    fun givenAClearedPool_whenDiceAreQueuedAgain_thenTheBarComesBackWithThem() {
        launchWithViewModel()
        increaseButton(Dice.D6).performClick()
        composeTestRule.onNodeWithTag(CLEAR_POOL_BUTTON_TAG).performClick()

        increaseButton(Dice.D20).performClick()

        composeTestRule.onNodeWithText("Roll 1D20").assertIsDisplayed().assertIsEnabled()
        composeTestRule.onNodeWithTag(CLEAR_POOL_BUTTON_TAG).assertIsDisplayed()
    }

    // --- The About sheet, and the footer it replaced (issue #66) ---

    /**
     * The half of issue #66 that frees the space: the credit is no longer a permanent band under
     * the roll button. Asserted as "not anywhere on the main screen at rest" rather than "not in
     * the bottom bar", since a footer moved a few dp up would still be the thing that was removed.
     */
    @Test
    fun givenTheScreenAtRest_whenDisplayed_thenTheAttributionFooterIsGone() {
        launchScreen()

        composeTestRule.onNodeWithText(ART_ATTRIBUTION).assertDoesNotExist()
    }

    /** The other half: it is still reachable, from a control that is on screen from the start. */
    @Test
    fun givenTheScreenAtRest_whenDisplayed_thenTheAboutButtonIsVisible() {
        launchScreen()

        composeTestRule.onNodeWithTag(ABOUT_BUTTON_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(ABOUT_SHEET_TAG).assertDoesNotExist()
    }

    @Test
    fun givenTheScreen_whenTheAboutButtonIsTapped_thenTheSheetShowsTheRequiredCredit() {
        launchWithViewModel()

        composeTestRule.onNodeWithTag(ABOUT_BUTTON_TAG).performClick()

        composeTestRule.onNodeWithText(ART_ATTRIBUTION).assertIsDisplayed()
    }

    /**
     * The sheet is a place to read the credit, not a place the pool goes to die: opening it must
     * leave the roll the user is looking at exactly as it was.
     */
    @Test
    fun givenARolledPool_whenTheAboutButtonIsTapped_thenTheResultIsUntouched() {
        launchWithViewModel()
        increaseButton(Dice.D6).performClick()
        composeTestRule.onNodeWithText("Roll 1D6").performClick()
        // The roll emptied the pool; the next run is already being queued when the credit is read.
        increaseButton(Dice.D8).performClick()

        composeTestRule.onNodeWithTag(ABOUT_BUTTON_TAG).performClick()

        composeTestRule.onNodeWithText("Roll 1D8").assertIsDisplayed().assertIsEnabled()
        countText(Dice.D8).assertTextEquals("1")
    }

    // --- What history costs the whole-screen fit (issues #64 and #3) ---

    /**
     * The band's real price, measured rather than assumed: at [COMPACT_PHONE_HEIGHT] the
     * pre-history layout cleared the densest *typical* pool by only ~16dp, and a collapsed band
     * costs ~45dp. So on the shortest supported viewport the result band now scrolls at that pool
     * — the vertical cost the inline design was chosen with. Every *control* still stays put,
     * which is the part that must never regress, and the total is one short scroll away rather
     * than gone.
     */
    @Test
    fun givenACollapsedHistoryBandOnTheShortestViewport_whenScreenIsDisplayed_thenEveryControlSurvives() {
        launchScreenInViewport(
            uiState = realisticRolledState(withHistory = true),
            width = COMPACT_PHONE_WIDTH,
            height = COMPACT_PHONE_HEIGHT,
        )

        Dice.entries.forEach { dice -> increaseButton(dice).assertIsDisplayed() }
        composeTestRule.onNodeWithText("4×D6").assertIsDisplayed()
        composeTestRule.onNodeWithText("Recent (1)").assertIsDisplayed()
        composeTestRule.onNodeWithText("Roll 4D6 + 4D8 + 4D20").assertIsDisplayed()
        composeTestRule.onNodeWithTag(ABOUT_BUTTON_TAG).assertIsDisplayed()
        // Issue #67's control takes width from the roll button rather than height from the
        // screen: on the narrowest supported viewport both still fit on the one line.
        composeTestRule.onNodeWithTag(CLEAR_POOL_BUTTON_TAG).assertIsDisplayed()
    }

    /**
     * One notch taller than the harshest bound. Before custom dice this was the height at which
     * everything fit outright; the third chip row moved that threshold to
     * [TOTAL_FIT_HEIGHT_WITH_HISTORY], so here the result band still scrolls its tail into reach
     * while every band and control remains on screen.
     */
    @Test
    fun givenACollapsedHistoryBandOnASmallPhone_whenScreenIsDisplayed_thenEveryBandIsPresentAndTheResultScrollsIntoReach() {
        launchScreenInViewport(
            uiState = realisticRolledState(withHistory = true),
            width = COMPACT_PHONE_WIDTH,
            height = SMALL_PHONE_HEIGHT,
        )

        Dice.entries.forEach { dice -> increaseButton(dice).assertIsDisplayed() }
        composeTestRule.onNodeWithText("4×D6").assertIsDisplayed()
        composeTestRule.onNodeWithText("4×D20").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Total $REALISTIC_POOL_TOTAL")
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Recent (1)").assertIsDisplayed()
        composeTestRule.onNodeWithText("Roll 4D6 + 4D8 + 4D20").assertIsDisplayed()
        composeTestRule.onNodeWithTag(ABOUT_BUTTON_TAG).assertIsDisplayed()
    }

    /**
     * With no rolls yet the history band is absent entirely — but the chip grid's third row is
     * charged whether or not a custom die exists, because the add chip is what makes the feature
     * discoverable. So on the shortest viewport even a history-free screen now scrolls its total
     * into reach rather than showing it outright.
     */
    @Test
    fun givenNoHistoryOnTheShortestViewport_whenScreenIsDisplayed_thenTheTotalScrollsIntoReach() {
        launchScreenInViewport(
            uiState = realisticRolledState(withHistory = false),
            width = COMPACT_PHONE_WIDTH,
            height = COMPACT_PHONE_HEIGHT,
        )

        composeTestRule.onNodeWithText("Total $REALISTIC_POOL_TOTAL")
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(ROLL_HISTORY_HEADER_TAG).assertDoesNotExist()
    }

    @Test
    fun givenAnExpandedHistoryBandOnTheShortestViewport_whenScreenIsDisplayed_thenTheRollBarSurvives() {
        launchScreenInViewport(
            uiState = realisticRolledState(withHistory = true, historyExpanded = true),
            width = COMPACT_PHONE_WIDTH,
            height = COMPACT_PHONE_HEIGHT,
        )

        // The entry list scrolls within the band's share of the free space; it never grows into
        // the pinned roll bar or the selector above it.
        composeTestRule.onNodeWithTag(ROLL_HISTORY_LIST_TAG).assertIsDisplayed()
        Dice.entries.forEach { dice -> increaseButton(dice).assertIsDisplayed() }
        composeTestRule.onNodeWithText("Roll 4D6 + 4D8 + 4D20").assertIsDisplayed()
        composeTestRule.onNodeWithTag(ABOUT_BUTTON_TAG).assertIsDisplayed()
    }

    @Test
    fun givenAnExpandedHistoryBandOnTheShortestViewport_whenScreenIsDisplayed_thenItSitsAboveTheRollButton() {
        launchScreenInViewport(
            uiState = realisticRolledState(withHistory = true, historyExpanded = true),
            width = COMPACT_PHONE_WIDTH,
            height = COMPACT_PHONE_HEIGHT,
        )

        val listBottom = composeTestRule
            .onNodeWithTag(ROLL_HISTORY_LIST_TAG)
            .fetchSemanticsNode()
            .boundsInRoot
            .bottom
        val buttonTop = composeTestRule
            .onNodeWithText("Roll 4D6 + 4D8 + 4D20")
            .fetchSemanticsNode()
            .boundsInRoot
            .top

        assertTrue(
            "History overlaps the Roll button: list ends at $listBottom, button starts at $buttonTop",
            listBottom <= buttonTop,
        )
    }

    private companion object {

        /** Deliberately smaller than the emulator's own screen, to bound the fit assertions. */
        val COMPACT_PHONE_WIDTH = 360.dp
        val COMPACT_PHONE_HEIGHT = 640.dp

        /** A small — but not extreme — phone, one notch above the harshest bound. */
        val SMALL_PHONE_HEIGHT = 680.dp

        /**
         * Measured heights at which the densest typical pool shows its total without scrolling,
         * with and without a collapsed history band, after custom dice (issue #4) took a third chip
         * row. Both were ~100dp lower before that row existed — that 106dp is the feature's price.
         */
        val TOTAL_FIT_HEIGHT_NO_HISTORY = 740.dp
        val TOTAL_FIT_HEIGHT_WITH_HISTORY = 780.dp

        /** Sum of [realisticRolledState]'s tallies. */
        const val REALISTIC_POOL_TOTAL = 80

        /**
         * Issue #64's acceptance pool: three die types, four dice each, each landing on three
         * distinct values — the densest arrangement the design still calls typical.
         */
        fun realisticRolledState(
            withHistory: Boolean = false,
            historyExpanded: Boolean = false,
        ): DiceRollerUiState {
            val counts = mapOf(Dice.D6 to 4, Dice.D8 to 4, Dice.D20 to 4)
            val result = DicePoolResult(
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
            )
            val now = 1_700_000_000_000L
            return DiceRollerUiState(
                pool = Dice.entries.associateWith { dice -> counts[dice] ?: 0 },
                result = result,
                history = if (withHistory) {
                    listOf(RollRecord(result = result, rolledAtMillis = now - 10_000L))
                } else {
                    emptyList()
                },
                isHistoryExpanded = historyExpanded,
                nowMillis = now,
            )
        }
    }
}
