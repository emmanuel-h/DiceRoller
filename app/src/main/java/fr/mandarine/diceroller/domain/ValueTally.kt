// app/src/main/java/fr/mandarine/diceroller/domain/ValueTally.kt
package fr.mandarine.diceroller.domain

/**
 * How many dice within a [DiceGroupResult] landed on a given rolled [value].
 *
 * @property value the rolled face value
 * @property count how many dice of the group landed on [value]; always greater than zero
 */
data class ValueTally(val value: Int, val count: Int)
