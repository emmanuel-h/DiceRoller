// app/src/test/java/fr/mandarine/diceroller/data/CustomDiceCodecTest.kt
package fr.mandarine.diceroller.data

import fr.mandarine.diceroller.domain.CustomDie
import fr.mandarine.diceroller.domain.DieType
import fr.mandarine.diceroller.presentation.MAX_CUSTOM_DICE
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Like [RollHistoryCodecTest], this covers both directions: that a definition survives a round
 * trip, and that nothing a corrupt or hand-edited file can contain gets past the decoder — the
 * decoder is the only thing between the stored string and a `CustomDie` constructor that throws.
 */
class CustomDiceCodecTest {

    private val dice = listOf(CustomDie(3), CustomDie(7), CustomDie(100))

    // --- Encoding ---

    @Test
    fun givenDice_whenEncoded_thenItIsTheirFaceCountsCommaSeparated() {
        assertEquals("3,7,100", CustomDiceCodec.encode(dice))
    }

    @Test
    fun givenNoDice_whenEncoded_thenProducesEmptyString() {
        assertEquals("", CustomDiceCodec.encode(emptyList()))
    }

    // --- Round trip ---

    @Test
    fun givenDice_whenRoundTripped_thenTheyAreUnchanged() {
        assertEquals(dice, CustomDiceCodec.decode(CustomDiceCodec.encode(dice)))
    }

    @Test
    fun givenTheRangeBounds_whenRoundTripped_thenBothSurvive() {
        val bounds = listOf(CustomDie(DieType.FACES_RANGE.first), CustomDie(DieType.FACES_RANGE.last))

        assertEquals(bounds, CustomDiceCodec.decode(CustomDiceCodec.encode(bounds)))
    }

    // --- Empty and absent input ---

    @Test
    fun givenNullInput_whenDecoded_thenReturnsEmptyList() {
        assertEquals(emptyList<CustomDie>(), CustomDiceCodec.decode(null))
    }

    @Test
    fun givenBlankInput_whenDecoded_thenReturnsEmptyList() {
        assertEquals(emptyList<CustomDie>(), CustomDiceCodec.decode("   "))
    }

    // --- Corruption: the bad entry goes, the good ones stay ---

    @Test
    fun givenOneUnparseableEntry_whenDecoded_thenOnlyThatEntryIsDropped() {
        assertEquals(listOf(CustomDie(3), CustomDie(7)), CustomDiceCodec.decode("3,seven,7"))
    }

    @Test
    fun givenAnEntryOutOfRange_whenDecoded_thenItIsDropped() {
        val tooMany = DieType.FACES_RANGE.last + 1

        assertEquals(listOf(CustomDie(7)), CustomDiceCodec.decode("$tooMany,7,1"))
    }

    /**
     * The case that would otherwise throw: a stored `6` is [fr.mandarine.diceroller.domain.Dice.D6],
     * and constructing a `CustomDie(6)` from it is a hard error. Resolving through
     * [DieType.ofFaces] turns it into a preset, which is then filtered out — so a file written by a
     * build where 6 was not yet a preset degrades instead of crashing the app on launch.
     */
    @Test
    fun givenAPresetsFaceCount_whenDecoded_thenItIsDroppedRatherThanBecomingACustomDie() {
        assertEquals(listOf(CustomDie(7)), CustomDiceCodec.decode("6,7,20"))
    }

    @Test
    fun givenSurroundingWhitespace_whenDecoded_thenEntriesStillParse() {
        assertEquals(listOf(CustomDie(3), CustomDie(7)), CustomDiceCodec.decode(" 3 , 7 "))
    }

    // --- Normalization is applied on the way out, so callers never see a bad shape ---

    @Test
    fun givenUnsortedEntries_whenDecoded_thenTheyComeBackAscendingByFaceCount() {
        assertEquals(dice, CustomDiceCodec.decode("100,3,7"))
    }

    @Test
    fun givenDuplicateEntries_whenDecoded_thenTheyAreDeduplicated() {
        assertEquals(listOf(CustomDie(7)), CustomDiceCodec.decode("7,7,7"))
    }

    @Test
    fun givenMoreEntriesThanTheCap_whenDecoded_thenTheExcessIsDropped() {
        val stored = (1..MAX_CUSTOM_DICE + 5)
            .mapNotNull { DieType.ofFaces(it + 100) }
            .joinToString(",") { it.faces.toString() }

        assertEquals(MAX_CUSTOM_DICE, CustomDiceCodec.decode(stored).size)
    }
}
