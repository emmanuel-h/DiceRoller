// app/src/test/java/fr/mandarine/diceroller/domain/DicePoolTest.kt
package fr.mandarine.diceroller.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class DicePoolTest {

    // --- Construction defaults ---

    @Test
    fun givenNoArguments_whenConstructed_thenPoolIsEmpty() {
        val pool = DicePool()

        assertTrue(pool.isEmpty)
        assertTrue(pool.entries.isEmpty())
    }

    // --- Validation ---

    @Test
    fun givenNegativeCount_whenConstructed_thenThrows() {
        assertThrows(IllegalArgumentException::class.java) {
            DicePool(mapOf(Dice.D6 to -1))
        }
    }

    @Test
    fun givenCountAboveMax_whenConstructed_thenThrows() {
        assertThrows(IllegalArgumentException::class.java) {
            DicePool(mapOf(Dice.D6 to DicePool.MAX_DICE_PER_TYPE + 1))
        }
    }

    @Test
    fun givenCountEqualToMax_whenConstructed_thenSucceeds() {
        val pool = DicePool(mapOf(Dice.D6 to DicePool.MAX_DICE_PER_TYPE))

        assertEquals(DicePool.MAX_DICE_PER_TYPE, pool.countFor(Dice.D6))
    }

    @Test
    fun givenCountOfZero_whenConstructed_thenSucceeds() {
        val pool = DicePool(mapOf(Dice.D6 to 0))

        assertEquals(0, pool.countFor(Dice.D6))
    }

    // --- entries: zero-count exclusion ---

    @Test
    fun givenPoolWithZeroCountEntries_whenReadingEntries_thenZeroCountsAreExcluded() {
        val pool = DicePool(mapOf(Dice.D6 to 4, Dice.D8 to 0, Dice.D4 to 0))

        assertEquals(listOf(Dice.D6 to 4), pool.entries)
    }

    // --- entries: ordering ---

    @Test
    fun givenMixedPool_whenReadingEntries_thenOrderedSmallestToLargestByFaces() {
        val pool = DicePool(mapOf(Dice.D20 to 2, Dice.D4 to 3, Dice.D8 to 1, Dice.D6 to 1))

        assertEquals(
            listOf(Dice.D4 to 3, Dice.D6 to 1, Dice.D8 to 1, Dice.D20 to 2),
            pool.entries,
        )
    }

    // --- isEmpty ---

    @Test
    fun givenPoolWithOnlyZeroCounts_whenReadingIsEmpty_thenTrue() {
        val pool = DicePool(mapOf(Dice.D6 to 0, Dice.D8 to 0))

        assertTrue(pool.isEmpty)
    }

    @Test
    fun givenPoolWithAtLeastOnePositiveCount_whenReadingIsEmpty_thenFalse() {
        val pool = DicePool(mapOf(Dice.D6 to 1))

        assertFalse(pool.isEmpty)
    }

    // --- countFor ---

    @Test
    fun givenDiceAbsentFromMap_whenReadingCountFor_thenZero() {
        val pool = DicePool(mapOf(Dice.D6 to 5))

        assertEquals(0, pool.countFor(Dice.D4))
    }

    @Test
    fun givenDicePresentInMap_whenReadingCountFor_thenActualCount() {
        val pool = DicePool(mapOf(Dice.D6 to 5))

        assertEquals(5, pool.countFor(Dice.D6))
    }

    // --- Custom dice sit in the same face-count ordering as the presets (issue #4) ---

    /**
     * The ordering is what makes `"4D6 + 1D7 + 2D8"` read correctly and the result ladder run
     * smallest-to-largest, so a custom die must interleave rather than be appended after the
     * presets the way the selector's chips are.
     */
    @Test
    fun givenAPoolMixingPresetsAndCustomDice_whenReadingEntries_thenTheyInterleaveByFaceCount() {
        val pool = DicePool(
            mapOf(Dice.D8 to 2, CustomDie(7) to 1, Dice.D6 to 4, CustomDie(3) to 5),
        )

        assertEquals(
            listOf<DieType>(CustomDie(3), Dice.D6, CustomDie(7), Dice.D8),
            pool.entries.map { (dice, _) -> dice },
        )
    }

    @Test
    fun givenACustomDieAtZero_whenReadingEntries_thenItIsExcludedLikeAnyPreset() {
        val pool = DicePool(mapOf(CustomDie(7) to 0, Dice.D6 to 1))

        assertEquals(listOf<DieType>(Dice.D6), pool.entries.map { (dice, _) -> dice })
    }

    @Test
    fun givenACustomDie_whenReadingCountFor_thenItIsItsOwnCount() {
        val pool = DicePool(mapOf(CustomDie(7) to 3))

        assertEquals(3, pool.countFor(CustomDie(7)))
        assertEquals(0, pool.countFor(CustomDie(9)))
    }

    @Test
    fun givenACustomDieAboveTheMaximumCount_whenConstructed_thenItThrows() {
        assertThrows(IllegalArgumentException::class.java) {
            DicePool(mapOf(CustomDie(7) to DicePool.MAX_DICE_PER_TYPE + 1))
        }
    }
}
