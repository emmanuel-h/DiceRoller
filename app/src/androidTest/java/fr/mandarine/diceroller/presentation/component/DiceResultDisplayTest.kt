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
import fr.mandarine.diceroller.domain.Dice
import fr.mandarine.diceroller.domain.DiceGroupResult
import fr.mandarine.diceroller.domain.DicePoolResult
import fr.mandarine.diceroller.domain.ValueTally
import fr.mandarine.diceroller.presentation.model.DiceColor
import fr.mandarine.diceroller.ui.theme.DiceRollerTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Compose UI tests for [DiceResultDisplay] — the per-die-type result face-ladder and total line
 * (issue #45 / #51). Covers the acceptance criteria: grouped rows sorted descending by value with
 * unrolled values omitted, a fixed-width count column, a demoted total line, the two-caption
 * empty/pre-roll state, and the selected color reaching every rendered die.
 */
@RunWith(AndroidJUnit4::class)
class DiceResultDisplayTest {

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

    private fun rowArt(dice: Dice, value: Int, color: DiceColor) =
        composeTestRule.onNode(hasTestTag("dice-row-art-${dice.name}-$value-${color.name}"))

    /** Vertical position of the row/line identified by [contentDescription], for ordering checks. */
    private fun topOf(contentDescription: String): Float =
        composeTestRule
            .onNodeWithContentDescription(contentDescription)
            .fetchSemanticsNode()
            .boundsInRoot
            .top

    /** Asserts that the row identified by [rowDescription] contains both the die art tagged
     * [artTestTag] and the count text [countText] — i.e. the row pairs the correct art with the
     * correct multiplier, not merely that both exist somewhere on screen. */
    private fun assertRowPairsArtWithCount(rowDescription: String, artTestTag: String, countText: String) {
        composeTestRule
            .onNode(
                hasContentDescription(rowDescription) and
                    hasAnyDescendant(hasTestTag(artTestTag)) and
                    hasAnyDescendant(hasText(countText)),
            )
            .assertExists()
    }

    // --- Empty state ---

    @Test
    fun givenEmptyPool_whenDisplayed_thenAddDiceCaptionIsShown() {
        launch(result = null, isPoolEmpty = true)

        composeTestRule.onNodeWithText("Add dice above to build your pool.").assertIsDisplayed()
    }

    @Test
    fun givenNonEmptyPoolNotYetRolled_whenDisplayed_thenTapRollCaptionIsShown() {
        launch(result = null, isPoolEmpty = false)

        composeTestRule.onNodeWithText("Tap Roll to see results.").assertIsDisplayed()
    }

    @Test
    fun givenEmptyPool_whenDisplayed_thenTapRollCaptionIsNotShown() {
        launch(result = null, isPoolEmpty = true)

        composeTestRule.onNodeWithText("Tap Roll to see results.").assertDoesNotExist()
    }

    @Test
    fun givenNullResult_whenDisplayed_thenNoGroupHeaderIsShown() {
        launch(result = null, isPoolEmpty = false)

        composeTestRule.onNodeWithText("4×D6").assertDoesNotExist()
    }

    // --- Populated state: group headers ---

    @Test
    fun givenPopulatedResult_whenDisplayed_thenEachGroupHeaderIsShown() {
        launch(result = sampleResult)

        composeTestRule.onNodeWithText("4×D6").assertIsDisplayed()
        composeTestRule.onNodeWithText("2×D8").assertIsDisplayed()
    }

    @Test
    fun givenPopulatedResult_whenDisplayed_thenGroupHeaderDescribesDiceCount() {
        launch(result = sampleResult)

        composeTestRule.onNodeWithContentDescription("4 D6 dice").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("2 D8 dice").assertIsDisplayed()
    }

    @Test
    fun givenPopulatedResult_whenDisplayed_thenExactlyOneGroupHeaderExistsPerDieTypeInTheResult() {
        launch(result = sampleResult)

        composeTestRule.onAllNodesWithText("×D", substring = true).assertCountEquals(2)
    }

    @Test
    fun givenPopulatedResult_whenDisplayed_thenNoGroupHeaderAppearsForADieTypeAbsentFromTheResult() {
        launch(result = sampleResult)

        // The result only covers D6 and D8; D20 was never part of this pool/roll.
        composeTestRule.onNodeWithContentDescription("3 D20 dice").assertDoesNotExist()
    }

    @Test
    fun givenASingleDieTypeResult_whenDisplayed_thenOnlyThatOneGroupHeaderIsShown() {
        launch(result = singleGroupResult)

        composeTestRule.onNodeWithText("3×D20").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("×D", substring = true).assertCountEquals(1)
    }

    // --- Populated state: rows ---

    @Test
    fun givenPopulatedResult_whenDisplayed_thenEveryRolledValueRowExists() {
        launch(result = sampleResult)

        composeTestRule
            .onNodeWithContentDescription("Value 6, rolled 1 time")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithContentDescription("Value 4, rolled 2 times")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithContentDescription("Value 3, rolled 1 time")
            .assertIsDisplayed()
    }

    @Test
    fun givenPopulatedResult_whenDisplayed_thenValuesNeverRolledProduceNoRow() {
        launch(result = sampleResult)

        // D6 never rolled a 5 in this sample result.
        composeTestRule.onNodeWithContentDescription("Value 5, rolled 1 time").assertDoesNotExist()
    }

    @Test
    fun givenPopulatedResult_whenDisplayed_thenCountColumnShowsMultiplier() {
        launch(result = sampleResult)

        composeTestRule.onNodeWithText("×1").assertExists()
        composeTestRule.onNodeWithText("×2").assertExists()
    }

    @Test
    fun givenAGroupWhereEveryDieLandedOnTheSameValue_whenDisplayed_thenExactlyOneRowExistsForIt() {
        launch(result = singleGroupResult)

        composeTestRule.onNodeWithContentDescription("Value 15, rolled 3 times").assertIsDisplayed()
    }

    @Test
    fun givenAGroupWhereEveryDieLandedOnTheSameValue_whenDisplayed_thenNoOtherValueRowExistsForIt() {
        launch(result = singleGroupResult)

        // Only 15 was ever rolled in this group; every other D20 face must have zero rows.
        composeTestRule.onNodeWithContentDescription("Value 14, rolled 1 time").assertDoesNotExist()
    }

    // --- Populated state: row ordering (descending by value, within and across groups) ---

    @Test
    fun givenPopulatedResult_whenDisplayed_thenTheD6SixRowAppearsAboveTheD6FourRow() {
        launch(result = sampleResult)

        val sixTop = topOf("Value 6, rolled 1 time")
        val fourTop = topOf("Value 4, rolled 2 times")

        assertTrue(sixTop < fourTop)
    }

    @Test
    fun givenPopulatedResult_whenDisplayed_thenTheD6FourRowAppearsAboveTheD6ThreeRow() {
        launch(result = sampleResult)

        val fourTop = topOf("Value 4, rolled 2 times")
        val threeTop = topOf("Value 3, rolled 1 time")

        assertTrue(fourTop < threeTop)
    }

    @Test
    fun givenPopulatedResult_whenDisplayed_thenTheD8SevenRowAppearsAboveTheD8TwoRow() {
        launch(result = sampleResult)

        val sevenTop = topOf("Value 7, rolled 1 time")
        val twoTop = topOf("Value 2, rolled 1 time")

        assertTrue(sevenTop < twoTop)
    }

    @Test
    fun givenPopulatedResult_whenDisplayed_thenTheD8GroupAppearsBelowTheD6Group() {
        launch(result = sampleResult)

        val lastD6RowTop = topOf("Value 3, rolled 1 time")
        val firstD8RowTop = topOf("Value 7, rolled 1 time")

        assertTrue(lastD6RowTop < firstD8RowTop)
    }

    // --- Populated state: each row pairs its own die art with its own count ---

    @Test
    fun givenPopulatedResult_whenDisplayed_thenTheD6SixRowPairsItsOwnArtWithItsOwnCount() {
        launch(result = sampleResult, selectedColor = DiceColor.Ruby)

        assertRowPairsArtWithCount(
            rowDescription = "Value 6, rolled 1 time",
            artTestTag = "dice-row-art-D6-6-Ruby",
            countText = "×1",
        )
    }

    @Test
    fun givenPopulatedResult_whenDisplayed_thenTheD6FourRowPairsItsOwnArtWithItsOwnCount() {
        launch(result = sampleResult, selectedColor = DiceColor.Ruby)

        assertRowPairsArtWithCount(
            rowDescription = "Value 4, rolled 2 times",
            artTestTag = "dice-row-art-D6-4-Ruby",
            countText = "×2",
        )
    }

    @Test
    fun givenPopulatedResult_whenDisplayed_thenTheD8SevenRowPairsItsOwnArtWithItsOwnCount() {
        launch(result = sampleResult, selectedColor = DiceColor.Ruby)

        assertRowPairsArtWithCount(
            rowDescription = "Value 7, rolled 1 time",
            artTestTag = "dice-row-art-D8-7-Ruby",
            countText = "×1",
        )
    }

    @Test
    fun givenPopulatedResult_whenDisplayed_thenTheD8TwoRowPairsItsOwnArtWithItsOwnCount() {
        launch(result = sampleResult, selectedColor = DiceColor.Ruby)

        assertRowPairsArtWithCount(
            rowDescription = "Value 2, rolled 1 time",
            artTestTag = "dice-row-art-D8-2-Ruby",
            countText = "×1",
        )
    }

    // --- Total line ---

    @Test
    fun givenPopulatedResult_whenDisplayed_thenTotalLineIsShown() {
        launch(result = sampleResult)

        composeTestRule.onNodeWithText("Total 26").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Total 26").assertExists()
    }

    @Test
    fun givenPopulatedResult_whenDisplayed_thenTotalLineAppearsBelowEveryGroupsRows() {
        launch(result = sampleResult)

        val lastRowTop = topOf("Value 2, rolled 1 time")
        val totalTop = topOf("Total 26")

        assertTrue(lastRowTop < totalTop)
    }

    @Test
    fun givenASingleDieTypeResult_whenDisplayed_thenTotalLineMatchesThatResultsSum() {
        launch(result = singleGroupResult)

        composeTestRule.onNodeWithText("Total 45").assertIsDisplayed()
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
