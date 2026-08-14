# Feature: Custom dice with an arbitrary number of faces

Issue [#4](https://github.com/emmanuel-h/DiceRoller/issues/4). Carried over from the
out-of-scope list of both earlier PRDs
([dice selection](dice-selection-and-roll.md), [multi-dice roll](multi-dice-roll.md)).

## Problem statement

The app ships six dice, because it ships artwork for six dice. Plenty of games want something
else — a d3 for a damage die, a d100 for a percentile roll, a d7 for a homebrew table — and a
user who needs one has no way to ask for it.

## Goals

- A user can define a die with any face count in **2–1000** and roll it alongside the presets.
- A custom die is a first-class member of the pool: it steps, notates, tallies, totals, sorts and
  persists exactly like a preset.
- Definitions survive process death; the pool they sit in does not, matching the existing rule.
- The six presets stay undeletable.

## User stories

- As a user, I want to add a die by typing its number of faces, so I can roll dice this app does
  not ship.
- As a user, I want my custom dice to still be there next time I open the app, so I do not
  re-create them every session.
- As a user, I want to remove a custom die I no longer need, and to recover from doing that by
  accident.

## Functional requirements

1. The pool selector's chip grid ends with an **add chip** that opens a creator dialog.
2. The creator takes a face count and refuses, with a specific reason, anything that is: not a
   whole number; outside 2–1000; already a preset (`6` → "D6 is already one of the standard
   dice"); already defined; or one die too many.
3. At most **3** custom dice exist at once. At the cap the add chip is *hidden*, not disabled.
4. A custom die has no artwork, so its chip and its result entries draw a
   **swatch-coloured badge** carrying the face count. It still follows the colour picker.
5. A custom chip carries an `×` badge in its corner that deletes the die immediately; a snackbar
   offers **Undo**, which restores both the definition and the pool count it had.
6. Deleting a die does not touch the roll log — past rolls of it stay, and keep rendering.
7. Ordering: the *selector* shows presets first, then custom dice ascending by face count. The
   *notation and result ladder* interleave everything by face count, so `4D6 + 1D7 + 2D8`.

## Out of scope

- Artwork for custom dice (a drawn polygon per face count).
- More than 3 custom dice, or naming them.
- Editing a die's face count in place — remove and re-add instead.
- Per-die modifiers ("+2"), which remain out of scope from the multi-dice PRD.

## What this cost the one-screen layout

Issue [#64](https://github.com/emmanuel-h/DiceRoller/issues/64) bought a scroll-free screen; the
add chip is a seventh grid cell and takes the grid from two rows to three. Measured on device: a
chip row is **98dp** and the third one costs **106dp** with its gap, against the ~16dp of slack
that layout had.

Consequences, all measured rather than estimated:

| | total fits without scrolling from |
|---|---|
| no history band | **740dp** (was ~640dp) |
| collapsed history band | **780dp** (was ~680dp) |

Below those heights the result band scrolls its last group and total into reach. **Every control
still fits at 640dp** — the swatch row, all chips, the history header, the Roll button and the
attribution — which is the guarantee that does not bend. The row is charged whether or not the
user ever defines a die, because the add chip is what makes the feature discoverable; the cap of
3 is what stops it ever becoming a fourth row.

## Related docs

- [Architecture](../architecture/custom-dice.md)
- [Design](../design/custom-dice.md)
- [Testing](../testing/custom-dice.md)
