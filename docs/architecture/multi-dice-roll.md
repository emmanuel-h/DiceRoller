# Architecture: Multi-Dice Roll (Pool Rolling)

> Covers GitHub Issues #46 and #47.
> Supersedes [Architecture: Dice Rolling](dice-rolling.md) for the pool-rolling model. See that doc's own pointer note for the single-die history it still documents.

## Overview

This feature replaces the single-die selection model with a mixed dice **pool**: any combination of the six die types (D4–D20), each with its own count, rolled together in one action. The work is split across two issues:

- **Issue #46** — domain layer: the pool data model and the pool-rolling contract (`DiceRoller.rollPool`).
- **Issue #47** — presentation layer: reshaping `DiceRollerUiState`/`DiceRollerViewModel` to be pool-aware, replacing single-die selection.

Related feature doc: [Roll Multiple Dice at Once](../features/multi-dice-roll.md).

## Domain pool model and pool-rolling contract (issue #46)

### New classes & responsibilities
- `DicePool` — validated die-type→count map (domain layer). Guarantees all counts are within the allowed range (0–20) and exposes the non-zero entries in smallest-to-largest die order.
- `DicePoolResult` — the outcome of rolling a `DicePool`: an ordered list of `DiceGroupResult` plus the total sum across all dice.
- `DiceGroupResult` — one die type's contribution: the `Dice` type, the pool count for that type, and an ordered list of `ValueTally` (descending by value, zero-count values omitted).
- `ValueTally` — a single `(value, count)` pair representing how many dice of a group landed on that value.

### Data flow
```
UI event (stepper +/-) → ViewModel updates pool → ViewModel calls DiceRoller.rollPool(pool)
DiceRoller.rollPool(pool: DicePool): DicePoolResult
  → for each (dice, count) in pool, roll `count` times via the existing DiceRoller.roll(dice) using the same injected Random
  → group per die type, tally values, sort groups smallest→largest, sort tallies descending, sum total
  → ViewModel stores DicePoolResult in UiState.result
```

### Key design decisions
- `DiceRoller` gains a new `rollPool(pool: DicePool): DicePoolResult` method. The existing `roll(dice: Dice): Int` is untouched and is reused internally by `rollPool`, preserving the seeded-determinism testing pattern already established for `DiceRoller`.
- Ordering (die types smallest-to-largest by `dice.faces`, values descending within a group) and zero-count exclusion are both computed inside `rollPool`, so the presentation layer never sorts or filters — it only renders what it's given.
- No UseCase/Repository layer is added for this feature, consistent with the existing ADR in [Architecture: Dice Rolling](dice-rolling.md) (this app deliberately skips that layer for dice rolling — it's pure, synchronous, in-memory domain logic).

### Risks & mitigations
- Risk: pool validation (0–20 clamp) could be duplicated between `DicePool` construction and the stepper UI's own clamp logic. Mitigation: `DicePool` is the single source of truth for the valid range; the stepper UI (issue #50) should read the same constant rather than hardcoding 20.
- Risk: an all-zero pool passed to `rollPool` — mitigation: `DicePool` should make an empty pool either unconstructable or `rollPool` should be a defensive no-op returning an empty `DicePoolResult`; the UI already prevents this via the disabled Roll button, but the domain layer should not crash if called anyway.

## Presentation: pool-aware UiState/ViewModel (issue #47)

### New classes & responsibilities
- `DiceRollerUiState` reshaped: `pool: Map<Dice, Int>` (all six `Dice.entries` always present as keys, default 0), `selectedColor: DiceColor` (unchanged), `result: DicePoolRollResult?` (structured tally, type owned by issue #46's `DicePoolResult`), plus a derived `canRoll` getter (true iff any pool value > 0). `selectedDice` and the old `result: Int?` are removed.
- `DiceRollerViewModel.selectDice(dice)` is replaced by `incrementCount(dice)` / `decrementCount(dice)`, clamped to [0, 20], idempotent (no-op, no state copy) at the floor/cap — mirroring the existing "re-select same die = no-op" pattern so the "count change clears result" rule stays exact.

### Data flow
```
UI event (+/- tap) → ViewModel.incrementCount(dice) / decrementCount(dice)
  → updates pool map, clamped [0,20]
  → if count actually changed, clears result to null (mirrors old selectDice's clear-on-change rule)
  → if count unchanged (clamp no-op), state is untouched (no recomposition)
UI event (Roll tap) → ViewModel.rollDice()
  → calls DiceRoller.rollPool(DicePool(state.pool)) → DicePoolResult
  → updates state.result
UI event (color tap) → ViewModel.selectColor(color) — unchanged, still persists via DiceColorStore, still never clears result
```

### Key design decisions
- `selectColor` and DataStore-backed color persistence are untouched; no persistence is added for the pool — `DiceRollerUiState()` defaults to an all-zero pool, no pre-selected die (matches the PRD's "empty pool on launch" assumption).
- Reaffirms the project's existing "no UseCase/Repository layer" ADR from [Architecture: Dice Rolling](dice-rolling.md) — pool rolling stays a direct ViewModel → domain `DiceRoller` call, no new layer introduced.
- This plan only commits to input/output *shapes* for the domain pool-rolling contract; issue #46 owns the concrete type names (`DicePool`, `DicePoolResult`, etc.) — this doc references them by name for consistency but the source of truth for their exact fields is the [issue #46 section above](#domain-pool-model-and-pool-rolling-contract-issue-46).

### Risks & mitigations
- Risk: `Map<Dice, Int>` could silently drop a key if constructed incorrectly (e.g., via a `filter` that removes zero-count entries) — mitigation: always initialize from `Dice.entries.associateWith { 0 }` and update via `.toMutableMap()` / `copy()` on the full map, never rebuild it from a subset.
- Risk: the 20-per-type cap constant could be duplicated between the ViewModel's clamp logic and `DicePool`'s validation (issue #46) — mitigation: both should reference a single shared constant (e.g. `Dice` companion or a shared `MAX_DICE_PER_TYPE` in the domain layer) rather than each hardcoding `20`.
- Risk: rolling with an empty pool if a caller bypasses the disabled Roll button (e.g. programmatically) — mitigation: `rollDice()` should be a defensive no-op when `!canRoll`, matching `DiceRoller.rollPool`'s own defensive no-op for an empty pool (issue #46).

## Related docs

- [Roll Multiple Dice at Once (feature)](../features/multi-dice-roll.md)
- [Architecture: Dice Rolling (superseded single-die model)](dice-rolling.md)

## Changelog

| Date | Change |
|---|---|
| 2026-08-08 | Initial version — architecture plans for issues #46 (domain pool model) and #47 (pool-aware UiState/ViewModel) |
