# Architecture: Roll History

> Covers GitHub Issue #3.
> Feature doc: [Roll History](../features/roll-history.md).

## Overview

Roll history adds one domain record, one store interface with two implementations, two pure
formatters, and one Compose band. It reuses the layering the colour-persistence feature already
established, so there is no new pattern to learn: a `*Store` interface in `presentation/`, a
DataStore implementation in `data/`, an in-memory implementation for tests and previews, and
constructor injection into the ViewModel.

```
domain/          RollRecord ──────────────────────┐
                                                  │
presentation/    RollHistoryStore (interface)     │  DiceRollerViewModel
                 InMemoryRollHistoryStore ────────┤    - collects history → UiState
                 DiceNotation, RelativeTime       │    - records on rollDice()
                 component/RollHistoryBand        │    - toggle expanded
                                                  │
data/            DataStoreRollHistoryStore ───────┘
                 RollHistoryCodec
                 DiceDataStore (shared delegate)
```

## Classes and responsibilities

### `domain/RollRecord`
`(result: DicePoolResult, rolledAtMillis: Long)`. Deliberately holds nothing else — notably **no
colour**, because colour is a display-time choice that already never affects a result. Pure
Kotlin, no Android imports, consistent with the rest of `domain/`.

### `presentation/RollHistoryStore`
```kotlin
interface RollHistoryStore {
    val history: Flow<List<RollRecord>>   // newest first, at most MAX_HISTORY_RECORDS
    suspend fun record(record: RollRecord)
}
```

There is **no `clear`**: the log is append-only and the UI offers no way to erase it, so the
capability simply does not exist at any layer rather than existing unused.
`MAX_HISTORY_RECORDS = 50` is a top-level constant and the **implementations** own the trimming,
so no caller has to remember to cap. With nothing able to clear the log, that cap is also the
app's only storage bound. `InMemoryRollHistoryStore` serves unit tests and previews,
exactly as `InMemoryDiceColorStore` does.

### `data/DataStoreRollHistoryStore`
Stores the log as a single `RollHistoryCodec`-encoded string under the `roll_history` key,
re-decoding and re-encoding on each write to prepend and trim atomically inside `dataStore.edit`.
The key is written but never removed.

### `data/DiceDataStore`
`preferencesDataStore` throws at runtime if two delegates are created for the same file, so the
delegate that used to live privately inside `DataStoreDiceColorStore` moved to its own file as an
`internal val Context.diceDataStore`. Both stores now share the one `dice_settings` file and own
only their own keys within it. **This is the constraint to remember when adding a third store.**

### `data/RollHistoryCodec`
A hand-rolled format, chosen over adding kotlinx.serialization because the project ships no
serialization library and this is its only persisted aggregate:

```
history := record ( "\n" record )*
record  := millis ";" ( group ( "|" group )* )?
group   := faces ":" tally ( "," tally )*
tally   := value "*" count
```
e.g. `1700000000000;6:6*1,4*2,3*1|8:7*1,2*1`.

Two rules make it safe:

- **Nothing derivable is stored.** `poolCount` is the sum of its tally counts and `total` is the
  sum of `value × count` across groups, both recomputed on decode — so the stored form can never
  disagree with itself.
- **Decoding never throws.** A record that fails to parse — truncated write, hand-edited file, a
  face count or value this build considers impossible — is dropped individually. One bad record
  costs that record, not the whole log.

### `presentation/DiceNotation`
`poolNotation(entries)` and `DicePoolResult.notation()`. Extracted so the Roll button label and a
history entry cannot drift into describing the same pool differently; `rollButtonLabel` now
delegates to it.

### `presentation/RelativeTime`
`relativeTimeLabel(rolledAtMillis, nowMillis)` — pure, so it is unit-tested off-device. Coarsens
as time recedes (`just now` → `min` → `h` → `d`), and treats a future timestamp (clock change
between launches) as `just now` rather than as a negative age.

### `presentation/component/RollHistoryBand`
Renders nothing when the history is empty. Collapsed it is a header row; expanded it adds a
`LazyColumn` with `Modifier.weight(1f)`. The header carries no trailing action — it is a single
control whose only effect is expand/collapse. Lazy is safe *here specifically* because the caller
bounds the band's height when expanded — the same constraint that made `LazyColumn` the wrong
choice inside `DiceResultDisplay`.

## Data flow

```
user taps Roll
   → DiceRollerViewModel.rollDice()
       → guard: state.canRoll (empty pool records nothing)
       → diceRoller.rollPool(DicePool(pool))            [synchronous]
       → _uiState.update { result, nowMillis = clock() } [synchronous — UI never waits on disk]
       → viewModelScope.launch { historyStore.record(...) }
           → DataStore edit: decode, prepend, take(50), encode
           → store's Flow re-emits
               → init-block collector updates UiState.history
```

The store is the **single source of truth** for the log: the ViewModel collects its flow rather
than reading it once, so a recorded roll lands in state through one path instead of being applied
twice.

## State additions

`DiceRollerUiState` gains `history`, `isHistoryExpanded`, `nowMillis`, and a `hasHistory`
convenience. `DiceRollerViewModel` exposes `toggleHistoryExpanded()` and nothing else for the
log — there is no `clearHistory`. Expansion is session state and deliberately **not** persisted —
a log worth keeping is not the same as a drawer worth reopening. `nowMillis` refreshes on roll and on expand rather
than ticking, which is why the labels are coarse: a coarse label stops being wrong slowly.

## Injection

`DiceRollerViewModel` gains two constructor parameters, both defaulted so existing call sites and
tests are unaffected:

- `historyStore: RollHistoryStore = InMemoryRollHistoryStore()`
- `clock: () -> Long = System::currentTimeMillis` — injected for the same reason `Random` is:
  deterministic timestamps in tests, no mocking framework.

`DiceRollerViewModel.factory(context)` wires the DataStore-backed store for the real app.

## Layout consequence

`DiceRollerScreen`'s content column now nests the result and the history band inside one
weighted sub-column with a 4dp gap, rather than making the band a fifth sibling with the outer
column's 12dp gap. Collapsed, the band wraps to its header; expanded, both children take
`weight(1f)` and split the flexible space, each scrolling internally. The Roll button stays in
`Scaffold`'s `bottomBar` and cannot be pushed off screen in either state. The measured vertical
cost is documented in the [feature doc](../features/roll-history.md#the-cost-measured).
