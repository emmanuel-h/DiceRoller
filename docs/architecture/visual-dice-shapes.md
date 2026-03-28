# Visual Dice Shapes — Architecture

## Overview

This document describes the data model layer introduced for the Visual Dice feature. Three new files are added under `fr.mandarine.diceroller.presentation.model`. The changes are additive — no existing files are modified.

## New files

### `DiceShape.kt`

A sealed class where each subclass represents one die type. Subclasses are declared as `data object` so they carry no mutable state.

| Subclass | `vertexCount` | `rotationDegrees` |
|---|---|---|
| `Triangle` | 3 | 0f |
| `Square` | 4 | 45f |
| `Pentagon` | 5 | 0f |
| `Octagon` | 8 | 22.5f |
| `Icosahedron` | 3 | 0f |

A companion `fromDice(dice: Dice): DiceShape` factory centralizes the `Dice` → `DiceShape` mapping so callers never need a `when` expression outside this class.

**Why sealed class over enum.** Enums cannot carry instance-level properties with different values per entry in a type-safe way. A sealed class allows each subclass to expose shape-specific properties (e.g., `rotationDegrees`, and any future properties such as inner-radius ratio for star rendering) without forcing all subclasses to carry irrelevant fields.

### `PipPosition.kt`

A data class holding `x: Float` and `y: Float` in unit-square coordinates (range `0f..1f`). Coordinates are relative to the bounding box of the die face so the rendering layer can scale them to any canvas size by multiplying by the target dimension.

Keeping coordinates in unit space makes pip definitions independent of display density, composable size, or preview dimensions.

### `D6PipLayout.kt`

A top-level object exposing a single `val positions: Map<Int, List<PipPosition>>` that maps each result value (1–6) to the standard pip arrangement for that face. The positions follow the conventional physical D6 layout.

## Data flow

```
Dice (domain)
  └─ DiceShape.fromDice(dice)       ← presentation model
       └─ DiceShape subclass
            ├─ vertexCount          → polygon path builder
            └─ rotationDegrees      → polygon path builder

RollResult (Int)
  └─ if selectedDice == D6
       └─ D6PipLayout.positions[result]   → list of PipPosition
            └─ PipPosition.(x, y) * size  → pip canvas coordinates
```

## Constraints

- No Compose imports in any of these three files. They are plain Kotlin data models, testable without an Android runtime.
- No changes to existing files — the feature is additive at this stage.

## Related documents

- Feature overview: [docs/features/visual-dice.md](../features/visual-dice.md)
- General architecture: [docs/architecture/dice-rolling.md](dice-rolling.md)

## Changelog

| Date | Change |
|------|--------|
| 2026-03-28 | Initial version — issue #15 |
