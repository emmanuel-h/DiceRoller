// app/src/androidTest/java/fr/mandarine/diceroller/presentation/component/DiceStepperChipTest.kt
package fr.mandarine.diceroller.presentation.component

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.mandarine.diceroller.domain.Dice
import fr.mandarine.diceroller.domain.DicePool
import fr.mandarine.diceroller.presentation.model.DiceColor
import fr.mandarine.diceroller.ui.theme.DiceRollerTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Compose UI tests for [DiceStepperChip], mounted standalone rather than through
 * `DiceRollerScreen`/`MainActivity`, which `DiceRollerScreenTest` covers.
 *
 * Since issue #62 the chip has no stepper buttons: its left and right halves are the decrement
 * and increment targets. They keep the same accessibility descriptions the buttons had, so most
 * of these tests express the same contract against the new interaction.
 */
@RunWith(AndroidJUnit4::class)
class DiceStepperChipTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun decreaseButton(dice: Dice = Dice.D6) =
        composeTestRule.onNodeWithContentDescription("Decrease ${dice.name} count")

    private fun increaseButton(dice: Dice = Dice.D6) =
        composeTestRule.onNodeWithContentDescription("Increase ${dice.name} count")

    /** On-screen bounds of a half, in dp, for the split-layout assertions. */
    private fun boundsOf(node: SemanticsNodeInteraction): DpRect =
        node.fetchSemanticsNode().let { semanticsNode ->
            with(composeTestRule.density) {
                val bounds = semanticsNode.boundsInRoot
                DpRect(
                    left = bounds.left.toDp(),
                    top = bounds.top.toDp(),
                    right = bounds.right.toDp(),
                    bottom = bounds.bottom.toDp(),
                )
            }
        }

    private fun launchChip(
        dice: Dice = Dice.D6,
        count: Int = 0,
        color: DiceColor = DiceColor.Default,
        onIncrement: () -> Unit = {},
        onDecrement: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            DiceRollerTheme(dynamicColor = false) {
                DiceStepperChip(
                    dice = dice,
                    count = count,
                    color = color,
                    onIncrement = onIncrement,
                    onDecrement = onDecrement,
                )
            }
        }
    }

    /** Mounts a stateful chip so tapping the buttons visibly changes the rendered count. */
    private fun launchStatefulChip(dice: Dice = Dice.D6, initialCount: Int = 0) {
        composeTestRule.setContent {
            DiceRollerTheme(dynamicColor = false) {
                var count by remember { mutableIntStateOf(initialCount) }
                DiceStepperChip(
                    dice = dice,
                    count = count,
                    color = DiceColor.Default,
                    onIncrement = { count = (count + 1).coerceAtMost(DicePool.MAX_DICE_PER_TYPE) },
                    onDecrement = { count = (count - 1).coerceAtLeast(0) },
                )
            }
        }
    }

    // -------------------------------------------------------------------------
    // Rendering
    // -------------------------------------------------------------------------

    @Test
    fun givenAnyCount_whenChipIsRendered_thenTheDieLabelIsShown() {
        launchChip(dice = Dice.D12, count = 3)

        composeTestRule.onNodeWithText("D12").assertIsDisplayed()
    }

    @Test
    fun givenAnyCount_whenChipIsRendered_thenTheCountIsShown() {
        launchChip(dice = Dice.D6, count = 5)

        composeTestRule.onNodeWithText("5").assertIsDisplayed()
    }

    @Test
    fun givenAnyCount_whenChipIsRendered_thenTheDieArtIsDecorative() {
        launchChip(dice = Dice.D6, count = 0, color = DiceColor.Amethyst)

        // A labelled control ("D6", plus the two stepper buttons) already conveys the die type,
        // so the artwork itself must carry no separate content description.
        composeTestRule
            .onNodeWithContentDescription("${Dice.D6.name}, ${DiceColor.Amethyst.label}")
            .assertDoesNotExist()
    }

    // -------------------------------------------------------------------------
    // The chip is the control: left half decrements, right half increments (#62)
    // -------------------------------------------------------------------------

    @Test
    fun givenAnyCount_whenChipIsRendered_thenTheDecrementTargetIsTheLeftHalfAndIncrementTheRight() {
        launchChip(count = 4)

        val left = boundsOf(decreaseButton())
        val right = boundsOf(increaseButton())

        assertTrue(
            "Expected the decrement half left of the increment half, got $left and $right",
            left.left < right.left && left.right <= right.left,
        )
    }

    @Test
    fun givenAnyCount_whenChipIsRendered_thenTheTwoHalvesAreTheSameWidth() {
        launchChip(count = 4)

        val leftWidth = boundsOf(decreaseButton()).let { it.right - it.left }
        val rightWidth = boundsOf(increaseButton()).let { it.right - it.left }

        assertEquals(leftWidth.value, rightWidth.value, 1f)
    }

    @Test
    fun givenAnyCount_whenChipIsRendered_thenBothHalvesMeetTheMinimumTouchTarget() {
        launchChip(count = 4)

        listOf(decreaseButton(), increaseButton()).forEach { half ->
            val bounds = boundsOf(half)
            val width = bounds.right - bounds.left
            val height = bounds.bottom - bounds.top

            assertTrue("Half is only ${width}×${height}", width >= 48.dp && height >= 48.dp)
        }
    }

    @Test
    fun givenAZeroCountChip_whenTheRightHalfIsTapped_thenTheCountRises() {
        launchStatefulChip(dice = Dice.D6, initialCount = 0)

        increaseButton(Dice.D6).performClick()

        composeTestRule.onNodeWithText("1").assertIsDisplayed()
    }

    @Test
    fun givenANonZeroCountChip_whenTheLeftHalfIsTapped_thenTheCountFalls() {
        launchStatefulChip(dice = Dice.D6, initialCount = 3)

        decreaseButton(Dice.D6).performClick()

        composeTestRule.onNodeWithText("2").assertIsDisplayed()
    }

    @Test
    fun givenAZeroCountChip_whenTheLeftHalfIsTapped_thenTheCountStaysClampedAtZero() {
        launchStatefulChip(dice = Dice.D6, initialCount = 0)

        decreaseButton(Dice.D6).performClick()

        composeTestRule.onNodeWithText("0").assertIsDisplayed()
    }

    // -------------------------------------------------------------------------
    // Enabled/disabled state per the design spec's count -> control table
    // -------------------------------------------------------------------------

    @Test
    fun givenCountAtZero_whenChipIsRendered_thenDecreaseIsDisabledAndIncreaseIsEnabled() {
        launchChip(count = 0)

        decreaseButton().assertIsNotEnabled()
        increaseButton().assertIsEnabled()
    }

    @Test
    fun givenMidRangeCount_whenChipIsRendered_thenBothControlsAreEnabled() {
        launchChip(count = 10)

        decreaseButton().assertIsEnabled()
        increaseButton().assertIsEnabled()
    }

    @Test
    fun givenCountAtCap_whenChipIsRendered_thenIncreaseIsDisabledAndDecreaseIsEnabled() {
        launchChip(count = DicePool.MAX_DICE_PER_TYPE)

        increaseButton().assertIsNotEnabled()
        decreaseButton().assertIsEnabled()
    }

    // -------------------------------------------------------------------------
    // Callbacks
    // -------------------------------------------------------------------------

    @Test
    fun givenMidRangeCount_whenIncreaseIsTapped_thenOnIncrementFires() {
        var incremented = false
        launchChip(count = 4, onIncrement = { incremented = true })

        increaseButton().performClick()

        assert(incremented)
    }

    @Test
    fun givenMidRangeCount_whenDecreaseIsTapped_thenOnDecrementFires() {
        var decremented = false
        launchChip(count = 4, onDecrement = { decremented = true })

        decreaseButton().performClick()

        assert(decremented)
    }

    @Test
    fun givenAStatefulChip_whenIncreaseIsTappedRepeatedly_thenTheDisplayedCountFollows() {
        launchStatefulChip(dice = Dice.D8, initialCount = 0)

        increaseButton(Dice.D8).performClick()
        increaseButton(Dice.D8).performClick()
        increaseButton(Dice.D8).performClick()

        composeTestRule.onNodeWithText("3").assertIsDisplayed()
    }

    @Test
    fun givenAStatefulChipAtCap_whenIncreaseIsTapped_thenTheCountStaysClampedAndControlDisables() {
        launchStatefulChip(dice = Dice.D4, initialCount = DicePool.MAX_DICE_PER_TYPE - 1)

        increaseButton(Dice.D4).performClick()

        composeTestRule.onNodeWithText("${DicePool.MAX_DICE_PER_TYPE}").assertIsDisplayed()
        increaseButton(Dice.D4).assertIsNotEnabled()
    }

    // -------------------------------------------------------------------------
    // Accessibility
    // -------------------------------------------------------------------------

    @Test
    fun givenAnyCount_whenChipIsRendered_thenStepperDescriptionsAreStaticNotEmbeddingTheCount() {
        launchChip(dice = Dice.D10, count = 7)

        // Descriptions must not read e.g. "Decrease D10 count, 7" — the count lives in
        // stateDescription instead, asserted below.
        composeTestRule.onNodeWithContentDescription("Decrease D10 count").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Increase D10 count").assertIsDisplayed()
    }

    @Test
    fun givenACount_whenChipIsRendered_thenTheCountNodeCarriesAMatchingStateDescription() {
        launchChip(dice = Dice.D6, count = 9)

        composeTestRule
            .onNode(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "9"))
            .assertIsDisplayed()
    }
}
