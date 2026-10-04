# Roll sound

Issue [#5](https://github.com/emmanuel-h/DiceRoller/issues/5). Every roll, tapped or
shaken, starts with the clatter of dice.

## Behaviour

- The sound starts on the same frame as the tumble (`ROLL_TUMBLE_MILLIS` = 600 ms).
- **One die or several:** a roll of exactly one die (any type, custom included) plays one
  of four single-die throws (0.47–0.73 s). Anything more, `1D6 + 1D8` included, plays one
  of two *handfuls*: a 3-dice mix (0.79 s) or a 6-dice mix (1.20 s). The clip is picked at
  random each time, so repeated rolls do not sound like a loop. The handfuls are random
  rather than chosen by pool size, and the tumble stays 600 ms, so a big handful is still
  clattering as the dice land, like the last die bouncing at the table's edge. The pack's
  own multi-dice throws were tried first and sounded too short for several dice; see the
  mix recipe in `third-party-assets.md`. The choice is `RollSound.of(result)`, which is pure Kotlin and covered by
  `RollSoundTest`.
- **Once per roll:** the sound hangs off `rememberRollReveal`'s `onRevealStart`, not the
  Roll button, so it follows the animation's rule. A rotation or any other recreation
  restores the landed result silently, and a shake sounds the same as a tap.
- **Volume:** the stream is `USAGE_GAME` / `CONTENT_TYPE_SONIFICATION`, which plays on the
  media volume. `MainActivity` sets `volumeControlStream = STREAM_MUSIC`, so the volume
  keys move that slider while the app is in front. Muting media mutes the dice. The
  ringer's silent and vibrate modes are deliberately not consulted.

## Setting

A **Sound** section in the settings sheet, between Theme and Rolling, holds one
*Sound effects* switch. It is its own section rather than a row under Rolling because the
sound plays for every roll, not just shaken ones. It is **on by default**: `SoundStore` /
`DataStoreSoundStore` read an absent `sound_enabled` key as true. Like colour, language
and theme, `setSoundEnabled` never clears the result or the log.

The switch shares `SwitchRow` with shake to roll; both are whole-row toggles.

## Implementation

| Piece | Role |
|---|---|
| `presentation/RollSound.kt` | Which kind of sound a result makes |
| `presentation/SoundStore.kt` | Persistence interface plus `InMemorySoundStore` |
| `data/DataStoreSoundStore.kt` | DataStore-backed store on the shared `diceDataStore` |
| `presentation/component/RollSoundPlayer.kt` | `SoundPool` with 2 streams; clips decoded once at composition. `rememberRollSoundPlayer()` releases it on dispose and is `Silent` in the preview pane |
| `RollReveal.kt` | `onRevealStart(result)` hook, called as the tumble begins |

`SoundPool` rather than `MediaPlayer`, because a media player's prepare would put the
clatter behind the dice. Loading is asynchronous, so a roll in the first instant after
launch may play nothing. That was preferred over blocking the first frame.

## Verification

Besides the JVM and Compose tests, this was checked on an API 36 emulator by reading
`dumpsys audio`'s playback events: a `started` event on each roll while on, none while
off, none on rotation with a result showing. Count the *timestamps* of those events, not
their number, because the log is a fixed-size ring buffer.

## Assets

Kenney's *Casino Audio* (CC0). See
[`docs/licenses/third-party-assets.md`](../licenses/third-party-assets.md).
