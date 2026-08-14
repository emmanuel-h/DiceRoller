// app/src/main/java/fr/mandarine/diceroller/domain/DiceGroupResult.kt
package fr.mandarine.diceroller.domain

/**
 * One die type's contribution to a [DicePoolResult]: how many dice of that [dice] type were
 * rolled, and the resulting per-value tallies.
 *
 * @property dice the die type this group covers — a [Dice] preset or a [CustomDie]
 * @property poolCount how many dice of [dice] were rolled, i.e. the pool count for this type
 * @property tallies per-value tallies, sorted descending by [ValueTally.value], with values that
 *   were rolled zero times omitted entirely
 */
data class DiceGroupResult(
    val dice: DieType,
    val poolCount: Int,
    val tallies: List<ValueTally>,
)
