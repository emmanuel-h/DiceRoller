# Testing: fix-triangle-dice-alignment

Related feature: [fix-triangle-dice-alignment](../features/fix-triangle-dice-alignment.md)
Issue: [#28](https://github.com/emmanuel-h/DiceRoller/issues/28) — PR: [#29](https://github.com/emmanuel-h/DiceRoller/pulls/29)
Branch: `feat/fix-triangle-dice-alignment/apply-vertical-offset`

## Overview

Two test classes were written to cover the triangle alignment fix: one unit test class
for the `DiceShape` model and one Compose UI test class for rendered alignment at both
polygon sizes.

## Unit tests — DiceShapeTest

File: `app/src/test/java/fr/mandarine/diceroller/presentation/model/DiceShapeTest.kt`
Count: 25 tests

### Coverage areas

**Vertex count correctness**
Asserts `vertexCount` for all five `DiceShape` subtypes:
- Triangle = 3, Square = 4, Octagon = 8, Pentagon = 5, Icosahedron = 3

**`verticalOffsetFraction` — triangle shapes**
- Is non-zero and negative for Triangle (D4) and Icosahedron (D20).
- Triangle and Icosahedron share the same constant value.

**`verticalOffsetFraction` — non-triangle shapes**
- Is exactly `0f` for Square (D6), Octagon (D8), and Pentagon (D12).

**`DiceShape.fromDice()` mapping**
- Maps all 5 `Dice` variants to their correct `DiceShape` subtype.
- Propagates the correct `verticalOffsetFraction` value per dice type.

## Compose UI tests — TriangleDiceAlignmentUiTest

File: `app/src/androidTest/java/fr/mandarine/diceroller/TriangleDiceAlignmentUiTest.kt`
Count: 18 tests

### Coverage areas

**Layout rendering — triangle dice**
- D4 renders without layout errors at `DicePolygonSize.Small` and `DicePolygonSize.Large`.
- D20 renders without layout errors at `DicePolygonSize.Small` and `DicePolygonSize.Large`.

**Selected state**
- D4 and D20 render correctly with `isSelected = true`.

**Dark theme**
- D4 and D20 render correctly under the dark theme.

**Regression guard — non-triangle dice**
- D6, D8, and D12 render without errors at both `Small` and `Large` sizes.

**Full-screen smoke tests**
- D4 as `selectedDice` in `DiceRollerScreen` with a result value shown.
- D20 as `selectedDice` in `DiceRollerScreen` with a result value shown.

## Test strategy alignment

These tests follow the project's layered testing approach:

| Layer | Class | Tooling |
|-------|-------|---------|
| Unit | `DiceShapeTest` | JUnit 4, standard assertions |
| UI | `TriangleDiceAlignmentUiTest` | Compose UI testing (`createComposeRule`) |

See [docs/testing/strategy.md](strategy.md) if a general strategy document exists.

## Changelog

| Date | Change |
|------|--------|
| 2026-03-29 | Initial version — unit tests (25) and Compose UI tests (18) documented |
