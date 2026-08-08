// app/src/androidTest/java/fr/mandarine/diceroller/presentation/component/DiceResultDisplayTest.kt
package fr.mandarine.diceroller.presentation.component

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.mandarine.diceroller.domain.Dice
import fr.mandarine.diceroller.domain.DiceGroupResult
import fr.mandarine.diceroller.domain.DicePoolResult
import fr.mandarine.diceroller.domain.ValueTally
import fr.mandarine.diceroller.presentation.model.DiceColor
import fr.mandarine.diceroller.ui.theme.DiceRollerTheme
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

    // --- Total line ---

    @Test
    fun givenPopulatedResult_whenDisplayed_thenTotalLineIsShown() {
        launch(result = sampleResult)

        composeTestRule.onNodeWithText("Total 26").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Total 26").assertExists()
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
