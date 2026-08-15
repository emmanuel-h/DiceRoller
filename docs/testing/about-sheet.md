# Testing: About sheet (issue #66)

Covers moving the CC BY credit out of the main screen's bottom bar and into the About sheet. The
license is the reason this feature has tests at all: "the credit is still reachable" is a legal
requirement, not a nicety, so it is asserted from both sides — gone from the layout, present
behind the ⓘ.

## Unit — `DiceRollerViewModelTest`

| Test | Asserts |
|---|---|
| `givenNewViewModel_whenReadingState_thenTheAboutSheetIsClosed` | `isAboutVisible` starts false — the sheet is not a launch surface |
| `givenTheAboutSheetShown_whenItIsDismissed_thenItCloses` | `showAbout` / `dismissAbout` round-trip |
| `givenARolledPool_whenTheAboutSheetIsShown_thenTheResultAndPoolSurvive` | Opening the sheet clears neither the result nor the pool — reading a credit is not an action on the roll |

## Instrumented — `DiceRollerScreenTest`

| Test | Asserts |
|---|---|
| `givenTheScreenAtRest_whenDisplayed_thenTheAttributionFooterIsGone` | `ART_ATTRIBUTION` does not exist **anywhere** on the resting screen — a footer nudged a few dp up would still be the thing that was removed |
| `givenTheScreenAtRest_whenDisplayed_thenTheAboutButtonIsVisible` | The ⓘ is displayed and the sheet is not yet in the tree |
| `givenTheScreen_whenTheAboutButtonIsTapped_thenTheSheetShowsTheRequiredCredit` | One tap surfaces the exact required wording |
| `givenARolledPool_whenTheAboutButtonIsTapped_thenTheResultIsUntouched` | The end-to-end version of the ViewModel guarantee above |

The four **fit-on-screen** tests changed one assertion each: where they asserted the pinned
attribution line, they now assert `ABOUT_BUTTON_TAG` is displayed. The guarantee is unchanged in
kind — *every control survives at 360×640dp* — only its cast has: the credit is no longer a
control, and the button that reaches it is.

## Instrumented — `FantasyDiceArtUiTest`

| Test | Asserts |
|---|---|
| `givenTheScreen_whenTheAboutButtonIsTapped_thenTheArtworkAttributionIsVisible` | Replaces the old "attribution visible on screen" test; the ⓘ is visible at rest and the credit is one tap behind it |
| `givenTheAboutSheet_whenOpened_thenTheCreditIsOneLineBesideTheAppsOwnLicense` | The app's own license line is shown, and the artwork section is the credit line alone — neither `CC BY 4.0` nor a pack-name row exists separately |

## Not covered

- **The links actually opening.** `AboutSheet` takes `onOpenLink` precisely so this *could* be
  asserted without leaving the app, but no test drives it yet; the rows are asserted by their
  text. The `runCatching` wrapper around the handler is likewise untested — it exists so a device
  with no mail app cannot crash the app.
- **The sheet's own dismissal gestures** (swipe, scrim tap) — Material's, not ours.
- **`BuildConfig.VERSION_NAME` rendering.** The header is asserted nowhere; the version is
  read from the build rather than typed, which is the property that mattered.

## Execution status

`./gradlew test` and `./gradlew lint` pass, and `./gradlew assembleDebugAndroidTest` compiles the
instrumented suite. **The instrumented tests have not been run** — no device or emulator was
available in the environment this was implemented in (`adb` is not installed). Whoever next has a
device should run `./gradlew connectedAndroidTest` before treating the About coverage as verified.
