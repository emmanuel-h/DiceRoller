# Visual identity: parchment and leather

**Issue:** [#72](https://github.com/emmanuel-h/DiceRoller/issues/72) — "Have a more effective UX"
**Status:** implemented

## Why

The app still wore the Android Studio template's theme: baseline purple, or
Material You's wallpaper colours on Android 12+. Every screen looked like any
other Compose sample. The issue asked for something more specific to the app,
playing on colour.

Three directions were mocked up side by side with the real dice art (the dice
colour seeding the whole scheme; an always-dark felt tabletop; fantasy
parchment). Parchment was chosen.

## What it is

| | Light: parchment | Dark: leather |
|---|---|---|
| Page | `#EFE2C4`, washed from `#F5EBD2` (top) to `#E2CFA6` (foot) | `#241A12`, washed from `#2E2218` (top) to `#17100A` (foot) |
| Text | ink `#3A2816` | cream `#ECDCB6` |
| Primary (Roll, selection ring) | ink stamp `#4E321A` with gold-leaf text | gold `#C9A352` |
| Selected chip | gold wash `#E0C993` | tooled leather `#4A3720` |

- **No dynamic colour.** `DiceRollerTheme` has no `dynamicColor` parameter any
  more: the palette is the app's identity and does not follow the wallpaper.
- **Every colour role is set** in `ui/theme/Theme.kt`, including the
  `surfaceContainer*` family, so Material-drawn pieces — the settings sheet,
  the custom-die dialog, the snackbar, text fields — come out on parchment
  without per-component overrides.
- **The dice colour still only recolours the art** (and the swatch row). The
  chrome is fixed so that it sits beside all twelve pack variants.
- **Lamplit wash:** `ParchmentBackground` draws a vertical gradient behind a
  transparent `Scaffold`: lightest at the top, the plain page colour 40% of
  the way down, slightly deeper at the foot by the Roll bar. It is one draw,
  no asset. It replaced a radial vignette whose default radius (half the
  shorter side) drew a visible disc in the middle of a portrait screen.
- **Roll bar** uses `surfaceContainer` explicitly instead of tonal elevation,
  which would have tinted it with the ink primary.
- **Shapes:** Material's corners tightened (`small` = 6dp), and the pool
  chips' `CHIP_SHAPE` from 16dp to 6dp. Roll uses `shapes.small` instead of a
  pill.
- **Window background** (`themes.xml`, `values-night/themes.xml`) is parchment
  or leather, so the frame before Compose draws matches.

## Typography

Cinzel (OFL, see `docs/licenses/third-party-assets.md`) is the display face,
applied through `displayStyle(base)` in `ui/theme/Type.kt`, which keeps the
base style's size and swaps family, weight and tracking. It is used **only**
on:

- the Roll button label (`titleMedium` size)
- the result's total line
- the history band header
- the settings sheet title
- the custom-die dialog title

Everything else — chip labels, rolled values on the dice, body text — stays
Roboto. Cinzel is inscriptional capitals, so it reads as small caps for
lowercase; that is right for a few short labels and wrong for anything read at
a glance. The `Typography` slots themselves are untouched, because slots like
`labelLarge` and `headlineSmall` are also used for badge and die numerals.

Both French and English strings render with accents (`PARAMÈTRES`,
`RÉCENTS`) — checked on device.

## Not changed

Layout, sizes, behaviour, strings and tests. Nothing gets taller, so the
640dp "every control fits" guarantee from `custom-dice.md` is unaffected.
