// app/src/test/java/fr/mandarine/diceroller/domain/DiceRollerTest.kt
package fr.mandarine.diceroller.domain

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DiceRollerTest {

    @Test
    fun `roll returns value within valid range for each dice type`() {
        val roller = DiceRoller()
        Dice.entries.forEach { dice ->
            repeat(100) {
                val result = roller.roll(dice)
                assertTrue(
                    "Expected result in 1..${dice.faces}, got $result",
                    result in 1..dice.faces,
                )
            }
        }
    }

    @Test
    fun `roll with seeded random returns deterministic result`() {
        val roller = DiceRoller(random = Random(seed = 42))
        val first = roller.roll(Dice.D6)
        val secondRoller = DiceRoller(random = Random(seed = 42))
        val second = secondRoller.roll(Dice.D6)
        assertEquals(first, second)
    }

    @Test
    fun `dice enum has correct face counts`() {
        assertEquals(4, Dice.D4.faces)
        assertEquals(6, Dice.D6.faces)
        assertEquals(8, Dice.D8.faces)
        assertEquals(12, Dice.D12.faces)
        assertEquals(20, Dice.D20.faces)
    }
}
