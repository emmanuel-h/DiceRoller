// app/src/test/java/fr/mandarine/diceroller/data/RollHistoryCodecTest.kt
package fr.mandarine.diceroller.data

import fr.mandarine.diceroller.domain.Dice
import fr.mandarine.diceroller.domain.DiceGroupResult
import fr.mandarine.diceroller.domain.DicePool
import fr.mandarine.diceroller.domain.DicePoolResult
import fr.mandarine.diceroller.domain.DiceRoller
import fr.mandarine.diceroller.domain.RollRecord
import fr.mandarine.diceroller.domain.ValueTally
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The codec is the app's only persistence format, so it is tested from both ends: that a record
 * survives a round trip unchanged, and that no malformed input can throw or smuggle an
 * impossible roll into the UI.
 */
class RollHistoryCodecTest {

    private val mixedRecord = RollRecord(
        result = DicePoolResult(
            groups = listOf(
                DiceGroupResult(
                    dice = Dice.D6,
                    poolCount = 4,
                    tallies = listOf(
                        ValueTally(value = 6, count = 1),
                        ValueTally(value = 4, count = 2),
                        ValueTally(value = 3, count = 1),
                    ),
                ),
                DiceGroupResult(
                    dice = Dice.D8,
                    poolCount = 2,
                    tallies = listOf(
                        ValueTally(value = 7, count = 1),
                        ValueTally(value = 2, count = 1),
                    ),
                ),
            ),
            // 6 + 4×2 + 3 + 7 + 2. Consistent by construction, as every real record is: the
            // total is not stored, so a round trip only holds for a record that adds up.
            total = 26,
        ),
        rolledAtMillis = 1_700_000_000_000L,
    )

    // --- Encoding ---

    @Test
    fun givenMixedRecord_whenEncoded_thenUsesTheDocumentedGrammar() {
        val encoded = RollHistoryCodec.encode(listOf(mixedRecord))

        assertEquals("1700000000000;6:6*1,4*2,3*1|8:7*1,2*1", encoded)
    }

    @Test
    fun givenNoRecords_whenEncoded_thenProducesEmptyString() {
        assertEquals("", RollHistoryCodec.encode(emptyList()))
    }

    @Test
    fun givenSeveralRecords_whenEncoded_thenOneLinePerRecordInOrder() {
        val older = mixedRecord.copy(rolledAtMillis = 1L)

        val encoded = RollHistoryCodec.encode(listOf(mixedRecord, older))

        assertEquals(2, encoded.split("\n").size)
        assertTrue(encoded.startsWith("1700000000000;"))
        assertTrue(encoded.endsWith("\n1;6:6*1,4*2,3*1|8:7*1,2*1"))
    }

    // --- Round trips ---

    @Test
    fun givenMixedRecord_whenRoundTripped_thenIsUnchanged() {
        val decoded = RollHistoryCodec.decode(RollHistoryCodec.encode(listOf(mixedRecord)))

        assertEquals(listOf(mixedRecord), decoded)
    }

    @Test
    fun givenManyRecords_whenRoundTripped_thenOrderIsPreserved() {
        val records = (1..10L).map { mixedRecord.copy(rolledAtMillis = it) }

        val decoded = RollHistoryCodec.decode(RollHistoryCodec.encode(records))

        assertEquals(records, decoded)
    }

    /**
     * The real generator, not a hand-written fixture: every die type at its maximum count, so the
     * round trip is exercised against the exact shapes [DiceRoller.rollPool] can actually produce.
     */
    @Test
    fun givenMaximumRealRoll_whenRoundTripped_thenIsUnchanged() {
        val pool = DicePool(Dice.entries.associateWith { DicePool.MAX_DICE_PER_TYPE })
        val record = RollRecord(
            result = DiceRoller(random = Random(42)).rollPool(pool),
            rolledAtMillis = 1_700_000_000_000L,
        )

        val decoded = RollHistoryCodec.decode(RollHistoryCodec.encode(listOf(record)))

        assertEquals(listOf(record), decoded)
    }

