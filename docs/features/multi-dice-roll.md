## PRD: Roll Multiple Dice at Once

### Problem statement
DiceRoller currently lets a user select and roll only one die type at a time. Tabletop and board-game rolls frequently combine multiple dice, including mixed types (e.g., "4d6 for stats, 2d8 for damage"). Today, users must roll each die type separately and track results by hand, which is slow and error-prone. This feature lets users build a mixed pool of dice — by type and quantity — and roll all of them in a single action, seeing a per-value tally for each die type.

### Goals & success metrics
- A user can add any combination of the six die types (D4, D6, D8, D10, D12, D20) to a single roll (e.g., 4×D6 + 2×D8) and get every result from one tap.
- The current pool composition is visible at a glance, per die type, at all times.
- Every roll shows, per die type, a tally of how many dice landed on each rolled value (a face ladder), so users can read off their own success thresholds without the app imposing one; a total sum is shown as a secondary reference only.
- The existing single-die flow (pick one type, roll, see one result) keeps working as the special case of a pool containing exactly one die.

### User stories
- As a player, I want to add multiple dice of different types to one roll, so that I can resolve compound rolls like "4d6 + 2d8" in a single action.
- As a player running systems like Warhammer or narrative RPGs, I want to see how many of my dice landed on each value, so that I can apply my own threshold(s) (e.g., "how many rolled 4+", or "2 dice = critical failure, 4 dice = simple failure") without the app hardcoding a single success rule.
- As a player, I want the roll button to state exactly what it will roll, so that I don't trigger the wrong pool by mistake.
- As a player, I want to quickly increase or decrease how many of each die type I'm rolling, so that adjusting my pool is fast.

### Functional requirements
1. Each die chip (D4, D6, D8, D10, D12, D20) gets its own stepper (`− count +`); all six chips and their counts are always visible.
2. Count 0 means that die type is excluded from the pool; count > 0 means that many dice of that type will be rolled.
3. Decrementing stops at 0. Incrementing stops at a maximum of **20 per die type** (assumption — keeps the results display and the roll readable; revisit if real usage needs more).
4. Changing any count (+ or −) clears the currently displayed results, consistent with the existing rule that changing what will be rolled invalidates the previous roll. Changing color never clears results (unchanged behavior).
5. The Roll button's label reflects the pool, e.g. "Roll 4D6 + 2D8", ordered smallest-to-largest die. When the pool is empty (all counts 0), the button is disabled.
6. Rolling produces one independent random result per die in the pool, each uniformly random over that die's face count.
7. Results are grouped by die type, in the same smallest-to-largest order as the Roll button label. Each group is headed by its pool size and type (e.g., "4×D6").
8. Within a group, show one row per distinct value that was actually rolled, sorted descending (highest value first); values rolled zero times are omitted entirely — a group is only as tall as the number of distinct values it hit.
9. Each row shows the die art for that value on the left and the count as a numeral (e.g., "×2") in a separate column on the right — not repeated icons/pips/bars — so row width stays constant whether the count is ×1 or ×20.
10. Individual dice are no longer rendered one-tile-per-die; the per-value tally rows described in FR 7-9 are the entire result display.
11. A total sum across all dice is still shown, demoted to a single quiet secondary line below all groups — not the headline.
12. The selected color applies to every die in the pool and every result shown, same as today's single global color choice.
13. This replaces the single-die selection model; there is no separate "single die" mode.

### Assumptions (decided to avoid re-asking)
- **Per-type cap:** 20 dice per die type.
- **Pool persistence:** the pool does **not** persist across app restarts — each launch starts with an empty pool (all counts 0), and the Roll button is disabled until the user adds at least one die. (Color persistence via DataStore is unaffected.) This is a small behavior change from today, where a die was always pre-selected.
- **Color scope:** stays a single global choice for the whole pool, not per-die-type.

### Out of scope
- Reordering die-type groups or filtering which groups appear (order is always smallest-to-largest; all groups with count > 0 are always shown).
- Configurable success threshold / success counting (e.g., "count dice ≥ 4") — the user reads their own thresholds off the face ladder; the app does not compute or highlight successes.
- Roll history or logging past rolls.
- Saving/naming pool presets for reuse across sessions.
- Per-die-type color customization.
- Roll animations, sound effects, or haptics.
- Persisting the pool across app restarts.
- Custom (non-standard) dice or per-die modifiers (e.g., "+2" to a result).
- Notation-entry input (typing "4d6+2d8") as an alternative to the stepper — the stepper chip is the only input mechanism for this feature.

### Open questions
None outstanding.

## Approved layout sketch

Face ladder, filtered to values actually rolled:

```
┌──────────────────────────────┐
│ ● ● ● ● ● ● ● ● ● ● ● ●      │   color swatch row (unchanged)
├──────────────────────────────┤
│ ┌────┐ ┌────┐ ┌────┐ ┌────┐  │
│ │ D4 │ │ D6 │ │ D8 │ │D10 │  │   stepper chips
│ │−0 +│ │−4 +│ │−2 +│ │−0 +│  │
│ └────┘ └────┘ └────┘ └────┘  │
├──────────────────────────────┤
│ 4×D6                         │   group header = die type + pool size
│    🎲6        ×1             │
│    🎲4        ×2             │
│    🎲3        ×1             │
│                              │
│ 2×D8                         │
│    🎲7        ×1             │
│    🎲2        ×1             │
│                              │
│ total 26                     │   quiet secondary line
├──────────────────────────────┤
│    [  Roll 4D6 + 2D8  ]      │
└──────────────────────────────┘
```

## Design
Not yet written. Will land at `docs/design/main-screen.md` (update) or a new `docs/design/multi-dice-roll.md`, covering the stepper chip layout, the per-die-type result groups, and the demoted total line.

## Architecture
Not yet written. Will land at `docs/architecture/multi-dice-roll.md`, covering the pool data model (die type → count), the tally computation (grouping rolled values per die type), and the ViewModel/UiState changes needed to replace single-die selection with a pool.

## Testing
Not yet written. Will land at `docs/testing/multi-dice-roll.md`.

### Related docs
- [Dice Selection and Roll (superseded single-die flow)](dice-selection-and-roll.md)
- [Fantasy Dices Pack art with color picker](fantasy-dice-art.md)
- [Architecture: Dice Rolling](../architecture/dice-rolling.md)
- [Main Screen Design Spec](../design/main-screen.md)

## Changelog
| Date | Change |
|------|--------|
| 2026-08-08 | Initial version — approved PRD for rolling multiple dice at once |
