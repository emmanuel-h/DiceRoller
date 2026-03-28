# Main Screen Design Spec

> Covers GitHub Issues #7 and #9.

## Screen layout

The screen uses a single `Scaffold` with a `CenterAlignedTopAppBar`. The body is a `Column` filling the full available size, with vertical arrangement set to `Arrangement.SpaceBetween` and horizontal alignment set to `Alignment.CenterHorizontally`. The column has horizontal padding of 24dp and vertical padding of 32dp.

The column contains three distinct regions stacked top to bottom:

1. **Dice selector region** — occupies the upper portion of the screen. Contains a label and a row of five dice chip selectors.
2. **Result region** — occupies the center of the screen and expands to fill remaining vertical space (`weight(1f)`). Displays the roll result or a placeholder.
3. **Roll action region** — anchored to the bottom. Contains the Roll button.

## Components

| Element | Implementation |
|---|---|
| App bar | `CenterAlignedTopAppBar` (title: "Dice Roller") |
| Die selector items | `FilterChip` (one per die type, 5 items) |
| Selector row | `Row` with `horizontalArrangement = Arrangement.spacedBy(8.dp)`, inside a `horizontalScroll` |
| Result display | `Text` with `displayLarge` typography |
| Result placeholder | `Text` with `titleMedium`, `onSurfaceVariant` colour, showing "–" |
| Roll button | `Button` (filled), full-width, label "Roll D{N}" |

## States

- **Initial**: Placeholder "–" in `titleMedium`/`onSurfaceVariant`. Roll button enabled, labelled "Roll D6".
- **Result shown**: Integer in `displayLarge`/`onSurface`. Roll button enabled.
- **Die re-selected after result**: Result remains until next roll. Button label updates immediately.

## FilterChip visual states

| State | Background | Label colour |
|---|---|---|
| Unselected | `surfaceVariant` | `onSurfaceVariant` |
| Selected | `secondaryContainer` | `onSecondaryContainer` |

## Spacing

- Screen horizontal padding: 24dp; top padding: 24dp; bottom: 32dp
- Chip row item spacing: 8dp; gap between label and chips: 12dp
- Result area: `weight(1f)`, text centred via `Box(contentAlignment = Center)`
- Roll button: `fillMaxWidth()`

## Accessibility

- Result `Box` wrapped in `semantics { liveRegion = LiveRegionMode.Polite }`
- `FilterChip` exposes selected state via `Role.Checkbox` (M3 default)
- Roll button label is self-describing

## Composable hierarchy

```
Scaffold
  CenterAlignedTopAppBar (title = "Dice Roller")
  Column (fillMaxSize, SpaceBetween, padding 24dp/32dp)
    Column (spacedBy 12dp)
      Text "Select a die" (labelLarge, onSurfaceVariant)
      Row (horizontalScrollable, spacedBy 8dp)
        FilterChip × 5 (D4, D6, D8, D12, D20)
    Box (fillMaxWidth, weight 1f, contentAlignment Center)
      Text (displayLarge | titleMedium depending on state)
    Button (fillMaxWidth)
      Text "Roll D{N}"
```

## Related docs

- [Dice Selection and Roll feature](../features/dice-selection-and-roll.md)
- [Architecture: Dice Rolling](../architecture/dice-rolling.md)

## Changelog

| Date | Change |
|---|---|
| 2026-03-28 | Initial version — Issues #7 and #9 |
