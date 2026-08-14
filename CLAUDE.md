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

- `domain/` — pure Kotlin, no Android imports. `DieType` is the sealed supertype of anything rollable, carrying `faces` and a `label` (`"D6"`); its two implementations are `Dice` (enum of the six presets D4/D6/D8/D10/D12/D20 — the only ones with artwork) and `CustomDie` (a user-defined face count, issue #4). `DiceRoller` is stateless and takes an injectable `Random` for deterministic testing. **No two `DieType`s ever share a face count**: `CustomDie`'s `init` rejects a preset's, and `DieType.ofFaces` is the single place that resolves a bare number to a die.
- `presentation/` — ViewModel (`DiceRollerViewModel`) holds `DiceRollerUiState` as a `StateFlow`. The rule for the result is "a change to what Roll would produce clears it": changing a count clears it, *adding* a custom die does not (it enters at count 0), *removing* one does only if its count was non-zero, and undoing that removal leaves it cleared. Selecting a *color* never clears the result, and never touches the roll history either. `DiceColorStore`, `RollHistoryStore` and `CustomDiceStore` abstract persistence, each with an `InMemory…` counterpart for tests; the custom-dice store owns `normalizeCustomDice` (dedupe, sort ascending, cap at `MAX_CUSTOM_DICE` = 3). A `clock: () -> Long` is injected alongside `Random` so recorded timestamps are deterministic under test. `DiceNotation` formats a pool as `"4D6 + 1D7 + 2D8"` for both the Roll button and history entries; `RelativeTime` formats `"3 min ago"`; `validateCustomFaces` is the pure single source of truth for what the custom-die creator will accept.
- `presentation/model/` — `DiceColor`, the 12 Fantasy Dices Pack variants, each carrying a `label`, a `swatch` color, and `drawableFor(dice)`.
- `presentation/component/` — Compose components. `DiceImage` takes a `DieType` and is **the one place that decides how a die is drawn**: a `Dice` preset renders its pack drawable with `ContentScale.Fit`, a `CustomDie` falls through to `CustomDieBadge` (a `swatch`-filled pill carrying the face count, since there is no artwork for a D7). `DiceImageSize` (enum: Compact/Inline/Small) sets the box art is fitted into and the badge's height. `DiceStepperChip` is one die type's count control, serving both kinds: the artwork *is* the control, with its left half decrementing and its right half incrementing, plus an optional `onRemove` that draws the `×` badge — presets pass null, which is the only thing making them undeletable. `AddDiceChip` and `CustomDieCreatorDialog` create a custom die. `DiceResultDisplay` shows the roll as one wrapping line of `art + value + ×N` entries per die type, then a demoted total. `RollHistoryBand` is the collapsible log of past rolls. `DiceColorSwatchRow` is the 12-dot color picker.
- `data/` — `DataStoreDiceColorStore` (persists by enum `name`, not ordinal), `DataStoreRollHistoryStore` (via `RollHistoryCodec`) and `DataStoreCustomDiceStore` (via `CustomDiceCodec`, just the face counts: `"3,7,100"`). Both codecs are hand-rolled string formats whose decoders drop corrupt entries instead of throwing. `RollHistoryCodec` keys groups **by face count, not enum name**, which is why custom dice needed no format change and why a past roll survives the user deleting the die's definition. All three stores share the one `Context.diceDataStore` delegate in `DiceDataStore.kt` — **a second `preferencesDataStore` delegate for the same file name throws at runtime**, so any new store must reuse that one.
- `MainActivity` owns the single screen, laid out as bands: the color swatch row, a three-row chip grid, a flexible `DiceResultDisplay`, the collapsible `RollHistoryBand`, and a pinned bottom bar holding the roll button with the artwork attribution beneath it. There is no top app bar — the title was not worth its height. The result and history bands share one weighted sub-column and are the only things that scroll; history is absent entirely until the first roll. The grid holds 6 presets + up to 3 custom dice + an add chip (hidden at the cap), which is **always exactly three rows** — that constant height is why the cap is 3. It costs 106dp against the ~16dp of slack issue #64 left, so at ≤680dp the result band scrolls its total into reach; every *control* still fits at 640dp, and that is the guarantee not to regress. See `docs/features/custom-dice.md` for the measured thresholds.

**Key invariant:** `DiceColor.drawableFor()` is the only place that maps `(color, die)` → drawable resource, and it takes a `Dice` — *not* a `DieType` — precisely so both its `when` branches stay compiler-enforced-exhaustive. That is why `Dice` remains an `enum` and custom dice are a separate implementation: adding a preset must stay a build error until its artwork exists. When adding a preset or a color variant, update `Dice`/`DiceColor`, add the 6 or 12 artwork files, and keep both branches exhaustive. Custom dice deliberately bypass this path entirely via `CustomDieBadge`.

**Artwork:** the dice art is a fixed render with numerals painted on the faces, so the rolled value can never be drawn on the die — it is always a separate number below it. Art is CC BY 4.0 and the in-app credit line is required; see `docs/licenses/third-party-assets.md`.

**Testing approach:**
- Unit tests (`src/test/`) test `DiceRoller`, `DiceRollerViewModel` and `DiceColor` with injected `Random` seeds for determinism. No mocking frameworks — constructor injection only. `MainDispatcherRule` swaps `Dispatchers.Main` so `viewModelScope` works off-device.
- Instrumented tests (`src/androidTest/`) use `createComposeRule()` and test the full `DiceRollerScreen` via `DiceRollerUiState` parameters or a real ViewModel with a seeded `Random`. Artwork is asserted through its content description (`"D20, amethyst"`).

## Multi-agent pipeline

New features flow through `.claude/agents/` agents orchestrated by **the-boss** (`/okay-boss <request or issue URL>`). The pipeline: boss → herald (GitHub issues) → artist (UI design) + sage (architecture) → craftsman (implementation) → inquisitor (PR review) → guardian (tests) → scribe (docs). Agent outputs land in `docs/` (features, architecture, design, testing subdirectories).
