# PRD: fix-triangle-dice-alignment

## Problem statement

D4 and D20 appear visually misaligned in the selector row and result display.
An equilateral triangle's visual centroid doesn't coincide with its geometric
center, so triangles "float" compared to other dice shapes. This affects both
`DicePolygonSize.Small` (selector) and `DicePolygonSize.Large` (result display).

## Goals & success metrics

- All five dice shapes appear visually aligned in both selector and result display.
- Fix applies to both `DicePolygonSize.Small` and `DicePolygonSize.Large`.
- No regression for non-triangle shapes (D6, D8, D12).

## User stories

- As a player, I want the D4 and D20 triangles to sit on the same visual
  baseline as the other dice, so the UI looks polished and consistent.

## Scope

- Only alignment fix in `DiceShape.kt` / `DicePolygon.kt`.
- No shape changes, no new dice types.

## Out of scope

- Adding new dice shapes or dice types.
- Changing the polygon geometry itself (vertex count, proportions).
- Changing colors, animations, or any other visual property.

## Open questions

- None — requirements are clear.

---

## Testing

> Last updated: test suite written for issue #28 / PR #29

Unit and Compose UI tests have been written for this feature.
See [docs/testing/fix-triangle-dice-alignment.md](../testing/fix-triangle-dice-alignment.md) for full coverage details.

- **DiceShapeTest** (25 unit tests): `vertexCount`, `verticalOffsetFraction` values, and
  `DiceShape.fromDice()` mapping for all 5 dice types.
- **TriangleDiceAlignmentUiTest** (18 Compose UI tests): layout rendering at Small/Large sizes,
  selected state, dark theme, non-triangle regression guard, and full-screen smoke tests.

---

## Ticket breakdown

| Issue | Title | Agent | Depends on |
|-------|-------|-------|------------|
| [#27](https://github.com/emmanuel-h/DiceRoller/issues/27) | [IMPLEMENTATION] Apply vertical offset to triangle polygons for visual alignment | the-craftsman | none |
| [#28](https://github.com/emmanuel-h/DiceRoller/issues/28) | [TEST] Add tests for triangle dice visual alignment fix | the-guardian | #27 |
