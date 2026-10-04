// app/src/main/java/fr/mandarine/diceroller/presentation/component/ShakeToRollEffect.kt
package fr.mandarine.diceroller.presentation.component

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import fr.mandarine.diceroller.presentation.ShakeDetector

/** Nanoseconds in a millisecond, for converting [SensorEvent.timestamp]. */
private const val NANOS_PER_MILLI = 1_000_000L

/**
 * Calls [onShake] whenever the phone is shaken, while [enabled] and while the screen is resumed
 * (issue #1).
 *
 * Listening only between `ON_RESUME` and `ON_PAUSE` is what keeps a backgrounded app from rolling
 * in someone's pocket, and from holding the accelerometer awake for nothing. A device without an
 * accelerometer simply never calls [onShake].
 *
 * What counts as a shake is entirely [ShakeDetector]'s business; this only delivers samples to it.
 * A fresh detector is made each time listening starts — and each time [thresholdG] changes — so
 * jolts from before a pause never combine with ones after it.
 *
 * @param enabled whether to listen at all
 * @param thresholdG how hard a jolt must be, from [ShakeDetector.thresholdForSensitivity]
 * @param onShake invoked on the main thread once per recognised shake
 */
@Composable
fun ShakeToRollEffect(
    enabled: Boolean,
    onShake: () -> Unit,
    thresholdG: Float = ShakeDetector.DEFAULT_THRESHOLD_G,
) {
    val context = LocalContext.current
    val currentOnShake by rememberUpdatedState(onShake)

    LifecycleResumeEffect(enabled, thresholdG, context) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val listener = if (enabled && accelerometer != null) {
            val detector = ShakeDetector(thresholdG = thresholdG)
            object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    val (x, y, z) = event.values
                    if (detector.onSample(x, y, z, event.timestamp / NANOS_PER_MILLI)) {
                        currentOnShake()
                    }
                }

                override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) = Unit
            }.also { sensorManager.registerListener(it, accelerometer, SensorManager.SENSOR_DELAY_GAME) }
        } else {
            null
        }

        onPauseOrDispose {
            listener?.let { sensorManager?.unregisterListener(it) }
        }
    }
}
