# Feature: an in-app language picker (the About sheet becomes Settings)

Follow-up to [#68](https://github.com/emmanuel-h/DiceRoller/issues/68), which shipped the French
translation but left the app following the device locale with no way to choose otherwise.

## What changed

The ⓘ beside the colour swatch row is now a **⚙**, and the sheet behind it is titled **Settings**
with a **Language** picker at the top. About is a section inside it rather than the headline.

```
  before (ⓘ)            after (⚙)
  ──────────────        ──────────────
  ●●●●●●●●●● →  (ⓘ)     ●●●●●●●●●● →  (⚙)

 ╭────────────────╮     ╭────────────────╮
 │      ────      │     │      ────      │
 │ About          │     │ Settings       │
 │ DiceRoller 1.0 │     │                │
 │                │     │ LANGUAGE       │
 │                │     │  ◉ System def… │
 │                │     │  ○ English     │
 │                │     │  ○ Français    │
 │                │     │                │
 │                │     │ ABOUT          │
 │                │     │ DiceRoller 1.0 │
 │                │     │                │
 │ LICENSE        │     │ LICENSE        │
 │ © 2026 Manda…  │     │ © 2026 Manda…  │
 │                │     │                │
 │ ARTWORK        │     │ ARTWORK        │
 │ … CC BY 4.0    │     │ … CC BY 4.0    │
 │                │     │                │
 │ CONTACT        │     │ CONTACT        │
 ╰────────────────╯     ╰────────────────╯
```

**A phone set to English can run DiceRoller in French.** Picking an option swaps every string in
one recomposition — no relaunch, no flash — and the pool, result and log are untouched. The choice
sticks across launches, and `System default` is the way back: it *removes* the override rather
than pinning the device's current language, so a user who later changes their phone's language
sees the app follow again.

On first launch nothing is stored, so the app is `System default` and reads the device's language.
A device set to a language this build does not ship falls through to English, which is what
`values/` holds.

## Why the language is not applied by the platform

Both platform mechanisms for a per-app language — `LocaleManager` on Android 13+, the
`AppCompatDelegate` backport below it — work by changing the app's *configuration*. Android applies
a configuration change to a running app by **relaunching the activity**, and that is what the blink
was: measured at **148ms of a fully black screen** between destroy and the first frame after
create.

That is not something the app can opt out of. `android:configChanges="locale|layoutDirection"` was
tried and made no difference, verified by driving `LocaleManager` directly with AppCompat out of
the picture: the relaunch happens either way.

So the configuration is left alone and the **composition** is localized instead.
`ProvideAppLanguage` in `MainActivity` supplies a `Context`, `Resources`, `Configuration` and
`LayoutDirection` built for the chosen language; `stringResource` reads them, and switching costs
one recomposition. Measured on the same recording setup, frame luma never leaves the 33–34 band and
the event log shows zero destroy/create.

| | before | after |
|---|---|---|
| black frames | 5 (148ms) | **0** |
| activity destroy/create | 1 | **0** |
| `androidx.appcompat` | required | **not used** |

### What that costs

- **The app no longer appears in Settings › DiceRoller › Language.** `res/xml/locales_config.xml`
  is gone, because keeping it would advertise a second control over the same setting that this app
  now deliberately ignores.
- **`Locale.getDefault()` stays on the device locale.** Only the composition is localized. That is
  safe here because nothing outside it shows the user a string — the launcher label is
  untranslated, and there are no toasts or notifications — and resource formatting (`%1$d`) runs
  through the `Resources` provided above, so it follows the choice.
- **Language splits had to be turned off.** `bundle { language { enableSplit = false } }`. Play's
  default ships each language as a separate split and installs only the one matching the device
  locale, which would have made the picker a lie on the store build: an English phone would have no
  `values-fr` on disk. Android lint flags exactly this (`AppBundleLocaleChanges`) and it is why the
  setting is there. Two short string tables in every install is nothing next to 72 dice renders.

## Why a radio group, and why the languages are not translated

Three options fit in the space a `Language ›` row would have taken, so the user can see what the
app *can* be without opening anything.

The named languages are listed in their own endonym — `English`, `Français` — and both are
`translatable="false"`. A language list is read by someone who does not yet read the language they
are looking at; translating "French" into "Französisch" would help exactly the people who did not
need the row. Only `System default` is translated, because it names a behaviour rather than a
language.

The option already in force is **inert** rather than a no-op: re-picking it would write the same
value and recompose to an identical screen.

## Why the icon and the title had to change together

An ⓘ promises *here is something to read*. Once the sheet holds something to **change**, that
promise is wrong, and a user looking for a language setting will not think to tap an info glyph.
The gear and the *Settings* title are the same decision made twice — and demoting About to a
section is the third part of it, since a sheet titled *About* containing a control reads as a
mistake.

The credit's discoverability, which is a licence requirement rather than a preference, is
unaffected: a gear at the end of the swatch row is at least as findable as the ⓘ it replaced, and
it is in the same place. See [third-party assets](../licenses/third-party-assets.md).

## Structure

| Layer | Type | Responsibility |
|---|---|---|
| `presentation` | `AppLanguage` | The options: `System`, `English`, `French`. `ofTag` resolves whatever was stored — including a region-qualified `fr-FR`, or a locale an older build shipped — back to one of them, falling back to `System` rather than throwing |
| `presentation/component` | `SettingsIconButton` | The pinned ⚙; owns `SETTINGS_BUTTON_TAG` |
| `presentation/component` | `SettingsSheet` | The sheet and its sections; owns `SETTINGS_SHEET_TAG` and `languageOptionTestTag` |
| `presentation/component` | `LanguagePicker` | The radio group, a real `selectableGroup` so it announces as one control |
| `MainActivity` | `ProvideAppLanguage` | Supplies the localized `Context`, `Resources`, `Configuration` and `LayoutDirection` to the whole composition. All four, each for its own reason — and `LocalLayoutDirection` especially, because it does not follow a configuration on its own, so an RTL language would otherwise lay out left-to-right |
| `presentation` | `AppLanguageStore` / `DataStoreAppLanguageStore` | Persists the choice as a BCP-47 tag, `System` being the absence of the key. `InMemoryAppLanguageStore` is the unit-test double, like every other store here |
| `presentation` | `DiceRollerUiState.language`, `DiceRollerViewModel.selectLanguage` | The choice is ordinary UI state, restored on creation and written back on change — exactly like the dice colour |
| `presentation` | `DiceRollerUiState.isSettingsVisible` | Session-only visibility (was `isAboutVisible`) |
| `presentation` | `DiceRollerViewModel.showSettings` / `dismissSettings` | Visibility only — they touch neither the pool, the result nor the log |
| `res/drawable` | `ic_settings.xml` | Hand-drawn, like the other icons: not worth `material-icons-extended` |

`selectLanguage` deliberately does **not** clear the roll on screen, for the same reason
`selectColor` does not: changing how the screen is *written* is not a change to what Roll would
produce. It can afford that guarantee only because the choice never leaves the ViewModel — the
platform's way of applying a locale would have taken the whole activity down with it.

`SettingsSheet` stays a pure function of `selectedLanguage` and reports picks through
`onSelectLanguage`, the same seam `onOpenLink` already used, so it can be tested without a
ViewModel.

## Tests

- `AppLanguageTest` (JVM) — the option set, and `ofTag` against a bare tag, a region-qualified one,
  an unshipped one, and nothing at all.
- `AppLanguageResourcesTest` — that every language the picker offers actually has strings behind
  it, and that an unshipped device language falls back to English. `AppLanguage` and the
  `values-<lang>/` folders are two lists that must agree, and nothing else makes them: adding
  `AppLanguage.German` without a `values-de/` compiles, ships, and silently shows English to
  anyone who picks it, because missing resources fall back rather than fail.
- `DiceRollerViewModelTest` — the choice restored on creation, written through to the store,
  surviving a new ViewModel over the same store, `System` clearing it, and a roll surviving a
  language change.
- `SettingsSheetTest` — every shipped language offered, the endonyms unlocalized, the current one
  selected, picking another requesting exactly it, `System default` requesting the *absence* of a
  tag, the in-force option inert, and the picker sitting above About.
- The whole instrumented suite runs a second time with the app's locale forced to `fr-FR`, which is
  what proves a translation cannot silently break a screen. See
  [internationalization.md](internationalization.md).

## Out of scope

- Any second setting. The sheet is a settings surface now, but it has exactly one thing to set.
- A full settings *screen* with navigation. The app is deliberately single-screen; a modal sheet
  still fits one picker and four read-only sections without one.
- Locales beyond `en` and `fr`. Adding one is three files — see
  [internationalization.md](internationalization.md#adding-a-locale) — plus an `AppLanguage` entry.

## Related docs

- [Internationalization](internationalization.md) — the translation this gives a control over
- [About sheet](about-sheet.md) — why the sheet exists and why its entry point hangs off the
  swatch row, both still true
- [Third-party assets](../licenses/third-party-assets.md)
