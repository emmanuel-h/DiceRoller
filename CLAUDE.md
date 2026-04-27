# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Run unit tests (JVM, no device needed)
./gradlew test

# Run a single unit test class
./gradlew test --tests "fr.mandarine.diceroller.domain.DiceRollerTest"

# Run instrumented tests (requires connected device or emulator)
./gradlew connectedAndroidTest

# Run a single instrumented test class
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=fr.mandarine.diceroller.DiceRollerScreenTest

# Lint
./gradlew lint
```

## Architecture

Single-module Android app (`app/`) using Jetpack Compose and MVVM.

**Layers:**

- `domain/` — pure Kotlin, no Android imports. `Dice` (enum of D4/D6/D8/D12/D20 with face counts) and `DiceRoller` (stateless, takes an injectable `Random` for deterministic testing).
- `presentation/` — ViewModel (`DiceRollerViewModel`) holds `DiceRollerUiState` as a `StateFlow`. Selecting a different die clears the result to null; re-selecting the same die preserves it.
- `presentation/model/` — pure Kotlin presentation models with no Compose imports. `DiceShape` is a sealed class mapping each `Dice` to polygon parameters (`vertexCount`, `rotationDegrees`). `D6PipLayout` maps roll results 1–6 to `PipPosition` lists in unit-square coordinates (`0f..1f`).
- `presentation/component/` — Compose components. `DicePolygon` draws any die shape as a regular polygon using Canvas math (center + radius + angle step). `DiceResultDisplay` composes `DicePolygon` + result: D6 shows `D6Pips`, others show the number as text. `DicePolygonSize` (enum: Small/Large) controls dimensions and stroke width.
- `MainActivity` owns the single screen: a die selector row (horizontally scrollable `DicePolygon` chips), a centered `DiceResultDisplay`, and a roll button.

**Key invariant:** `DiceShape.fromDice()` is the only place that maps `Dice` → polygon parameters. When adding a new die type, update `Dice`, `DiceShape`, and ensure the `when` branch is exhaustive (compiler-enforced).

**Testing approach:**
- Unit tests (`src/test/`) test `DiceRoller` and `DiceRollerViewModel` with injected `Random` seeds for determinism. No mocking frameworks — constructor injection only.
- Instrumented tests (`src/androidTest/`) use `createComposeRule()` and test the full `DiceRollerScreen` via `DiceRollerUiState` parameters or a real ViewModel with a seeded `Random`.

## Multi-agent pipeline

New features flow through `.claude/agents/` agents orchestrated by **the-boss** (`/okay-boss <request or issue URL>`). The pipeline: boss → herald (GitHub issues) → artist (UI design) + sage (architecture) → craftsman (implementation) → inquisitor (PR review) → guardian (tests) → scribe (docs). Agent outputs land in `docs/` (features, architecture, design, testing subdirectories).
