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

- `domain/` — pure Kotlin, no Android imports. `Dice` (enum of D4/D6/D8/D10/D12/D20 with face counts) and `DiceRoller` (stateless, takes an injectable `Random` for deterministic testing).
- `presentation/` — ViewModel (`DiceRollerViewModel`) holds `DiceRollerUiState` as a `StateFlow`. Selecting a different die clears the result to null; re-selecting the same die preserves it. Selecting a *color* never clears the result, and never touches the roll history either. `DiceColorStore` and `RollHistoryStore` abstract persistence, with `InMemoryDiceColorStore`/`InMemoryRollHistoryStore` for tests. A `clock: () -> Long` is injected alongside `Random` so recorded timestamps are deterministic under test. `DiceNotation` formats a pool as `"4D6 + 2D8"` for both the Roll button and history entries; `RelativeTime` formats `"3 min ago"`.
- `presentation/model/` — `DiceColor`, the 12 Fantasy Dices Pack variants, each carrying a `label`, a `swatch` color, and `drawableFor(dice)`.
- `presentation/component/` — Compose components. `DiceImage` renders one pack drawable with `ContentScale.Fit`. `DiceImageSize` (enum: Compact/Inline/Small) sets the box it is fitted into. `DiceStepperChip` is one die type's count control: the artwork *is* the control, with its left half decrementing and its right half incrementing. `DiceResultDisplay` shows the roll as one wrapping line of `art + value + ×N` entries per die type, then a demoted total. `RollHistoryBand` is the collapsible log of past rolls. `DiceColorSwatchRow` is the 12-dot color picker.
- `data/` — `DataStoreDiceColorStore` (persists by enum `name`, not ordinal) and `DataStoreRollHistoryStore` (persists via `RollHistoryCodec`, a hand-rolled string format whose decoder drops corrupt records instead of throwing). Both share the one `Context.diceDataStore` delegate in `DiceDataStore.kt` — **a second `preferencesDataStore` delegate for the same file name throws at runtime**, so any new store must reuse that one.
- `MainActivity` owns the single screen, laid out as bands so a realistic pool fits in one view without scrolling: the color swatch row, a 3×2 grid of `DiceStepperChip`s, a flexible `DiceResultDisplay`, the collapsible `RollHistoryBand`, and a pinned bottom bar holding the roll button with the artwork attribution beneath it. There is no top app bar — the title was not worth its height. The result and history bands share one weighted sub-column and are the only things that scroll; history is absent entirely until the first roll.

**Key invariant:** `DiceColor.drawableFor()` is the only place that maps `(color, die)` → drawable resource. When adding a die type or a color variant, update `Dice`/`DiceColor`, add the 6 or 12 artwork files, and keep both `when` branches exhaustive (compiler-enforced).

**Artwork:** the dice art is a fixed render with numerals painted on the faces, so the rolled value can never be drawn on the die — it is always a separate number below it. Art is CC BY 4.0 and the in-app credit line is required; see `docs/licenses/third-party-assets.md`.

**Testing approach:**
- Unit tests (`src/test/`) test `DiceRoller`, `DiceRollerViewModel` and `DiceColor` with injected `Random` seeds for determinism. No mocking frameworks — constructor injection only. `MainDispatcherRule` swaps `Dispatchers.Main` so `viewModelScope` works off-device.
- Instrumented tests (`src/androidTest/`) use `createComposeRule()` and test the full `DiceRollerScreen` via `DiceRollerUiState` parameters or a real ViewModel with a seeded `Random`. Artwork is asserted through its content description (`"D20, amethyst"`).

## Multi-agent pipeline

New features flow through `.claude/agents/` agents orchestrated by **the-boss** (`/okay-boss <request or issue URL>`). The pipeline: boss → herald (GitHub issues) → artist (UI design) + sage (architecture) → craftsman (implementation) → inquisitor (PR review) → guardian (tests) → scribe (docs). Agent outputs land in `docs/` (features, architecture, design, testing subdirectories).
