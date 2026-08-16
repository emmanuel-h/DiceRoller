// app/src/androidTest/java/fr/mandarine/diceroller/presentation/component/DiceResultDisplayTest.kt
package fr.mandarine.diceroller.presentation.component

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.mandarine.diceroller.R
import fr.mandarine.diceroller.dieNotation
import fr.mandarine.diceroller.domain.Dice
import fr.mandarine.diceroller.domain.DiceGroupResult
import fr.mandarine.diceroller.domain.DicePoolResult
import fr.mandarine.diceroller.domain.ValueTally
import fr.mandarine.diceroller.groupHeader
import fr.mandarine.diceroller.plural
import fr.mandarine.diceroller.presentation.model.DiceColor
import fr.mandarine.diceroller.str
import fr.mandarine.diceroller.ui.theme.DiceRollerTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Compose UI tests for [DiceResultDisplay] — the per-die-type result face-ladder and total line
 * (issues #45 / #51, densified in #63). Covers the acceptance criteria: grouped entries sorted
 * descending by value with unrolled values omitted, entries wrapping along a line rather than
 * stacking, a demoted total line, the two-caption empty/pre-roll state, and the selected color
 * reaching every rendered die.
 */
@RunWith(AndroidJUnit4::class)
class DiceResultDisplayTest {

    /**
     * The `×` that every group header has between its count and its die.
     *
     * Only used to *count* headers on screen, which is why it survives issue #68 as a literal:
     * `result_group_header` is `translatable="false"` dice notation, so this marker is the same
     * in every locale.
     */
    private val groupHeaderMarker = "×D"

    @get:Rule
    val composeTestRule = createComposeRule()

    private val sampleResult = DicePoolResult(
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

    /** A single-group result where every die in the group landed on the same value. */
    private val singleGroupResult = DicePoolResult(
        groups = listOf(
            DiceGroupResult(
                dice = Dice.D20,
                poolCount = 3,
                tallies = listOf(ValueTally(value = 15, count = 3)),
            ),
        ),
        total = 45,
    )

    private fun launch(
        result: DicePoolResult? = null,
        isPoolEmpty: Boolean = true,
        selectedColor: DiceColor = DiceColor.Default,
    ) {
        composeTestRule.setContent {
            DiceRollerTheme(dynamicColor = false) {
                DiceResultDisplay(
                    result = result,
                    isPoolEmpty = isPoolEmpty,
                    selectedColor = selectedColor,
                )
            }
        }
    }

    /**
     * The die art inside one entry.
     *
     * Read from the unmerged tree: an entry merges its descendants into a single node so it is
     * announced as one unit ("Value 6, rolled 1 time"), which by design hides the art and count
     * nodes from the merged tree.
     */
    private fun rowArt(dice: Dice, value: Int, color: DiceColor) = composeTestRule.onNode(
        hasTestTag("dice-row-art-${dice.name}-$value-${color.name}"),
        useUnmergedTree = true,
    )

    /** A group header's spoken description, e.g. "4 D6 dice". */
    private fun groupDescription(poolCount: Int, notation: String): String =
        plural(R.plurals.result_group_description, poolCount, poolCount, notation)

    /** One face entry's spoken description, e.g. "Value 4, rolled 2 times". */
    private fun faceDescription(value: Int, count: Int): String =
        plural(R.plurals.result_face_description, count, value, count)

    /** Vertical position of the row/line identified by [contentDescription], for ordering checks. */
    private fun topOf(contentDescription: String): Float =
        composeTestRule
            .onNodeWithContentDescription(contentDescription)
            .fetchSemanticsNode()
            .boundsInRoot
            .top

    /**
     * Reading-order position of the entry identified by [contentDescription]: line first, then
     * position along that line.
     *
     * Entries now wrap, so two entries of the same group commonly share a `top` and are ordered
     * left to right; comparing `top` alone would call them equal.
     */
    private fun readingOrderOf(contentDescription: String): Pair<Float, Float> {
        val bounds = composeTestRule
            .onNodeWithContentDescription(contentDescription)
            .fetchSemanticsNode()
            .boundsInRoot
        return bounds.top to bounds.left
    }

    /** Asserts [earlier] is read before [later]: on an earlier line, or further left on the same. */
    private fun assertReadsBefore(earlier: String, later: String) {
        val (earlierTop, earlierLeft) = readingOrderOf(earlier)
        val (laterTop, laterLeft) = readingOrderOf(later)

        assertTrue(
            "Expected \"$earlier\" ($earlierTop, $earlierLeft) to be read before " +
                "\"$later\" ($laterTop, $laterLeft)",
            earlierTop < laterTop || (earlierTop == laterTop && earlierLeft < laterLeft),
        )
    }

    /** Asserts that the row identified by [rowDescription] contains both the die art tagged
     * [artTestTag] and the count text [countText] — i.e. the row pairs the correct art with the
     * correct multiplier, not merely that both exist somewhere on screen. */
    private fun assertRowPairsArtWithCount(rowDescription: String, artTestTag: String, countText: String) {
        composeTestRule
            .onNode(
                hasContentDescription(rowDescription) and
                    hasAnyDescendant(hasTestTag(artTestTag)) and
                    hasAnyDescendant(hasText(countText)),
                // The entry's descendants only exist as separate nodes in the unmerged tree;
                // merged, they collapse into the single node this same matcher describes.
                useUnmergedTree = true,
            )
            .assertExists()
    }

    // --- Empty state ---

    @Test
    fun givenEmptyPool_whenDisplayed_thenAddDiceCaptionIsShown() {
        launch(result = null, isPoolEmpty = true)

        composeTestRule.onNodeWithText(str(R.string.result_empty_pool_caption)).assertIsDisplayed()
    }

    @Test
    fun givenNonEmptyPoolNotYetRolled_whenDisplayed_thenTapRollCaptionIsShown() {
        launch(result = null, isPoolEmpty = false)

        composeTestRule.onNodeWithText(str(R.string.result_not_rolled_caption)).assertIsDisplayed()
    }

    @Test
    fun givenEmptyPool_whenDisplayed_thenTapRollCaptionIsNotShown() {
        launch(result = null, isPoolEmpty = true)

        composeTestRule.onNodeWithText(str(R.string.result_not_rolled_caption)).assertDoesNotExist()
    }

    @Test
    fun givenNullResult_whenDisplayed_thenNoGroupHeaderIsShown() {
        launch(result = null, isPoolEmpty = false)

        composeTestRule.onNodeWithText(groupHeader(4, dieNotation(Dice.D6))).assertDoesNotExist()
    }

    // --- Populated state: group headers ---

    @Test
    fun givenPopulatedResult_whenDisplayed_thenEachGroupHeaderIsShown() {
        launch(result = sampleResult)

        composeTestRule.onNodeWithText(groupHeader(4, dieNotation(Dice.D6))).assertIsDisplayed()
        composeTestRule.onNodeWithText(groupHeader(2, dieNotation(Dice.D8))).assertIsDisplayed()
    }

    @Test
    fun givenPopulatedResult_whenDisplayed_thenGroupHeaderDescribesDiceCount() {
        launch(result = sampleResult)

        composeTestRule.onNodeWithContentDescription(groupDescription(4, dieNotation(Dice.D6)))
            .assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(groupDescription(2, dieNotation(Dice.D8)))
            .assertIsDisplayed()
    }

    @Test
    fun givenPopulatedResult_whenDisplayed_thenExactlyOneGroupHeaderExistsPerDieTypeInTheResult() {
        launch(result = sampleResult)

        composeTestRule.onAllNodesWithText(groupHeaderMarker, substring = true)
            .assertCountEquals(2)
    }

    @Test
    fun givenPopulatedResult_whenDisplayed_thenNoGroupHeaderAppearsForADieTypeAbsentFromTheResult() {
        launch(result = sampleResult)

        // The result only covers D6 and D8; D20 was never part of this pool/roll.
        composeTestRule.onNodeWithContentDescription(groupDescription(3, dieNotation(Dice.D20)))
            .assertDoesNotExist()
    }

    @Test
    fun givenASingleDieTypeResult_whenDisplayed_thenOnlyThatOneGroupHeaderIsShown() {
        launch(result = singleGroupResult)

        composeTestRule.onNodeWithText(groupHeader(3, dieNotation(Dice.D20))).assertIsDisplayed()
        composeTestRule.onAllNodesWithText(groupHeaderMarker, substring = true)
            .assertCountEquals(1)
    }

    // --- Populated state: rows ---

    @Test
    fun givenPopulatedResult_whenDisplayed_thenEveryRolledValueRowExists() {
        launch(result = sampleResult)

        composeTestRule
            .onNodeWithContentDescription(faceDescription(value = 6, count = 1))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithContentDescription(faceDescription(value = 4, count = 2))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithContentDescription(faceDescription(value = 3, count = 1))
            .assertIsDisplayed()
    }

    @Test
    fun givenPopulatedResult_whenDisplayed_thenValuesNeverRolledProduceNoRow() {
        launch(result = sampleResult)

        // D6 never rolled a 5 in this sample result.
        composeTestRule.onNodeWithContentDescription(faceDescription(value = 5, count = 1))
            .assertDoesNotExist()
    }

    @Test
    fun givenPopulatedResult_whenDisplayed_thenEveryEntryShowsItsOwnMultiplier() {
        launch(result = sampleResult)

        // The sample rolled four values once (D6 6, D6 3, D8 7, D8 2) and one value twice (D6 4),
        // so the multipliers are counted, not merely shown to exist somewhere.
        composeTestRule.onAllNodesWithText(str(R.string.multiplier, 1), useUnmergedTree = true)
            .assertCountEquals(4)
        composeTestRule.onAllNodesWithText(str(R.string.multiplier, 2), useUnmergedTree = true)
            .assertCountEquals(1)
    }

    @Test
    fun givenAGroupWhereEveryDieLandedOnTheSameValue_whenDisplayed_thenExactlyOneRowExistsForIt() {
        launch(result = singleGroupResult)

        composeTestRule.onNodeWithContentDescription(faceDescription(value = 15, count = 3))
            .assertIsDisplayed()
    }

    @Test
    fun givenAGroupWhereEveryDieLandedOnTheSameValue_whenDisplayed_thenNoOtherValueRowExistsForIt() {
        launch(result = singleGroupResult)

        // Only 15 was ever rolled in this group; every other D20 face must have zero rows.
        composeTestRule.onNodeWithContentDescription(faceDescription(value = 14, count = 1))
            .assertDoesNotExist()
    }

    // --- Populated state: entry ordering (descending by value, within and across groups) ---

    @Test
    fun givenPopulatedResult_whenDisplayed_thenTheD6SixEntryIsReadBeforeTheD6FourEntry() {
        launch(result = sampleResult)

        assertReadsBefore(
            faceDescription(value = 6, count = 1),
            faceDescription(value = 4, count = 2),
        )
    }

    @Test
    fun givenPopulatedResult_whenDisplayed_thenTheD6FourEntryIsReadBeforeTheD6ThreeEntry() {
        launch(result = sampleResult)

        assertReadsBefore(
            faceDescription(value = 4, count = 2),
            faceDescription(value = 3, count = 1),
        )
    }

    @Test
    fun givenPopulatedResult_whenDisplayed_thenTheD8SevenEntryIsReadBeforeTheD8TwoEntry() {
        launch(result = sampleResult)

        assertReadsBefore(
            faceDescription(value = 7, count = 1),
            faceDescription(value = 2, count = 1),
        )
    }

    @Test
    fun givenPopulatedResult_whenDisplayed_thenTheD8GroupAppearsBelowTheD6Group() {
        launch(result = sampleResult)

        // Across groups the separation is still strictly vertical, wrapping or not.
        val lastD6EntryTop = topOf(faceDescription(value = 3, count = 1))
        val firstD8EntryTop = topOf(faceDescription(value = 7, count = 1))

        assertTrue(lastD6EntryTop < firstD8EntryTop)
    }

    @Test
    fun givenAGroupWithSeveralValues_whenDisplayed_thenItsEntriesShareALineInsteadOfStacking() {
        launch(result = sampleResult)

        // The density goal of issue #63: a die type costs about one line, not one per value.
        val sixTop = topOf(faceDescription(value = 6, count = 1))
        val fourTop = topOf(faceDescription(value = 4, count = 2))

        assertTrue(
            "Expected the D6 entries to wrap onto one line, got tops $sixTop and $fourTop",
            sixTop == fourTop,
        )
    }

    // --- Populated state: each row pairs its own die art with its own count ---

    @Test
    fun givenPopulatedResult_whenDisplayed_thenTheD6SixRowPairsItsOwnArtWithItsOwnCount() {
        launch(result = sampleResult, selectedColor = DiceColor.Ruby)

        assertRowPairsArtWithCount(
            rowDescription = faceDescription(value = 6, count = 1),
            artTestTag = "dice-row-art-D6-6-Ruby",
            countText = str(R.string.multiplier, 1),
        )
    }

    @Test
    fun givenPopulatedResult_whenDisplayed_thenTheD6FourRowPairsItsOwnArtWithItsOwnCount() {
        launch(result = sampleResult, selectedColor = DiceColor.Ruby)

        assertRowPairsArtWithCount(
            rowDescription = faceDescription(value = 4, count = 2),
            artTestTag = "dice-row-art-D6-4-Ruby",
            countText = str(R.string.multiplier, 2),
        )
    }

    @Test
    fun givenPopulatedResult_whenDisplayed_thenTheD8SevenRowPairsItsOwnArtWithItsOwnCount() {
        launch(result = sampleResult, selectedColor = DiceColor.Ruby)

        assertRowPairsArtWithCount(
            rowDescription = faceDescription(value = 7, count = 1),
            artTestTag = "dice-row-art-D8-7-Ruby",
            countText = str(R.string.multiplier, 1),
        )
    }

    @Test
    fun givenPopulatedResult_whenDisplayed_thenTheD8TwoRowPairsItsOwnArtWithItsOwnCount() {
        launch(result = sampleResult, selectedColor = DiceColor.Ruby)

        assertRowPairsArtWithCount(
            rowDescription = faceDescription(value = 2, count = 1),
            artTestTag = "dice-row-art-D8-2-Ruby",
            countText = str(R.string.multiplier, 1),
        )
    }

    // --- Total line ---

    @Test
    fun givenPopulatedResult_whenDisplayed_thenTotalLineIsShown() {
        launch(result = sampleResult)

        composeTestRule.onNodeWithText(str(R.string.result_total, 26)).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(str(R.string.result_total, 26)).assertExists()
    }

    @Test
    fun givenPopulatedResult_whenDisplayed_thenTotalLineAppearsBelowEveryGroupsRows() {
        launch(result = sampleResult)

        val lastRowTop = topOf(faceDescription(value = 2, count = 1))
        val totalTop = topOf(str(R.string.result_total, 26))

        assertTrue(lastRowTop < totalTop)
    }

    @Test
    fun givenASingleDieTypeResult_whenDisplayed_thenTotalLineMatchesThatResultsSum() {
        launch(result = singleGroupResult)

        composeTestRule.onNodeWithText(str(R.string.result_total, 45)).assertIsDisplayed()
    }

    // --- Color applied to every group ---

    @Test
    fun givenRubySelected_whenDisplayed_thenEveryRowUsesRuby() {
        launch(result = sampleResult, selectedColor = DiceColor.Ruby)

        rowArt(Dice.D6, 6, DiceColor.Ruby).assertExists()
        rowArt(Dice.D6, 4, DiceColor.Ruby).assertExists()
        rowArt(Dice.D8, 7, DiceColor.Ruby).assertExists()
        rowArt(Dice.D8, 2, DiceColor.Ruby).assertExists()
    }

    @Test
    fun givenJadeSelected_whenDisplayed_thenNoRowUsesTheOldRubyColor() {
        launch(result = sampleResult, selectedColor = DiceColor.Jade)

        rowArt(Dice.D6, 6, DiceColor.Jade).assertExists()
        rowArt(Dice.D6, 6, DiceColor.Ruby).assertDoesNotExist()
    }
}
