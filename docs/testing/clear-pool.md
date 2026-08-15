# Testing: emptying the dice pool (issue #67, and the roll that ends a run)

Covers the ✕ in the roll bar that puts every die count back to 0, and the follow-up decision that
`rollDice` does the same thing on its way out. The tests concentrate on what could quietly go
wrong: what emptying touches beyond the counts, whether the ✕ is offered when it would do nothing,
whether the result of a roll survives the roll's own emptying, and whether sharing the roll bar
cost the Roll button its label on a narrow screen.

## Unit — `DiceRollerViewModelTest`, the ✕

| Test | Asserts |
|---|---|
| `givenAMixedPool_whenCleared_thenEveryCountIsBackToZero` | Three die types at different counts all land on 0 in one action |
| `givenAMixedPool_whenCleared_thenCanRollIsFalse` | The state the UI reads for "there is a pool" flips, which is also what hides the button |
| `givenARolledPool_whenCleared_thenTheResultOfThatRollSurvives` | The ✕ never takes away a ladder it does not conflict with — the roll already emptied the pool, so there is nothing to clear |
| `givenAnUnrolledPoolAfterAPreviousRoll_whenCleared_thenThatPreviousResultGoesToo` | The state the ✕ exists for: a pool queued and abandoned before rolling |
| `givenCustomDiceInThePool_whenCleared_thenTheyKeepTheirDefinitionsAndZeroedEntries` | Clearing zeroes counts, it does not undefine dice: `customDice`, `dieTypes` and the D7's pool *key* all survive at 0 — the selector's "every visible chip has a pool entry" invariant |
| `givenPastRolls_whenThePoolIsCleared_thenTheLogIsUntouched` | The log records what *was* rolled; emptying the pool afterwards cannot rewrite it |
| `givenAnEmptyPool_whenCleared_thenTheStateIsUntouched` | `assertSame` — an already-empty pool is a true no-op, not a same-valued copy, matching `updateCount`'s "no change, no new state" rule |
| `givenAClearedPool_whenADieIsIncrementedAgain_thenTheCountResumesFromZero` | Nothing is left behind that a later increment would resume from |
| `givenAChosenColor_whenThePoolIsCleared_thenTheColorSurvives` | Color is cosmetic and orthogonal — clearing a pool is not a reason to lose one |

## Unit — `DiceRollerViewModelTest`, the roll that ends a run

| Test | Asserts |
|---|---|
| `givenAMixedPool_whenRolled_thenEveryCountIsBackToZero` | The whole point: the selector is clean after a roll, and `canRoll` is false |
| `givenAPool_whenRolled_thenTheResultOfThatRollSurvivesTheEmptying` | The single exception to "a pool change clears the result" |
| `givenAJustRolledPool_whenRollingAgainWithoutQueueing_thenNothingHappens` | `assertSame` on the state and a log of exactly one entry — the accepted cost, pinned so it cannot regress into a double-record |
| `givenACustomDieInThePool_whenRolled_thenItsChipKeepsItsZeroedEntry` | Definitions outlive a roll; only counts are emptied |
| `givenAnyRoll_whenTheNextPoolIsQueued_thenAResultNeverCoexistsWithANonEmptyPool` | The invariant the auto-clear establishes, asserted end to end: result present with an empty pool, then gone the moment a die is queued |

Four older cases were reworked rather than deleted, because a roll no longer leaves a pool behind
for them to act on: the decrement-clears-the-result case now queues the next pool first
(`givenRollPerformed_whenQueueingTheNextPool…`), the two history-cap loops re-queue a die before
each roll, and the custom-die removal cases queue their die again after rolling. One was dropped
outright — "incrementing at the cap preserves the result" needed a result and a maxed-out count at
the same time, which the new invariant makes impossible; the no-op-does-not-clear rule it guarded
is still held by its decrement-at-floor twin.

## Instrumented — `DiceRollerScreenTest`

| Test | Asserts |
|---|---|
| `givenEmptyPool_whenScreenIsDisplayed_thenTheClearButtonIsAbsent` | `assertDoesNotExist`, not `assertIsNotEnabled` — hidden was the chosen behaviour |
| `givenANonEmptyPool_whenScreenIsDisplayed_thenTheClearButtonIsVisibleBesideRoll` | Both controls coexist in the bar; Roll is still enabled and still names the pool |
| `givenAMixedPool_whenTheClearButtonIsTapped_thenEveryChipReturnsToZero` | End to end through a real ViewModel: all six preset chips read `0` after one tap |
| `givenAnUnrolledPool_whenTheClearButtonIsTapped_thenTheEmptyStateAndTheButtonItselfAreGone` | The empty-pool caption is back, Roll is disabled with its placeholder, and the ✕ has removed itself |
| `givenAPool_whenItIsRolled_thenTheCountsResetAndTheClearButtonGoesWithThem` | The roll's own emptying, from the UI: every chip at `0`, the bar back to `Add dice to roll`, the ✕ gone — and the ladder (`3×D6`) still up |
| `givenAClearedPool_whenDiceAreQueuedAgain_thenTheBarComesBackWithThem` | The round trip — which is the argument for not confirming the action |

`givenTwoRollsAreMade_whenTheScreenUpdates_thenTheHistoryCountGrows` and
`givenARolledPool_whenTheAboutButtonIsTapped_thenTheResultIsUntouched` both had to queue a die
again after their first roll; the second now reads the credit while the *next* pool is being built,
which is the only state in which "the pool survives an unrelated action" is still observable.

## Instrumented — `CustomDiceUiTest`

| Test | Asserts |
|---|---|
| `givenACustomDieInThePool_whenThePoolIsCleared_thenItsChipStaysAtZero` | The ViewModel guarantee, driven through the UI: a D7's count goes to 0 but its chip stays on screen. Only the chip's own `×` badge removes a definition — and that one offers an undo |

## The fit guard

`givenACollapsedHistoryBandOnTheShortestViewport_whenScreenIsDisplayed_thenEveryControlSurvives`
gained one assertion: `CLEAR_POOL_BUTTON_TAG` is displayed at **360×640dp**, the harshest bound the
suite holds. That is the test that would catch the one regression this placement risks — the ✕
takes 52dp of width from the Roll button, and on the narrowest supported screen the label
`Roll 4D6 + 4D8 + 4D20` still has to fit beside it. It does, with room to spare.

The other fit tests inherit the check for free: they all use `realisticRolledState`, whose pool is
non-empty, so the ✕ is present in every one of them.

## Not covered

- **Long roll labels wrapping.** A pool dense enough to wrap the Roll button's label wrapped
  before this change too; the ✕ makes it 52dp likelier, and no test pins the threshold.
- **The button's outlined-vs-filled treatment.** Asserted by eye and by preview, not by test — a
  screenshot test would be the only way, and the suite has none.
- **Talkback ordering** in the roll bar. The button carries a content description
  (`Clear the dice pool`); where it lands in the traversal order is Compose's.

## Execution status

`./gradlew testDebugUnitTest` and `./gradlew lint` pass. `./gradlew connectedDebugAndroidTest` was
run on a `Medium_Phone_API_36.1` emulator (API 36): **133 tests, 0 failures**. The behaviour was
also confirmed by hand on that emulator — empty pool with no ✕, a `4D6 + 2D8 + 1D20` roll with the
✕ beside a single-line Roll label, and the cleared state with every chip at 0 and `Recent (1)`
intact.

## Related docs

- [Feature: clear the pool](../features/clear-pool.md)
- [Testing: custom dice](custom-dice.md)
