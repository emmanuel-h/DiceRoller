# Feature: Fantasy Dices Pack art with color picker

Closes [#42](https://github.com/emmanuel-h/DiceRoller/issues/42). Supersedes the
Canvas-polygon rendering described in `visual-dice.md` and
`visual-dice-shapes.md`, both of which are now historical.

## What changed

The Canvas-drawn `DicePolygon` shapes are replaced by pre-rendered art from the
Fantasy Dices Pack — 12 color variants × 6 die types. The user picks a color from
a swatch row, and `D10` joins the roster.

```
  before                              after
  ──────────────────────────────      ──────────────────────────────
  ┌────────────────────────┐          ┌────────────────────────┐
  │      Dice Roller       │          │      Dice Roller       │
  ├────────────────────────┤          ├────────────────────────┤
  │ Select a die           │          │ Select a die           │
  │ (△)(◇)(⬡)(⬠)(△)        │          │ (d4)(d6)(d8)(d10)      │
  │  DicePolygon chips     │          │     (d12)(d20)  ← art  │
  │                        │          │ Color                  │
  │                        │          │ ●●⦿●●●●●●●●●  ← 12 dots│
  │        ╱‾‾‾‾‾╲         │          │ Dice art by Aeynit…    │
  │       │  17   │        │          │                        │
  │        ╲_____╱         │          │       ◢ d20  ◣  ← art  │
  │   DicePolygon + number │          │                        │
  │                        │          │          17            │
  │                        │          │   displayLarge result  │
  │ [      Roll D20      ] │          │ [      Roll D20      ] │
  └────────────────────────┘          └────────────────────────┘
```

## Key constraint: the numerals are baked in

Each pack image is a single fixed render with numerals painted on the faces —
`d20.png` always reads `20`. The rolled value therefore **cannot** be drawn on
the die face. The die art is decorative; the result is a large number *below* it.

Two consequences:

- `D6Pips` / `D6PipLayout` / `PipPosition` are gone — pips cannot represent a
  roll on fixed art.
- The result number sits on the surface rather than over the artwork, so its
  contrast is governed by the Material color scheme and stays legible against
  all 12 variants in both themes.

## Structure

| Layer | Type | Responsibility |
|---|---|---|
| `domain` | `Dice` | Gains `D10(faces = 10)`; order is `D4, D6, D8, D10, D12, D20` |
| `presentation/model` | `DiceColor` | 12 variants: `label`, `swatch`, and `drawableFor(dice)` |
| `presentation/component` | `DiceImage` | Renders one drawable, `ContentScale.Fit`, `"D20, amethyst"` description |
| `presentation/component` | `DiceImageSize` | `Small` (56.dp) for chips, `Large` (160.dp) for the result |
| `presentation/component` | `DiceColorSwatchRow` | Scrollable row of 12 dots, selected one ringed |
| `presentation/component` | `DiceResultDisplay` | Die art + result number below |
| `presentation` | `DiceColorStore` | Interface + `InMemoryDiceColorStore` for tests |
| `data` | `DataStoreDiceColorStore` | DataStore Preferences persistence |

**Key invariant:** `DiceColor.drawableFor()` is the only place mapping
`(color, die)` → drawable, mirroring the old `DiceShape.fromDice()` convention.
Both `when` expressions are exhaustive, so adding a `Dice` or a `DiceColor` is a
compile error until the artwork is wired up.

## State behaviour

`DiceRollerUiState` gains `selectedColor`, defaulting to amethyst.

- `selectDice()` clears `result` on a *change* of die (unchanged behaviour).
- `selectColor()` **preserves** `result` — recoloring is cosmetic, not a reroll.
- The stored color is read once in the ViewModel's `init` and written back on
  every `selectColor()`. It is persisted by enum `name`, not ordinal, so
  reordering `DiceColor` cannot silently reassign a user's saved choice; an
  unknown name falls back to the default.

## Assets

`app/src/main/res/drawable-nodpi/dice_<color>_<die>.webp` — 72 files, 1,076 KiB.
`nodpi` keeps the bitmap at its authored pixel size; the composable's size
modifier, not the device density, decides how large it renders.

See [`../licenses/third-party-assets.md`](../licenses/third-party-assets.md) for
the license, the required credit line, and the exact conversion parameters.

## Compose BOM alignment

Bundled with this change: the Compose BOM moved `2024.09.00` → `2025.10.00`.

The project compiled `foundation-layout` at 1.7.0 (BOM-pinned) while resolving
1.9.2 at runtime, via transitive constraints from `lifecycle` and `activity`.
Any call into an API whose signature changed across those versions crashed with
`NoSuchMethodError` on device — `FlowRow` in the new die selector was the first
to hit it. The bump aligns compile and runtime on 1.9.3.

## Size impact

Measured as debug and release APKs built from this branch versus `main`, both on
the same Compose BOM so the bump above does not distort the comparison:

| | debug | release |
|---|---|---|
| Dice artwork | +1,101 KB | +1,101 KB |
| dex (DataStore + protobuf-javalite) | +736 KB | +560 KB |
| resources.arsc, misc | +31 KB | +30 KB |
| **Total** | **+1.78 MB** | **+1.61 MB** |

The artwork itself lands at **1.05 MB**, within the 1.5 MB budget set for it. The
total exceeds 1.5 MB because DataStore Preferences — required for persisting the
color choice — pulls in protobuf-javalite, and neither build type enables R8
(`isMinifyEnabled = false` on release).

Two ways to bring the total under 1.5 MB if that matters more than the current
implementation choices:

- Swap DataStore for `SharedPreferences`. One enum string does not need
  DataStore's machinery; this removes essentially all of the dex growth and puts
  the release delta near 1.13 MB. `DiceColorStore` already isolates the change to
  a single class.
- Enable `isMinifyEnabled` on release, which lets R8 strip the unused protobuf
  surface.

## Testing

- **Unit (61):** `D10` bounds and enum ordering; `selectColor` preserving the
  result while `selectDice` still clears it; persistence round-trip and restore;
  `drawableFor()` returning 72 distinct non-zero resources; `fromNameOrDefault`
  fallbacks.
- **Instrumented (37):** six selectable dice including D10; every swatch changing
  the rendered art; result number below the die; empty-state placeholder;
  content descriptions; light and dark theme; the attribution line.
