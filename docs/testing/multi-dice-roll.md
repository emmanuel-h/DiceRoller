# Testing: Roll Multiple Dice at Once

> Covers GitHub issues #53, #54, #55, #56, plus PR #61 (screen integration).
> Related: [Roll Multiple Dice at Once (feature)](../features/multi-dice-roll.md) · [Architecture: Multi-Dice Roll (Pool Rolling)](../architecture/multi-dice-roll.md) · [Multi-Dice Roll — Design Spec](../design/multi-dice-roll.md)

## Overview

Test coverage for the dice-pool feature, verified directly against the source on branch `feat/multi-dice-roll/screen-integration` (PR #61) — the final branch in the stack `#48 → #49 → #50/#51 (merged) → #52`, which carries every commit from the whole pipeline.

**Verification method:** rather than taking the pipeline's own summary at face value, this doc was written after checking out that branch tip in a scratch worktree, reading each test file in full, and running the build:
- `./gradlew clean test` — genuinely executed (not just cache-hit) and **passed**: 96 relevant tests across `DicePoolTest` (11), `DiceRollerTest` (35), `DiceRollerViewModelTest` (29), `RollButtonLabelTest` (9), plus the pre-existing `DiceColorTest` (11) and `ExampleUnitTest` (1) — 0 failures, 0 errors.
- `./gradlew compileDebugAndroidTestKotlin` — **succeeded**, confirming all instrumented test sources compile against the new pool-based API.

**Important caveat — no device/emulator was available to any agent in this pipeline.** Every instrumented test under `src/androidTest/` (`DiceStepperChipTest`, `DiceResultDisplayTest`, `DiceRollerScreenTest`, `FantasyDiceArtUiTest`) has been verified to **compile** but has **not actually been executed** on a device or emulator. This is an open item — see [Open items](#open-items) below.

## Unit tests (`src/test/`) — run and passing

### `domain/DicePoolTest.kt` (11 tests, issue #53)
Tests for the `DicePool` value type:
- Default/no-arg construction yields an empty pool (`isEmpty == true`, `entries` empty).
- Validation: a negative count throws `IllegalArgumentException`; a count above `DicePool.MAX_DICE_PER_TYPE` (20) throws; a count exactly at the max, and a count of exactly 0, both succeed.
- `entries`: zero-count entries are excluded from the list; a mixed pool's entries are ordered smallest-to-largest by face count (D4 < D6 < D8 < D20), regardless of map insertion order.
- `isEmpty`: true when every count is 0 (including a pool with explicit zero entries), false as soon as any count is positive.
- `countFor`: returns 0 for a die type absent from the map, and the actual count for one present.

### `domain/DiceRollerTest.kt` (35 tests total; 15 new for pool rolling, issue #53; remainder pre-existing single-die coverage)
Pre-existing coverage (`Dice` enum shape/ordering/face counts, `DiceRoller.roll()` range/boundary/determinism per die type) is unchanged. New `rollPool()` coverage:
- **Empty pool:** an empty `DicePool()` and a pool where every entry is explicitly 0 both roll to zero groups and a total of 0 — a safe no-op.
- **Group shape:** a single-die-type pool produces exactly one group whose `dice` and `poolCount` match; a pool of count 1 produces a group with exactly one tally of count 1; a mixed pool's groups come back ordered smallest-to-largest by face count.
- **Tally correctness:**
  - Per group, `tallies.sumOf { it.count }` equals `poolCount` (every rolled die is accounted for).
  - Every tallied value is within `1..dice.faces`.
  - Tallies within a group are sorted descending by value.
  - No tally with `count == 0` ever appears.
  - A **golden-value test** (`givenSeededMixedPool_whenRollingPool_thenTalliesMatchIndependentlyReplayedRolls`) independently replays the same seeded `Random` sequence, die-by-die and group-by-group, via `List(count) { replayRoller.roll(dice) }.groupingBy { it }.eachCount()`, and asserts the replayed tally map matches `rollPool`'s output exactly — this is the strongest correctness check, verifying the grouping/tallying logic itself rather than just its invariants.
  - A pool of 3 D6 is asserted to have strictly fewer distinct tallied values than the 6 possible faces (pigeonhole check that untallied values are genuinely absent, not just zero-padded).
- **Total:** `result.total` equals the sum of `value * count` across every tally in every group.
- **Single-die-pool parity:** rolling a pool of exactly one D6 with a seeded `Random` produces the same value as calling `DiceRoller.roll(Dice.D6)` directly with an identically-seeded `Random` — the old single-roll path is preserved as the pool's degenerate case.
- **Determinism:** rolling the same pool twice with two `Random` instances sharing the same seed produces `equals()`-identical `DicePoolResult`s.

### `presentation/DiceRollerViewModelTest.kt` (29 tests, new file, issue #54)
Pool-aware `DiceRollerViewModel` coverage:
- **Initial state:** pool starts as `Dice.entries.associateWith { 0 }` (all six keys present, all zero); `canRoll` is false; `result` is null; `selectedColor` is the default.
- **`incrementCount`:** raises the target die's count by 1 without affecting other die types; repeated calls increase it by 1 each time.
- **Clamping at the cap:** incrementing at `MAX_DICE_PER_TYPE` (20) is a no-op — count stays at 20 even after 30 more increment calls.
- **`decrementCount`:** lowers a positive count by 1; clamping at the floor — decrementing at 0 is a no-op, including after 10 repeated calls.
- **Result-clearing rules:**
  - Both `incrementCount` and `decrementCount` clear `result` to null when they actually change a count.
  - Incrementing at the cap, or decrementing at the floor (both true no-ops), **preserve** an existing result — mirrors the "re-select same die is a no-op" rule from the single-die model.
  - `selectColor` never clears `result`, and never touches `pool` — verified independently.
- **`rollDice()` with an empty pool:** a no-op, `result` stays null; also verified after incrementing then decrementing the same die back to empty.
- **`rollDice()` with a non-empty pool:** produces a non-null result; for both a single-type pool and a mixed pool, the resulting `DicePoolResult` is asserted `equals()` against `DiceRoller(random = Random(seed)).rollPool(DicePool(poolBeforeRoll))` computed independently with the same seed — i.e. the ViewModel is a thin, faithful pass-through to the domain API. A further test rolls twice (changing the pool in between) and independently replays both random draws in sequence to confirm the second roll's `Random` state picks up exactly where the first left off.
- **Pool persistence / color restore (unchanged behavior, re-verified for the pool-aware state shape):** a color already in the store is restored on construction while the pool and result stay at their defaults; `selectColor` writes through to the store; a second ViewModel sharing the same store sees a previously-selected color; every `DiceColor` entry round-trips through `selectColor` correctly.

### `presentation/RollButtonLabelTest.kt` (9 tests, issue #54)
Tests for the `rollButtonLabel(pool: DicePool): String` formatter:
- Empty pool (no entries, or entries present but all zero) formats to `"Add dice to roll"`.
- A single die type formats without a `+`: `"Roll 4D6"`; a count of 1 still reads `"Roll 1D20"` (no special-cased singular wording).
- A mixed pool is ordered smallest-to-largest regardless of map insertion order: `"Roll 4D6 + 2D8"`, and a 4-type pool built in reverse insertion order still comes out `"Roll 3D4 + 1D6 + 2D12 + 1D20"`.
- All six die types populated at once produce all six terms in order.
- Zero-count entries present in the map are excluded from the label.
- A count at the cap (20) renders in full: `"Roll 20D20"`.

## Instrumented tests (`src/androidTest/`) — compile-verified only, not run on device

### `presentation/component/DiceStepperChipTest.kt` (12 tests, issue #50)
Standalone Compose tests for `DiceStepperChip`, mounted outside `DiceRollerScreen`:
- Rendering: the die label (`"D12"`) and the count (`"5"`) are both shown as text; the chip's `DiceImage` is decorative (no content description of its own — the label/buttons already convey the die type).
- Per-chip enabled state follows the design spec's count table: at count 0 the `−` button is disabled and `+` enabled; at a mid-range count both are enabled; at the cap (`DicePool.MAX_DICE_PER_TYPE`) `+` is disabled and `−` enabled.
- Callbacks: tapping `+`/`−` invokes `onIncrement`/`onDecrement`. A stateful wrapper (chip driven by `remember { mutableIntStateOf(...) }`, clamped the same way the ViewModel clamps) confirms the displayed count follows repeated taps and that tapping `+` at one-below-cap both shows the capped value and disables `+`.
- Accessibility: the two `IconButton`s carry **static** content descriptions (`"Decrease D10 count"` / `"Increase D10 count"`) that do not embed the live count; the count `Text` instead carries a matching `stateDescription` (asserted via `SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "9")`).

### `presentation/component/DiceResultDisplayTest.kt` (27 tests, issue #45/#51)
Standalone Compose tests for `DiceResultDisplay`'s face-ladder, using two fixture `DicePoolResult`s (a two-group D6+D8 result and a single-group D20 result):
- Empty state: `isPoolEmpty = true` shows the caption *"Add dice above to build your pool."*; `isPoolEmpty = false` with a null result shows *"Tap Roll to see results."* instead — and each caption is confirmed absent under the other's trigger condition. A null result never renders a group header.
- Populated state — group headers: each group's header text (e.g. `"4×D6"`, `"2×D8"`) is shown, with a matching content description (`"4 D6 dice"`); exactly one header exists per die type actually present in the result (counted via substring match on `"×D"`), and no header appears for a die type absent from the result (e.g. no `"3 D20 dice"` when the result has no D20 group).
- Populated state — rows: every distinct rolled value gets a row with content description `"Value $value, rolled $count time(s)"` (singular/plural handled); a value never rolled (D6's `5` in the fixture) produces no row; the count column renders as `"×1"`/`"×2"` etc.; a group where every die landed on the same value produces exactly one row for that value and none for any other face.
- Row ordering: verified via `boundsInRoot.top` comparisons — within the D6 group, the row for `6` sits above `4`, which sits above `3` (strictly descending by value); same check for the D8 group; and the last D6 row sits above the first D8 row (groups render in the smallest-to-largest order, not interleaved).
- Row/art pairing: each row is checked to contain **both** its own die-art test tag (`dice-row-art-$dice-$value-$color`) and its own count text as descendants of the same semantics node — i.e. it's not enough for the right art and the right count to exist somewhere on screen, they must be paired within the same row (guards against a mis-zipped rows/tallies bug).
- Total line: rendered as `"Total $sum"` with a matching content description, and confirmed to sit below every group's last row (`boundsInRoot` comparison); a single-group fixture's total matches that group's own sum.
- Color: every row's die-art test tag reflects the `selectedColor` passed in (e.g. switching from Ruby to Jade makes the Jade-tagged art appear and the old Ruby-tagged art disappear for the same die/value).

### `DiceRollerScreenTest.kt` (18 tests, issue #55, rewritten by the-craftsman in PR #61 for the new pool API)
End-to-end screen tests combining the stepper row, Roll button, and result region. Uses both a static `DiceRollerUiState` (for state-driven assertions) and a real `DiceRollerViewModel` with a seeded `Random` (for interaction-driven assertions):
- Initial state: all six stepper chips' increase buttons are visible on launch; every decrease control starts disabled (all counts at 0); the Roll button is disabled and reads *"Add dice to roll"*; the empty-pool caption is shown.
- Stepper-driven Roll button: incrementing D6 once makes the button read *"Roll 1D6"* and become enabled; incrementing D8 then D6 produces *"Roll 1D6 + 1D8"* (smallest-to-largest, independent of tap order); a state built directly with a `{D6: 4, D8: 2}` pool renders *"Roll 4D6 + 2D8"*; decrementing a die back to 0 returns the button to the disabled placeholder.
- Stepper count display: tapping `+` on a zero-count chip shows `"1"`; a second tap shows `"2"`; tapping `−` from 2 shows `"1"`; incrementing one die type leaves an untouched sibling chip's displayed count at `"0"`. The `countText(dice)` helper disambiguates a chip's count node from its five siblings by requiring it sit alongside that specific die's two labelled stepper buttons and have no click action of its own.
- Stepper bounds: a chip constructed at `MAX_DICE_PER_TYPE` has its increase control disabled; one count below the max still has it enabled; any non-zero count has its decrease control enabled.
- Rolling: tapping Roll on a non-empty pool removes the "not yet rolled" caption and shows a group header plus a total line matching `viewModel.uiState.value.result!!.total`; changing a count again after a roll clears the result and brings back the "Tap Roll to see results" caption.

### `FantasyDiceArtUiTest.kt` (7 tests, rewritten by the-craftsman in PR #61 to keep pre-existing color/artwork coverage compiling against the new pool API)
Covers the color picker's integration with the rest of the (now pool-based) screen — not per-die artwork rendering itself, which moved to the component-level tests above:
- All 12 color swatches exist; the currently-selected color's swatch is marked selected and others are not; tapping a different swatch (e.g. Indigo) selects it and deselects the previous one.
- Recoloring a rolled result: after rolling a 1×D6 pool, changing the selected color switches the result row's die-art test tag to the new color and removes the old one; the roll's total is preserved across the color change (color never clears results — re-verified end-to-end).
- Color choice persists across a pool change: selecting a color, then incrementing a die count, leaves the swatch still marked selected.
- The CC BY 4.0 attribution line (*"Dice art by Aeynit · CC BY 4.0"*) is still visible on screen.

## Open items

- **No instrumented test has actually been executed on a device or emulator.** `DiceStepperChipTest`, `DiceResultDisplayTest`, `DiceRollerScreenTest`, and `FantasyDiceArtUiTest` are confirmed to compile (`./gradlew compileDebugAndroidTestKotlin` succeeds) but their assertions — semantics matching, `boundsInRoot` ordering, click interactions — have never run against real Compose UI. Whoever next has emulator or CI access should run `./gradlew connectedAndroidTest` on `feat/multi-dice-roll/screen-integration` (or `main` post-merge) before treating this coverage as verified.

## Related docs
- [Roll Multiple Dice at Once (feature)](../features/multi-dice-roll.md)
- [Architecture: Multi-Dice Roll (Pool Rolling)](../architecture/multi-dice-roll.md)
- [Multi-Dice Roll — Design Spec](../design/multi-dice-roll.md)

## Changelog
| Date | Change |
|------|--------|
| 2026-08-08 | Initial version — test coverage notes for issues #53–#56, verified against `feat/multi-dice-roll/screen-integration` (PR #61) source and a clean local build |
