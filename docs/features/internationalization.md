# Feature: internationalization (string resources + a French translation)

Closes [#68](https://github.com/emmanuel-h/DiceRoller/issues/68).

## What changed

Every user-facing string — visible labels *and* `contentDescription`s — now comes from
`app/src/main/res/values/strings.xml` instead of a Kotlin literal. `values-fr/strings.xml` ships a
complete French translation. With no choice made, the app follows the device locale, and Android's
own resource resolution turns a device language this build does not ship into English.

> An in-app picker landed just after, letting the user run the app in a language other than the
> device's — along with the ⓘ becoming a ⚙. It does **not** use `locales_config.xml`, which was
> removed with it; see [language-and-settings.md](language-and-settings.md) for why. This document
> covers the strings; that one covers the control over them.

The layout is unchanged. What changes is what the same layout says:

```
  en                              fr
  ────────────────────────        ────────────────────────
  ┌────────────────────┐          ┌────────────────────┐
  │ ●●●●●●●●●● →   (⚙) │          │ ●●●●●●●●●● →   (⚙) │
  │ [D4 ] [D6 ] [D8 ]  │          │ [D4 ] [D6 ] [D8 ]  │
  │ [D10] [D12] [D20]  │          │ [D10] [D12] [D20]  │
  │ [D3 ] [Custom]     │          │ [D3 ] [Perso ]     │
  │  ⚄ 6  ⚄ 4×2        │          │  ⚄ 6  ⚄ 4×2        │
  │        Total 40    │          │        Total 40    │
  │ ▸ Recent (3)       │          │ ▸ Récents (3)      │
  ├────────────────────┤          ├────────────────────┤
  │ ✕ [ Roll 4D6+2D8 ] │          │ ✕ [Lancer 4D6+2D8] │
  └────────────────────┘          └────────────────────┘

  empty state:
  "Add dice above to build       "Ajoutez des dés ci-dessus
   your pool."                    pour composer votre réserve."
```

Note what did **not** move: `D6`, `4D6 + 2D8` and `4×D6` read identically in both columns. Those
are dice notation, not English.

## Three rules the string set follows

### 1. Dice notation is not translatable text

`D6` is what a die is called in every language this app will ever ship in, so `die_label`,
`number`, `multiplier` and `result_group_header` are all `translatable="false"`. They are
*resources* rather than concatenations only so the face count lands in the string as a formatted
argument — which is what lets the platform shape digits per locale and lay the label out correctly
under RTL.

The domain's `DieType.label` stays the value used wherever the string is not shown to a person:
test tags, persisted keys, and the pool notation `poolNotation` builds. `dieLabel(die)` in
`presentation/component/DieLabel.kt` is the composable used everywhere it *is*.

### 2. Counts go through `plurals`, never through `if (n == 1)`

Eight quantity strings replace the hand-rolled singular/plural switches: `"1 D6 die"` vs
`"4 D6 dice"`, `"rolled 2 times"`, `"1 roll"` vs `"2 rolls"`, and the relative-time units.

French carries a `many` item on every one of them — the CLDR category for values like 1 000 000,
where the noun takes *de*. Unreachable in a dice roller; included so the set is complete for the
language rather than for this app's expected inputs.

The English number words the accessibility summary used to spell out (`"two 4s"`) went with this:
they were 21 hard-coded English strings, and a screen reader reads `2` as "two" anyway.

### 3. `DiceColor.name` is the identity, `labelRes` is the display name

`DiceColor` used to carry `label = "amethyst"`, doing double duty as the word shown to users *and*
as an identity. Translating it in place would have broken the stored colour: `DataStoreDiceColorStore`
persists by enum `name`, and a colour saved yesterday must still resolve after the phone changes
language.

So the enum now carries `@StringRes val labelRes` and nothing else changed hands. `name` still
backs persistence and the artwork test tags; `labelRes` is free to become `améthyste`.

## Where a string gets resolved

Most composables call `stringResource` directly. Two cases could not:

**Pure functions that decide on text.** `rollButtonLabel`, `relativeTimeLabel` and
`validateCustomFaces` are plain Kotlin covered by fast JVM unit tests. Making them `@Composable`
would have pushed ~300 lines of those tests onto a device; making them take a `Resources` would
have made them untestable off it. They return a **`UiText`** instead — a resource id plus its
arguments — resolved by `.resolve()` at the display site. The unit tests then assert *which*
string was chosen and with what arguments, which is the decision those functions actually make,
and no translator can break them.

**Sentences assembled from a variable number of clauses.** The roll summary announced to screen
readers, and a history entry's spoken sentence, loop over the result's groups; a composable cannot
do resource lookups inside that loop. Both take a `Resources` read once by their caller from
`LocalResources.current`. Every clause is still a resource, including the `" and "` that joins pool
entries.

## The attribution line has a constraint translators must keep

`about_art_attribution` is required by CC BY 4.0. `Aeynit` and `CC BY 4.0` must appear verbatim —
and the line must still **end** with `CC BY 4.0`, because `SettingsSheet` splits that suffix off to
turn it into the link to the licence deed. A translation that ends differently renders the licence
name twice rather than losing the link. Both `strings.xml` files carry that note for translators.
See [third-party assets](../licenses/third-party-assets.md).

## Tests

Instrumented tests no longer spell English sentences out. `androidTest/.../TestStrings.kt` exposes
`str`, `plural`, `dieNotation`, `rollLabel` and `groupHeader`, all resolved against the *target*
context — the same resources the composables read. The suite therefore passes on a device set to
French, and a translation that broke a screen shows up as a red test rather than as a bug report.
What the assertions still pin is the structure: that the button carries the pool's notation, that
the header counts the entries, that a value and its multiplier land in the same row.

Two coverage notes:

- `DiceColorTest` (JVM) can only see that the twelve variants point at twelve *different* string
  resources. That each resolves to a real, distinct, lowercase colour word is a fact about the
  shipped XML, so it is asserted in `FantasyDiceArtUiTest` against whatever locale the device runs
  — which is what catches a missing or copy-pasted French translation.
- `RelativeTimeTest` pins the saturation of the elapsed-units count: the arithmetic is in `Long`
  milliseconds but a quantity string selects on an `Int`.

Run the suite in French to check the translation end to end:

```bash
adb shell cmd locale set-app-locales fr.mandarine.diceroller --locales fr-FR
./gradlew connectedAndroidTest
adb shell cmd locale set-app-locales fr.mandarine.diceroller --locales ""
```

That sets the *framework's* per-app locale, which the app does not manage but does follow when the
in-app choice is `System default` — so this run also covers the first-launch path.

## Adding a locale

1. Add `app/src/main/res/values-<lang>/strings.xml`, translating everything `values/` does not mark
   `translatable="false"`.
2. Add a `language_<lang>` string (`translatable="false"`, the language's own endonym) and an
   `AppLanguage` entry pointing at it, so the in-app picker offers it. `AppLanguageResourcesTest`
   fails if you do one without the other. See
   [language-and-settings.md](language-and-settings.md).
3. Run `./gradlew lintDebug` — `MissingTranslation` and `MissingQuantity` are what tell you the set
   is incomplete.
