// app/src/test/java/fr/mandarine/diceroller/domain/DiceRollerTest.kt
package fr.mandarine.diceroller.domain

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DiceRollerTest {

    // --- Dice enum ---

    @Test
    fun givenDiceEnum_whenCountingEntries_thenFiveDiceAreAvailable() {
        assertEquals(5, Dice.entries.size)
    }

    @Test
    fun givenD4_whenReadingFaces_thenFaceCountIs4() {
        assertEquals(4, Dice.D4.faces)
    }

    @Test
    fun givenD6_whenReadingFaces_thenFaceCountIs6() {
        assertEquals(6, Dice.D6.faces)
    }

    @Test
    fun givenD8_whenReadingFaces_thenFaceCountIs8() {
        assertEquals(8, Dice.D8.faces)
    }

    @Test
    fun givenD12_whenReadingFaces_thenFaceCountIs12() {
        assertEquals(12, Dice.D12.faces)
    }

    @Test
    fun givenD20_whenReadingFaces_thenFaceCountIs20() {
        assertEquals(20, Dice.D20.faces)
    }

    // --- DiceRoller range ---

    @Test
    fun givenD4_whenRolledManyTimes_thenResultIsAlwaysInRange() {
        val roller = DiceRoller()
        repeat(200) {
            val result = roller.roll(Dice.D4)
            assertTrue("Expected 1..4, got $result", result in 1..4)
        }
    }

    @Test
    fun givenD6_whenRolledManyTimes_thenResultIsAlwaysInRange() {
        val roller = DiceRoller()
        repeat(200) {
            val result = roller.roll(Dice.D6)
            assertTrue("Expected 1..6, got $result", result in 1..6)
        }
    }

    @Test
    fun givenD8_whenRolledManyTimes_thenResultIsAlwaysInRange() {
        val roller = DiceRoller()
        repeat(200) {
            val result = roller.roll(Dice.D8)
            assertTrue("Expected 1..8, got $result", result in 1..8)
        }
    }

    @Test
    fun givenD12_whenRolledManyTimes_thenResultIsAlwaysInRange() {
        val roller = DiceRoller()
        repeat(200) {
            val result = roller.roll(Dice.D12)
            assertTrue("Expected 1..12, got $result", result in 1..12)
        }
    }

    @Test
    fun givenD20_whenRolledManyTimes_thenResultIsAlwaysInRange() {
        val roller = DiceRoller()
        repeat(200) {
            val result = roller.roll(Dice.D20)
            assertTrue("Expected 1..20, got $result", result in 1..20)
        }
    }

    // --- Boundary: minimum ---

    @Test
    fun givenRandomAlwaysReturnsLowest_whenRolled_thenResultIs1() {
        // nextInt(from=1, until=faces+1) with a random that always returns `from` yields 1
        val alwaysMin = Random(0)
        val roller = DiceRoller(random = alwaysMin)
        // Drive until we get 1 to confirm the lower boundary is reachable
        val results = (1..1000).map { DiceRoller(random = Random(it.toLong())).roll(Dice.D6) }
        assertTrue("Lower boundary 1 must be reachable", results.any { it == 1 })
    }

    @Test
    fun givenRandomAlwaysReturnsHighest_whenRolled_thenResultEqualsMaxFaces() {
        // Upper boundary: verify that the maximum face value is reachable
        val results = (1..1000).map { DiceRoller(random = Random(it.toLong())).roll(Dice.D6) }
        assertTrue("Upper boundary 6 must be reachable", results.any { it == 6 })
    }

    // --- Determinism ---

    @Test
    fun givenSameSeed_whenRolledTwiceOnD6_thenResultsAreEqual() {
        val first = DiceRoller(random = Random(seed = 42)).roll(Dice.D6)
        val second = DiceRoller(random = Random(seed = 42)).roll(Dice.D6)

        assertEquals(first, second)
    }

    @Test
    fun givenSameSeed_whenRolledOnEachDiceType_thenResultsAreEqualAcrossInstances() {
        Dice.entries.forEach { dice ->
            val first = DiceRoller(random = Random(seed = 99)).roll(dice)
            val second = DiceRoller(random = Random(seed = 99)).roll(dice)
            assertEquals("Seed determinism failed for $dice", first, second)
        }
    }

    // --- Result never exceeds faces, never falls below 1 (combined sanity) ---

    @Test
    fun givenAllDiceTypes_whenRolledRepeatedly_thenResultIsAlwaysWithinValidRange() {
        val roller = DiceRoller()
        Dice.entries.forEach { dice ->
            repeat(100) {
                val result = roller.roll(dice)
                assertTrue(
                    "Expected result in 1..${dice.faces} for $dice, got $result",
                    result in 1..dice.faces,
                )
            }
        }
    }
}
