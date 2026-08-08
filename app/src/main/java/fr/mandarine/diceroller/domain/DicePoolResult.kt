// app/src/main/java/fr/mandarine/diceroller/domain/DicePoolResult.kt
package fr.mandarine.diceroller.domain

/**
 * The outcome of rolling a [DicePool]: one result group per non-zero die type plus the total
 * sum across every die rolled.
 *
 * @property groups per-die-type results, ordered smallest-to-largest by [Dice.faces], matching
 *   [DicePool.entries]'s ordering
 * @property total the sum of every rolled value across every group
 */
data class DicePoolResult(
    val groups: List<DiceGroupResult>,
    val total: Int,
)
