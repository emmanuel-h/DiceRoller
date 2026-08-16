# Third-party assets

Every non-original asset bundled in the app is listed here with its license and
the credit line required to satisfy it.

## Fantasy Dices Pack — Aeynit

| | |
|---|---|
| **Asset** | Fantasy Dices Pack (72 dice renders: 12 colors × d4/d6/d8/d10/d12/d20) |
| **Author** | Aeynit |
| **License** | [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/) |
| **Source** | https://opengameart.org/content/fantasy-dices-pack |
| **Author pages** | https://aeynit.itch.io/ · https://opengameart.org/users/aeynit |
| **Published** | 23 October 2020 |
| **Verified** | 8 August 2026 |

### Credit line

> Dice art by Aeynit · CC BY 4.0

This string is rendered in-app in the **settings sheet**
(`R.string.about_art_attribution`, shown by `presentation/component/SettingsSheet.kt`),
reached by the ⚙ button pinned at the trailing end of the color swatch row. It
sat pinned under the Roll button until
[#66](https://github.com/emmanuel-h/DiceRoller/issues/66) moved it — see
[`docs/features/about-sheet.md`](../features/about-sheet.md). That sheet was
titled *About* behind an ⓘ until it gained a language picker and became the
settings sheet — see
[`docs/features/language-and-settings.md`](../features/language-and-settings.md).

It became a string resource in
[#68](https://github.com/emmanuel-h/DiceRoller/issues/68), so it is now
translated: French reads *Illustrations des dés par Aeynit · CC BY 4.0*. Two
things are fixed in every locale and are noted for translators in both
`strings.xml` files — the author name **Aeynit** and the licence name
**CC BY 4.0** appear verbatim, and the line must still **end** with
`CC BY 4.0`, because the sheet splits that suffix off to turn it into the link
to the licence deed. Only the surrounding wording is translatable.

CC BY 4.0 requires attribution to be visible to users, not permanently on
screen, so one tap from a control that is visible at rest satisfies it. What
must not happen is the string being removed, or its entry point becoming
something a user would not find: `DiceRollerScreenTest` and
`FantasyDiceArtUiTest` both assert the ⚙ is on screen at rest and that tapping
it shows this exact wording.

The trailing `CC BY 4.0` of that line is itself the link to the deed
(<https://creativecommons.org/licenses/by/4.0/>), so the license is one tap from
the credit. That line is the *whole* of what the app shows about the artwork:
neither the pack name nor the OpenGameArt listing appears in-app, since the
credit already carries the author and the license the deed requires. Both are
recorded above as the provenance to re-verify from, which is a desk job rather
than a phone one.

The app's own code is under the Apache License 2.0 ([`LICENSE.md`](../../LICENSE.md)),
which does **not** cover this artwork; the sheet shows both so neither is
mistaken for the other.

### Licensing note

The `ReadMe.txt` shipped inside the downloadable pack states no license terms —
it only lists the author's contact details. The license was confirmed from the
OpenGameArt listing above, which records the submission as **CC BY 4.0** and
states the art may be used in any project, commercial or not. Do not rely on the
bundled `ReadMe.txt` alone when re-verifying.

### What is committed

Only the derived WebP drawables are committed, under
`app/src/main/res/drawable-nodpi/dice_<color>_<die>.webp`. The pack's original
PNGs and the 24 MB `dices.psd` are **not** in the repository.

Derivation from the original PNGs (a permitted CC BY 4.0 adaptation):

- scaled so the longest edge is 320 px, Lanczos resampling
- encoded as lossy WebP, quality 85, method 6, alpha preserved
- 72 files, 1,076 KiB total

The `color1`…`color12` directory names from the pack map to readable names in
`DiceColor`:

| Pack dir | Name | Swatch | Pack dir | Name | Swatch |
|---|---|---|---|---|---|
| color1 | amethyst | `#875E95` | color7 | smoke | `#4D4851` |
| color2 | amber | `#E18744` | color8 | jade | `#599B89` |
| color3 | sapphire | `#4C86B3` | color9 | moss | `#859A59` |
| color4 | ruby | `#B14B58` | color10 | bronze | `#AA7F51` |
| color5 | gold | `#A59654` | color11 | rose | `#A4565D` |
| color6 | orchid | `#B34997` | color12 | indigo | `#5F6095` |
