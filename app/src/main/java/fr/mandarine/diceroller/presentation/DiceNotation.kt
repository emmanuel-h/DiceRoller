// app/src/main/java/fr/mandarine/diceroller/presentation/DiceNotation.kt
package fr.mandarine.diceroller.presentation

import fr.mandarine.diceroller.domain.DicePool
import fr.mandarine.diceroller.domain.DicePoolResult
import fr.mandarine.diceroller.domain.DieType

/**
 * Formats a set of (die type, count) pairs as standard dice notation: `"4D6 + 2D8"`, or `"4D6"`
 * for a single die type, or `""` for nothing at all.
 *
 * Custom dice need no special case — a `D7` notates as `"1D7"` from its [DieType.label] like any
 * preset, and lands between the D6 and the D8 because the ordering is by face count.
 *
 * Ordering is inherited from [entries] and never re-sorted here — both callers already receive
 * their entries sorted smallest-to-largest by face count, [DicePool.entries] and
 * [DicePoolResult.groups] alike.
 *
 * @param entries non-empty (die type, count) pairs in the order they should read
 */
fun poolNotation(entries: List<Pair<DieType, Int>>): String =
    entries.joinToString(separator = " + ") { (dice, count) -> "$count${dice.label}" }

/** The notation for the pool this result came from, e.g. `"4D6 + 2D8"`. */
fun DicePoolResult.notation(): String =
    poolNotation(groups.map { group -> group.dice to group.poolCount })
