// app/src/main/java/fr/mandarine/diceroller/presentation/RelativeTime.kt
package fr.mandarine.diceroller.presentation

import androidx.annotation.PluralsRes
import fr.mandarine.diceroller.R

/** Rolls newer than this read as "just now" rather than "0 min ago". */
private const val JUST_NOW_MILLIS = 60_000L

private const val MILLIS_PER_MINUTE = 60_000L
private const val MILLIS_PER_HOUR = 60L * MILLIS_PER_MINUTE
private const val MILLIS_PER_DAY = 24L * MILLIS_PER_HOUR

/**
 * Formats how long ago a roll happened, coarsening as it recedes: `"just now"` under a minute,
 * then `"3 min ago"`, `"2 h ago"` and `"5 d ago"`.
 *
 * Only ever as precise as the reader needs — nobody cares that a roll was 47 minutes and 12
 * seconds ago, and a coarse label also stops being wrong as slowly, which matters because the
 * screen re-reads [nowMillis] on roll and on expand rather than ticking every second.
 *
 * Each unit is a `plurals` rather than one format string with a number in it, so a language whose
 * "minute" inflects can say so; English simply repeats itself across both quantities.
 *
 * A [rolledAtMillis] in the future (a clock or timezone change between launches) reads as
 * `"just now"` rather than as a negative age.
 *
 * @param rolledAtMillis when the roll happened, in epoch milliseconds
 * @param nowMillis the current time, in epoch milliseconds
 */
fun relativeTimeLabel(rolledAtMillis: Long, nowMillis: Long): UiText {
    val age = nowMillis - rolledAtMillis
    return when {
        age < JUST_NOW_MILLIS -> UiText.Res(R.string.relative_time_just_now)
        age < MILLIS_PER_HOUR -> elapsed(R.plurals.relative_time_minutes, age / MILLIS_PER_MINUTE)
        age < MILLIS_PER_DAY -> elapsed(R.plurals.relative_time_hours, age / MILLIS_PER_HOUR)
        else -> elapsed(R.plurals.relative_time_days, age / MILLIS_PER_DAY)
    }
}

/**
 * One elapsed-whole-units label.
 *
 * [units] is narrowed to `Int` here rather than in the caller: the arithmetic above is in `Long`
 * milliseconds, but a quantity string selects on an `Int`, and a count of whole minutes, hours or
 * days that overflows one is a timestamp from several million years ago.
 */
private fun elapsed(@PluralsRes pluralsId: Int, units: Long): UiText =
    UiText.Plural(pluralsId, units.coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
