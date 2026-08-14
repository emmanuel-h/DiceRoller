// app/src/androidTest/java/fr/mandarine/diceroller/presentation/component/RollHistoryBandTest.kt
package fr.mandarine.diceroller.presentation.component

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.mandarine.diceroller.domain.Dice
import fr.mandarine.diceroller.domain.DiceGroupResult
import fr.mandarine.diceroller.domain.DicePoolResult
import fr.mandarine.diceroller.domain.RollRecord
import fr.mandarine.diceroller.domain.ValueTally
import fr.mandarine.diceroller.presentation.model.DiceColor
import fr.mandarine.diceroller.ui.theme.DiceRollerTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The roll history band standalone: what it shows collapsed, what it reveals expanded, and the
 * single action it exposes — the log is append-only, so expand/collapse is the only thing the
 * band can do. Its place in the screen's band layout is covered by `DiceRollerScreenTest`.
 */
@RunWith(AndroidJUnit4::class)
class RollHistoryBandTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val now = 1_700_000_000_000L

    private val mixedRoll = DicePoolResult(
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
                poolCount = 2,
                tallies = listOf(
                    ValueTally(value = 7, count = 1),
                    ValueTally(value = 2, count = 1),
                ),
            ),
        ),
        total = 26,
    )

    private val singleRoll = DicePoolResult(
        groups = listOf(
            DiceGroupResult(
                dice = Dice.D20,
                poolCount = 1,
                tallies = listOf(ValueTally(value = 14, count = 1)),
            ),
        ),
        total = 14,
    )

    private val history = listOf(
        RollRecord(result = mixedRoll, rolledAtMillis = now - 10_000L),
        RollRecord(result = singleRoll, rolledAtMillis = now - 2L * 60_000L),
    )

    private var toggleCount = 0

    private fun launchBand(
        history: List<RollRecord> = this.history,
        isExpanded: Boolean = false,
        selectedColor: DiceColor = DiceColor.Ruby,
    ) {
        composeTestRule.setContent {
            DiceRollerTheme(dynamicColor = false) {
                RollHistoryBand(
                    history = history,
                    isExpanded = isExpanded,
                    nowMillis = now,
                    selectedColor = selectedColor,
                    onToggleExpanded = { toggleCount++ },
                )
            }
        }
    }

    // --- Absent until there is something to show ---

    @Test
    fun givenNoHistory_whenDisplayed_thenTheBandRendersNothingAtAll() {
        launchBand(history = emptyList())

        composeTestRule.onNodeWithTag(ROLL_HISTORY_HEADER_TAG).assertDoesNotExist()
    }

    @Test
    fun givenNoHistoryButExpanded_whenDisplayed_thenStillRendersNothing() {
        launchBand(history = emptyList(), isExpanded = true)

        composeTestRule.onNodeWithTag(ROLL_HISTORY_HEADER_TAG).assertDoesNotExist()
        composeTestRule.onNodeWithTag(ROLL_HISTORY_LIST_TAG).assertDoesNotExist()
    }

    // --- Collapsed ---

    @Test
    fun givenCollapsedBand_whenDisplayed_thenHeaderShowsTheEntryCount() {
        launchBand()

        composeTestRule.onNodeWithText("Recent (2)").assertIsDisplayed()
    }

    @Test
    fun givenCollapsedBand_whenDisplayed_thenNoEntriesAreShown() {
        launchBand()

        composeTestRule.onNodeWithTag(ROLL_HISTORY_LIST_TAG).assertDoesNotExist()
        composeTestRule.onNodeWithText("4D6 + 2D8").assertDoesNotExist()
    }

    // --- Expanded ---

    @Test
    fun givenExpandedBand_whenDisplayed_thenEachEntryShowsItsNotationAndTotal() {
        launchBand(isExpanded = true)

        composeTestRule.onNodeWithText("4D6 + 2D8").assertIsDisplayed()
        composeTestRule.onNodeWithText("26").assertIsDisplayed()
        composeTestRule.onNodeWithText("1D20").assertIsDisplayed()
        composeTestRule.onNodeWithText("14").assertIsDisplayed()
    }

    @Test
    fun givenExpandedBand_whenDisplayed_thenEntriesShowHowLongAgoTheyHappened() {
        launchBand(isExpanded = true)

        composeTestRule.onNodeWithText("just now").assertIsDisplayed()
        composeTestRule.onNodeWithText("2 min ago").assertIsDisplayed()
    }

    @Test
    fun givenExpandedBand_whenDisplayed_thenRepeatedValuesAreShownAsAMultiplier() {
        launchBand(isExpanded = true)

        // The 4 was rolled twice; the 6 only once and so carries no multiplier.
        composeTestRule.onNodeWithText("×2").assertIsDisplayed()
        composeTestRule.onNodeWithText("×1").assertDoesNotExist()
    }

    /** The log is append-only: no state of this band offers a way to erase it. */
    @Test
    fun givenExpandedBand_whenDisplayed_thenNoClearActionExists() {
        launchBand(isExpanded = true)

        composeTestRule.onNodeWithText("Clear").assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription("Clear roll history").assertDoesNotExist()
    }

    // --- Actions ---

    @Test
    fun givenCollapsedBand_whenHeaderIsTapped_thenToggleIsRequested() {
        toggleCount = 0
        launchBand()

        composeTestRule.onNodeWithTag(ROLL_HISTORY_HEADER_TAG).performClick()

        assertEquals(1, toggleCount)
    }

    @Test
    fun givenExpandedBand_whenHeaderIsTapped_thenToggleIsRequested() {
        toggleCount = 0
        launchBand(isExpanded = true)

        composeTestRule.onNodeWithTag(ROLL_HISTORY_HEADER_TAG).performClick()

        assertEquals(1, toggleCount)
    }

    // --- Accessibility ---

    @Test
    fun givenCollapsedBand_whenDisplayed_thenHeaderAnnouncesCountAndState() {
        launchBand()

        composeTestRule
            .onNodeWithContentDescription("Recent rolls, 2 rolls, collapsed")
            .assertIsDisplayed()
    }

    @Test
    fun givenExpandedBand_whenDisplayed_thenHeaderAnnouncesItIsExpanded() {
        launchBand(isExpanded = true)

        composeTestRule
            .onNodeWithContentDescription("Recent rolls, 2 rolls, expanded")
            .assertIsDisplayed()
    }

    @Test
    fun givenSingleEntry_whenDisplayed_thenHeaderUsesTheSingularWording() {
        launchBand(history = history.take(1))

        composeTestRule
            .onNodeWithContentDescription("Recent rolls, 1 roll, collapsed")
            .assertIsDisplayed()
    }

    @Test
    fun givenExpandedBand_whenDisplayed_thenEachEntryReadsAsOneSentence() {
        launchBand(isExpanded = true)

        composeTestRule
            .onNodeWithContentDescription(
                "4D6 + 2D8, total 26, just now. D6: 6, 4 2 times, 3. D8: 7, 2.",
            )
            .assertIsDisplayed()
    }

    @Test
    fun givenExpandedBand_whenDisplayed_thenTheArtworkItselfIsNotAnnounced() {
        launchBand(isExpanded = true)

        // Individual faces are cleared from the semantics tree, so the entry's own sentence is
        // the only thing a screen reader stops on.
        composeTestRule.onNodeWithContentDescription("D6, ruby").assertDoesNotExist()
    }
}
