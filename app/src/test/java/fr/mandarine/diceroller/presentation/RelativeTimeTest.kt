// app/src/test/java/fr/mandarine/diceroller/presentation/RelativeTimeTest.kt
package fr.mandarine.diceroller.presentation

import fr.mandarine.diceroller.R
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Where each unit boundary falls, and which quantity string an age lands in (issue #68). The
 * wording of "3 min ago" belongs to `values/strings.xml`; the arithmetic that picks minutes over
 * hours, and the whole-units count it hands over, belongs here.
 */
class RelativeTimeTest {

    private val now = 1_700_000_000_000L

    private fun labelForAge(ageMillis: Long): UiText =
        relativeTimeLabel(rolledAtMillis = now - ageMillis, nowMillis = now)

    // --- Just now ---

    @Test
    fun givenRollThisInstant_whenLabelled_thenTheJustNowStringIsChosen() {
        assertEquals(UiText.Res(R.string.relative_time_just_now), labelForAge(0L))
    }

    @Test
    fun givenRollJustUnderAMinuteOld_whenLabelled_thenStillJustNow() {
        assertEquals(UiText.Res(R.string.relative_time_just_now), labelForAge(59_999L))
    }

    // --- Minutes ---

    @Test
    fun givenRollExactlyOneMinuteOld_whenLabelled_thenSwitchesToMinutes() {
        assertEquals(UiText.Plural(R.plurals.relative_time_minutes, 1), labelForAge(60_000L))
    }

    @Test
    fun givenRollWithinTheHour_whenLabelled_thenRoundsDownToWholeMinutes() {
        assertEquals(
            UiText.Plural(R.plurals.relative_time_minutes, 3),
            labelForAge(3L * 60_000L + 59_000L),
        )
    }

    @Test
    fun givenRollJustUnderAnHourOld_whenLabelled_thenStillReadsInMinutes() {
        assertEquals(
            UiText.Plural(R.plurals.relative_time_minutes, 59),
            labelForAge(60L * 60_000L - 1L),
        )
    }

    // --- Hours ---

    @Test
    fun givenRollExactlyOneHourOld_whenLabelled_thenSwitchesToHours() {
        assertEquals(UiText.Plural(R.plurals.relative_time_hours, 1), labelForAge(60L * 60_000L))
    }

    @Test
    fun givenRollJustUnderADayOld_whenLabelled_thenStillReadsInHours() {
        assertEquals(
            UiText.Plural(R.plurals.relative_time_hours, 23),
            labelForAge(24L * 60L * 60_000L - 1L),
        )
    }

    // --- Days ---

    @Test
    fun givenRollExactlyOneDayOld_whenLabelled_thenSwitchesToDays() {
        assertEquals(
            UiText.Plural(R.plurals.relative_time_days, 1),
            labelForAge(24L * 60L * 60_000L),
        )
    }

    @Test
    fun givenVeryOldRoll_whenLabelled_thenCountsWholeDays() {
        assertEquals(
            UiText.Plural(R.plurals.relative_time_days, 9),
            labelForAge(9L * 24L * 60L * 60_000L + 60_000L),
        )
    }

    // --- Clock going backwards between launches ---

    @Test
    fun givenRollTimestampedInTheFuture_whenLabelled_thenReadsJustNowRatherThanNegative() {
        assertEquals(
            UiText.Res(R.string.relative_time_just_now),
            relativeTimeLabel(rolledAtMillis = now + 1_000L, nowMillis = now),
        )
    }

    /**
     * The elapsed-days count is narrowed from `Long` to the `Int` a quantity string selects on.
     * A timestamp old enough to overflow that is nonsense rather than a date, and must still
     * produce a label instead of a wrapped-around negative one.
     */
    @Test
    fun givenAnAbsurdlyOldRoll_whenLabelled_thenTheDayCountSaturatesRatherThanOverflowing() {
        val label = relativeTimeLabel(rolledAtMillis = Long.MIN_VALUE / 2, nowMillis = now)
            as UiText.Plural

        assertEquals(R.plurals.relative_time_days, label.id)
        assertEquals(Int.MAX_VALUE, label.count)
    }
}
