# Multi-Dice Roll — Design Spec

> Covers GitHub Issues #44 and #45.
> Supersedes the die-selector portion of [Main Screen Design Spec](main-screen.md), which described the retired single-select `FilterChip` model.
> Related: [Roll Multiple Dice at Once (feature)](../features/multi-dice-roll.md)

## Stepper chip selector and Roll button (issue #44)

### Screen layout
Restructures `DiceRollerScreen`'s `Scaffold`:

```
Scaffold
  topBar: CenterAlignedTopAppBar("Dice Roller")            [unchanged]
  content: Column(fillMaxWidth, verticalScroll)             [NEW: scrollable]
    Column(spacedBy 12dp, padding 24dp/32dp)
      "Color" label + DiceColorSwatchRow                    [unchanged]
      "Dice pool" label (renamed from "Select a die")
      FlowRow(spacedBy 8dp h/v)  ← 6× DiceStepperChip        [NEW]
      ART_ATTRIBUTION text                                   [unchanged]
    Box(weight(1f), contentAlignment=Center)
      result region                                          [owned by #45]
  bottomBar: Surface { RollButton }                          [NEW: moved out of scrolling column]
```

Rationale for the two structural changes:
- **`verticalScroll` on the content column** — six always-visible stepper chips (FR1) take roughly 3 rows × 100dp ≈ 316dp, more than the old single row of chips. On compact-height phones this can compress or clip the result region. Scrolling guarantees every chip stays reachable without shrinking anything below a usable size.
- **Roll button promoted to `Scaffold.bottomBar`** — so it never scrolls out of view while the pool/result content above it does.

### Material 3 components
- `Scaffold` with `topBar` + new `bottomBar` slot (`Surface` wrapping the `Button`, tonal elevation matching default bottom bar treatment, horizontal padding 24dp / vertical 12dp).
- `FlowRow` (`ExperimentalLayoutApi`, already opted into in `MainActivity.kt`) replaces the `FilterChip`/`DiceSelectorChip` loop — reused, not new API.
- New composable **`DiceStepperChip`** (suggested file: `presentation/component/DiceStepperChip.kt`), replacing `DiceSelectorChip`. Per chip:
  - Outer container: `Box`, shape `RoundedCornerShape(12.dp)`, **not** wrapped in `selectable`/`Role.RadioButton` — only the two `IconButton`s are interactive.
  - Row 1 (top, centered): `DiceImage` at a **new `DiceImageSize.Micro = 28.dp`** entry (add to the existing enum) + `Text("D${dice.faces}")` (`labelMedium`), `spacedBy(4.dp)`.
  - Row 2 (stepper, centered): default-sized `IconButton` (glyph `Text("−")`, U+2212 MINUS SIGN — not `Icons.Filled.Remove`, which requires `material-icons-extended`, not a project dependency) — count `Text` (`titleMedium`, `minWidth` ~24dp, centered) — default-sized `IconButton` (glyph `Text("+")`).
  - Chip padding: 8dp all sides. Internal row spacing: 4dp vertical between the two rows.
- Use default `IconButton` sizing (48dp touch target), not `IconButtonDefaults.smallContainerSize()`. Resulting chip ~144dp × 100dp; `FlowRow` wraps to 2 chips per row on typical phone widths.
- Roll button: unchanged `Button` (filled), `fillMaxWidth()`, lives in `bottomBar`; label allowed to wrap to 2 lines for long pools (no `maxLines`/`overflow`).

### User interactions & transitions
- Tapping `+`/`−` changes that die type's count by exactly 1, synchronously — no debounce, no animation. Long-press-to-fast-adjust is out of scope.
- Any count change (+ or −) clears the current result immediately, reusing the existing "invalidate on change" treatment (FR4).
- Changing color never clears results (unchanged).
- `−` enabled whenever `count > 0`; disabled exactly at `count == 0`.
- `+` enabled whenever `count < 20`; disabled exactly at `count == 20`.
- Chip container visually switches from "excluded" to "included" styling the instant `count` crosses 0 ↔ 1.
- Roll button label recomputes on every count change: `"Roll " + pool.entries.sortedBy{faces}.joinToString(" + "){ "${count}D${faces}" }`, e.g. `"Roll 4D6 + 2D8"`. Single-type pool collapses to `"Roll 4D6"` (no `+`).

### States to handle
- **Loading state:** not applicable — synchronous, in-memory.
- **Empty state:** app launch / all six counts at 0. All chips render "excluded". Roll button disabled, label reads **"Add dice to roll"**.
- **Error state:** not applicable.
- **Success state:** pool has ≥1 die counted anywhere → Roll button enabled, label shows composed pool string, wraps to 2nd line rather than truncating.

Per-chip visual state table:

