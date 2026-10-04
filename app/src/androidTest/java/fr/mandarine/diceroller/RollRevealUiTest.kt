// app/src/androidTest/java/fr/mandarine/diceroller/RollRevealUiTest.kt
package fr.mandarine.diceroller

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.mandarine.diceroller.domain.Dice
import fr.mandarine.diceroller.domain.DiceGroupResult
import fr.mandarine.diceroller.domain.DicePoolResult
import fr.mandarine.diceroller.domain.DiceRoller
import fr.mandarine.diceroller.domain.ValueTally
import fr.mandarine.diceroller.presentation.DiceRollerUiState
import fr.mandarine.diceroller.presentation.DiceRollerViewModel
import fr.mandarine.diceroller.presentation.component.ROLL_HISTORY_HEADER_TAG
import fr.mandarine.diceroller.presentation.component.ROLL_TUMBLE_MILLIS
import fr.mandarine.diceroller.ui.theme.DiceRollerTheme
import kotlin.random.Random
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The spoken roll summary: the one node in the result band that is a live region. */
private val rollSummary = SemanticsMatcher.keyIsDefined(SemanticsProperties.LiveRegion)

/** Comfortably past the tumble plus its landing bounce. */
private const val PAST_THE_LANDING_MILLIS = ROLL_TUMBLE_MILLIS + 1_000L

/**
 * Mounts the real screen on a seeded ViewModel and queues one D6, with the test clock paused so
 * the reveal can be inspected mid-flight.
 */
internal fun ComposeContentTestRule.launchPausedWithOneD6() {
    val viewModel = DiceRollerViewModel(diceRoller = DiceRoller(random = Random(42)))
    mainClock.autoAdvance = false
    setContent {
        DiceRollerTheme {
            val uiState by viewModel.uiState.collectAsState()
            DiceRollerScreen(
                uiState = uiState,
                onIncrementCount = viewModel::incrementCount,
                onDecrementCount = viewModel::decrementCount,
                onSelectColor = viewModel::selectColor,
                onRollDice = viewModel::rollDice,
                onToggleHistory = viewModel::toggleHistoryExpanded,
            )
        }
    }
    onNodeWithContentDescription(str(R.string.chip_increase_description, dieNotation(Dice.D6)))
        .performClick()
    mainClock.advanceTimeByFrame()
}

/**
 * The roll animation (issue #1): a roll is decided at once but revealed over a short tumble, and
 * nothing that gives the outcome away — the spoken summary, the log entry — appears before the dice
 * land.
 */
@RunWith(AndroidJUnit4::class)
class RollRevealUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun givenARoll_whileTheDiceTumble_thenTheOutcomeIsNotYetAnnounced() {
        composeTestRule.launchPausedWithOneD6()

        composeTestRule.onNodeWithText(rollLabel()).performClick()
        composeTestRule.mainClock.advanceTimeBy(ROLL_TUMBLE_MILLIS / 2L)

        composeTestRule.onNode(rollSummary).assertDoesNotExist()
    }

    @Test
    fun givenARoll_onceTheDiceLand_thenTheOutcomeIsAnnounced() {
        composeTestRule.launchPausedWithOneD6()

        composeTestRule.onNodeWithText(rollLabel()).performClick()
        composeTestRule.mainClock.advanceTimeBy(PAST_THE_LANDING_MILLIS)

        composeTestRule.onNode(rollSummary).assertExists()
    }

    /** The first roll creates the log; it must not appear, entry and all, under tumbling dice. */
    @Test
    fun givenAFirstRoll_whileTheDiceTumble_thenTheLogIsHeldBackUntilTheyLand() {
        composeTestRule.launchPausedWithOneD6()

        composeTestRule.onNodeWithText(rollLabel()).performClick()
        composeTestRule.mainClock.advanceTimeBy(ROLL_TUMBLE_MILLIS / 2L)
        composeTestRule.onNodeWithTag(ROLL_HISTORY_HEADER_TAG).assertDoesNotExist()

        composeTestRule.mainClock.advanceTimeBy(PAST_THE_LANDING_MILLIS)
        composeTestRule.onNodeWithTag(ROLL_HISTORY_HEADER_TAG).assertExists()
    }

    /**
     * A result already on screen when the screen is (re)built — a rotation, a language switch — was
     * revealed when it was rolled, and must not tumble a second time.
     */
    @Test
    fun givenAResultAlreadyOnScreen_whenTheScreenIsBuilt_thenItIsShownLandedAtOnce() {
        val result = DicePoolResult(
            groups = listOf(
                DiceGroupResult(dice = Dice.D6, poolCount = 1, tallies = listOf(ValueTally(4, 1))),
            ),
            total = 4,
        )
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            DiceRollerTheme {
                DiceRollerScreen(
                    uiState = DiceRollerUiState(result = result),
                    onIncrementCount = {},
                    onDecrementCount = {},
                    onSelectColor = {},
                    onRollDice = {},
                    onToggleHistory = {},
                )
            }
        }
        composeTestRule.mainClock.advanceTimeByFrame()

        composeTestRule.onNode(rollSummary).assertExists()
    }
}
