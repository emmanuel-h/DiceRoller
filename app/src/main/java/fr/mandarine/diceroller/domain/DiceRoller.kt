// app/src/main/java/fr/mandarine/diceroller/domain/DiceRoller.kt
package fr.mandarine.diceroller.domain

import kotlin.random.Random

/**
 * Stateless dice roller that produces uniformly random results.
 *
 * Rolls any [DieType]: a [Dice] preset and a user-defined [CustomDie] follow the same code path,
 * because a die's behaviour is entirely determined by its [DieType.faces].
 *
 * @param random the random number generator, injected for testability
 */
class DiceRoller(private val random: Random = Random.Default) {

    /**
     * Rolls the given [dice] and returns a value between 1 and [DieType.faces] inclusive.
     */
    fun roll(dice: DieType): Int = random.nextInt(from = 1, until = dice.faces + 1)

    /**
     * Rolls every die in [pool], one independent uniformly-random result per die, and returns
     * the results grouped and tallied per die type.
     *
     * Die-type groups are ordered smallest-to-largest by [DieType.faces], matching
     * [DicePool.entries]. Within each group, tallies are sorted descending by rolled value, and
     * values that were never rolled are omitted. An empty [pool] is a no-op that returns an
     * empty [DicePoolResult].
     */
    fun rollPool(pool: DicePool): DicePoolResult {
        val groups = pool.entries.map { (dice, count) -> rollGroup(dice, count) }
        val total = groups.sumOf { group -> group.tallies.sumOf { it.value * it.count } }
        return DicePoolResult(groups = groups, total = total)
    }

    private fun rollGroup(dice: DieType, count: Int): DiceGroupResult {
        val tallies = List(count) { roll(dice) }
            .groupingBy { it }
            .eachCount()
            .map { (value, tallyCount) -> ValueTally(value = value, count = tallyCount) }
            .sortedByDescending { it.value }
        return DiceGroupResult(dice = dice, poolCount = count, tallies = tallies)
    }
}
