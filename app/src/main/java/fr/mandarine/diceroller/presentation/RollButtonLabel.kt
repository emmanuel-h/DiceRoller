// app/src/main/java/fr/mandarine/diceroller/presentation/RollButtonLabel.kt
package fr.mandarine.diceroller.presentation

import fr.mandarine.diceroller.domain.DicePool

/** Label shown on the Roll button when [DicePool.isEmpty] — there is nothing yet to roll. */
private const val EMPTY_POOL_LABEL = "Add dice to roll"

/**
 * Formats the Roll button's label from [pool].
 *
 * A non-empty pool renders as `"Roll " + "<count>D<faces>"` per die type present, joined with
 * `" + "` and ordered smallest-to-largest by face count — e.g. `"Roll 4D6 + 2D8"` for a mixed
 * pool, or `"Roll 4D6"` for a single die type (no `+`). Ordering is inherited from
 * [DicePool.entries], which is already sorted, so this formatter never re-sorts.
 *
 * An empty pool (all counts 0) renders as [EMPTY_POOL_LABEL] instead, matching the Roll button's
 * disabled empty state.
 *
 * @param pool the dice pool to format a label for
 */
fun rollButtonLabel(pool: DicePool): String {
    if (pool.isEmpty) return EMPTY_POOL_LABEL
    val poolNotation = pool.entries.joinToString(separator = " + ") { (dice, count) ->
        "${count}D${dice.faces}"
    }
    return "Roll $poolNotation"
}
