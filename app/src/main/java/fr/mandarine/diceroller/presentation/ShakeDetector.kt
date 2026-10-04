// app/src/main/java/fr/mandarine/diceroller/presentation/ShakeDetector.kt
package fr.mandarine.diceroller.presentation

import kotlin.math.sqrt

/** Standard gravity in m/s², what an accelerometer reads for a phone lying still. */
private const val STANDARD_GRAVITY = 9.80665f

/**
 * Recognises a deliberate shake in a stream of accelerometer samples (issue #1).
 *
 * Pure Kotlin on purpose: the sensor plumbing lives in `ShakeToRollEffect`, which hands every
 * sample straight to [onSample], so the part that can be wrong — what counts as a shake — is
 * covered by fast JVM tests instead of a device being waved about.
 *
 * A shake is [requiredJolts] **jolts** within [windowMillis], where a jolt is the acceleration
 * *rising* through [thresholdG]. Counting rising edges rather than samples above the threshold is
 * what keeps one hard knock — a phone set down on a table, which stays above the threshold for
 * several samples — from counting as a shake: a shake goes back and forth, so it crosses the line
 * repeatedly. After firing, the detector ignores everything for [cooldownMillis], so one long shake
 * rolls once rather than once per pair of jolts.
 *
 * Not thread-safe; feed it from a single thread (the sensor callback's).
 *
 * @param thresholdG how hard a jolt must be, in multiples of gravity. A phone at rest reads 1g.
 * @param requiredJolts how many jolts make a shake
 * @param windowMillis how close together those jolts must be
 * @param cooldownMillis how long after a shake further samples are ignored
 */
class ShakeDetector(
    private val thresholdG: Float = DEFAULT_THRESHOLD_G,
    private val requiredJolts: Int = DEFAULT_REQUIRED_JOLTS,
    private val windowMillis: Long = DEFAULT_WINDOW_MILLIS,
    private val cooldownMillis: Long = DEFAULT_COOLDOWN_MILLIS,
) {

    init {
        require(requiredJolts >= 1) { "requiredJolts must be at least 1, was $requiredJolts" }
    }

    /** Timestamps of the jolts still inside the window, oldest first. */
    private val jolts = ArrayDeque<Long>()

    /** Whether the previous sample was already above the threshold. */
    private var wasAboveThreshold = false

    /** When the last shake fired, or null if none has. */
    private var lastShakeMillis: Long? = null

    /**
     * Feeds one accelerometer sample, in m/s² per axis, and returns true exactly when it completes
     * a shake.
     *
     * @param timestampMillis when the sample was taken, on any monotonic clock
     */
    fun onSample(x: Float, y: Float, z: Float, timestampMillis: Long): Boolean {
        val above = sqrt(x * x + y * y + z * z) / STANDARD_GRAVITY >= thresholdG
        val isJolt = above && !wasAboveThreshold
        wasAboveThreshold = above

        val lastShake = lastShakeMillis
        if (lastShake != null && timestampMillis - lastShake < cooldownMillis) return false
        if (!isJolt) return false

        while (jolts.isNotEmpty() && timestampMillis - jolts.first() > windowMillis) {
            jolts.removeFirst()
        }
        jolts.addLast(timestampMillis)
        if (jolts.size < requiredJolts) return false

        jolts.clear()
        lastShakeMillis = timestampMillis
        return true
    }

    companion object {
        /**
         * Above walking (about 1.2–1.5g at the hip) yet reachable with a flick of the wrist. 2.5g
         * was tried first and proved hard to reach on a real phone.
         */
        const val DEFAULT_THRESHOLD_G: Float = 1.8f

        /**
         * Back and forth: two jolts. One is not enough, since that is what a phone set down hard
         * produces; three made the gesture feel laboured.
         */
        const val DEFAULT_REQUIRED_JOLTS: Int = 2

        const val DEFAULT_WINDOW_MILLIS: Long = 1_000L

        /** Longer than the roll animation, so a shake that carries on does not roll again. */
        const val DEFAULT_COOLDOWN_MILLIS: Long = 1_500L
    }
}
