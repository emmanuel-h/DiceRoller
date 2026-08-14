// app/src/main/java/fr/mandarine/diceroller/data/CustomDiceCodec.kt
package fr.mandarine.diceroller.data

import fr.mandarine.diceroller.domain.CustomDie
import fr.mandarine.diceroller.domain.DieType
import fr.mandarine.diceroller.presentation.normalizeCustomDice

/**
 * Encodes and decodes the user's custom die definitions as one comma-separated list of face
 * counts, for storage in DataStore Preferences — e.g. `"3,7,100"`.
 *
 * A definition *is* its face count, so there is nothing else to store; the format is the same
 * hand-rolled, dependency-free style as [RollHistoryCodec], for the same reason.
 *
 * **Decoding never throws.** Every entry goes through [DieType.ofFaces], so a face count this
 * build considers out of range, one that has since become a preset, and outright garbage are all
 * dropped individually — the user loses that one definition rather than all of them. The survivors
 * come back [normalizeCustomDice]d, so a hand-edited file cannot smuggle in duplicates, a bad
 * order, or more dice than the cap allows.
 */
internal object CustomDiceCodec {

    private const val SEPARATOR = ","

    /** Encodes [dice] as their face counts, in the order given. */
    fun encode(dice: List<CustomDie>): String =
        dice.joinToString(separator = SEPARATOR) { it.faces.toString() }

    /**
     * Decodes [encoded] back into definitions, silently dropping any entry that fails to parse.
     *
     * A null or blank input decodes to an empty list — the state a fresh install is in.
     */
    fun decode(encoded: String?): List<CustomDie> {
        if (encoded.isNullOrBlank()) return emptyList()
        val dice = encoded.split(SEPARATOR).mapNotNull { entry ->
            // ofFaces resolves a preset's face count to the preset itself, so a stored "6" is
            // filtered out here rather than becoming a second die that renders as "D6".
            entry.trim().toIntOrNull()?.let { DieType.ofFaces(it) } as? CustomDie
        }
        return normalizeCustomDice(dice)
    }
}
