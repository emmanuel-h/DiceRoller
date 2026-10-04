// app/src/main/java/fr/mandarine/diceroller/presentation/RollSound.kt
package fr.mandarine.diceroller.presentation

import fr.mandarine.diceroller.domain.DicePoolResult

/**
 * Which kind of clatter a roll makes (issue #5): one die skittering, or a handful landing together.
 *
 * Kept as pure Kotlin, apart from the player that maps each kind to its clips, so the choice is
 * covered by JVM tests. The clips themselves are recordings of real throws, several per kind, and
 * the player picks one at random so repeated rolls do not sound like a loop.
 */
enum class RollSound {
    /** A single die thrown on its own. */
    OneDie,

    /** Two or more dice thrown together. */
    SeveralDice,
    ;

    companion object {
        /**
         * The sound for [result]: [OneDie] when exactly one die was rolled, [SeveralDice] otherwise.
         *
         * Counted across every group, so `1D6 + 1D8` is two dice landing at once and sounds like it.
         */
        fun of(result: DicePoolResult): RollSound =
            if (result.groups.sumOf { it.poolCount } == 1) OneDie else SeveralDice
    }
}
