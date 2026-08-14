# Architecture: Custom dice

Implements [the custom-dice PRD](../features/custom-dice.md) (issue #4).

## The pivot: `Dice` enum → `DieType` sealed interface

`Dice` was an `enum` used as the pool's `Map` key, and `DiceColor.drawableFor()` is an exhaustive
`when` over its six entries — the invariant that stops a preset shipping without its 12 renders.
A custom die has no artwork, so it cannot be an enum entry without destroying that check.

```kotlin
sealed interface DieType {
    val faces: Int
    val label: String get() = "D$faces"          // "D6" for a preset, "D7" for a custom die
    companion object {
        val FACES_RANGE: IntRange = 2..1000
        fun ofFaces(faces: Int): DieType?         // preset if one matches, else CustomDie, else null
    }
}

enum class Dice(override val faces: Int) : DieType { D4(4), … D20(20) }
data class CustomDie(override val faces: Int) : DieType
```

`Dice` **keeps its name**, so `drawableFor(dice: Dice)` stays compiler-checked and every existing
`Dice.D6` call site compiles untouched. The retype was therefore small: `Dice` → `DieType` on the
pool key and `DiceGroupResult.dice`, and `dice.name` → `dice.label` in the handful of
accessibility strings and test tags that used it. `label` is defined to equal `name` for presets,
so not one user-visible string changed.

### The one invariant everything leans on

**No two `DieType`s share a face count.** `CustomDie`'s `init` rejects a preset's face count, and
`DieType.ofFaces` resolves a bare number to the preset when there is one. Without this, a
`CustomDie(6)` would be a second pool key rendering as `"D6"`, and the pool could hold
`"1D6 + 1D6"` meaning two different things. Two doors, one rule, enforced at both.

## What came for free

Two pre-existing decisions turned out to already accommodate this:

- **`DicePool.entries` sorts by `faces`.** A `D7` slots between the D6 and the D8 with no code
  change, and `"4D6 + 1D7 + 2D8"` notation and the result ladder's ordering both fall out.
- **`RollHistoryCodec` keys groups by face count, not by enum name.** The stored format is
  *unchanged*: existing logs keep decoding, and a custom-die roll persists with nothing new
  written. The decoder swapped a `diceByFaces[faces]` lookup for `DieType.ofFaces(faces)`, which
  is also why a past roll survives the user deleting the definition — the log records face counts,
  not references.

## New pieces

| Layer | Type | Responsibility |
|---|---|---|
| `domain/` | `DieType`, `CustomDie` | The die model above |
| `presentation/` | `CustomDiceStore` (+ `InMemoryCustomDiceStore`) | Persists definitions; owns `normalizeCustomDice` (dedupe, sort ascending, cap at `MAX_CUSTOM_DICE`) |
| `presentation/` | `validateCustomFaces` → `CustomFacesResult` | Pure validation of the typed face count |
| `data/` | `CustomDiceCodec`, `DataStoreCustomDiceStore` | `"3,7,100"` under one key in the shared `diceDataStore` |
| `component/` | `CustomDieBadge` | The swatch-coloured stand-in for missing artwork |
| `component/` | `AddDiceChip`, `CustomDieCreatorDialog` | Creating a die |

`DataStoreCustomDiceStore` reuses `Context.diceDataStore` — a second `preferencesDataStore`
delegate for the same file name throws at runtime, so it must.

## Where the branch on "which kind of die" lives

Exactly one place: **`DiceImage`**. It takes a `DieType` and dispatches — `is Dice` → the pack
drawable, `is CustomDie` → `CustomDieBadge`. Every caller (stepper chip, result ladder, history
face line) passes a `DieType` and is indifferent. `DiceStepperChip` likewise serves both, with a
nullable `onRemove` that presets pass as null; that nullability is the only thing making the six
presets undeletable, rather than a second chip composable.

## State and the pool-reconciliation rule

`DiceRollerUiState` gains `customDice`, `isCustomDieCreatorVisible`, `removedCustomDie`, and two
derived values: `dieTypes` (`Dice.entries + customDice`, the selector's cell order) and
`canAddCustomDie`.

The ViewModel collects `customDiceStore.customDice` as the single source of truth, and every
emission runs `withCustomDice`, which reconciles the pool: a zero entry for each newly-defined
die, no key at all for one that is gone, existing counts preserved. That keeps the selector's
invariant — *every chip on screen has a pool entry* — true however the definitions changed,
including the restore-from-storage case where they go from none to three at once.

### When the result is cleared

The existing rule is "a change to what Roll would produce clears the result", and custom dice
follow it rather than inventing a second one:

- **adding** a die → result kept (it enters at count 0, so the pool is unchanged)
- **removing** a die → result cleared *only if its count was > 0*
- **undoing** a removal → result stays cleared. The pool is back, but a result is only ever
  produced by pressing Roll; reviving one would show an outcome the user did not ask for.

### Undo

`removeCustomDie` stores the removed die in state (so the screen can show a snackbar) and its
count in a private field. `undoRemoveCustomDie` restores both; `dismissRemovedCustomDie` retires
the offer when the snackbar times out — after which Undo is correctly a no-op. The undo exists
because the `×` badge sits on the edge of the increment half, so a mistap is likely enough that a
definition should not vanish irrecoverably.

## Related docs

- [Feature PRD](../features/custom-dice.md)
- [Design](../design/custom-dice.md)
- [Testing](../testing/custom-dice.md)
