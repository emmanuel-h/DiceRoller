// app/src/androidTest/java/fr/mandarine/diceroller/DiceStaticAssetsTest.kt
package fr.mandarine.diceroller

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.mandarine.diceroller.domain.Dice
import fr.mandarine.diceroller.presentation.component.DiceImage
import fr.mandarine.diceroller.presentation.component.DiceImageSize
import fr.mandarine.diceroller.presentation.model.DiceAsset
import fr.mandarine.diceroller.ui.theme.DiceRollerTheme
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests for [DiceAsset] and [DiceImage].
 *
 * [DiceAsset.fromDice] returns Android resource IDs (R.drawable.*), so these
 * tests run in the instrumented source set where the R class is available.
 */
@RunWith(AndroidJUnit4::class)
class DiceStaticAssetsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // -------------------------------------------------------------------------
    // DiceAsset.fromDice — resource ID contract
    // -------------------------------------------------------------------------

    @Test
    fun givenD4_whenFromDiceIsCalled_thenReturnsNonZeroResourceId() {
        val resId = DiceAsset.fromDice(Dice.D4)

        assertTrue("Expected non-zero resource ID for D4", resId != 0)
    }

    @Test
    fun givenD6_whenFromDiceIsCalled_thenReturnsNonZeroResourceId() {
        val resId = DiceAsset.fromDice(Dice.D6)

        assertTrue("Expected non-zero resource ID for D6", resId != 0)
    }

    @Test
    fun givenD8_whenFromDiceIsCalled_thenReturnsNonZeroResourceId() {
        val resId = DiceAsset.fromDice(Dice.D8)

        assertTrue("Expected non-zero resource ID for D8", resId != 0)
    }

    @Test
    fun givenD12_whenFromDiceIsCalled_thenReturnsNonZeroResourceId() {
        val resId = DiceAsset.fromDice(Dice.D12)

        assertTrue("Expected non-zero resource ID for D12", resId != 0)
    }

    @Test
    fun givenD20_whenFromDiceIsCalled_thenReturnsNonZeroResourceId() {
        val resId = DiceAsset.fromDice(Dice.D20)

        assertTrue("Expected non-zero resource ID for D20", resId != 0)
    }

    @Test
    fun givenAllDiceTypes_whenFromDiceIsCalled_thenAllResourceIdsAreDistinct() {
        val ids = Dice.entries.map { DiceAsset.fromDice(it) }

        val distinctIds = ids.toSet()
        assertTrue(
            "Expected all five resource IDs to be distinct, got: $ids",
            distinctIds.size == Dice.entries.size,
        )
    }

    @Test
    fun givenD4AndD6_whenFromDiceIsCalled_thenResourceIdsAreDifferent() {
        val d4Id = DiceAsset.fromDice(Dice.D4)
        val d6Id = DiceAsset.fromDice(Dice.D6)

        assertNotEquals("D4 and D6 must map to different resource IDs", d4Id, d6Id)
    }

    @Test
    fun givenD8AndD20_whenFromDiceIsCalled_thenResourceIdsAreDifferent() {
        val d8Id = DiceAsset.fromDice(Dice.D8)
        val d20Id = DiceAsset.fromDice(Dice.D20)

        assertNotEquals("D8 and D20 must map to different resource IDs", d8Id, d20Id)
    }

    @Test
    fun givenAllDiceTypes_whenFromDiceIsCalledTwice_thenResultIsStable() {
        Dice.entries.forEach { dice ->
            val first = DiceAsset.fromDice(dice)
            val second = DiceAsset.fromDice(dice)
            assertTrue(
                "fromDice($dice) must return the same value on repeated calls",
                first == second,
            )
        }
    }

    // -------------------------------------------------------------------------
    // DiceImage — renders without error for each die type and size
    // -------------------------------------------------------------------------

    @Test
    fun givenD4_whenDiceImageIsRenderedSmall_thenNodeWithContentDescriptionIsDisplayed() {
        composeTestRule.setContent {
            DiceRollerTheme(dynamicColor = false) {
                DiceImage(dice = Dice.D4, sizeVariant = DiceImageSize.Small)
            }
        }

        composeTestRule.onNodeWithContentDescription("D4 die").assertIsDisplayed()
    }

    @Test
    fun givenD6_whenDiceImageIsRenderedSmall_thenNodeWithContentDescriptionIsDisplayed() {
        composeTestRule.setContent {
            DiceRollerTheme(dynamicColor = false) {
                DiceImage(dice = Dice.D6, sizeVariant = DiceImageSize.Small)
            }
        }

        composeTestRule.onNodeWithContentDescription("D6 die").assertIsDisplayed()
    }

    @Test
    fun givenD8_whenDiceImageIsRenderedSmall_thenNodeWithContentDescriptionIsDisplayed() {
        composeTestRule.setContent {
            DiceRollerTheme(dynamicColor = false) {
                DiceImage(dice = Dice.D8, sizeVariant = DiceImageSize.Small)
            }
        }

        composeTestRule.onNodeWithContentDescription("D8 die").assertIsDisplayed()
    }

    @Test
    fun givenD12_whenDiceImageIsRenderedSmall_thenNodeWithContentDescriptionIsDisplayed() {
        composeTestRule.setContent {
            DiceRollerTheme(dynamicColor = false) {
                DiceImage(dice = Dice.D12, sizeVariant = DiceImageSize.Small)
            }
        }

        composeTestRule.onNodeWithContentDescription("D12 die").assertIsDisplayed()
    }

    @Test
    fun givenD20_whenDiceImageIsRenderedSmall_thenNodeWithContentDescriptionIsDisplayed() {
        composeTestRule.setContent {
            DiceRollerTheme(dynamicColor = false) {
                DiceImage(dice = Dice.D20, sizeVariant = DiceImageSize.Small)
            }
        }

        composeTestRule.onNodeWithContentDescription("D20 die").assertIsDisplayed()
    }

    @Test
    fun givenD4_whenDiceImageIsRenderedLarge_thenNodeWithContentDescriptionIsDisplayed() {
        composeTestRule.setContent {
            DiceRollerTheme(dynamicColor = false) {
                DiceImage(dice = Dice.D4, sizeVariant = DiceImageSize.Large)
            }
        }

        composeTestRule.onNodeWithContentDescription("D4 die").assertIsDisplayed()
    }

    @Test
    fun givenD20_whenDiceImageIsRenderedLarge_thenNodeWithContentDescriptionIsDisplayed() {
        composeTestRule.setContent {
            DiceRollerTheme(dynamicColor = false) {
                DiceImage(dice = Dice.D20, sizeVariant = DiceImageSize.Large)
            }
        }

        composeTestRule.onNodeWithContentDescription("D20 die").assertIsDisplayed()
    }

    @Test
    fun givenD6_whenDiceImageIsRenderedLargeInDarkTheme_thenNodeWithContentDescriptionIsDisplayed() {
        composeTestRule.setContent {
            DiceRollerTheme(darkTheme = true, dynamicColor = false) {
                DiceImage(dice = Dice.D6, sizeVariant = DiceImageSize.Large)
            }
        }

        composeTestRule.onNodeWithContentDescription("D6 die").assertIsDisplayed()
    }
}
