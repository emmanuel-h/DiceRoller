// app/src/test/java/fr/mandarine/diceroller/domain/DiceRollerTest.kt
package fr.mandarine.diceroller.domain

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DiceRollerTest {

    // --- Dice enum ---

    @Test
    fun givenDiceEnum_whenCountingEntries_thenSixDiceAreAvailable() {
        assertEquals(6, Dice.entries.size)
    }

    @Test
    fun givenDiceEnum_whenReadingOrder_thenDiceAscendByFaceCount() {
        assertEquals(
            listOf(Dice.D4, Dice.D6, Dice.D8, Dice.D10, Dice.D12, Dice.D20),
            Dice.entries,
        )
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
    fun givenD10_whenReadingFaces_thenFaceCountIs10() {
        assertEquals(10, Dice.D10.faces)
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
    fun givenD10_whenRolledManyTimes_thenResultIsAlwaysInRange() {
        val roller = DiceRoller()
        repeat(200) {
            val result = roller.roll(Dice.D10)
            assertTrue("Expected 1..10, got $result", result in 1..10)
        }
    }

    @Test
    fun givenD10_whenRolledManyTimes_thenBothBoundsAreReachable() {
        val results = (1..1000).map { DiceRoller(random = Random(it.toLong())).roll(Dice.D10) }

        assertTrue("Lower bound 1 must be reachable on D10", results.any { it == 1 })
        assertTrue("Upper bound 10 must be reachable on D10", results.any { it == 10 })
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

    // --- rollPool: empty pool ---

    @Test
    fun givenEmptyPool_whenRollingPool_thenResultHasNoGroupsAndZeroTotal() {
        val result = DiceRoller().rollPool(DicePool())

        assertTrue(result.groups.isEmpty())
        assertEquals(0, result.total)
    }

    @Test
    fun givenPoolWhereEveryCountIsZero_whenRollingPool_thenResultHasNoGroupsAndZeroTotal() {
        val pool = DicePool(mapOf(Dice.D6 to 0, Dice.D8 to 0, Dice.D20 to 0))

        val result = DiceRoller().rollPool(pool)

        assertTrue(result.groups.isEmpty())
        assertEquals(0, result.total)
    }

    // --- rollPool: group shape ---

    @Test
    fun givenSingleDieTypePool_whenRollingPool_thenOneGroupWithMatchingPoolCount() {
        val pool = DicePool(mapOf(Dice.D6 to 5))

        val result = DiceRoller(random = Random(seed = 1)).rollPool(pool)

        assertEquals(1, result.groups.size)
        assertEquals(Dice.D6, result.groups[0].dice)
        assertEquals(5, result.groups[0].poolCount)
    }

    @Test
    fun givenSingleDiePoolOfCountOne_whenRollingPool_thenGroupHasExactlyOneTallyOfCountOne() {
        val pool = DicePool(mapOf(Dice.D8 to 1))

        val result = DiceRoller(random = Random(seed = 3)).rollPool(pool)

        val group = result.groups.single()
        assertEquals(1, group.tallies.single().count)
    }

    @Test
    fun givenMixedPool_whenRollingPool_thenGroupsOrderedSmallestToLargest() {
        val pool = DicePool(mapOf(Dice.D20 to 2, Dice.D4 to 3, Dice.D8 to 1))

        val result = DiceRoller().rollPool(pool)

        assertEquals(listOf(Dice.D4, Dice.D8, Dice.D20), result.groups.map { it.dice })
    }

    // --- rollPool: tally correctness ---

    @Test
    fun givenPool_whenRollingPool_thenTalliesSumToPoolCountPerGroup() {
        val pool = DicePool(mapOf(Dice.D6 to 10, Dice.D8 to 6))

        val result = DiceRoller().rollPool(pool)

        result.groups.forEach { group ->
            assertEquals(group.poolCount, group.tallies.sumOf { it.count })
        }
    }

    @Test
    fun givenPool_whenRollingPool_thenEveryTalliedValueIsWithinDiceFaceRange() {
        val pool = DicePool(mapOf(Dice.D4 to 20, Dice.D20 to 20))

        val result = DiceRoller().rollPool(pool)

        result.groups.forEach { group ->
            group.tallies.forEach { tally ->
                assertTrue(
                    "Expected value in 1..${group.dice.faces}, got ${tally.value}",
                    tally.value in 1..group.dice.faces,
                )
            }
        }
    }

    @Test
    fun givenPool_whenRollingPool_thenTalliesWithinGroupAreSortedDescendingByValue() {
        val pool = DicePool(mapOf(Dice.D6 to 20))

        val result = DiceRoller().rollPool(pool)

        val values = result.groups.first().tallies.map { it.value }
        assertEquals(values.sortedDescending(), values)
    }

    @Test
    fun givenPool_whenRollingPool_thenNoZeroCountTalliesAppear() {
        val pool = DicePool(mapOf(Dice.D6 to 20))

        val result = DiceRoller().rollPool(pool)

        result.groups.forEach { group ->
            group.tallies.forEach { tally -> assertTrue(tally.count > 0) }
        }
    }

    @Test
    fun givenSeededMixedPool_whenRollingPool_thenEachGroupRollsExactlyItsPoolCountIndependently() {
        val pool = DicePool(mapOf(Dice.D6 to 4, Dice.D8 to 2))

        val result = DiceRoller(random = Random(seed = 2024)).rollPool(pool)

        assertEquals(listOf(4, 2), result.groups.map { it.poolCount })
        assertEquals(listOf(4, 2), result.groups.map { group -> group.tallies.sumOf { it.count } })
    }

    @Test
    fun givenSeededMixedPool_whenRollingPool_thenTalliesMatchIndependentlyReplayedRolls() {
        val seed = 2024L
        val pool = DicePool(mapOf(Dice.D6 to 4, Dice.D8 to 2))

        val result = DiceRoller(random = Random(seed)).rollPool(pool)

        // Replay the same die-by-die, group-by-group sequence with a fresh roller seeded
        // identically, to independently derive what the per-value tallies should be.
        val replayRoller = DiceRoller(random = Random(seed))
        val expectedTalliesPerGroup = pool.entries.map { (dice, count) ->
            List(count) { replayRoller.roll(dice) }.groupingBy { it }.eachCount()
        }

        result.groups.forEachIndexed { index, group ->
            val actualTallyMap = group.tallies.associate { it.value to it.count }
            assertEquals(expectedTalliesPerGroup[index], actualTallyMap)
        }
    }

    @Test
    fun givenPool_whenRollingPool_thenValuesNeverRolledAreAbsentFromTallies() {
        // A D6 pool of size 3 has at most 3 distinct rolled values out of 6 possible faces,
        // so at least some face values are guaranteed to be absent from the tally.
        val pool = DicePool(mapOf(Dice.D6 to 3))

        val result = DiceRoller(random = Random(seed = 11)).rollPool(pool)

        val talliedValues = result.groups.single().tallies.map { it.value }.toSet()
        val possibleValues = (1..Dice.D6.faces).toSet()
        assertTrue(
            "Expected at least one untallied value, tallied=$talliedValues",
            talliedValues.size < possibleValues.size,
        )
        assertTrue(possibleValues.containsAll(talliedValues))
    }

    // --- rollPool: total ---

    @Test
    fun givenPool_whenRollingPool_thenTotalEqualsSumOfAllRolledValues() {
        val pool = DicePool(mapOf(Dice.D6 to 4, Dice.D8 to 2))

        val result = DiceRoller().rollPool(pool)

        val expectedTotal = result.groups.sumOf { group ->
            group.tallies.sumOf { it.value * it.count }
        }
        assertEquals(expectedTotal, result.total)
    }

    // --- rollPool: existing single-die roll is unaffected ---

    @Test
    fun givenSameSeed_whenRollingPoolOfOne_thenMatchesDirectRollCall() {
        val poolRoller = DiceRoller(random = Random(seed = 7))
        val directRoller = DiceRoller(random = Random(seed = 7))

        val poolResult = poolRoller.rollPool(DicePool(mapOf(Dice.D6 to 1)))
        val directResult = directRoller.roll(Dice.D6)

        assertEquals(directResult, poolResult.groups.first().tallies.first().value)
    }

    // --- rollPool: determinism ---

    @Test
    fun givenSameSeed_whenRollingPoolTwice_thenResultsAreEqual() {
        val pool = DicePool(mapOf(Dice.D6 to 4, Dice.D8 to 2))

        val first = DiceRoller(random = Random(seed = 55)).rollPool(pool)
        val second = DiceRoller(random = Random(seed = 55)).rollPool(pool)

        assertEquals(first, second)
    }
}
