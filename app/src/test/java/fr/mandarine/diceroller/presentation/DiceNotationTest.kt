// app/src/test/java/fr/mandarine/diceroller/presentation/DiceNotationTest.kt
package fr.mandarine.diceroller.presentation

import fr.mandarine.diceroller.domain.CustomDie
import fr.mandarine.diceroller.domain.Dice
import fr.mandarine.diceroller.domain.DiceGroupResult
import fr.mandarine.diceroller.domain.DicePool
import fr.mandarine.diceroller.domain.DicePoolResult
import fr.mandarine.diceroller.domain.DieType
import fr.mandarine.diceroller.domain.ValueTally
import org.junit.Assert.assertEquals
import org.junit.Test

class DiceNotationTest {

    private fun group(dice: Dice, poolCount: Int) = DiceGroupResult(
        dice = dice,
        poolCount = poolCount,
        tallies = listOf(ValueTally(value = 1, count = poolCount)),
    )

    // --- poolNotation ---

    @Test
    fun givenNoEntries_whenFormatted_thenIsEmpty() {
        assertEquals("", poolNotation(emptyList()))
    }

    @Test
    fun givenOneDieType_whenFormatted_thenHasNoPlusSign() {
        assertEquals("4D6", poolNotation(listOf(Dice.D6 to 4)))
    }

    @Test
    fun givenSeveralDieTypes_whenFormatted_thenJoinedWithPlus() {
        val notation = poolNotation(listOf(Dice.D6 to 4, Dice.D8 to 2, Dice.D20 to 1))

        assertEquals("4D6 + 2D8 + 1D20", notation)
    }

    @Test
    fun givenEntriesInAnOrder_whenFormatted_thenThatOrderIsKept() {
        assertEquals("1D20 + 4D6", poolNotation(listOf(Dice.D20 to 1, Dice.D6 to 4)))
    }

    // --- DicePoolResult.notation ---

    @Test
    fun givenResult_whenFormatted_thenReadsAsThePoolThatProducedIt() {
        val result = DicePoolResult(
            groups = listOf(group(Dice.D6, 4), group(Dice.D8, 2)),
            total = 6,
        )

        assertEquals("4D6 + 2D8", result.notation())
    }

    @Test
    fun givenEmptyResult_whenFormatted_thenIsEmpty() {
        assertEquals("", DicePoolResult(groups = emptyList(), total = 0).notation())
    }

    /** The Roll button and a history entry must never describe the same pool differently. */
    @Test
    fun givenSamePool_whenFormattedForButtonAndForResult_thenTheNotationMatches() {
        val counts: Map<DieType, Int> = mapOf(Dice.D6 to 4, Dice.D8 to 2)
        val result = DicePoolResult(
            groups = listOf(group(Dice.D6, 4), group(Dice.D8, 2)),
            total = 6,
        )

        assertEquals("Roll ${result.notation()}", rollButtonLabel(DicePool(counts)))
    }

    // --- Custom dice notate exactly like presets (issue #4) ---

    @Test
    fun givenACustomDie_whenFormatted_thenItUsesItsFaceCount() {
        assertEquals("2D7", poolNotation(listOf(CustomDie(7) to 2)))
    }

    @Test
    fun givenAMixOfPresetsAndCustomDice_whenFormatted_thenTheOrderGivenIsKept() {
        assertEquals(
            "4D6 + 1D7 + 2D8",
            poolNotation(listOf(Dice.D6 to 4, CustomDie(7) to 1, Dice.D8 to 2)),
        )
    }

    /** A pool built from a map notates in face-count order, custom dice included. */
    @Test
    fun givenAPoolMixingPresetsAndACustomDie_whenLabelled_thenItReadsSmallestToLargest() {
        val pool = DicePool(mapOf(Dice.D8 to 2, CustomDie(7) to 1, Dice.D6 to 4))

        assertEquals("Roll 4D6 + 1D7 + 2D8", rollButtonLabel(pool))
    }

    @Test
    fun givenAResultIncludingACustomDie_whenNotated_thenTheCustomDieAppears() {
        val result = DicePoolResult(
            groups = listOf(
                DiceGroupResult(
                    dice = CustomDie(100),
                    poolCount = 1,
                    tallies = listOf(ValueTally(value = 73, count = 1)),
                ),
            ),
            total = 73,
        )

        assertEquals("1D100", result.notation())
    }
}
