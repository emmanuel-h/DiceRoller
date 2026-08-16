// app/src/androidTest/java/fr/mandarine/diceroller/presentation/component/RollHistoryBandTest.kt
package fr.mandarine.diceroller.presentation.component

import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.mandarine.diceroller.R
import fr.mandarine.diceroller.dieNotation
import fr.mandarine.diceroller.domain.Dice
import fr.mandarine.diceroller.domain.DiceGroupResult
import fr.mandarine.diceroller.domain.DicePoolResult
import fr.mandarine.diceroller.domain.DieType
import fr.mandarine.diceroller.domain.RollRecord
import fr.mandarine.diceroller.domain.ValueTally
import fr.mandarine.diceroller.plural
import fr.mandarine.diceroller.presentation.model.DiceColor
import fr.mandarine.diceroller.str
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

    /** The band header's spoken description for [entryCount] entries in the given open state. */
    private fun headerDescription(entryCount: Int, @StringRes stateRes: Int): String = str(
        R.string.history_header_description,
        plural(R.plurals.history_roll_count, entryCount),
        str(stateRes),
    )

    /** One die type's clause of an entry's sentence, e.g. "D6: 6, 4 2 times, 3." */
    private fun groupSummary(die: DieType, vararg values: String): String =
        str(R.string.a11y_group_summary, die.label, values.joinToString(separator = ", "))

    /** One repeated value within such a clause, e.g. "4 2 times". */
    private fun tally(value: Int, count: Int): String =
        plural(R.plurals.history_face_tally, count, count, value)

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

        composeTestRule.onNodeWithText(str(R.string.history_header, 2)).assertIsDisplayed()
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
        composeTestRule.onNodeWithText(str(R.string.number, 26)).assertIsDisplayed()
        composeTestRule.onNodeWithText("1D20").assertIsDisplayed()
        composeTestRule.onNodeWithText(str(R.string.number, 14)).assertIsDisplayed()
    }

    @Test
    fun givenExpandedBand_whenDisplayed_thenEntriesShowHowLongAgoTheyHappened() {
        launchBand(isExpanded = true)

        composeTestRule.onNodeWithText(str(R.string.relative_time_just_now)).assertIsDisplayed()
        composeTestRule.onNodeWithText(plural(R.plurals.relative_time_minutes, 2))
            .assertIsDisplayed()
    }

    @Test
    fun givenExpandedBand_whenDisplayed_thenRepeatedValuesAreShownAsAMultiplier() {
        launchBand(isExpanded = true)

        // The 4 was rolled twice; the 6 only once and so carries no multiplier.
        composeTestRule.onNodeWithText(str(R.string.multiplier, 2)).assertIsDisplayed()
        composeTestRule.onNodeWithText(str(R.string.multiplier, 1)).assertDoesNotExist()
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
            .onNodeWithContentDescription(headerDescription(2, R.string.history_state_collapsed))
            .assertIsDisplayed()
    }

    @Test
    fun givenExpandedBand_whenDisplayed_thenHeaderAnnouncesItIsExpanded() {
        launchBand(isExpanded = true)

        composeTestRule
            .onNodeWithContentDescription(headerDescription(2, R.string.history_state_expanded))
            .assertIsDisplayed()
    }

    @Test
    fun givenSingleEntry_whenDisplayed_thenHeaderUsesTheSingularWording() {
        launchBand(history = history.take(1))

        composeTestRule
            .onNodeWithContentDescription(headerDescription(1, R.string.history_state_collapsed))
            .assertIsDisplayed()
    }

    @Test
    fun givenExpandedBand_whenDisplayed_thenEachEntryReadsAsOneSentence() {
        launchBand(isExpanded = true)

        val faces = groupSummary(Dice.D6, tally(6, 1), tally(4, 2), tally(3, 1)) +
            " " +
            groupSummary(Dice.D8, tally(7, 1), tally(2, 1))

        composeTestRule
            .onNodeWithContentDescription(
                str(
                    R.string.history_entry_description,
                    "4D6 + 2D8",
                    26,
                    str(R.string.relative_time_just_now),
                    faces,
                ),
            )
            .assertIsDisplayed()
    }

    @Test
    fun givenExpandedBand_whenDisplayed_thenTheArtworkItselfIsNotAnnounced() {
        launchBand(isExpanded = true)

        // Individual faces are cleared from the semantics tree, so the entry's own sentence is
        // the only thing a screen reader stops on.
        composeTestRule
            .onNodeWithContentDescription(
                str(
                    R.string.dice_image_description,
                    dieNotation(Dice.D6),
                    str(DiceColor.Ruby.labelRes),
                ),
            )
            .assertDoesNotExist()
    }
}