    @Test
    fun givenDecodedRecord_whenReadingTotal_thenItIsRecomputedFromTheTallies() {
        // The encoded form carries no total at all, so this can only come from summing the
        // tallies: 6 + 4×2 + 3 + 7 + 2.
        val decoded = RollHistoryCodec.decode("1;6:6*1,4*2,3*1|8:7*1,2*1")

        assertEquals(26, decoded.single().result.total)
    }

    @Test
    fun givenDecodedGroup_whenReadingPoolCount_thenItIsTheSumOfTallyCounts() {
        val decoded = RollHistoryCodec.decode("1;6:6*1,4*2,3*1")

        assertEquals(4, decoded.single().result.groups.single().poolCount)
    }

    // --- Empty and absent input ---

    @Test
    fun givenNullInput_whenDecoded_thenReturnsEmptyList() {
        assertEquals(emptyList<RollRecord>(), RollHistoryCodec.decode(null))
    }

    @Test
    fun givenBlankInput_whenDecoded_thenReturnsEmptyList() {
        assertEquals(emptyList<RollRecord>(), RollHistoryCodec.decode("   "))
    }

    // --- Corruption: the bad record is dropped, the good ones survive ---

    @Test
    fun givenOneCorruptRecordAmongGoodOnes_whenDecoded_thenOnlyThatRecordIsDropped() {
        val decoded = RollHistoryCodec.decode("1;6:6*1\nnot-a-record\n3;8:7*1")

        assertEquals(listOf(1L, 3L), decoded.map { it.rolledAtMillis })
    }

    @Test
    fun givenNonNumericTimestamp_whenDecoded_thenRecordIsDropped() {
        assertEquals(emptyList<RollRecord>(), RollHistoryCodec.decode("later;6:6*1"))
    }

    @Test
    fun givenUnknownFaceCount_whenDecoded_thenRecordIsDropped() {
        assertEquals(emptyList<RollRecord>(), RollHistoryCodec.decode("1;7:5*1"))
    }

    @Test
    fun givenValueAboveTheDieMaximum_whenDecoded_thenRecordIsDropped() {
        assertEquals(emptyList<RollRecord>(), RollHistoryCodec.decode("1;6:7*1"))
    }

    @Test
    fun givenValueBelowOne_whenDecoded_thenRecordIsDropped() {
        assertEquals(emptyList<RollRecord>(), RollHistoryCodec.decode("1;6:0*1"))
    }

    @Test
    fun givenNonPositiveCount_whenDecoded_thenRecordIsDropped() {
        assertEquals(emptyList<RollRecord>(), RollHistoryCodec.decode("1;6:5*0"))
    }

    @Test
    fun givenGroupWithNoTallies_whenDecoded_thenRecordIsDropped() {
        assertEquals(emptyList<RollRecord>(), RollHistoryCodec.decode("1;6:"))
    }

    @Test
    fun givenTallyMissingItsCount_whenDecoded_thenRecordIsDropped() {
        assertEquals(emptyList<RollRecord>(), RollHistoryCodec.decode("1;6:5"))
    }

    @Test
    fun givenTruncatedRecord_whenDecoded_thenRecordIsDropped() {
        assertEquals(emptyList<RollRecord>(), RollHistoryCodec.decode("1700000000000"))
    }

    @Test
    fun givenStraySeparatorInThePayload_whenDecoded_thenRecordIsDroppedRatherThanTruncated() {
        // The limit-2 split leaves "6:6*1;8:7*1" as the group payload, which must fail to parse
        // rather than quietly decode as a D6-only roll.
        assertEquals(emptyList<RollRecord>(), RollHistoryCodec.decode("1;6:6*1;8:7*1"))
    }

    @Test
    fun givenRecordWithNoGroups_whenDecoded_thenIsAnEmptyResultRatherThanADrop() {
        val decoded = RollHistoryCodec.decode("1;")

        assertEquals(DicePoolResult(groups = emptyList(), total = 0), decoded.single().result)
    }
}
