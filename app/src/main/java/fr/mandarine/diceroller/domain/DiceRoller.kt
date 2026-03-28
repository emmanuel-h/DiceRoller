// app/src/main/java/fr/mandarine/diceroller/domain/DiceRoller.kt
package fr.mandarine.diceroller.domain

import kotlin.random.Random

/**
 * Stateless dice roller that produces uniformly random results.
 *
 * @param random the random number generator, injected for testability
 */
class DiceRoller(private val random: Random = Random.Default) {

    /**
     * Rolls the given [dice] and returns a value between 1 and [Dice.faces] inclusive.
     */
    fun roll(dice: Dice): Int = random.nextInt(from = 1, until = dice.faces + 1)
}
