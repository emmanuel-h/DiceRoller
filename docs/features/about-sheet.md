# Feature: About sheet (the credit leaves the footer)

Closes [#66](https://github.com/emmanuel-h/DiceRoller/issues/66).

## What changed

The CC BY credit line — `Dice art by Aeynit · CC BY 4.0` — no longer sits pinned under the Roll
button. It moved into an **About bottom sheet**, opened by an info button pinned at the trailing
end of the color swatch row. The bottom bar now holds the Roll button alone.

```
  before                          after
  ────────────────────────        ────────────────────────
  ┌────────────────────┐          ┌────────────────────┐
  │ ●●●●●●●●●●●● →     │          │ ●●●●●●●●●● →   (ⓘ) │
  │ [D4 ] [D6 ] [D8 ]  │          │ [D4 ] [D6 ] [D8 ]  │
  │ [D10] [D12] [D20]  │          │ [D10] [D12] [D20]  │
  │ [D3 ] [ +  ]       │          │ [D3 ] [ +  ]       │
  │  ⚄ 6  ⚄ 4×2        │          │  ⚄ 6  ⚄ 4×2        │
  │        Total 40    │          │  ⚅ 7  ⚅ 2          │
  │ ▸ Recent (3)       │          │        Total 40    │
  ├────────────────────┤          │ ▸ Recent (3)       │
  │ [ Roll 4D6 + 2D8 ] │          ├────────────────────┤
  │ Dice art by Aeynit │          │ [ Roll 4D6 + 2D8 ] │
  └────────────────────┘          └────────────────────┘

  tapping ⓘ:
  ┌────────────────────┐
  │                    │
  │ ╭────────────────╮ │
  │ │      ────      │ │
  │ │ About          │ │
  │ │ DiceRoller 1.0 │ │
  │ │                │ │
  │ │ LICENSE        │ │
  │ │ © 2026 Mandar… │ │
  │ │  · Apache 2.0  │ │
  │ │                │ │
  │ │ ARTWORK        │ │
  │ │ Dice art by    │ │
  │ │  Aeynit·CC BY… │ │
  │ │                │ │
  │ │ CONTACT        │ │
  │ │ mandarinetech… │ │
  │ ╰────────────────╯ │
  └────────────────────┘
```

## Why the footer had to go, and why the credit could not

Two separate facts, and the design is what reconciles them:

- The footer cost every screen a permanent band for a line read once, directly beneath — and
  competing with — the screen's one primary action.
- CC BY 4.0 requires the attribution to be **visible to users**, not permanently on screen. One
  tap from the main screen, behind a control that is visible at rest, satisfies it. Deleting the
  line does not, which is why this was a move rather than a removal. See
  [third-party assets](../licenses/third-party-assets.md).

## Why the swatch row's trailing edge

There was no top app bar to hang an info action on — the title was judged not worth its ~64dp back
in issue #64, and that verdict stands. The swatch row is the one band whose height is already set
by something else: its twelve 44dp touch targets. An icon button dropped at its end is therefore
**free vertically**, which is the whole point — a placement that spent 22dp to save 28dp would
have been theatre.

The button sits *outside* `DiceColorSwatchRow`'s horizontal scroll rather than inside it. A credit
the license requires to be discoverable cannot live somewhere the user has to scroll sideways to
find.

## What the sheet holds

| Section | Content |
|---|---|
| Header | `About`, then the app name and `BuildConfig.VERSION_NAME`, so the version on screen cannot drift from the one declared in `build.gradle.kts` |
| License | `© 2026 Mandarine Tech · Apache License 2.0`, the license name linking to [its text](https://www.apache.org/licenses/LICENSE-2.0) and matching the new [`LICENSE.md`](../../LICENSE.md) |
| Artwork | The exact required credit line and nothing else — its trailing `CC BY 4.0` *is* the link to the deed |
| Contact | `mandarinetech.dev@gmail.com`, opened as a `mailto:` |

Every link rides on words that had to be there anyway. `Dice art by Aeynit · CC BY 4.0` already
names the license, so the link goes on those five characters rather than on a second row repeating
them; likewise the copyright and the license name are one line.

The artwork section is that credit line alone. Two rows were tried and dropped — an "OpenGameArt
listing" link and a "Fantasy Dices Pack by Aeynit" title — because the credit already names the
author and the license, which is the whole of what CC BY 4.0 asks a user-facing surface to carry.
Which pack it was and where it came from are recorded in
[third-party-assets.md](../licenses/third-party-assets.md), for a re-verification that is a desk
job rather than a phone one.

A modal bottom sheet rather than a dialog: three sections scroll comfortably in a sheet and
shorten with the viewport, where a fixed dialog box would clip the contact row off the bottom on a
short screen.

## Structure

| Layer | Type | Responsibility |
|---|---|---|
| `presentation/component` | `AboutIconButton` | The pinned ⓘ; owns `ABOUT_BUTTON_TAG` |
| `presentation/component` | `AboutSheet` | The sheet, its copy, and `ART_ATTRIBUTION` — which moved here from `MainActivity` |
| `presentation` | `DiceRollerUiState.isAboutVisible` | Session-only visibility, like the custom-die creator |
| `presentation` | `DiceRollerViewModel.showAbout` / `dismissAbout` | Visibility only — they touch neither the pool, the result nor the log |
| `res/drawable` | `ic_info_outline.xml` | Hand-drawn, like `ic_casino` and `ic_expand_more`: not worth `material-icons-extended` |

`AboutSheet` takes `onOpenLink` (defaulting to the platform `LocalUriHandler`) so tests can assert
which URI a row opens without leaving the app, and wraps the call in `runCatching` — a device with
no mail app is not a reason to crash a dice roller. Inline links route through it via
`LinkAnnotation.Url`'s `linkInteractionListener`, which replaces the default open-the-URL
behaviour; full-row links like the contact address stay a clickable `Text` with a 40dp minimum
height, since an inline link's tap target is only its own glyph box.

## What the freed space bought

The footer was a `labelSmall` line plus its 4dp gap, and the bar's bottom padding grew from 8dp to
12dp to sit right without it: **~24dp net**, and it goes to the weighted result/history band with
no code change, because that band already absorbs whatever the fixed bands leave.

That walks back part of what custom dice charged: the third chip row cost 106dp against ~16dp of
slack, pushing "the total fits outright" from ~640dp to ~740dp (no history) and ~680dp to ~780dp
(collapsed band). This gives ~24dp of it back. The guarantee that never bent — **every control
fits at 640dp** — is unchanged, with the ⓘ now among the controls asserted.

## Out of scope

- A settings screen, or anything else behind the ⓘ. It is an About sheet, not a menu.
- Showing the license texts in-app; the sheet links out to them.
- A top app bar. Still not worth its height.

## Related docs

- [Third-party assets](../licenses/third-party-assets.md)
- [Custom dice](custom-dice.md) — where the vertical budget this returns was spent
- [Roll history](roll-history.md) — the other band sharing the freed weight
