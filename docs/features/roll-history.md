# Feature: Roll History

> Covers GitHub Issue #3 ("Roll history / logging"), a backlog item carried over from the
> multi-dice-roll PRD's out-of-scope list.

## Problem statement

Every roll DiceRoller produced was gone the moment the next one replaced it — and, because
changing the pool clears the current result, often sooner than that. Players who wanted to
compare a roll against the previous one, settle a "what did I actually get?" question at the
table, or check how a session had been going had no way to look back.

## Goals

- A user can see their past rolls without leaving the dice screen.
- Past rolls survive closing and reopening the app.
- The log is glanceable — what was rolled, what it came to, and roughly when.
- None of this costs the screen's controls: the pool selector and the Roll button stay exactly
  where they were and never scroll away.

## What was built

A **collapsible history band** sitting between the result and the Roll button.

```
  before                              after (collapsed)         after (expanded)
  ────────────────────────            ────────────────────      ────────────────────
  ┌────────────────────┐              ┌────────────────────┐    ┌────────────────────┐
  │ ● ● ● ● ● ● ● ● ●  │              │ ● ● ● ● ● ● ● ● ●  │    │ ● ● ● ● ● ● ● ● ●  │
  ├────────────────────┤              ├────────────────────┤    ├────────────────────┤
  │ [D4 ][D6 ][D8 ]    │              │ [D4 ][D6 ][D8 ]    │    │ [D4 ][D6 ][D8 ]    │
  │ [D10][D12][D20]    │              │ [D10][D12][D20]    │    │ [D10][D12][D20]    │
  ├────────────────────┤              ├────────────────────┤    ├────────────────────┤
  │ 5×D6               │              │ 5×D6               │    │ 5×D6               │
  │ 🎲4 ×2  🎲2  🎲1 ×2│              │ 🎲4 ×2  🎲2  🎲1 ×2│    │ 🎲4 ×2 🎲2 🎲1 ×2  │
  │           Total 34 │              │           Total 34 │    │          Total 34  │
  │                    │              ├────────────────────┤    ├────────────────────┤
  │                    │              │ ⌄ Recent (3)       │    │ ⌃ Recent (3)       │
  ├────────────────────┤              ├────────────────────┤    │ 5D6+2D8+1D20    34 │
  │ [  Roll 5D6 …    ] │              │ [  Roll 5D6 …    ] │    │ 🎲4×2 🎲2 · 🎲8 …  │
  └────────────────────┘              └────────────────────┘    │ just now           │
                                                                │ 4D6+2D8+1D20    27 │
                                                                ├────────────────────┤
                                                                │ [  Roll 5D6 …    ] │
                                                                └────────────────────┘
```

> The attribution line these sketches showed under the Roll button moved to the About sheet in
> [#66](https://github.com/emmanuel-h/DiceRoller/issues/66); the bottom bar now holds the button
> alone, and the ~24dp it freed goes to this band and the result above it. See
> [about-sheet.md](about-sheet.md).

### Behaviour

1. **Absent until earned.** With no rolls recorded, the band renders nothing at all — a first
   launch is identical to the screen before this feature existed.
2. **Collapsed by default**, showing `⌄ Recent (N)`. Tapping the header expands or collapses it.
3. **Each entry** shows the pool in dice notation with its total on the right, the individual
   faces as compact artwork below (repeats as `×N`, matching the live result's compression), and
   how long ago it happened — `just now`, `3 min ago`, `2 h ago`, `5 d ago`.
4. **Newest first**, capped at **50** records; older rolls fall off the end.
5. **Persisted.** The log survives closing and reopening the app. The *pool* still does not —
   every launch starts empty, unchanged from before.
6. **Append-only.** There is deliberately no way to clear the log from the UI. Collapsing the
   band hides the entries; it never discards them. The 50-record cap is the only thing that ever
   removes a record, which also makes it the app's storage bound.
7. **Never rewritten.** Changing the pool clears the live result, as always, but never the log.
8. **Recolouring recolours history too.** Records store no colour of their own; they render in
   whatever colour is selected now. This follows the app's existing rule that colour is a
   display-time choice — picking one never even clears the live result.

### Accessibility

- The header announces as `"Recent rolls, 3 rolls, collapsed"`. It is the band's only control, so
  the whole row has exactly one meaning: open or close the list.
- Each entry is one merged node reading as a single sentence:
  `"4D6 + 2D8, total 26, just now. D6: 6, 4 2 times, 3. D8: 7, 2."` — rather than making a screen
  reader step through twenty separate numerals to hear one past roll.

## The cost, measured

The inline band was chosen over a bottom sheet or a separate screen knowing it permanently
consumes vertical space. That cost was measured rather than assumed:

- At **360×640dp** — the shortest viewport the fit tests bound — the pre-history layout cleared
  its densest *typical* pool (4×D6 + 4×D8 + 4×D20, three distinct values each) by only ~16dp.
- A collapsed band costs ~45dp: a 40dp header (kept at 40dp so it stays a reachable touch
  target), a 4dp gap, and a 1dp divider.

So at that pool on that viewport, the total line is now one short scroll away instead of visible
at rest. From ~680dp up — a small phone rather than the harshest bound — everything fits again,
and every *control* (selector, history header, Roll button, and — since #66 — the About button)
stays visible at every supported height. See `DiceRollerScreenTest`'s fit section, which asserts exactly this boundary.

## Out of scope

- Clearing, deleting or editing entries — the log is append-only by design.
- Re-rolling a past roll, or restoring its pool into the selector.
- Naming, tagging, filtering or searching entries.
- Exporting or sharing the log.
- Grouping by day, or absolute timestamps.
- Live-ticking timestamps: the reference time refreshes when a roll happens and when the band is
  opened, not every second.
