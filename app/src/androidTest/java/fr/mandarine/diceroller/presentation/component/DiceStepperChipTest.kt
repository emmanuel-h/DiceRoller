// app/src/androidTest/java/fr/mandarine/diceroller/presentation/component/DiceStepperChipTest.kt
package fr.mandarine.diceroller.presentation.component

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.mandarine.diceroller.domain.Dice
import fr.mandarine.diceroller.domain.DicePool
import fr.mandarine.diceroller.presentation.model.DiceColor
import fr.mandarine.diceroller.ui.theme.DiceRollerTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Compose UI tests for [DiceStepperChip], mounted standalone rather than through
 * `DiceRollerScreen`/`MainActivity` — wiring the chip into the screen is issue #52's scope.
 */
@RunWith(AndroidJUnit4::class)
class DiceStepperChipTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun decreaseButton(dice: Dice = Dice.D6) =
        composeTestRule.onNodeWithContentDescription("Decrease ${dice.name} count")

    private fun increaseButton(dice: Dice = Dice.D6) =
        composeTestRule.onNodeWithContentDescription("Increase ${dice.name} count")

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
