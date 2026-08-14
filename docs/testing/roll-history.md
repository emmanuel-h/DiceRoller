# Testing: Roll History

> Covers GitHub Issue #3.
> Feature doc: [Roll History](../features/roll-history.md) ·
> Architecture: [Roll History](../architecture/roll-history.md).

## Totals

| Suite | Where | Added |
|---|---|---|
| `RollHistoryCodecTest` | `src/test/` | 21 |
| `RelativeTimeTest` | `src/test/` | 10 |
| `DiceNotationTest` | `src/test/` | 7 |
| `DiceRollerViewModelTest` (history sections) | `src/test/` | 15 |
| `RollHistoryBandTest` | `src/androidTest/` | 15 |
| `DiceRollerScreenTest` (history sections) | `src/androidTest/` | 11 |

All 149 unit tests and 100 instrumented tests pass (`./gradlew test`,
`./gradlew connectedDebugAndroidTest` on a `Medium_Phone_API_36.1` emulator).

## What each suite pins down

### `RollHistoryCodecTest` — the persistence format
The codec is the only thing in the app that parses data written by a *past version* of itself, so
it is tested from both ends:

- **Grammar** — the encoded form is asserted literally (`1700000000000;6:6*1,4*2,3*1|8:7*1,2*1`),
  so the format cannot change silently.
- **Round trips** — including one built by the real `DiceRoller` at every die type's maximum
  count, rather than only by hand-written fixtures.
- **Derivation** — `total` and `poolCount` are absent from the stored form and recomputed, proven
  by decoding a literal string and checking the sum.
- **Corruption** — 11 cases (non-numeric timestamp, unknown face count, value above the die's
  maximum, value below 1, non-positive count, empty group, tally missing its count, truncated
  record, stray separator, blank/null input). Each asserts the record is *dropped*, never thrown
  on; one case mixes a corrupt record between two good ones and asserts only the bad one is lost.

> A fixture caught a real property here during development: a hand-written record whose `total`
> disagreed with its tallies failed the round trip, which is correct — the codec recomputes rather
> than trusting storage.

### `RelativeTimeTest` — boundaries, not samples
Each threshold is tested on both sides (59_999 ms is `just now`, 60_000 ms is `1 min ago`;
one second under an hour vs exactly an hour; likewise for days), plus a future timestamp reading
as `just now` rather than a negative age.

### `DiceNotationTest` — one formatter, two callers
Ordering is preserved rather than re-sorted, and one test asserts the Roll button label and a
result's notation describe the same pool identically — the drift this extraction exists to
prevent.

### `DiceRollerViewModelTest` — recording, persistence, expansion
Driven by an injected `clock` the test moves by hand, so timestamps are assertable. Covers:
recording on roll; recording *nothing* on an empty pool; newest-first ordering; the 50-record cap
(asserting both that the newest is kept and that the oldest fell off); history surviving a pool
change that clears the live result; restoration from a pre-populated store; write-through to the
store; expand/collapse; and `nowMillis` refreshing on both roll and expand.

Two tests pin the append-only rule specifically: collapsing the band leaves the entries intact,
and rolling repeatedly only ever grows the stored log (1, 2, 3, 4, 5) — it never shrinks.

### `RollHistoryBandTest` — the component
Absence with no history (even when told to expand), the collapsed header, the expanded entry
contents, `×N` only for repeats, and the header toggling in both states. One test asserts the
expanded band exposes **no** clear action, by text or by content description — the append-only
rule, pinned at the component. Five accessibility tests cover the header's announcement (singular
and plural), the entry's one-sentence description, and that the artwork itself is not separately
announced.

### `DiceRollerScreenTest` — the band in its layout
Band appearance after a real roll through a real ViewModel, count growth, expansion, the absence
of any erase control on the assembled screen, collapsing without losing entries, and the fit tests
described below.

## The fit tests, and what changed

Issue #64's original assertion — the densest typical pool fully visible at 360×640dp — is
**unchanged and still passing**; it runs without history, which is the state a fresh install is
in. The new tests bound the *with-history* case honestly, after measuring the real numbers on
device rather than assuming them:

| Viewport | History | Asserted |
|---|---|---|
| 360×640dp | none | Total visible, band absent (issue #64's guarantee, intact) |
| 360×640dp | collapsed | Every control survives; result groups visible; total scrolls |
| 360×680dp | collapsed | Everything fits, total included |
| 360×640dp | expanded | List, selector, Roll button and attribution all visible; list bottom ≤ button top |

The 640dp row is the honest one: the pre-history layout cleared that pool by only ~16dp and the
band costs ~45dp, so the total moved one short scroll away there. That is the vertical price the
inline design was chosen with, and it is asserted rather than hidden.

## Manual verification

Driven on the emulator via `adb` in addition to the automated suites: built a 5D6 + 2D8 + 1D20
pool, rolled three times, expanded the band, then force-stopped and relaunched the app. All three
entries survived the restart with correct values and totals, their timestamps had aged from
`just now` to `1 min ago`, and the pool had reset to empty as designed.

> This run predates the removal of the Clear action; the header it exercised has since lost its
> trailing button, which is covered by the automated tests above.
