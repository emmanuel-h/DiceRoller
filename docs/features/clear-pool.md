# Feature: the pool empties — by ✕, and after every roll

Closes [#67](https://github.com/emmanuel-h/DiceRoller/issues/67), plus the follow-up decision that
**a roll ends a run**: the pool is zeroed once it has been rolled, so the next run starts from a
clean selector.

## What changed

The pool could only ever be torn down one die at a time: `DiceStepperChip` decrements by one per
tap on its left half, so `3D6 + 2D8 + 1D20` back to empty cost six taps across three chips. A **✕
button now sits at the leading end of the roll bar**, beside the Roll button, and puts every count
back to 0 at once — dropping the result with them.

It is **hidden while the pool is empty**, so the Roll button spans the bar exactly as it did
before the first die is queued.

```
  before                          after — pool empty
  ────────────────────────        ────────────────────────
  ┌────────────────────┐          ┌────────────────────┐
  │ ●●●●●●●●●● →   (ⓘ) │          │ ●●●●●●●●●● →   (ⓘ) │
  │ [D4 ] [D6 ] [D8 ]  │          │ [D4 ] [D6 ] [D8 ]  │
  │ [D10] [D12] [D20]  │          │ [D10] [D12] [D20]  │
  │ [D7 ] [ +  ]       │          │ [D7 ] [ +  ]       │
  │  ⚄ 6  ⚄ 4×2        │          │  Add dice above to │
  │        Total 40    │          │  build your pool.  │
  │ ▸ Recent (3)       │          │                    │
  ├────────────────────┤          ├────────────────────┤
  │ [ Roll 4D6 + 2D8 ] │          │ [ Add dice to roll]│
  └────────────────────┘          └────────────────────┘

  after — pool non-empty          after — the ✕ is tapped
  ────────────────────────        ────────────────────────
  ┌────────────────────┐          ┌────────────────────┐
  │ ●●●●●●●●●● →   (ⓘ) │          │ ●●●●●●●●●● →   (ⓘ) │
  │ [D4 ] [D6 ] [D8 ]  │          │ [D4 ] [D6 ] [D8 ]  │
  │ [D10] [D12] [D20]  │          │ [D10] [D12] [D20]  │
  │ [D7 ] [ +  ]       │          │ [D7 ] [ +  ]       │
  │  ⚄ 6  ⚄ 4×2        │          │  Add dice above to │
  │        Total 40    │          │  build your pool.  │
  │ ▸ Recent (3)       │          │ ▸ Recent (3)       │
  ├────────────────────┤          ├────────────────────┤
  │ (✕) [Roll 4D6+2D8] │          │ [ Add dice to roll]│
  └────────────────────┘          └────────────────────┘

  every chip back to 0 · result gone · history untouched
```

## A roll ends the run

`rollDice` zeroes every count on its way out. Rolling therefore returns the bar to exactly the
state above — ✕ gone, Roll disabled and reading `Add dice to roll` — while the ladder of the roll
just made stays on screen.

```
  just rolled                     after the first chip tap
  ────────────────────────        ────────────────────────
  ┌────────────────────┐          ┌────────────────────┐
  │ [D4 0][D6 0][D8 0] │          │ [D4 0][D6 0][D8 2] │
  │ [D10 ][D12 ][D20 ] │          │ [D10 ][D12 ][D20 ] │
  │ [ +  ]             │          │ [ +  ]             │
  │ 4×D6  ⬢5×2 ⬢4 ⬢3   │          │                    │
  │ 2×D8  ⬢8 ⬢2        │          │  Tap Roll to see   │
  │        Total 31    │          │  results.          │
  │ ▸ Recent (1)       │          │ ▸ Recent (1)       │
  ├────────────────────┤          ├────────────────────┤
  │ [ Add dice to roll]│          │ (✕) [ Roll 2D8    ]│
  └────────────────────┘          └────────────────────┘
```

This is the **one pool change that does not clear the result** — the result *is* what that change
produced. It survives until the user queues the next pool, where `updateCount`'s ordinary rule
takes it away.

What it costs, accepted deliberately: **there is no reroll.** Tapping Roll twice does nothing the
second time, because there is no longer a pool to roll; the same dice have to be queued again.

What it buys, beyond the fresh start itself, is an invariant worth knowing when touching this
layer: **a non-null result implies an empty pool.** Every route back to a non-empty pool runs
through `incrementCount`, which clears the result. Two clearing clauses are consequently no longer
reachable — `clearPool`'s and `removeCustomDie`'s — and are kept anyway, because they state the
rule rather than merely implement a reachable branch.

The ✕'s job narrows accordingly: it is the control for **abandoning a pool that has not been
rolled**. Issue #67's "clearing also drops the current result" clause is now satisfied for free,
since no result is ever on screen next to a pool the ✕ could clear.

## Why the roll bar

The screen has been a fixed set of bands with no top app bar since [#64](../features/multi-dice-roll.md),
and custom dice ([#4](custom-dice.md)) spent 106dp of what little slack was left. So the placement
question was really "which band's height is already set by something else":

- The **roll bar** is set by the Roll button. A 40dp icon button centred beside it is free.
- The **swatch row** is set by its 44dp touch targets — the [#66](about-sheet.md) precedent, and
  the runner-up here. Rejected because it is a band about *color*, and it would put the control
  that empties the pool as far from the dice as the screen allows.

Sharing the roll bar means the two actions that operate on the pool as a whole — roll it, empty it
— sit together. The risk that buys is the one the issue names: it must not be mistakable for Roll.
Four things separate them, and they compound:

| | Clear | Roll |
|---|---|---|
| Emphasis | Outlined | Filled |
| Content | A glyph | A sentence naming the pool |
| Width | 40dp | All the rest |
| At an empty pool | Absent | Present, disabled, `Add dice to roll` |

## Hidden, not disabled

The issue allowed either. Hidden wins on two counts: a control that is present but inert still
reads as an offer, and hiding it lets the Roll button go back to spanning the full bar rather than
sitting permanently indented behind a dead ✕. The cost is that the bar's contents shift when the
first die is queued — accepted, because that shift only ever happens in the direction the user
just asked for.

## No confirmation

Deliberately none. Rebuilding a pool costs a handful of taps on chips that are already on screen
and never left it, which is cheaper than a dialog on every use. This is the one place the feature
differs from removing a *custom die*, which does offer an undo — because that destroys a
definition the user typed, not a count they can re-tap.

## What clearing does and does not touch

| | Clearing the pool |
|---|---|
| Every count | → 0 |
| The current result | Cleared — the pool it described is gone, and `canRoll` is now false. (Unreachable in practice; see above) |
| Custom die *definitions* | Untouched; their chips stay, at 0 |
| The roll log | Untouched — it records what *was* rolled |
| The selected color | Untouched |
| An already-empty pool | No-op; the state object is not even replaced |

Zeroing every key rather than rebuilding the map from `Dice.entries` is what keeps custom dice in
the pool at 0 instead of dropping out of it — the selector's invariant is that **every visible chip
has a pool entry**.

## Structure

| Layer | Type | Responsibility |
|---|---|---|
| `presentation/component` | `ClearPoolButton` | The outlined ✕; owns `CLEAR_POOL_BUTTON_TAG` and its content description |
| `presentation` | `DiceRollerViewModel.clearPool` | Zeroes every count and clears the result, in one state update |
| `presentation` | `DiceRollerViewModel.rollDice` | Zeroes every count too — but keeps the result it just produced. Deliberately does *not* call `clearPool`, for exactly that reason |
| `MainActivity` | `RollBar` | Became a `Row`; renders the button only while `canRoll` |
| `res/drawable` | `ic_close.xml` | Hand-drawn, like `ic_casino`, `ic_expand_more` and `ic_settings` |

No new UI state: `DiceRollerUiState.canRoll` already meant "the pool has something in it", which is
exactly the button's visibility condition.

## Vertical cost

**None.** The roll bar's height is the Roll button's height plus its 12dp padding, and the icon
button is shorter than that. The guarantee that has held since #64 — *every control fits at
360×640dp* — is unchanged, with the ✕ now among the controls asserted at that size.

The horizontal cost is real but small: the Roll button loses 52dp (the button plus its gap) to
label its pool in. At 360dp wide that leaves it ~276dp for text like `Roll 4D6 + 4D8 + 4D20`,
which fits on one line.

## Out of scope

- Undo, or a confirmation dialog — see above.
- Clearing the *history*; the log stays append-only by design.
- Clearing custom die definitions. Their `×` badges already do that, with an undo.
- A "roll again" shortcut carrying the last pool forward. It was weighed against the fresh start
  and lost: a button naming a pool no chip shows is a contradiction to hold on screen.

## Related docs

- [Testing: clearing the pool](../testing/clear-pool.md)
- [Custom dice](custom-dice.md) — the definitions clearing deliberately leaves alone
- [About sheet](about-sheet.md) — the same free-band argument, one band up
- [Multi-dice roll](multi-dice-roll.md) — the stepper chips this is the bulk counterpart to
