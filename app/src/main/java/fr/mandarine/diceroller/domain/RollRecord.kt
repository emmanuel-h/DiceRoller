// app/src/main/java/fr/mandarine/diceroller/domain/RollRecord.kt
package fr.mandarine.diceroller.domain

/**
 * One past roll, as kept by the roll history log (issue #3).
 *
 * Deliberately holds only what the roll itself produced. The dice color is *not* part of a
 * record: colour is a display-time choice that already never affects a result (selecting one
 * does not even clear the current roll), so history entries render in whatever colour is
 * selected now rather than freezing the one that happened to be active at the time.
 *
 * @property result the tallied outcome, exactly as [DiceRoller.rollPool] produced it
 * @property rolledAtMillis when the roll happened, in epoch milliseconds
 */
data class RollRecord(
    val result: DicePoolResult,
    val rolledAtMillis: Long,
)
