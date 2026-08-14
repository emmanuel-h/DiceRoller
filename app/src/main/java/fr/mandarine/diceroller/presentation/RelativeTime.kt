// app/src/main/java/fr/mandarine/diceroller/presentation/RelativeTime.kt
package fr.mandarine.diceroller.presentation

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
 * A [rolledAtMillis] in the future (a clock or timezone change between launches) reads as
 * `"just now"` rather than as a negative age.
 *
 * @param rolledAtMillis when the roll happened, in epoch milliseconds
 * @param nowMillis the current time, in epoch milliseconds
 */
fun relativeTimeLabel(rolledAtMillis: Long, nowMillis: Long): String {
    val age = nowMillis - rolledAtMillis
    return when {
        age < JUST_NOW_MILLIS -> "just now"
        age < MILLIS_PER_HOUR -> "${age / MILLIS_PER_MINUTE} min ago"
        age < MILLIS_PER_DAY -> "${age / MILLIS_PER_HOUR} h ago"
        else -> "${age / MILLIS_PER_DAY} d ago"
    }
}
