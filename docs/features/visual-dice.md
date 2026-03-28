## PRD: Visual Dice

### Problem statement
The dice selector and result display currently use plain text chips and a large number. This makes the app feel generic and un-thematic. Users of a dice roller app expect to see recognizable die shapes, not flat buttons and raw numbers.

### Goals & success metrics
- Replace the FilterChip dice selectors with small, recognizable polygon outlines (triangle, square, pentagon, octagon, icosahedron-face) so users can identify each die type visually.
- Replace the plain-number result display with a large polygon outline containing the rolled number inside it.
- For D6 specifically, show traditional dot pips on the face when the result is 1 through 6.
- All other dice (D4, D8, D12, D20) show the rolled number centered inside their polygon outline.
- The visual treatment must follow the app's existing Material 3 theme (colors, dark/light mode support).

### User stories
- As a player, I want the dice selector to show die shapes instead of text chips, so that I can quickly recognize and pick the die I need.
- As a player, I want the roll result to appear inside a die-shaped outline, so that the app feels like a real dice experience.
- As a player, I want the D6 to show dots like a physical die, so that the most common die type feels familiar.

### Shape mapping
| Die  | Polygon outline          |
|------|--------------------------|
| D4   | Triangle (3 sides)       |
| D6   | Square (4 sides)         |
| D8   | Octagon (8 sides)        |
| D12  | Pentagon (5 sides)       |
| D20  | Icosahedron face (triangle with clipped corners, or a regular triangle -- designer's discretion) |

### Behavior details
- Selector: each die is rendered as a small version of its polygon with "D4", "D6", etc. labeled inside. The selected die is visually highlighted (filled or outlined per Material 3 selected/unselected states).
- Result: after a roll, the center area shows a large version of the selected die's polygon with the result number (or dots for D6) inside.
- Before any roll has been performed, the center area shows the selected die's polygon outline with a dash or empty state inside.

## Architecture

> Last updated: added link to shape and pip model architecture (issue #15)

The presentation model layer for this feature is documented in [docs/architecture/visual-dice-shapes.md](../architecture/visual-dice-shapes.md). It covers `DiceShape`, `PipPosition`, and `D6PipLayout`.

### Out of scope
- Rolling animation or physics simulation.
- Sound effects.
- 3D rendering of the dice.
- Supporting dice types beyond the existing five (D4, D6, D8, D12, D20).
- Changes to the roll button at the bottom.
