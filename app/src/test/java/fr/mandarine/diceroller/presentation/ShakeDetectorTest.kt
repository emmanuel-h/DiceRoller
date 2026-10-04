package fr.mandarine.diceroller.presentation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShakeDetectorTest {

    /** A phone lying flat: gravity alone, 1g on the z axis. */
    private val rest = floatArrayOf(0f, 0f, 9.81f)

    /** A hard swing along x: about 3.3g in total, well past the default threshold. */
    private val jolt = floatArrayOf(31f, 0f, 9.81f)

    /**
     * Feeds [samples] (`true` = a jolt sample, `false` = a resting one) at [intervalMillis] apart
     * starting at [startMillis], and returns the timestamps at which a shake fired.
     */
    private fun ShakeDetector.feed(
        samples: List<Boolean>,
        startMillis: Long = 0L,
        intervalMillis: Long = 20L,
    ): List<Long> = samples.mapIndexedNotNull { index, isJolt ->
        val (x, y, z) = if (isJolt) jolt else rest
        val time = startMillis + index * intervalMillis
        time.takeIf { onSample(x, y, z, time) }
    }

    /** Back and forth [times]: a jolt sample then a resting one, repeated. */
    private fun swings(times: Int): List<Boolean> = List(times) { listOf(true, false) }.flatten()

    @Test
    fun givenAPhoneAtRest_whenSampled_thenNoShakeIsDetected() {
        val shakes = ShakeDetector().feed(List(200) { false })

        assertTrue(shakes.isEmpty())
    }

    @Test
    fun givenTwoQuickSwings_whenSampled_thenOneShakeFiresOnTheSecond() {
        val shakes = ShakeDetector().feed(swings(2))

        assertEquals(listOf(40L), shakes)
    }

    @Test
    fun givenOneSwing_whenSampled_thenNoShakeIsDetected() {
        val shakes = ShakeDetector().feed(swings(1))

        assertTrue(shakes.isEmpty())
    }

    /** A phone put down hard stays above the threshold for several samples: one jolt, not a shake. */
    @Test
    fun givenOneSustainedKnock_whenSampled_thenItCountsAsASingleJolt() {
        val shakes = ShakeDetector().feed(List(10) { true } + List(10) { false })

        assertTrue(shakes.isEmpty())
    }

    @Test
    fun givenSwingsSpreadWiderThanTheWindow_whenSampled_thenNoShakeIsDetected() {
        val detector = ShakeDetector(windowMillis = 800L)

        val shakes = listOf(0L, 900L, 1_800L, 2_700L, 3_600L).filter { time ->
            val jolted = detector.onSample(jolt[0], jolt[1], jolt[2], time)
            detector.onSample(rest[0], rest[1], rest[2], time + 20)
            jolted
        }

        assertTrue(shakes.isEmpty())
    }

    @Test
    fun givenALongShake_whenSampled_thenItFiresOncePerCooldown() {
        // 50 swings at 40ms each = 2s of continuous shaking, against a 1.5s cooldown.
        val shakes = ShakeDetector(cooldownMillis = 1_500L).feed(swings(50))

        assertEquals(2, shakes.size)
        assertTrue(shakes[1] - shakes[0] >= 1_500L)
    }

    @Test
    fun givenAShakeThenAPause_whenShakenAgainAfterTheCooldown_thenItFiresAgain() {
        val detector = ShakeDetector()

        val first = detector.feed(swings(3))
        val second = detector.feed(swings(3), startMillis = 5_000L)

        assertEquals(1, first.size)
        assertEquals(1, second.size)
    }

    @Test
    fun givenAJoltBelowTheThreshold_whenSampled_thenItDoesNotCount() {
        val detector = ShakeDetector(thresholdG = 2.5f)
        val gentle = 2f * 9.81f

        val fired = (0 until 20).any { index ->
            val time = index * 20L
            if (index % 2 == 0) detector.onSample(gentle, 0f, 0f, time) else detector.onSample(0f, 0f, 9.81f, time)
        }

        assertFalse(fired)
    }

    /** Walking jolts a phone held in the hand about 1.2–1.3g, over and over; that must never roll. */
    @Test
    fun givenAWalkingRhythm_whenSampledWithDefaults_thenNoShakeIsDetected() {
        val detector = ShakeDetector()
        val step = 1.3f * 9.81f

        val fired = (0 until 200).any { index ->
            val time = index * 20L
            if (index % 25 == 0) detector.onSample(0f, step, 0f, time) else detector.onSample(0f, 0f, 9.81f, time)
        }

        assertFalse(fired)
    }

    // --- Sensitivity steps ---

    @Test
    fun givenTheDefaultSensitivity_whenMappedToAThreshold_thenItIsTheDefaultThreshold() {
        assertEquals(
            ShakeDetector.DEFAULT_THRESHOLD_G,
            ShakeDetector.thresholdForSensitivity(ShakeDetector.DEFAULT_SENSITIVITY),
            0f,
        )
    }

    @Test
    fun givenEachSensitivityStep_whenMappedToAThreshold_thenMoreSensitiveMeansALowerThreshold() {
        val thresholds = (0 until ShakeDetector.SENSITIVITY_LEVELS)
            .map(ShakeDetector::thresholdForSensitivity)

        assertEquals(thresholds.sortedDescending(), thresholds)
        assertEquals(thresholds.size, thresholds.distinct().size)
    }

    @Test
    fun givenAStepOutOfRange_whenMappedToAThreshold_thenItIsClampedToTheNearestEnd() {
        val last = ShakeDetector.SENSITIVITY_LEVELS - 1

        assertEquals(ShakeDetector.thresholdForSensitivity(0), ShakeDetector.thresholdForSensitivity(-3), 0f)
        assertEquals(ShakeDetector.thresholdForSensitivity(last), ShakeDetector.thresholdForSensitivity(99), 0f)
    }

    /** The point of the most sensitive step: a swing the default ignores now rolls. */
    @Test
    fun givenAGentleShake_whenTheMostSensitiveStepIsUsed_thenItRollsWhereTheDefaultDoesNot() {
        val gentle = floatArrayOf(1.3f * 9.81f, 0f, 0f)
        fun shakesAt(thresholdG: Float): Int {
            val detector = ShakeDetector(thresholdG = thresholdG)
            return (0 until 8).count { index ->
                val (x, y, z) = if (index % 2 == 0) gentle else rest
                detector.onSample(x, y, z, index * 20L)
            }
        }

        assertEquals(0, shakesAt(ShakeDetector.DEFAULT_THRESHOLD_G))
        assertEquals(1, shakesAt(ShakeDetector.thresholdForSensitivity(ShakeDetector.SENSITIVITY_LEVELS - 1)))
    }

    @Test(expected = IllegalArgumentException::class)
    fun givenZeroRequiredJolts_whenCreated_thenItIsRejected() {
        ShakeDetector(requiredJolts = 0)
    }
}
