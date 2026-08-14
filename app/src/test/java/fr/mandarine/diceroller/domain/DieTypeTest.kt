// app/src/test/java/fr/mandarine/diceroller/domain/DieTypeTest.kt
package fr.mandarine.diceroller.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [DieType] carries one invariant the rest of the app leans on hard: **no two die types share a
 * face count.** The pool is a `Map<DieType, Int>`, the roll log persists groups keyed by face
 * count, and notation renders a die as `"D$faces"` — all three become ambiguous the moment a
 * `CustomDie(6)` can coexist with [Dice.D6]. These tests pin that invariant at both of the doors
 * a die can come through: the constructor, and [DieType.ofFaces].
 */
class DieTypeTest {

    // --- label ---

    /**
     * [DieType.label] replaced every use of the enum's `name`, so it has to agree with it exactly
     * or the change silently rewrote accessibility strings and test tags.
     */
    @Test
    fun givenAPreset_whenReadingLabel_thenItMatchesTheEnumName() {
        Dice.entries.forEach { dice ->
            assertEquals(dice.name, dice.label)
        }
    }

    @Test
    fun givenACustomDie_whenReadingLabel_thenItIsDFollowedByTheFaceCount() {
        assertEquals("D7", CustomDie(7).label)
        assertEquals("D100", CustomDie(100).label)
    }

    // --- ofFaces: presets win ---

    @Test
    fun givenAPresetsFaceCount_whenResolved_thenItIsThePresetAndNotACustomDie() {
        Dice.entries.forEach { dice ->
            assertEquals(dice, DieType.ofFaces(dice.faces))
        }
    }

    @Test
    fun givenANonPresetFaceCount_whenResolved_thenItIsACustomDie() {
        assertEquals(CustomDie(7), DieType.ofFaces(7))
    }

    @Test
    fun givenTheRangeBounds_whenResolved_thenBothEndsAreInclusive() {
        val (low, high) = DieType.FACES_RANGE.first to DieType.FACES_RANGE.last

        assertEquals(CustomDie(low), DieType.ofFaces(low))
        assertEquals(CustomDie(high), DieType.ofFaces(high))
    }

    // --- ofFaces: out of range returns null rather than throwing ---

    /**
     * The decoder and the creator both hand [DieType.ofFaces] unvalidated numbers, so it must
     * reject rather than throw — a corrupt log record costs that record, not the whole log.
     */
    @Test
    fun givenAFaceCountBelowTheRange_whenResolved_thenItIsNull() {
        assertNull(DieType.ofFaces(1))
        assertNull(DieType.ofFaces(0))
        assertNull(DieType.ofFaces(-3))
    }

    @Test
    fun givenAFaceCountAboveTheRange_whenResolved_thenItIsNull() {
        assertNull(DieType.ofFaces(DieType.FACES_RANGE.last + 1))
    }

    // --- CustomDie construction is validated, not clamped ---

    @Test
    fun givenAPresetsFaceCount_whenConstructingACustomDie_thenItThrows() {
        Dice.entries.forEach { dice ->
            assertThrows(IllegalArgumentException::class.java) { CustomDie(dice.faces) }
        }
    }

    @Test
    fun givenAFaceCountOutOfRange_whenConstructingACustomDie_thenItThrows() {
        assertThrows(IllegalArgumentException::class.java) { CustomDie(1) }
        assertThrows(IllegalArgumentException::class.java) {
            CustomDie(DieType.FACES_RANGE.last + 1)
        }
    }

    /** The whole point of the invariant: every face count resolves to exactly one die type. */
    @Test
    fun givenEveryFaceCountInRange_whenResolved_thenExactlyOneDieTypeHasThatFaceCount() {
        val resolved = DieType.FACES_RANGE.mapNotNull { DieType.ofFaces(it) }

        assertEquals(DieType.FACES_RANGE.count(), resolved.size)
        assertEquals(resolved.size, resolved.distinct().size)
        assertEquals(resolved.size, resolved.map { it.faces }.distinct().size)
    }

    /** A custom die is a [DieType] and never a [Dice], so `when` branches cannot confuse them. */
    @Test
    fun givenACustomDie_whenTypeChecked_thenItIsNotAPreset() {
        val die: DieType = CustomDie(7)

        assertTrue(die is CustomDie)
        assertTrue(die !is Dice)
    }
}
