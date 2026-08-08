// app/src/main/java/fr/mandarine/diceroller/domain/DicePool.kt
package fr.mandarine.diceroller.domain

/**
 * A validated, immutable mixed dice pool: how many dice of each [Dice] type are queued to be
 * rolled together in one [DiceRoller.rollPool] call.
 *
 * Every count must be within [0, MAX_DICE_PER_TYPE]. Die types absent from [counts] are treated
 * as a count of zero. [entries] exposes only the non-zero die types, already sorted
 * smallest-to-largest by [Dice.faces] so callers never need to re-derive that ordering.
 *
 * @property counts the raw die-type-to-count map this pool was built from
 * @throws IllegalArgumentException if any count is negative or exceeds [MAX_DICE_PER_TYPE]
 */
data class DicePool(private val counts: Map<Dice, Int> = emptyMap()) {

    init {
        counts.forEach { (dice, count) ->
            require(count in 0..MAX_DICE_PER_TYPE) {
                "Count for $dice must be within 0..$MAX_DICE_PER_TYPE, was $count"
            }
        }
    }

    /**
     * Non-zero (die type, count) pairs, ordered smallest-to-largest by [Dice.faces].
     */
    val entries: List<Pair<Dice, Int>> =
        counts.filterValues { it > 0 }.toList().sortedBy { (dice, _) -> dice.faces }

    /**
     * True when every die type in this pool has a count of zero.
     */
    val isEmpty: Boolean get() = entries.isEmpty()

    /**
     * Returns the count for [dice], or 0 if it is absent from this pool.
     */
    fun countFor(dice: Dice): Int = counts[dice] ?: 0

    companion object {
        /**
         * The maximum number of dice allowed for a single die type within a pool.
         */
        const val MAX_DICE_PER_TYPE: Int = 20
    }
}