| Count | Container | Border | Content color | `−` | `+` |
|---|---|---|---|---|---|
| 0 (excluded) | `Color.Transparent` | 1dp `outlineVariant` | `onSurfaceVariant` | disabled | enabled |
| 1–19 (included) | `primaryContainer` | 1dp `primary` | `onPrimaryContainer` | enabled | enabled |
| 20 (cap) | `primaryContainer` | 1dp `primary` | `onPrimaryContainer` | enabled | disabled |

Disabled icon buttons use M3's default disabled treatment (content alpha 38%); they remain in the composition (enabled = false) so TalkBack still announces them.

### Accessibility
- Each `IconButton` carries a **static** `contentDescription`: `"Decrease D6 count"`, `"Increase D6 count"` (no live count embedded).
- The count `Text` carries `Modifier.semantics { stateDescription = "$count" }`.
- The chip's `DiceImage` (28dp) must be marked **decorative** (`contentDescription = null`), since the adjacent "D6" label and the two buttons' descriptions already say "D6". This requires widening `DiceImage`'s `contentDescription` parameter from `String` to `String?` (currently non-nullable with a default) — flagged as a small required signature change for the craftsman.
- Minimum touch target 48×48dp — satisfied via default `IconButton` sizing.
- Color contrast: `primaryContainer`/`onPrimaryContainer` and `outlineVariant`/`onSurfaceVariant` are theme-guaranteed AA pairs already used elsewhere.
- Roll button disabled state: `contentDescription` equals the visible "Add dice to roll" text.
- Reading order: top row (decorative image skipped) → "D6" text → `−` → count → `+`.

### Assets & icons
- No new icons needed for `−`/`+` — plain text glyphs inside `IconButton`, avoiding a `material-icons-extended` dependency.
- One new enum entry: `DiceImageSize.Micro = 28.dp`, alongside existing `Small (56dp)` and `Large (160dp)`.
- Reuses all 72 existing Fantasy Dices Pack drawables via `DiceColor.drawableFor()` — no new artwork.

## Result face-ladder and total line (issue #45)

### Scope note
Covers only the result display region. The pool selector / stepper chips above it are issue #44's scope (see above). Assumes the pool and per-value tally are already computed upstream (issues #46/#47/#49) and consumed here as read-only data: an ordered list of `(Dice, List<value to count>)` groups, smallest die to largest, plus a total sum.

### Screen layout
Replaces the current centered `Box(weight(1f)) { DiceResultDisplay(...) }` in `MainActivity.kt` with a region that has two mutually-exclusive layouts depending on whether a tally exists:

```
Result region — populated                 Result region — empty
┌────────────────────────────┐            ┌────────────────────────────┐
│ 4×D6                       │            │                            │
│  🎲  6              ×1     │            │           🎲               │
│  🎲  4              ×2     │            │      (dimmed, 40% alpha)   │
│  🎲  3              ×1     │            │                            │
│                             │            │   "Tap Roll to see        │
│ 2×D8                       │            │    results" / "Add dice   │
│  🎲  7              ×1     │            │    above to build your    │
│  🎲  2              ×1     │            │    pool"                  │
│ ─────────────────────────  │            │                            │
│         Total 26           │            └────────────────────────────┘
└────────────────────────────┘            (Column, vertically centered)
(LazyColumn, top-anchored,
 scrolls if content overflows)
```

Row anatomy (fixed 3-slot layout, `fillMaxWidth`): die art (56dp, fixed) — value (flexible, left-aligned) — count (40dp fixed, end-aligned). The die art is decorative and identical for every row of the same die type/color; the value column is the only place the rolled number appears. The count column has a fixed width so `×` glyphs align vertically down a group whether count is `×1` or `×20`.

### Composable hierarchy
```
Box (fillMaxWidth, weight(1f), contentAlignment = Center)
  if (tally == null) {
    Column (centered, spacedBy 12dp)                       // empty state
      Icon (Material Symbols "casino", 96dp, alpha 0.4f, contentDescription = null)
      Text (caption, bodyMedium, onSurfaceVariant)
  } else {
    LazyColumn (fillMaxSize, contentPadding vertical 8dp)   // populated state
      for each group (smallest → largest die, count > 0):
        item("header-$dice") { GroupHeader(dice, count) }
        items(rowsDescByValue, key = "row-$dice-$value") { ResultRow(dice, value, count) }
        item("spacer-$dice") { Spacer(20dp) }
      item("divider") { HorizontalDivider() }
      item("total") { TotalLine(sum) }
  }
```

### Material 3 components
- `LazyColumn` — populated-state container (up to 6 groups × up to 20 rows can exceed viewport height).
- `Text` — group header, row value, row count, total line, empty-state caption, using existing `MaterialTheme.typography` roles.
- `Icon` — Material Symbols "casino" (outlined), empty state only.
- `HorizontalDivider` — separates the last group from the total line.
- `DiceImage` (existing component, reused as-is) — `sizeVariant = DiceImageSize.Small`, `color = selectedColor` — for row art.
- `Spacer` — inter-group and internal spacing.

