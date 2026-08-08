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

This string is rendered in-app beneath the color picker on the main screen
(`ART_ATTRIBUTION` in `MainActivity.kt`). CC BY 4.0 requires attribution to be
visible to users, so it must not be removed when the screen is restyled.

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
