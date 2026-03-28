# Design spec: Polygon Geometry and Visual States (Issue #14)

## Screen layout

This spec extends the existing main-screen layout defined in `docs/design/main-screen.md`. No new screens are introduced. Two regions within the existing `Scaffold` body `Column` are affected:

1. **Dice selector region** — the five `FilterChip` components are replaced by five custom `DiceSelector` composable items. Each item is a `Box` (56x56dp) containing a `Canvas` that draws the die's polygon outline, with the die label ("D4", "D6", etc.) centered inside using a `Text`.
2. **Result region** — the plain `Text` result display is replaced by a custom `DiceResultDisplay` composable. It is a `Box` (200x200dp, centered in the `weight(1f)` container) containing a `Canvas` that draws a large polygon outline and fill, with the rolled number (or D6 pip dots) centered inside.

All enclosing layout elements — `Scaffold`, outer `Column`, `Box` with `weight(1f)`, `Button` — remain unchanged from `main-screen.md`.

**Composable hierarchy delta:**

```
Scaffold
  CenterAlignedTopAppBar (title = "Dice Roller")
  Column (fillMaxSize, SpaceBetween, padding 24dp h / 32dp v)
    Column (spacedBy 12dp)
      Text "Select a die" (labelLarge, onSurfaceVariant)
      Row (horizontalScrollable, spacedBy 8dp)
        DiceSelector x5   ← replaces FilterChip x5
    Box (fillMaxWidth, weight 1f, contentAlignment Center)
      DiceResultDisplay   ← replaces Text
    Button (fillMaxWidth)
      Text "Roll D{N}"
```

---

## Polygon geometry

All polygons are drawn on a Compose `Canvas` using a `Path` built from equally-spaced vertices inscribed in a circle. The circle radius is `(bounding box short side / 2) - (stroke width / 2) - 4dp` so the stroke is never clipped by the bounding box.

**Shape mapping:**

| Die | Shape | Vertex count | Starting angle | Notes |
|-----|-------|-------------|----------------|-------|
| D4  | Triangle | 3 | -90 deg (apex up) | Equilateral triangle, flat bottom |
| D6  | Square | 4 | 0 deg | Flat sides top and bottom, matches a physical D6 face |
| D8  | Octagon | 8 | -90 deg | Regular octagon, flat top |
| D12 | Pentagon | 5 | -90 deg (apex up) | Regular pentagon, flat bottom |
| D20 | Chamfered triangle | 3 | -90 deg (apex up) | Triangle with corners clipped at 12% of edge length (see below) |

**D20 chamfer construction:** Each vertex of the base equilateral triangle is replaced by a short straight chord. The chord connects two points, each offset 12% of the full edge length inward along each adjacent edge from that vertex. The path uses `lineTo` across this chord rather than reaching the vertex. This is applied at both selector and result sizes by proportion, requiring no absolute pixel values.

**Size tokens:**

| Context | Bounding box | Stroke width |
|---------|-------------|--------------|
| DiceSelector (small) | 56x56dp | 2dp |
| DiceResultDisplay (large) | 200x200dp | 3dp |

---

## Material 3 components

- `Canvas` (Compose) — polygon path drawing and D6 pip dot drawing.
- `Box` — tap-target and clipping wrapper for each `DiceSelector` item; also the centering container for `DiceResultDisplay`.
- `Text` — die label inside selector ("D4", "D6", etc.) and rolled number inside result display for non-D6 dice.
- `Row` with `horizontalScroll` — selector row container (unchanged).
- `Box` with `contentAlignment = Center` and `weight(1f)` — result area container (unchanged).

No additional M3 compound components are used. All polygon drawing is custom via `Canvas`.

---

## User interactions and transitions

- **Tap on DiceSelector item:** Instantly switches the selected die. The previously selected item reverts to unselected visual state; the tapped item switches to selected visual state. No animation between states. A ripple `indication` is shown on the 56x56dp `Box` bounding area on press, using `rememberRipple()` with color `MaterialTheme.colorScheme.primary`.
- **Roll button tap:** The `DiceResultDisplay` updates its drawn content (number or pips) and transitions from empty-state styling to result-state styling immediately when the new `UiState` is received from the ViewModel.
- **Die re-selection after a roll:** The `DiceResultDisplay` polygon shape changes immediately to match the newly selected die. The previously rolled result number clears and returns to the empty state ("–") because the result belongs to a different die type.

---

## States to handle

- **Empty state:** `DiceResultDisplay` draws the current die's polygon outline in `MaterialTheme.colorScheme.outlineVariant` with no fill, and renders "–" centered inside in `MaterialTheme.colorScheme.onSurfaceVariant` / `headlineMedium`. This is the state on app launch and whenever the selected die type changes after a roll.
- **Success state:** `DiceResultDisplay` draws the current die's polygon outline in `MaterialTheme.colorScheme.primary` with interior fill `MaterialTheme.colorScheme.primaryContainer`. For D4, D8, D12, and D20, the rolled number is centered in `displayLarge` / `MaterialTheme.colorScheme.onPrimaryContainer`. For D6, pip dots are drawn in `MaterialTheme.colorScheme.onPrimaryContainer`.

**DiceSelector visual states:**

| State | Stroke color | Fill | Label color | Label weight |
|-------|-------------|------|-------------|--------------|
| Unselected | `outline` | transparent | `onSurfaceVariant` | Normal |
| Selected | `primary` | `primaryContainer` | `onPrimaryContainer` | Bold |

---

## D6 pip layout

Pips are filled circles drawn on the `Canvas` inside the D6 square polygon. The inscribed square area is divided into a 3x3 virtual grid. Pip diameter is 12% of the polygon's inscribed circle diameter (approximately 10dp at result display size). Pips are shown only in `DiceResultDisplay`, never in the selector. The selector always shows the "D6" text label.

| Value | Active grid cells (row, col) |
|-------|------------------------------|
| 1 | (2,2) |
| 2 | (1,3), (3,1) |
| 3 | (1,3), (2,2), (3,1) |
| 4 | (1,1), (1,3), (3,1), (3,3) |
| 5 | (1,1), (1,3), (2,2), (3,1), (3,3) |
| 6 | (1,1), (1,3), (2,1), (2,3), (3,1), (3,3) |

Pip fill: `MaterialTheme.colorScheme.onPrimaryContainer`. No stroke on pips.

---

## Accessibility

- Each `DiceSelector` `Box` carries a `semantics` block with `Role.RadioButton`, `selected = <boolean>`, and `contentDescription = "Select [die name]"`.
- `liveRegion = LiveRegionMode.Polite` on `DiceResultDisplay` so the result is announced after each roll.
  - Empty state: "No roll yet"
  - Result for D4/D8/D12/D20: "Rolled [number] on [die name]"
  - Result for D6: "Rolled [number] on D6"
- Minimum touch target: 48x48dp required. The `DiceSelector` bounding `Box` is 56x56dp, satisfying this requirement.
- Color is never the sole indicator of selected state — fill vs. no fill is perceivable independently of color.
- WCAG AA contrast (4.5:1): `onPrimaryContainer` on `primaryContainer` and `onSurfaceVariant` on `surface` both satisfy AA under the M3 baseline palette.

---

## Assets and icons

No Material Symbols and no raster drawables are required. All polygon shapes and pip dots are rendered programmatically via `Canvas`.
