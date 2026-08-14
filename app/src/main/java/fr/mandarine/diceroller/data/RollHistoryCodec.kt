// app/src/main/java/fr/mandarine/diceroller/data/RollHistoryCodec.kt
package fr.mandarine.diceroller.data

import fr.mandarine.diceroller.domain.Dice
import fr.mandarine.diceroller.domain.DiceGroupResult
import fr.mandarine.diceroller.domain.DicePoolResult
import fr.mandarine.diceroller.domain.RollRecord
import fr.mandarine.diceroller.domain.ValueTally

/**
 * Encodes and decodes the roll history as one flat string, for storage in DataStore Preferences.
 *
 * The format is hand-rolled rather than JSON because the project ships no serialization library
 * and this is its only persisted aggregate; the grammar below is small enough to test
 * exhaustively, and it stays compact — 50 records of a realistic pool cost a couple of kilobytes.
 *
 * ```
 * history := record ( "\n" record )*
 * record  := millis ";" ( group ( "|" group )* )?
 * group   := faces ":" tally ( "," tally )*
 * tally   := value "*" count
 * ```
 *
 * For example `1700000000000;6:6*1,4*2,3*1|8:7*1,2*1` is a 4D6 + 2D8 roll.
 *
 * Everything derivable is left out and recomputed on decode: [DiceGroupResult.poolCount] is the
 * sum of its tally counts, and [DicePoolResult.total] is the sum of `value × count` across every
 * group. That keeps the stored form from ever disagreeing with itself.
 *
 * **Decoding never throws.** Anything unparseable — a truncated write, a hand-edited file, a
 * die type or face count this build no longer knows — is dropped record by record, so one bad
 * record costs the user that record and not their whole log.
 */
internal object RollHistoryCodec {

    private const val RECORD_SEPARATOR = "\n"
    private const val TIMESTAMP_SEPARATOR = ";"
    private const val GROUP_SEPARATOR = "|"
    private const val FACES_SEPARATOR = ":"
    private const val TALLY_SEPARATOR = ","
    private const val COUNT_SEPARATOR = "*"

    /** Face count to die type, so a decoded `6:` resolves back to [Dice.D6]. */
    private val diceByFaces: Map<Int, Dice> = Dice.entries.associateBy { it.faces }

    /** Encodes [records] in order, newest first, as the caller supplies them. */
    fun encode(records: List<RollRecord>): String =
        records.joinToString(separator = RECORD_SEPARATOR) { encodeRecord(it) }

    /**
     * Decodes [encoded] back into records, silently skipping any that fail to parse.
     *
     * A null or blank input decodes to an empty list — the state a fresh install is in.
     */
    fun decode(encoded: String?): List<RollRecord> {
        if (encoded.isNullOrBlank()) return emptyList()
        return encoded.split(RECORD_SEPARATOR).mapNotNull { decodeRecord(it) }
    }

    private fun encodeRecord(record: RollRecord): String {
        val groups = record.result.groups.joinToString(separator = GROUP_SEPARATOR) { group ->
            val tallies = group.tallies.joinToString(separator = TALLY_SEPARATOR) { tally ->
                "${tally.value}$COUNT_SEPARATOR${tally.count}"
            }
            "${group.dice.faces}$FACES_SEPARATOR$tallies"
        }
        return "${record.rolledAtMillis}$TIMESTAMP_SEPARATOR$groups"
    }

    private fun decodeRecord(encoded: String): RollRecord? {
        if (encoded.isBlank()) return null
        // limit = 2 so a stray ';' inside the payload fails the group parse below rather than
        // silently truncating the record to its first group.
        val parts = encoded.split(TIMESTAMP_SEPARATOR, limit = 2)
        if (parts.size != 2) return null
        val rolledAtMillis = parts[0].toLongOrNull() ?: return null

        val groups = if (parts[1].isEmpty()) {
            emptyList()
        } else {
            parts[1].split(GROUP_SEPARATOR).map { group -> decodeGroup(group) ?: return null }
        }
        val total = groups.sumOf { group -> group.tallies.sumOf { it.value * it.count } }
        return RollRecord(
            result = DicePoolResult(groups = groups, total = total),
            rolledAtMillis = rolledAtMillis,
        )
    }

    private fun decodeGroup(encoded: String): DiceGroupResult? {
        val parts = encoded.split(FACES_SEPARATOR, limit = 2)
        if (parts.size != 2) return null
        val faces = parts[0].toIntOrNull() ?: return null
        val dice = diceByFaces[faces] ?: return null
        if (parts[1].isEmpty()) return null

        val tallies = parts[1].split(TALLY_SEPARATOR).map { tally ->
            decodeTally(tally, faces) ?: return null
        }
        return DiceGroupResult(
            dice = dice,
            poolCount = tallies.sumOf { it.count },
            tallies = tallies,
        )
    }

    private fun decodeTally(encoded: String, faces: Int): ValueTally? {
        val parts = encoded.split(COUNT_SEPARATOR, limit = 2)
        if (parts.size != 2) return null
        val value = parts[0].toIntOrNull() ?: return null
        val count = parts[1].toIntOrNull() ?: return null
        // A value outside 1..faces, or a non-positive count, could never have come from a real
        // roll; treating it as corruption keeps impossible entries out of the UI.
        if (value !in 1..faces || count <= 0) return null
        return ValueTally(value = value, count = count)
    }
}
