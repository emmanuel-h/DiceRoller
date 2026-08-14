// app/src/test/java/fr/mandarine/diceroller/presentation/RelativeTimeTest.kt
package fr.mandarine.diceroller.presentation

import org.junit.Assert.assertEquals
import org.junit.Test

class RelativeTimeTest {

    private val now = 1_700_000_000_000L

    private fun labelForAge(ageMillis: Long): String =
        relativeTimeLabel(rolledAtMillis = now - ageMillis, nowMillis = now)

    // --- Just now ---

    @Test
    fun givenRollThisInstant_whenLabelled_thenReadsJustNow() {
        assertEquals("just now", labelForAge(0L))
    }

    @Test
    fun givenRollJustUnderAMinuteOld_whenLabelled_thenStillReadsJustNow() {
        assertEquals("just now", labelForAge(59_999L))
    }

    // --- Minutes ---

    @Test
    fun givenRollExactlyOneMinuteOld_whenLabelled_thenSwitchesToMinutes() {
        assertEquals("1 min ago", labelForAge(60_000L))
    }

    @Test
    fun givenRollWithinTheHour_whenLabelled_thenRoundsDownToWholeMinutes() {
        assertEquals("3 min ago", labelForAge(3L * 60_000L + 59_000L))
    }

    @Test
    fun givenRollJustUnderAnHourOld_whenLabelled_thenStillReadsInMinutes() {
        assertEquals("59 min ago", labelForAge(60L * 60_000L - 1L))
    }

    // --- Hours ---

    @Test
    fun givenRollExactlyOneHourOld_whenLabelled_thenSwitchesToHours() {
        assertEquals("1 h ago", labelForAge(60L * 60_000L))
    }

    @Test
    fun givenRollJustUnderADayOld_whenLabelled_thenStillReadsInHours() {
        assertEquals("23 h ago", labelForAge(24L * 60L * 60_000L - 1L))
    }

    // --- Days ---

    @Test
    fun givenRollExactlyOneDayOld_whenLabelled_thenSwitchesToDays() {
        assertEquals("1 d ago", labelForAge(24L * 60L * 60_000L))
    }

    @Test
    fun givenVeryOldRoll_whenLabelled_thenCountsWholeDays() {
        assertEquals("9 d ago", labelForAge(9L * 24L * 60L * 60_000L + 60_000L))
    }

    // --- Clock going backwards between launches ---

    @Test
    fun givenRollTimestampedInTheFuture_whenLabelled_thenReadsJustNowRatherThanNegative() {
        assertEquals("just now", relativeTimeLabel(rolledAtMillis = now + 1_000L, nowMillis = now))
    }
}
