# Architecture: Dice Rolling Feature

> Covers GitHub Issues #8 and #10.

## Overview

The dice rolling feature spans two layers: domain and presentation. It introduces four classes total, keeping the implementation intentionally flat given the simplicity of the logic.

## Class inventory

### Domain layer

| Class | Kind | Responsibility |
|---|---|---|
| `Dice` | `enum` | Defines D4, D6, D8, D12, D20 with their face counts |
| `DiceRoller` | plain class | Stateless `roll(dice: Dice): Int`; `Random` injected via constructor |

### Presentation layer

| Class | Kind | Responsibility |
|---|---|---|
| `DiceRollerUiState` | `data class` | Immutable state snapshot: `selectedDice` (default D6) and nullable `result: Int?` |
| `DiceRollerViewModel` | `ViewModel` | Owns `StateFlow<DiceRollerUiState>`; exposes `selectDice(dice: Dice)` and `rollDice()` |

## Data flow

```
User action
  → ViewModel (selectDice / rollDice)
    → DiceRoller.roll(dice)  [domain]
      → Random.nextInt(faces)
    → emits new DiceRollerUiState
  → UI recomposes
```

## Key architectural decisions

### No Hilt / no UseCase layer
The core logic is a pure function with no external dependencies. Adding DI infrastructure would be disproportionate at this stage. Introduce Hilt when a feature genuinely needs it (e.g., Room persistence).

### Constructor-injected `Random`
Production code passes `Random.Default`; tests pass a seeded instance for deterministic assertions. This removes the need for mocking frameworks.

### Single flat UiState data class
A nullable `result: Int?` is used rather than a sealed hierarchy. The state space is trivially small (two variants: no result / has result), so a sealed class would add ceremony with no benefit.

## Build dependencies to add

```kotlin
// build.gradle.kts (app module)
implementation("androidx.lifecycle:lifecycle-viewmodel-compose:<version>")
implementation("androidx.lifecycle:lifecycle-runtime-compose:<version>")
```

## Related docs

- [Dice Selection and Roll feature](../features/dice-selection-and-roll.md)
- [UI Design Spec: Main Screen](../design/main-screen.md)

## Changelog

| Date | Change |
|---|---|
| 2026-03-28 | Initial version — Issues #8 and #10 |
