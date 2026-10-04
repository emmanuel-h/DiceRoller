// app/src/main/java/fr/mandarine/diceroller/presentation/component/RollSoundPlayer.kt
package fr.mandarine.diceroller.presentation.component

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import fr.mandarine.diceroller.R
import fr.mandarine.diceroller.presentation.RollSound
import kotlin.random.Random

/**
 * Clips per [RollSound], from Kenney's Casino Audio (CC0). A single die plays one of the pack's own
 * throws; several dice play one of two handfuls mixed from them — three dice and six dice landing
 * in a staggered rush — because the pack's multi-dice throws alone were too short to sound like one.
 */
private val CLIPS: Map<RollSound, List<Int>> = mapOf(
    RollSound.OneDie to listOf(
        R.raw.roll_one_die_1,
        R.raw.roll_one_die_2,
        R.raw.roll_one_die_3,
        R.raw.roll_one_die_4,
    ),
    RollSound.SeveralDice to listOf(
        R.raw.roll_several_dice_1,
        R.raw.roll_several_dice_2,
    ),
)

/**
 * Two rolls can overlap only when a shake lands on the end of the previous clip; two streams is
 * enough for that, and SoundPool silences the oldest beyond it.
 */
private const val MAX_STREAMS = 2

/**
 * Plays the dice sound for a roll (issue #5).
 *
 * A [SoundPool] rather than a `MediaPlayer`: the clips are under a second, decoded once up front, and
 * must start on the frame the tumble does — a media player's prepare would put the clatter behind
 * the dice. Loading is asynchronous, so a roll in the first instant after launch may play nothing;
 * that is preferred to blocking the first frame on decoding.
 *
 * The stream is tagged as game sonification, which plays on the media volume — the slider the volume
 * keys move while the app is in front (see `MainActivity`) — so muting media mutes the dice.
 */
class RollSoundPlayer internal constructor(
    private val soundPool: SoundPool?,
    private val soundIds: Map<RollSound, List<Int>>,
    private val random: Random,
) {

    /** Plays one of [sound]'s clips, picked at random. A no-op while that clip is still loading. */
    fun play(sound: RollSound) {
        val ids = soundIds[sound].orEmpty()
        if (ids.isEmpty()) return
        soundPool?.play(ids.random(random), 1f, 1f, 1, 0, 1f)
    }

    internal fun release() {
        soundPool?.release()
    }

    companion object {
        /** A player that never makes a sound, which is what the preview pane gets. */
        val Silent: RollSoundPlayer = RollSoundPlayer(soundPool = null, soundIds = emptyMap(), random = Random)

        internal fun create(context: Context): RollSoundPlayer {
            val soundPool = SoundPool.Builder()
                .setMaxStreams(MAX_STREAMS)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                .build()
            val soundIds = CLIPS.mapValues { (_, clips) ->
                clips.map { clip -> soundPool.load(context, clip, 1) }
            }
            return RollSoundPlayer(soundPool, soundIds, Random.Default)
        }
    }
}

/**
 * Remembers a [RollSoundPlayer] for as long as this composition lives, releasing its decoded clips
 * when it leaves. In the preview pane, which has no audio, it is [RollSoundPlayer.Silent].
 */
@Composable
fun rememberRollSoundPlayer(): RollSoundPlayer {
    if (LocalInspectionMode.current) return RollSoundPlayer.Silent
    val context = LocalContext.current.applicationContext
    val player = remember(context) { RollSoundPlayer.create(context) }
    DisposableEffect(player) {
        onDispose { player.release() }
    }
    return player
}