### Typography & color (hierarchy: row value > group header > count/total)

| Element | Style | Color |
|---|---|---|
| Group header ("4×D6") | `titleMedium` | `onSurfaceVariant` |
| Row value ("6") | `headlineSmall` | `onSurface` — the "headline" the total line is deliberately demoted below |
| Row count ("×1") | `bodyLarge` | `onSurfaceVariant` |
| Total line ("Total 26") | `bodyMedium` | `onSurfaceVariant`, centered |
| Empty-state caption | `bodyMedium` | `onSurfaceVariant`, centered |

Header format is `"{count}×D{faces}"` with the `×` glyph (e.g. "4×D6") — distinct from the Roll button's "4D6" notation (no `×`). Do not reuse the same formatter for both.

### Ordering rules (rendering order only — computed upstream)
- Groups render smallest die to largest (D4, D6, D8, D10, D12, D20), same order as the Roll button label; groups with count 0 are omitted entirely.
- Within a group, rows sorted descending by value (highest face value first).
- Values rolled zero times produce no row.

### Spacing
Header → first row: 8dp. Between rows in a group: 4dp. Between groups: 20dp. Divider → total line: 16dp above, 8dp below. Row internal horizontal spacing (art/value/count): 12dp.

### User interactions & transitions
- Rows and group headers are display-only — no ripple, no `selectable`/`clickable`.
- `LazyColumn` scrolls with default Compose overscroll if content exceeds available height.
- No animation on roll: list content swaps immediately (roll animations are explicitly out of scope in the PRD).
- State transition rule (mirrors FR4): tally `null` → empty state; tally present → populated state. Driven entirely by `tally == null`.

### States to handle
- **Loading**: not applicable — rolling is synchronous.
- **Empty state** — two triggers, same visual treatment, different caption:
  - *Empty pool* (all six counts 0): icon dimmed to 0.4 alpha (reuse `EMPTY_STATE_ALPHA` from `DiceResultDisplay.kt`), caption "Add dice above to build your pool." Roll button disabled.
  - *Non-empty pool, not yet rolled*: same dimmed icon, caption "Tap Roll to see results."
- **Error state**: not applicable.
- **Success state**: populated `LazyColumn`, one group per die type present, rows sorted descending, total line at bottom.

### Accessibility
- Each row: single merged semantics node, `Modifier.semantics(mergeDescendants = true) { contentDescription = "Value $value, rolled $count time" }` (pluralize "times" for count > 1).
- Row's `DiceImage` marked decorative (`contentDescription = null`) since the row's merged node already carries the description.
- Group header content description: `"$count $dice dice"` (e.g. "4 D6 dice").
- Total line content description: `"Total $sum"`.
- Live region: per-row `liveRegion` is unreliable inside a `LazyColumn`. Instead add one invisible node above the list — `Modifier.clearAndSetSemantics { liveRegion = LiveRegionMode.Polite; contentDescription = fullSummary }` on a zero-size `Spacer` — carrying one generated summary string (e.g. "Rolled 4 D6 and 2 D8. D6: two 4s, one 6, one 3. D8: one 7, one 2. Total 26."), mirroring the pattern in `DiceResultDisplay.kt` but scoped to a hidden summary node.
- Empty-state icon: `contentDescription = null` (decorative); the caption `Text` is the accessible label.
- Minimum touch target: not applicable (rows/header/total are non-interactive).
- Color contrast: no new custom colors — existing `onSurface`/`onSurfaceVariant` roles against `surface`.

### Assets & icons
- **Material Symbols "casino" (outlined, weight 400)** — new asset needed for the empty state. The project has no `material-icons-extended` dependency; recommend adding a single hand-picked vector drawable (`res/drawable/ic_casino.xml`, 24dp viewport) rather than the library dependency — flagged for the-sage/craftsman.
- Row art reuses the existing 72 Fantasy Dices Pack drawables via `DiceImage`/`DiceImageSize.Small` — no new artwork.
- No changes to the existing CC BY 4.0 attribution requirement.

## Related docs
- [Roll Multiple Dice at Once (feature)](../features/multi-dice-roll.md)
- [Main Screen Design Spec (superseded die-selector portion)](main-screen.md)
- [Architecture: Dice Rolling](../architecture/dice-rolling.md)
- [Fantasy Dices Pack art with color picker](../features/fantasy-dice-art.md)

## Changelog
| Date | Change |
|------|--------|
| 2026-08-08 | Initial version — design specs for issues #44 (stepper chip selector and Roll button) and #45 (result face-ladder and total line) |
