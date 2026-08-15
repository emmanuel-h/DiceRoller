# Testing: Custom dice

Covers [the custom-dice PRD](../features/custom-dice.md) (issue #4).

Totals after this feature: **222 unit tests** (was 149) and **121 instrumented tests** (was 100).
All pass; `./gradlew lint` is clean.

## Unit tests

| File | Tests | What it pins |
|---|---|---|
| `domain/DieTypeTest` | 11 | The no-two-dice-share-a-face-count invariant, at both doors: `CustomDie`'s constructor throws for a preset's face count or an out-of-range one, and `DieType.ofFaces` returns the preset / a custom die / null. Also that `label == name` for every preset, which is what made the `name` → `label` swap a no-op for user-visible strings. |
| `presentation/CustomFacesValidationTest` | 14 | Every refusal reason, and which one wins when an input breaks more than one rule (duplicate and preset both beat "no room"). Also that empty and `"1"` are *incomplete* rather than errors. |
| `data/CustomDiceCodecTest` | 13 | Round trip; and that a corrupt entry, an out-of-range one, and — the case that would otherwise throw — a *preset's* face count are each dropped individually rather than crashing the app on launch. Plus normalization on decode (dedupe, sort, cap). |
| `presentation/DiceRollerViewModelTest` | +33 | Add / remove / undo; pool reconciliation; the result-clearing rule in all three of its cases; write-through and restore for each store operation; that presets always keep a pool entry; that the log keeps a deleted die's past rolls. |
| `domain/DicePoolTest` | +4 | Custom dice interleave with presets by face count, and are excluded at count 0 like any preset. |
| `presentation/DiceNotationTest` | +4 | `2D7`, and `4D6 + 1D7 + 2D8` ordering from a map. |
| `data/RollHistoryCodecTest` | +5 | See below. |

### The one test whose premise this feature changed

`givenUnknownFaceCount_whenDecoded_thenRecordIsDropped` asserted that `"1;7:5*1"` — 7 faces —
decoded to nothing. That is no longer true and should not be: a custom-die roll must survive a
restart. It was replaced by five tests that state the new truth precisely: 7 faces decodes to
`CustomDie(7)`; a face count above the range, below it, and a value above a custom die's own
maximum are all still dropped; and a record mixing presets with custom dice round-trips unchanged.

## Instrumented tests

`CustomDiceUiTest` — 19 tests, driven through a **real ViewModel** rather than hand-fed UI states,
because the feature's whole point is that a die the user *creates* then behaves like a preset.
Covers: the add chip's presence and disappearance at the cap; the creator's enablement and its two
refusal messages; creating and cancelling; incrementing a custom chip and the resulting
`Roll 1D6 + 1D7 + 1D8` label; the `1×D7` group appearing in the ladder; the `×` badge removing the
chip; **presets carrying no badge at all**; and Undo restoring the die together with its count.

### The three fit-on-screen tests that had to change

Issue #64's suite guarantees two different things, and only one of them bent:

- **Every control fits at 640dp** — swatch row, all chips, history header, Roll button,
  attribution. Still passes, untouched. This is the guarantee that does not bend. (Issue #66 later
  replaced the attribution line in these assertions with the About button that now surfaces it.)
- **The last result group and the total fit outright** at 640dp / 680dp. This no longer holds: the
  third chip row costs 106dp against ~16dp of slack.

Two tests were failing on `4×D20` and one on `Total 80`. Rather than delete those assertions they
now use `performScrollTo().assertIsDisplayed()` — a weaker but still real guarantee that the
content exists and is reachable, not gone. `performScrollTo` is a no-op for an already-visible
node, so it is the same assertion whichever of them still fits on a given device.

Two **new** tests pin the measured thresholds so they cannot drift silently:
`TOTAL_FIT_HEIGHT_NO_HISTORY = 740.dp` and `TOTAL_FIT_HEIGHT_WITH_HISTORY = 780.dp`.

Both numbers were measured on a Medium_Phone_API_36.1 emulator with a temporary sweep harness, not
estimated — as was the 98dp chip height that showed the `IntrinsicSize.Min` row change added no
height of its own. The harness was deleted after use.

## Manual verification on device

Walked the whole flow on the emulator and confirmed by screenshot: the grid at three rows with the
add chip as the seventh cell; the creator accepting `7`; the D7 chip's badge and `×`;
`Roll 1D6 + 2D7 + 1D8` with the ladder interleaving `1×D6 / 2×D7 / 1×D8` and `Total 18`; the
removal snackbar; Undo restoring `D7 2`; the definition surviving a `force-stop` with its pool count
correctly reset to 0; and the persisted log entry rendering the D7 rolls as compact badges.

One behaviour worth recording because it looks like a bug and is not: tapping Undo *after* the
snackbar's ~4s timeout does nothing. The timeout fires `dismissRemovedCustomDie`, which retires the
offer by design.

## Related docs

- [Feature PRD](../features/custom-dice.md)
- [Architecture](../architecture/custom-dice.md)
- [Design](../design/custom-dice.md)
