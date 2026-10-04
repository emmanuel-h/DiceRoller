// app/src/main/java/fr/mandarine/diceroller/presentation/component/RollReveal.kt
package fr.mandarine.diceroller.presentation.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import fr.mandarine.diceroller.domain.DicePoolResult
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/** How long the dice tumble before landing. The landing bounce follows it. */
const val ROLL_TUMBLE_MILLIS: Int = 600

/** How much a die overshoots its size as it lands, before springing back to 1. */
private const val LANDING_SCALE = 1.15f

/** How many different faces a tumbling die shows; spread so the flicker slows as it lands. */
private const val FLICKER_STEPS = 12

/** Peak tilt of a tumbling die either way, in degrees. */
private const val TUMBLE_MAX_DEGREES = 15f

/** How many times a die rocks back and forth over the tumble. */
private const val TUMBLE_ROCKS = 3

/**
 * Where the roll animation is (issue #1): the dice tumbling, landing, or at rest.
 *
 * The roll itself is decided the instant Roll is tapped — the ViewModel publishes the result and
 * records it at once, exactly as before. This is only how that result is *revealed*, which is why it
 * lives in the UI and the ViewModel knows nothing of it: a process death mid-tumble loses an
 * animation, never a roll.
 *
 * Hoisted to the screen rather than kept inside [DiceResultDisplay] because the history band needs
 * it too: the log must not show the new roll's faces while the result band is still tumbling them.
 *
 * Both values are read through lambdas over animation state, so a reader in a `graphicsLayer` block
 * redraws without recomposing.
 */
@Stable
class RollReveal internal constructor(
    private val progressOf: () -> Float,
    private val landingScaleOf: () -> Float,
) {

    /** 0 when the dice start tumbling, 1 once they have landed. */
    val progress: Float get() = progressOf()

    /** The dice's scale for the bounce that follows the tumble; 1 at rest. */
    val landingScale: Float get() = landingScaleOf()

    /**
     * True while the dice are still tumbling, i.e. the result is on screen but not yet readable.
     *
     * Derived, so a composable reading only this recomposes twice per roll rather than every frame.
     */
    val isRevealing: Boolean by derivedStateOf { progress < 1f }

    companion object {
        /** A reveal that has already finished, for a result that should simply be shown. */
        val Settled: RollReveal = RollReveal(progressOf = { 1f }, landingScaleOf = { 1f })
    }
}

/**
 * Remembers a [RollReveal] that plays once each time [result] goes from null to a roll.
 *
 * "Once" survives recreation: whether the current result has been revealed is saved state, so
 * rotating the phone, or anything else that rebuilds the screen, shows the landed result rather than
 * rolling it again. For the same reason a result already present when this is first composed counts
 * as revealed. Every real roll does pass through null first — rolling empties the pool, and the only
 * way back to a non-empty one clears the result — so keying on null is the same as keying on "a new
 * roll".
 *
 * The animation needs no reduced-motion branch of its own: Compose scales every animation by the
 * system's animator duration scale, and at 0 both the tumble and the landing finish on their first
 * frame.
 *
 * [onRevealStart] is called as the tumble begins, with the result being revealed — the roll's sound
 * (issue #5) hangs off it, so it follows the same "once per roll" rule and a rotation stays silent.
 */
@Composable
fun rememberRollReveal(
    result: DicePoolResult?,
    onRevealStart: (DicePoolResult) -> Unit = {},
): RollReveal {
    val tumble = remember { Animatable(1f) }
    val landing = remember { Animatable(1f) }
    val hasRevealed = rememberSaveable { mutableStateOf(result != null) }
    val currentResult by rememberUpdatedState(result)
    val currentOnRevealStart by rememberUpdatedState(onRevealStart)

    LaunchedEffect(result) {
        if (result == null) {
            hasRevealed.value = false
            tumble.snapTo(1f)
            landing.snapTo(1f)
        } else if (!hasRevealed.value) {
            tumble.snapTo(0f)
            hasRevealed.value = true
            currentOnRevealStart(result)
            tumble.animateTo(1f, tween(durationMillis = ROLL_TUMBLE_MILLIS, easing = LinearEasing))
            landing.snapTo(LANDING_SCALE)
            landing.animateTo(
                1f,
                spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
            )
        }
    }

    return remember {
        RollReveal(
            // Between a result arriving and the effect above starting there is a frame; reporting
            // 0 for it is what keeps the landed faces from flashing up before the tumble begins.
            progressOf = {
                if (currentResult != null && !hasRevealed.value) 0f else tumble.value
            },
            landingScaleOf = { landing.value },
        )
    }
}

/**
 * The face a die shows at [progress] through the tumble: [finalValue] once landed, otherwise a
 * pseudo-random face that changes [FLICKER_STEPS] times, more slowly towards the end.
 *
 * Seeded from the step and [seed] rather than drawn fresh each frame, so a recomposition that is
 * not a new step does not change the face, and two dice side by side do not flicker in lockstep.
 */
internal fun tumblingFace(faces: Int, finalValue: Int, progress: Float, seed: Int): Int {
    if (progress >= 1f) return finalValue
    val easedOut = 1f - (1f - progress) * (1f - progress)
    val step = (easedOut * FLICKER_STEPS).toInt()
    return Random(seed * FLICKER_STEPS + step).nextInt(from = 1, until = faces + 1)
}

/**
 * A tumbling die's tilt at [progress], in degrees: it rocks [TUMBLE_ROCKS] times, each swing
 * smaller than the last, and is level once landed. [seed] offsets the phase so neighbours differ.
 */
internal fun tumblingTilt(progress: Float, seed: Int): Float {
    if (progress >= 1f) return 0f
    val phase = progress * TUMBLE_ROCKS * 2 * PI + seed
    return TUMBLE_MAX_DEGREES * sin(phase).toFloat() * (1f - progress)
}
