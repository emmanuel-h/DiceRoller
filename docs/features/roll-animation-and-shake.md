# Roll animation and shake to roll

Issue #1. A roll is still decided the instant it is requested; what changed is how it is *revealed*,
and that a shake can request it.

## The reveal

```
  tap Roll ──► ViewModel: result published, pool emptied, roll recorded   (unchanged, synchronous)
          └──► UI: rememberRollReveal(result) plays
                 0–600ms   tumble: each die rocks ±15° (decaying) and flickers through
                           12 random faces, slowing towards the end
                 600ms+    landing: scale 1.15 → 1 on a bouncy spring
                 landed    ×N counts and the total fade in, the live region announces,
                           the log shows the new entry
```

- **UI-only.** `RollReveal` lives in `presentation/component/RollReveal.kt` and the ViewModel knows
  nothing of it, so process death mid-tumble loses an animation, never a roll.
- **Plays once per roll.** Whether the current result has been revealed is `rememberSaveable`, and a
  result present at first composition counts as revealed — rotation or a language switch never
  replays it. Every real roll passes through `result == null` first (rolling empties the pool; the
  only way back clears the result), so "null → non-null" is exactly "a new roll".
- **Nothing gives the outcome away early.** The layout is the final one from the first frame (so
  landing moves only numerals), but `×N`, the total and the spoken summary wait for landing, and
  `DiceRollerScreen` drops the log's newest entry while it matches the tumbling result.
- **Reduced motion is free.** Compose scales every animation by the system animator duration scale;
  at 0 the tumble and landing finish on their first frame. `RollRevealReducedMotionUiTest` pins this
  with a `MotionDurationScale` of 0 in the rule's effect context.
- **No re-roll mid-tumble.** Roll has already emptied the pool, so the button is disabled and a
  shake is a no-op until a die is queued again.
- Faces and tilt are pure functions (`tumblingFace`, `tumblingTilt`) seeded per die, so a
  recomposition within one flicker step does not change the face, and neighbours do not move in
  lockstep.

## Shake to roll

- `ShakeDetector` (pure Kotlin, JVM-tested): a shake is **2 jolts within 1s**, a jolt being the
  acceleration *rising* through 1.8g. (First shipped at 3 jolts / 2.5g / 800ms, which proved hard
  to trigger on a real phone.) Rising edges, not samples, so a phone set down hard counts
  once. A 1.5s cooldown makes one long shake roll once.
- `ShakeToRollEffect` registers the accelerometer only between `ON_RESUME` and `ON_PAUSE`
  (`LifecycleResumeEffect`), and only while enabled. A device without one never fires.
- The screen disables it while the settings sheet or the custom-die dialog is open.
- **On by default**, switchable under *Rolling* in the settings sheet. Persisted by
  `DataStoreShakeToRollStore` (key `shake_to_roll_enabled`, absent = on) in the shared
  `diceDataStore`. Like colour and language, flipping it never touches the result or the log.

## Side effect: the settings sheet opens fully expanded

The Rolling section pushed the artwork credit below the half-open sheet's fold, and the licence
needs it one tap away (`…TheRequiredCredit…` tests). The sheet now uses
`skipPartiallyExpanded = true`.

## Tests

| Test | Covers |
|---|---|
| `ShakeDetectorTest` | threshold, jolt counting by rising edge, window, cooldown |
| `RollRevealTest` | faces stay in range and land on the result; tilt bounded and level when landed |
| `DiceRollerViewModelTest` (shake section) | default on, restore, write-through, result/log untouched |
| `RollRevealUiTest` | no announcement and no log entry mid-tumble; both after landing; no replay of a result already on screen |
| `RollRevealReducedMotionUiTest` | animations off → outcome within a few frames |
| `SettingsSheetTest` (shake section) | switch state and toggling |
