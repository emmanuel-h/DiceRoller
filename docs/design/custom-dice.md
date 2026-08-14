# Design: Custom dice

Implements [the custom-dice PRD](../features/custom-dice.md) (issue #4).

## Screen, before and after

```
  before                                  after
  ──────────────────────────────          ──────────────────────────────
  ┌────────────────────────────┐          ┌────────────────────────────┐
  │ ●●●●●●●●●●●●  swatches     │          │ ●●●●●●●●●●●●  swatches     │
  ├────────────────────────────┤          ├────────────────────────────┤
  │ ┌─────┐┌─────┐┌─────┐      │          │ ┌─────┐┌─────┐┌─────┐      │
  │ │  ▲  ││  ⬢  ││  ◆  │      │          │ │  ▲  ││  ⬢  ││  ◆  │      │
  │ │−D4 0││−D6 4││−D8 2│      │          │ │−D4 0││−D6 4││−D8 2│      │
  │ └─────┘└─────┘└─────┘      │          │ └─────┘└─────┘└─────┘      │
  │ ┌─────┐┌─────┐┌─────┐      │          │ ┌─────┐┌─────┐┌─────┐      │
  │ │ D10 ││ D12 ││ D20 │      │          │ │ D10 ││ D12 ││ D20 │      │
  │ └─────┘└─────┘└─────┘      │          │ └─────┘└─────┘└─────┘      │
  │                            │          │ ┌─────┐┌─────┐┌─────┐      │ ← 3rd row:
  │                            │          │ │(7) ⊗││(100)││  +  │      │   custom dice,
  │                            │          │ │−D7 1││−100 0│ Custom│    │   then the add
  │                            │          │ └─────┘└─────┘└─────┘      │   chip
  ├────────────────────────────┤          ├────────────────────────────┤
  │ 4×D6                       │          │ 4×D6                       │
  │ ⬢6  ⬢4 ×2  ⬢3              │          │ ⬢6  ⬢4 ×2  ⬢3              │
  │ 2×D8                       │          │ 1×D7                       │
  │ ◆7  ◆2                     │          │ (7)5                       │
  │                  Total 40  │          │                  Total 45  │
  ├────────────────────────────┤          ├────────────────────────────┤
  │ ▾ Recent (3)               │          │ ▾ Recent (3)               │
  ├────────────────────────────┤          ├────────────────────────────┤
  │ [    Roll 4D6 + 2D8     ]  │          │ [ Roll 4D6 + 1D7 + 2D8  ]  │
  │ Dice art by Aeynit         │          │ Dice art by Aeynit         │
  └────────────────────────────┘          └────────────────────────────┘
```

## The grid is always exactly three rows

Cells = 6 presets + *n* custom dice + (the add chip, unless `n == MAX_CUSTOM_DICE`).

| custom dice | cells | rows |
|---|---|---|
| 0 | 7 | 3 |
| 1 | 8 | 3 |
| 2 | 9 | 3 |
| 3 | 9 (add chip hidden) | 3 |

Hiding the add chip at the cap rather than disabling it is what makes the bottom row of that table
3 and not 4. The cap of 3 was chosen *because* it makes the grid's height constant: the feature
costs one row, always, and can never cost a second. See the PRD for the measured 106dp and the
viewport thresholds it moved.

Rows stay 3-up rather than widening to 4: a chip's two tap halves each need 48dp, so
`CHIP_MIN_WIDTH` is 96dp, and four chips on a 360dp screen would give each half only ~38dp.

Each grid row is measured at `IntrinsicSize.Min` with its cells at `fillMaxHeight()`, so all cells
in a row are as tall as the tallest. `AddDiceChip` additionally reuses `DiceStepperChip`'s own
geometry constants and puts its `+` inside a box of exactly `DiceImageSize.Small.sizeDp`, so it
measures to the same 98dp by construction rather than by a tuned constant.

## The custom die's mark

There is no artwork for a D7. Rather than borrow the nearest preset's render — which would imply a
shape the die does not have, and fight the numerals already painted on it — a custom die gets a
deliberately different mark: a rounded pill filled with the selected variant's `DiceColor.swatch`,
carrying the face count.

```
  chip (Small, 56dp)      result (Inline, 36dp)     log (Compact, 24dp)
  ╭───────────⊗           ╭────╮                    ╭──╮
  │  ╭─────╮  │           │ 7  │ 5  ×2              │ 7│ 5
  │  │  7  │  │           ╰────╯                    ╰──╯
  │  ╰─────╯  │
  │  − D7 1 + │
  ╰───────────╯
```

- It **follows the colour picker**, so choosing a colour still recolours the whole pool.
- The numeral is black or white by `swatch.luminance()`, not a hard-coded white — the twelve
  variants span too wide a range for one choice to be readable on all of them.
- Height always equals `DiceImageSize.sizeDp` so a badge and a rendered die share a baseline; the
  pill **grows wider** for a longer numeral instead of shrinking the text, which is what keeps
  `1000` legible in the 24dp log variant.

## Creating a die

```
┌──────────────────────────────┐
│ Add a custom die             │
│  ┌────────────────────┐      │
│  │ Faces        7     │      │
│  └────────────────────┘      │
│  Adds D7 to your dice.       │  ← turns into the specific refusal
│         [Cancel]  [ Add ]    │     when the input is wrong
└──────────────────────────────┘
```

One field, because a die is nothing but a face count. Removal is deliberately *not* here — it
lives on the chip — which is what keeps this a form rather than a management screen.

The typed text is local `remember` state, not ViewModel state: a half-finished number is transient
UI. `validateCustomFaces` produces one verdict that both the Add button's enablement and the
message read, so they cannot disagree. The field filters to digits at the source, so the validator
only ever explains real mistakes.

**Unfinished ≠ wrong.** An empty field shows the range hint, and a lone `"1"` is treated as
incomplete rather than out-of-range — it is on its way to `"12"` as often as it is a mistake. Both
keep Add disabled without scolding the user mid-type. Too *large*, by contrast, cannot become
correct by typing more digits, so it reads as an error immediately.

**Refusal precedence** is duplicate/preset *before* "no room": re-typing a die you already have
while at the cap is a duplicate, not a capacity problem, and "remove one to add another" would
send the user to delete a die they did not need to.

## Removing a die

The `×` badge sits in the chip's top-right, visible at rest — a die the user created is one they
should be able to see how to remove, rather than discovering a long-press. It is composed *after*
the two tap halves so it wins hit-testing in the overlap, and drawn as its own bordered circle so
it reads as a control sitting *on* the chip, whose surface everywhere else under it means
"increment".

Its target is 36dp, below the 48dp guideline on purpose: a 48dp target there would swallow a
quarter of the increment half. The mitigation is on the other side — the removal is undoable:

```
┌────────────────────────────────────┐
│ D7 removed                    Undo │   ← restores the die AND its pool count
└────────────────────────────────────┘
```

Ordering note: the selector lists presets first, then custom dice ascending, so defining a D7 never
shuffles the presets a user knows the position of. The *results and notation* interleave by face
count instead — that ordering belongs to `DicePool.entries` and is what makes
`4D6 + 1D7 + 2D8` read correctly.

## Related docs

- [Feature PRD](../features/custom-dice.md)
- [Architecture](../architecture/custom-dice.md)
- [Testing](../testing/custom-dice.md)
