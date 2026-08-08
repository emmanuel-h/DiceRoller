// app/src/test/java/fr/mandarine/diceroller/presentation/RollButtonLabelTest.kt
package fr.mandarine.diceroller.presentation

import fr.mandarine.diceroller.domain.Dice
import fr.mandarine.diceroller.domain.DicePool
import org.junit.Assert.assertEquals
import org.junit.Test

class RollButtonLabelTest {

    // --- Empty pool ---

    @Test
    fun givenEmptyPool_whenFormattingLabel_thenReadsAddDiceToRoll() {
        val label = rollButtonLabel(DicePool())

        assertEquals("Add dice to roll", label)
    }

    @Test
    fun givenPoolWithOnlyZeroCounts_whenFormattingLabel_thenReadsAddDiceToRoll() {
        val label = rollButtonLabel(DicePool(mapOf(Dice.D6 to 0, Dice.D8 to 0)))

        assertEquals("Add dice to roll", label)
    }

    // --- Single die type ---

    @Test
    fun givenSingleDieType_whenFormattingLabel_thenNoPlusSignIsIncluded() {
        val label = rollButtonLabel(DicePool(mapOf(Dice.D6 to 4)))

        assertEquals("Roll 4D6", label)
    }

    @Test
    fun givenSingleDieTypeWithCountOfOne_whenFormattingLabel_thenReadsSingularCountPrefix() {
        val label = rollButtonLabel(DicePool(mapOf(Dice.D20 to 1)))

        assertEquals("Roll 1D20", label)
    }

    // --- Mixed pool: ordering ---

    @Test
    fun givenMixedPool_whenFormattingLabel_thenOrderedSmallestToLargest() {
        val label = rollButtonLabel(DicePool(mapOf(Dice.D8 to 2, Dice.D6 to 4)))

        assertEquals("Roll 4D6 + 2D8", label)
    }

    @Test
    fun givenMixedPoolWithReversedInsertionOrder_whenFormattingLabel_thenStillOrderedByFaces() {
        val label = rollButtonLabel(
            DicePool(mapOf(Dice.D20 to 1, Dice.D4 to 3, Dice.D12 to 2, Dice.D6 to 1)),
        )

        assertEquals("Roll 3D4 + 1D6 + 2D12 + 1D20", label)
    }

    @Test
    fun givenAllSixDiceTypesPopulated_whenFormattingLabel_thenAllGroupsAppearInOrder() {
        val pool = DicePool(
            mapOf(
                Dice.D4 to 1,
                Dice.D6 to 2,
                Dice.D8 to 3,
                Dice.D10 to 4,
                Dice.D12 to 5,
                Dice.D20 to 6,
            ),
        )

        val label = rollButtonLabel(pool)

        assertEquals("Roll 1D4 + 2D6 + 3D8 + 4D10 + 5D12 + 6D20", label)
    }

    @Test
    fun givenMixedPoolWithZeroCountEntries_whenFormattingLabel_thenZeroCountsAreExcluded() {
        val label = rollButtonLabel(DicePool(mapOf(Dice.D6 to 4, Dice.D8 to 0, Dice.D20 to 2)))

        assertEquals("Roll 4D6 + 2D20", label)
    }

    // --- Cap ---

    @Test
    fun givenCountAtMax_whenFormattingLabel_thenFullCapValueIsShown() {
        val label = rollButtonLabel(DicePool(mapOf(Dice.D20 to DicePool.MAX_DICE_PER_TYPE)))

        assertEquals("Roll 20D20", label)
    }
}
