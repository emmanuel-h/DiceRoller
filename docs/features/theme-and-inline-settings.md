# Feature: a light/dark setting, inline pickers, and a leaner settings sheet

## What changed

The settings sheet gained a **Theme** setting, and both pickers lost their *System default* row and
became inline. About, the app's own licence and the contact address are gone from the sheet.

```
  before                     after
  ─────────────────────      ──────────────────────────
  Settings                   Settings

  LANGUAGE                   LANGUAGE
  (•) System                 (•) English   ( ) Français
  ( ) English
  ( ) Français               THEME
                             (•) Light     ( ) Dark
  ROLLING
  Shake to roll     [on]     ROLLING
  Sensitivity                Shake to roll          [on]
  Less ──●──── More          Sensitivity
                             Less ──●──── More
  ABOUT / LICENSE /
  ARTWORK / CONTACT          ARTWORK
                             Dice art by … CC BY 4.0
```

The **Artwork** credit stays: the dice art is CC BY 4.0 and the licence requires the credit to be
reachable in the app (see `docs/licenses/third-party-assets.md`).

## No "System" option, but the system is still the default

Neither picker offers *follow the device*. Instead, an install that has never picked reads the
device once and stores nothing:

- **Language** — the device's language if the app ships it, otherwise **English**
  (`AppLanguage.forDevice`), which is also what Android's resource fallback would show.
- **Theme** — **Dark** if the device is in dark mode, otherwise **Light** (`AppTheme.forDevice`).

The resolution lives in `data/DeviceDefaults.kt` (`Context.deviceLanguage()`,
`Context.deviceTheme()`), used both by the DataStore stores — an absent key reads back as the
device's value — and by `DiceRollerViewModel.factory`, which seeds `initialLanguage` /
`initialTheme` so the first frame is right before DataStore answers. Once the user picks, the
choice is written and the device no longer matters. An install upgraded from a build that stored
*System default* has no key, so it lands on the same device default.

## How the theme is applied

Like the language, the theme is ViewModel state (`DiceRollerUiState.theme`, `selectTheme`), never
the platform's (`AppCompatDelegate.setDefaultNightMode` would recreate the activity, the same
blink `ProvideAppLanguage` exists to avoid). `MainActivity` builds the ViewModel *outside*
`DiceRollerTheme` and passes `darkTheme = uiState.theme.isDark`. Selecting a theme never clears
the result or the log, by the same rule as colour and language.

Two windows draw system-bar icons, and both must follow the app rather than the device:

- the activity: `enableEdgeToEdge` is re-applied with `SystemBarStyle.auto { darkTheme }` whenever
  the theme flips;
- the settings sheet, which is its own window: `ModalBottomSheetProperties(isAppearanceLight…)`
  derived from the colour scheme's surface luminance. Without it, dark mode on a light device
  showed dark status-bar icons on leather while the sheet was open (observed on the emulator).

The window background (`values-night/themes.xml`) still follows the device; it only shows for the
frame before Compose's first draw.

## Classes

| Layer | Class | Role |
|---|---|---|
| `presentation` | `AppLanguage` | Now just `English`, `French`; `ofTag` returns null for unknown tags, `forDevice` falls back to English |
| `presentation` | `AppTheme` | `Light`, `Dark`; persisted by enum `name` |
| `presentation` | `AppThemeStore` / `InMemoryAppThemeStore` | Persistence seam, as for every other store |
| `data` | `DataStoreAppThemeStore` | Key `app_theme`, sharing `diceDataStore` |
| `data` | `DeviceDefaults.kt` | The device's language and dark mode |
| `component` | `SettingsSheet` → `InlineRadioGroup` | One `selectableGroup` row per picker; tags `language-option-<tag>`, `theme-option-<light|dark>` |

`buildConfig` was switched off in `app/build.gradle.kts`, since the version line it existed for is
gone.
