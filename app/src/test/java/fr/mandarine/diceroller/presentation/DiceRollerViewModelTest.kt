// app/src/test/java/fr/mandarine/diceroller/presentation/DiceRollerViewModelTest.kt
package fr.mandarine.diceroller.presentation

import fr.mandarine.diceroller.MainDispatcherRule
import fr.mandarine.diceroller.domain.CustomDie
import fr.mandarine.diceroller.domain.Dice
import fr.mandarine.diceroller.domain.DicePool
import fr.mandarine.diceroller.domain.DicePoolResult
import fr.mandarine.diceroller.domain.DiceRoller
import fr.mandarine.diceroller.domain.DieType
import fr.mandarine.diceroller.domain.RollRecord
import fr.mandarine.diceroller.presentation.model.DiceColor
import kotlin.random.Random
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class DiceRollerViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    /**
     * A clock the tests drive by hand, so recorded timestamps and relative-time refreshes are
     * assertable rather than whatever the wall clock happened to say.
     */
    private var nowMillis: Long = 1_700_000_000_000L

    private fun viewModel(
        seed: Long = 42,
        colorStore: DiceColorStore = InMemoryDiceColorStore(),
        historyStore: RollHistoryStore = InMemoryRollHistoryStore(),
        customDiceStore: CustomDiceStore = InMemoryCustomDiceStore(),
    ): DiceRollerViewModel = DiceRollerViewModel(
        diceRoller = DiceRoller(random = Random(seed)),
        colorStore = colorStore,
        historyStore = historyStore,
        customDiceStore = customDiceStore,
        clock = { nowMillis },
    )

    // --- Initial state: pool ---

    @Test
    fun givenNewViewModel_whenReadingState_thenPoolStartsEmpty() {
        val state = viewModel().uiState.value

        assertEquals(Dice.entries.associateWith { 0 }, state.pool)
    }

    @Test
    fun givenNewViewModel_whenReadingState_thenCanRollIsFalse() {
        val state = viewModel().uiState.value

        assertFalse(state.canRoll)
    }

    @Test
    fun givenNewViewModel_whenReadingState_thenResultIsNull() {
        val state = viewModel().uiState.value

        assertNull(state.result)
    }

    @Test
    fun givenNewViewModel_whenReadingState_thenSelectedColorIsTheDefault() {
        val state = viewModel().uiState.value

        assertEquals(DiceColor.Default, state.selectedColor)
    }

    // --- incrementCount ---

    @Test
    fun givenEmptyPool_whenIncrementingD6_thenCountBecomesOne() {
        val vm = viewModel()

        vm.incrementCount(Dice.D6)

        assertEquals(1, vm.uiState.value.pool[Dice.D6])
    }

    @Test
    fun givenCountBelowMax_whenIncrementing_thenOtherDiceTypesAreUnaffected() {
        val vm = viewModel()

        vm.incrementCount(Dice.D6)

        val pool = vm.uiState.value.pool
        Dice.entries.filter { it != Dice.D6 }.forEach { dice ->
            assertEquals("Expected $dice to remain at 0, got ${pool[dice]}", 0, pool[dice])
        }
    }

    @Test
    fun givenCountBelowMax_whenIncrementingRepeatedly_thenCountIncreasesByOneEachTime() {
        val vm = viewModel()

        repeat(5) { vm.incrementCount(Dice.D8) }

        assertEquals(5, vm.uiState.value.pool[Dice.D8])
    }

    // --- incrementCount: clamping at the cap (acceptance criterion) ---

    @Test
    fun givenCountAtMax_whenIncrementingAgain_thenCountHasNoFurtherEffect() {
        val vm = viewModel()
        repeat(DicePool.MAX_DICE_PER_TYPE) { vm.incrementCount(Dice.D6) }
        assertEquals(DicePool.MAX_DICE_PER_TYPE, vm.uiState.value.pool[Dice.D6])

        vm.incrementCount(Dice.D6)

        assertEquals(DicePool.MAX_DICE_PER_TYPE, vm.uiState.value.pool[Dice.D6])
    }

    @Test
    fun givenCountWellPastMax_whenIncrementingManyMoreTimes_thenCountStaysClampedAtMax() {
        val vm = viewModel()

        repeat(DicePool.MAX_DICE_PER_TYPE + 30) { vm.incrementCount(Dice.D20) }

        assertEquals(DicePool.MAX_DICE_PER_TYPE, vm.uiState.value.pool[Dice.D20])
    }

    // --- decrementCount ---

    @Test
    fun givenCountAboveZero_whenDecrementing_thenCountDecreasesByOne() {
        val vm = viewModel()
        vm.incrementCount(Dice.D12)
        vm.incrementCount(Dice.D12)

        vm.decrementCount(Dice.D12)

        assertEquals(1, vm.uiState.value.pool[Dice.D12])
    }

    // --- decrementCount: clamping at the floor (acceptance criterion) ---

    @Test
    fun givenCountAtZero_whenDecrementing_thenCountHasNoFurtherEffect() {
        val vm = viewModel()

        vm.decrementCount(Dice.D6)

        assertEquals(0, vm.uiState.value.pool[Dice.D6])
    }

    @Test
    fun givenCountAtZero_whenDecrementingManyTimes_thenCountStaysClampedAtZero() {
        val vm = viewModel()

        repeat(10) { vm.decrementCount(Dice.D4) }

        assertEquals(0, vm.uiState.value.pool[Dice.D4])
    }

    // --- Result-clearing rules: count changes clear the result ---

    @Test
    fun givenRollPerformed_whenIncrementingCount_thenResultIsClearedToNull() {
        val vm = viewModel()
        vm.incrementCount(Dice.D6)
        vm.rollDice()
        assertNotNull(vm.uiState.value.result)

        vm.incrementCount(Dice.D8)

        assertNull(vm.uiState.value.result)
    }

    @Test
    fun givenRollPerformed_whenDecrementingCount_thenResultIsClearedToNull() {
        val vm = viewModel()
        vm.incrementCount(Dice.D6)
        vm.incrementCount(Dice.D6)
        vm.rollDice()
        assertNotNull(vm.uiState.value.result)

        vm.decrementCount(Dice.D6)

        assertNull(vm.uiState.value.result)
    }

    @Test
    fun givenRollPerformed_whenIncrementingAtCap_thenResultIsPreservedBecauseCountDidNotActuallyChange() {
        val vm = viewModel()
        repeat(DicePool.MAX_DICE_PER_TYPE) { vm.incrementCount(Dice.D6) }
        vm.rollDice()
        val resultBeforeIncrement = vm.uiState.value.result
        assertNotNull(resultBeforeIncrement)

        vm.incrementCount(Dice.D6) // already at the cap, no-op

        assertEquals(resultBeforeIncrement, vm.uiState.value.result)
    }

    @Test
    fun givenRollPerformed_whenDecrementingAtFloor_thenResultIsPreservedBecauseCountDidNotActuallyChange() {
        val vm = viewModel()
        vm.incrementCount(Dice.D6)
        vm.rollDice()
        val resultBeforeDecrement = vm.uiState.value.result
        assertNotNull(resultBeforeDecrement)

        vm.decrementCount(Dice.D4) // already at 0, no-op

        assertEquals(resultBeforeDecrement, vm.uiState.value.result)
    }

    // --- Result-clearing rules: selectColor never clears the result ---

    @Test
    fun givenRollPerformed_whenSelectingColor_thenResultIsPreserved() {
        val vm = viewModel()
        vm.incrementCount(Dice.D6)
        vm.rollDice()
        val resultBeforeSelection = vm.uiState.value.result
        assertNotNull(resultBeforeSelection)

        vm.selectColor(DiceColor.Jade)

        assertEquals(resultBeforeSelection, vm.uiState.value.result)
    }

    @Test
    fun givenRollPerformed_whenSelectingColor_thenPoolIsUnchanged() {
        val vm = viewModel()
        vm.incrementCount(Dice.D6)
        vm.incrementCount(Dice.D20)
        vm.rollDice()
        val poolBeforeSelection = vm.uiState.value.pool

        vm.selectColor(DiceColor.Moss)

        assertEquals(poolBeforeSelection, vm.uiState.value.pool)
    }

    // --- rollDice: empty pool ---

    @Test
    fun givenEmptyPool_whenRolling_thenResultRemainsNull() {
        val vm = viewModel()

        vm.rollDice()

        assertNull(vm.uiState.value.result)
    }

    @Test
    fun givenPoolEmptiedAfterIncrementThenDecrement_whenRolling_thenResultRemainsNull() {
        val vm = viewModel()
        vm.incrementCount(Dice.D6)
        vm.decrementCount(Dice.D6) // back to empty

        vm.rollDice()

        assertNull(vm.uiState.value.result)
    }

    // --- rollDice: non-empty pool matches the domain pool-rolling API ---

    @Test
    fun givenNonEmptyPool_whenRolling_thenResultIsNotNull() {
        val vm = viewModel()
        vm.incrementCount(Dice.D6)

        vm.rollDice()

        assertNotNull(vm.uiState.value.result)
    }

    @Test
    fun givenSingleDieTypePool_whenRolling_thenResultMatchesDicePoolResultFromDomainApi() {
        val seed = 123L
        val vm = viewModel(seed = seed)
        vm.incrementCount(Dice.D6)
        vm.incrementCount(Dice.D6)
        val poolBeforeRoll = vm.uiState.value.pool

        vm.rollDice()

        val expected = DiceRoller(random = Random(seed)).rollPool(DicePool(poolBeforeRoll))
        assertEquals(expected, vm.uiState.value.result)
    }

    @Test
    fun givenMixedDieTypePool_whenRolling_thenResultMatchesDicePoolResultFromDomainApi() {
        val seed = 2024L
        val vm = viewModel(seed = seed)
        vm.incrementCount(Dice.D4)
        vm.incrementCount(Dice.D4)
        vm.incrementCount(Dice.D4)
        vm.incrementCount(Dice.D20)
        val poolBeforeRoll = vm.uiState.value.pool

        vm.rollDice()

        val expected = DiceRoller(random = Random(seed)).rollPool(DicePool(poolBeforeRoll))
        assertEquals(expected, vm.uiState.value.result)
    }

    @Test
    fun givenFirstRollDone_whenChangingPoolAndRollingAgain_thenResultIsUpdatedToTheNewSeededRoll() {
        val seed = 7L
        val vm = viewModel(seed = seed)
        vm.incrementCount(Dice.D6)
        vm.rollDice()
        val firstResult = vm.uiState.value.result
        assertNotNull(firstResult)

        vm.incrementCount(Dice.D8)
        val poolBeforeSecondRoll = vm.uiState.value.pool
        vm.rollDice()

        val replayRoller = DiceRoller(random = Random(seed))
        replayRoller.rollPool(DicePool(mapOf(Dice.D6 to 1))) // replays the first roll's random draw
        val expectedSecondResult = replayRoller.rollPool(DicePool(poolBeforeSecondRoll))
        assertEquals(expectedSecondResult, vm.uiState.value.result)
    }

    // --- Pool starts empty / color restoration is unchanged ---

    @Test
    fun givenStoreHoldingAColor_whenViewModelIsCreated_thenThatColorIsRestored() {
        val store = InMemoryDiceColorStore(initial = DiceColor.Sapphire)

        val vm = viewModel(colorStore = store)

        assertEquals(DiceColor.Sapphire, vm.uiState.value.selectedColor)
    }

    @Test
    fun givenStoreHoldingAColor_whenViewModelIsCreated_thenPoolAndResultAreUntouched() {
        val store = InMemoryDiceColorStore(initial = DiceColor.Smoke)

        val vm = viewModel(colorStore = store)

        assertEquals(Dice.entries.associateWith { 0 }, vm.uiState.value.pool)
    }

    @Test
    fun givenColorSelected_whenReadingTheStore_thenTheChoiceWasWrittenThrough() = runTest {
        val store = InMemoryDiceColorStore()
        val vm = viewModel(colorStore = store)

        vm.selectColor(DiceColor.Bronze)

        assertEquals(DiceColor.Bronze, store.selectedColor.first())
    }

    @Test
    fun givenColorSelected_whenANewViewModelSharesTheStore_thenTheChoiceSurvives() {
        val store = InMemoryDiceColorStore()
        viewModel(colorStore = store).selectColor(DiceColor.Orchid)

        val restarted = viewModel(colorStore = store)

        assertEquals(DiceColor.Orchid, restarted.uiState.value.selectedColor)
    }

    @Test
    fun givenEveryColor_whenSelected_thenStateReflectsIt() {
        val vm = viewModel()

        DiceColor.entries.forEach { color ->
            vm.selectColor(color)
            assertEquals(color, vm.uiState.value.selectedColor)
        }
    }

    // --- Roll history: recording ---

    @Test
    fun givenNewViewModelWithEmptyStore_whenReadingState_thenHistoryIsEmpty() {
        val state = viewModel().uiState.value

        assertEquals(emptyList<RollRecord>(), state.history)
        assertFalse(state.hasHistory)
    }

    @Test
    fun givenNonEmptyPool_whenRolling_thenTheRollIsAppendedToTheHistory() {
        val vm = viewModel()
        vm.incrementCount(Dice.D6)

        vm.rollDice()

        val recorded = vm.uiState.value.history.single()
        assertEquals(vm.uiState.value.result, recorded.result)
        assertEquals(nowMillis, recorded.rolledAtMillis)
        assertTrue(vm.uiState.value.hasHistory)
    }

    @Test
    fun givenEmptyPool_whenRolling_thenNothingIsRecorded() {
        val vm = viewModel()

        vm.rollDice()

        assertEquals(emptyList<RollRecord>(), vm.uiState.value.history)
    }

    @Test
    fun givenSeveralRolls_whenReadingHistory_thenNewestComesFirst() {
        val vm = viewModel()
        vm.incrementCount(Dice.D6)
        vm.rollDice()
        nowMillis += 60_000L
        vm.incrementCount(Dice.D20)
        vm.rollDice()

        val timestamps = vm.uiState.value.history.map { it.rolledAtMillis }

        assertEquals(listOf(nowMillis, nowMillis - 60_000L), timestamps)
    }

    @Test
    fun givenRollRecorded_whenChangingThePool_thenTheHistoryIsUntouchedEvenThoughTheResultClears() {
        val vm = viewModel()
        vm.incrementCount(Dice.D6)
        vm.rollDice()

        vm.incrementCount(Dice.D6)

        assertNull(vm.uiState.value.result)
        assertEquals(1, vm.uiState.value.history.size)
    }

    @Test
    fun givenHistoryOverTheCap_whenReadingIt_thenOnlyTheNewestRecordsAreKept() {
        val vm = viewModel()
        vm.incrementCount(Dice.D6)
        repeat(MAX_HISTORY_RECORDS + 1) {
            nowMillis += 1_000L
            vm.rollDice()
        }

        val history = vm.uiState.value.history

        assertEquals(MAX_HISTORY_RECORDS, history.size)
        assertEquals(nowMillis, history.first().rolledAtMillis)
        // The very first roll fell off the end; the second-oldest is now the tail.
        assertEquals(nowMillis - (MAX_HISTORY_RECORDS - 1) * 1_000L, history.last().rolledAtMillis)
    }

    // --- Roll history: persistence through the store ---

    @Test
    fun givenStoreHoldingRecords_whenViewModelIsCreated_thenTheyAreRestored() {
        val record = RollRecord(
            result = DicePoolResult(groups = emptyList(), total = 0),
            rolledAtMillis = 1L,
        )

        val vm = viewModel(historyStore = InMemoryRollHistoryStore(listOf(record)))

        assertEquals(listOf(record), vm.uiState.value.history)
    }

    @Test
    fun givenRollRecorded_whenANewViewModelSharesTheStore_thenTheRollSurvives() {
        val store = InMemoryRollHistoryStore()
        val vm = viewModel(historyStore = store)
        vm.incrementCount(Dice.D6)
        vm.rollDice()

        val restarted = viewModel(historyStore = store)

        assertEquals(vm.uiState.value.history, restarted.uiState.value.history)
    }

    @Test
    fun givenRollRecorded_whenReadingTheStore_thenTheRollWasWrittenThrough() = runTest {
        val store = InMemoryRollHistoryStore()
        val vm = viewModel(historyStore = store)
        vm.incrementCount(Dice.D6)

        vm.rollDice()

        assertEquals(vm.uiState.value.result, store.history.first().single().result)
    }

    // --- Roll history: expanding ---

    @Test
    fun givenNewViewModel_whenReadingState_thenHistoryStartsCollapsed() {
        assertFalse(viewModel().uiState.value.isHistoryExpanded)
    }

    @Test
    fun givenCollapsedHistory_whenToggling_thenItExpandsAndBackAgain() {
        val vm = viewModel()

        vm.toggleHistoryExpanded()
        assertTrue(vm.uiState.value.isHistoryExpanded)

        vm.toggleHistoryExpanded()
        assertFalse(vm.uiState.value.isHistoryExpanded)
    }

    @Test
    fun givenTimePassedSinceTheRoll_whenExpandingHistory_thenTheReferenceTimeIsRefreshed() {
        val vm = viewModel()
        vm.incrementCount(Dice.D6)
        vm.rollDice()
        nowMillis += 5L * 60_000L

        vm.toggleHistoryExpanded()

        assertEquals(nowMillis, vm.uiState.value.nowMillis)
    }

    @Test
    fun givenRoll_whenItHappens_thenTheReferenceTimeIsTheRollTime() {
        val vm = viewModel()
        vm.incrementCount(Dice.D6)
        nowMillis += 90_000L

        vm.rollDice()

        assertEquals(nowMillis, vm.uiState.value.nowMillis)
    }

    /**
     * The log is append-only: collapsing the band hides the entries, it does not discard them.
     * With no clear action anywhere, only the [MAX_HISTORY_RECORDS] cap ever removes a record.
     */
    @Test
    fun givenRecordedHistory_whenCollapsingTheBand_thenTheEntriesAreStillThere() {
        val vm = viewModel()
        vm.incrementCount(Dice.D6)
        vm.rollDice()
        vm.toggleHistoryExpanded()

        vm.toggleHistoryExpanded()

        assertFalse(vm.uiState.value.isHistoryExpanded)
        assertEquals(1, vm.uiState.value.history.size)
        assertTrue(vm.uiState.value.hasHistory)
    }

    @Test
    fun givenRecordedHistory_whenRollingRepeatedly_thenTheLogOnlyEverGrowsUntilTheCap() = runTest {
        val store = InMemoryRollHistoryStore()
        val vm = viewModel(historyStore = store)
        vm.incrementCount(Dice.D6)

        val sizes = (1..5).map {
            nowMillis += 1_000L
            vm.rollDice()
            store.history.first().size
        }

        assertEquals(listOf(1, 2, 3, 4, 5), sizes)
    }

    // --- Custom dice: defining them (issue #4) ---

    @Test
    fun givenNewViewModel_whenReadingState_thenThereAreNoCustomDice() {
        val state = viewModel().uiState.value

        assertEquals(emptyList<CustomDie>(), state.customDice)
        assertTrue(state.canAddCustomDie)
    }

    @Test
    fun givenNewViewModel_whenReadingDieTypes_thenItIsJustThePresets() {
        assertEquals(Dice.entries.toList(), viewModel().uiState.value.dieTypes)
    }

    @Test
    fun givenNoCustomDice_whenOneIsAdded_thenItAppearsInTheDiceAndInTheDieTypes() {
        val vm = viewModel()

        vm.addCustomDie(CustomDie(7))

        assertEquals(listOf(CustomDie(7)), vm.uiState.value.customDice)
        assertEquals(Dice.entries + CustomDie(7), vm.uiState.value.dieTypes)
    }

    /** A chip with no pool entry would read its count as 0 and then write to a key nothing owns. */
    @Test
    fun givenACustomDieIsAdded_whenReadingThePool_thenItHasAZeroEntry() {
        val vm = viewModel()

        vm.addCustomDie(CustomDie(7))

        assertEquals(0, vm.uiState.value.pool[CustomDie(7)])
        assertTrue(vm.uiState.value.pool.containsKey(CustomDie(7)))
    }

    /**
     * Adding a die puts nothing in the pool, so what Roll would produce has not changed and the
     * result on screen is still an accurate answer to it.
     */
    @Test
    fun givenARolledResult_whenACustomDieIsAdded_thenTheResultIsKept() {
        val vm = viewModel()
        vm.incrementCount(Dice.D6)
        vm.rollDice()
        val before = vm.uiState.value.result

        vm.addCustomDie(CustomDie(7))

        assertEquals(before, vm.uiState.value.result)
    }

    @Test
    fun givenSeveralCustomDice_whenAdded_thenTheyAreOrderedAscendingByFaceCount() {
        val vm = viewModel()

        vm.addCustomDie(CustomDie(100))
        vm.addCustomDie(CustomDie(3))
        vm.addCustomDie(CustomDie(7))

        assertEquals(listOf(CustomDie(3), CustomDie(7), CustomDie(100)), vm.uiState.value.customDice)
    }

    @Test
    fun givenTheCapIsReached_whenAnotherIsAdded_thenItIsIgnoredAndAddingIsClosed() {
        val vm = viewModel()
        repeat(MAX_CUSTOM_DICE) { index -> vm.addCustomDie(CustomDie(index + 101)) }

        vm.addCustomDie(CustomDie(200))

        assertEquals(MAX_CUSTOM_DICE, vm.uiState.value.customDice.size)
        assertFalse(vm.uiState.value.canAddCustomDie)
        assertFalse(vm.uiState.value.customDice.contains(CustomDie(200)))
    }

    @Test
    fun givenADieAlreadyDefined_whenAddedAgain_thenItIsNotDuplicated() {
        val vm = viewModel()
        vm.addCustomDie(CustomDie(7))

        vm.addCustomDie(CustomDie(7))

        assertEquals(listOf(CustomDie(7)), vm.uiState.value.customDice)
    }

    // --- Custom dice: they roll like presets ---

    @Test
    fun givenACustomDieInThePool_whenRolled_thenItProducesAGroupWithinItsOwnFaceRange() {
        val vm = viewModel()
        vm.addCustomDie(CustomDie(7))
        vm.incrementCount(CustomDie(7))
        vm.incrementCount(CustomDie(7))

        vm.rollDice()

        val group = vm.uiState.value.result!!.groups.single()
        assertEquals(CustomDie(7), group.dice)
        assertEquals(2, group.poolCount)
        assertTrue(group.tallies.all { it.value in 1..7 })
    }

    /** Groups are ordered by face count, so a D7 lands between the D6 and the D8 — not after them. */
    @Test
    fun givenAPoolMixingPresetsAndACustomDie_whenRolled_thenGroupsInterleaveByFaceCount() {
        val vm = viewModel()
        vm.addCustomDie(CustomDie(7))
        vm.incrementCount(Dice.D8)
        vm.incrementCount(Dice.D6)
        vm.incrementCount(CustomDie(7))

        vm.rollDice()

        assertEquals(
            listOf<DieType>(Dice.D6, CustomDie(7), Dice.D8),
            vm.uiState.value.result!!.groups.map { it.dice },
        )
    }

    @Test
    fun givenACustomDieRolled_whenReadingTheHistory_thenTheEntryRecordsIt() = runTest {
        val store = InMemoryRollHistoryStore()
        val vm = viewModel(historyStore = store)
        vm.addCustomDie(CustomDie(7))
        vm.incrementCount(CustomDie(7))

        vm.rollDice()

        assertEquals(CustomDie(7), store.history.first().single().result.groups.single().dice)
    }

    // --- Custom dice: removing them ---

    @Test
    fun givenACustomDie_whenRemoved_thenItLeavesTheDiceTheDieTypesAndThePool() {
        val vm = viewModel()
        vm.addCustomDie(CustomDie(7))

        vm.removeCustomDie(CustomDie(7))

        assertEquals(emptyList<CustomDie>(), vm.uiState.value.customDice)
        assertEquals(Dice.entries.toList(), vm.uiState.value.dieTypes)
        assertFalse(vm.uiState.value.pool.containsKey(CustomDie(7)))
    }

    @Test
    fun givenACustomDieWithNoDiceQueued_whenRemoved_thenTheResultIsKept() {
        val vm = viewModel()
        vm.addCustomDie(CustomDie(7))
        vm.incrementCount(Dice.D6)
        vm.rollDice()
        val before = vm.uiState.value.result

        vm.removeCustomDie(CustomDie(7))

        assertEquals(before, vm.uiState.value.result)
    }

    /** Removing a die that *was* queued changes what Roll would produce, so the result must go. */
    @Test
    fun givenACustomDieWithDiceQueued_whenRemoved_thenTheResultIsCleared() {
        val vm = viewModel()
        vm.addCustomDie(CustomDie(7))
        vm.incrementCount(CustomDie(7))
        vm.rollDice()

        vm.removeCustomDie(CustomDie(7))

        assertNull(vm.uiState.value.result)
    }

    /** The log records face counts, not definitions, so deleting a die cannot rewrite history. */
    @Test
    fun givenAPastRollOfACustomDie_whenTheDieIsRemoved_thenTheLogEntryRemains() {
        val vm = viewModel()
        vm.addCustomDie(CustomDie(7))
        vm.incrementCount(CustomDie(7))
        vm.rollDice()

        vm.removeCustomDie(CustomDie(7))

        assertEquals(1, vm.uiState.value.history.size)
        assertEquals(
            CustomDie(7),
            vm.uiState.value.history.single().result.groups.single().dice,
        )
    }

    @Test
    fun givenADieThatWasNeverDefined_whenRemoved_thenNothingHappens() {
        val vm = viewModel()
        vm.addCustomDie(CustomDie(7))

        vm.removeCustomDie(CustomDie(9))

        assertEquals(listOf(CustomDie(7)), vm.uiState.value.customDice)
        assertNull(vm.uiState.value.removedCustomDie)
    }

    @Test
    fun givenTheCapWasReached_whenOneIsRemoved_thenAddingIsPossibleAgain() {
        val vm = viewModel()
        repeat(MAX_CUSTOM_DICE) { index -> vm.addCustomDie(CustomDie(index + 101)) }

        vm.removeCustomDie(CustomDie(101))

        assertTrue(vm.uiState.value.canAddCustomDie)
    }

    // --- Custom dice: undoing a removal ---

    @Test
    fun givenACustomDie_whenRemoved_thenTheUndoIsArmed() {
        val vm = viewModel()
        vm.addCustomDie(CustomDie(7))

        vm.removeCustomDie(CustomDie(7))

        assertEquals(CustomDie(7), vm.uiState.value.removedCustomDie)
    }

    /** The point of the undo: a mistap on a badge that overlaps the increment half costs one tap. */
    @Test
    fun givenARemovedCustomDie_whenUndone_thenTheDieAndItsCountComeBack() {
        val vm = viewModel()
        vm.addCustomDie(CustomDie(7))
        repeat(3) { vm.incrementCount(CustomDie(7)) }
        vm.removeCustomDie(CustomDie(7))

        vm.undoRemoveCustomDie()

        assertEquals(listOf(CustomDie(7)), vm.uiState.value.customDice)
        assertEquals(3, vm.uiState.value.pool[CustomDie(7)])
        assertNull(vm.uiState.value.removedCustomDie)
    }

    /**
     * An undo restores the pool but never the result: a result is only ever produced by pressing
     * Roll, and reviving one would show an outcome the user did not ask for.
     */
    @Test
    fun givenARemovedCustomDieThatHadBeenRolled_whenUndone_thenTheResultStaysCleared() {
        val vm = viewModel()
        vm.addCustomDie(CustomDie(7))
        vm.incrementCount(CustomDie(7))
        vm.rollDice()
        vm.removeCustomDie(CustomDie(7))

        vm.undoRemoveCustomDie()

        assertNull(vm.uiState.value.result)
    }

    @Test
    fun givenARemovedCustomDie_whenTheSnackbarIsDismissed_thenTheRemovalStands() {
        val vm = viewModel()
        vm.addCustomDie(CustomDie(7))
        vm.removeCustomDie(CustomDie(7))

        vm.dismissRemovedCustomDie()

        assertNull(vm.uiState.value.removedCustomDie)
        assertEquals(emptyList<CustomDie>(), vm.uiState.value.customDice)
    }

    @Test
    fun givenNoPendingRemoval_whenUndoIsCalled_thenNothingHappens() {
        val vm = viewModel()
        vm.addCustomDie(CustomDie(7))

        vm.undoRemoveCustomDie()

        assertEquals(listOf(CustomDie(7)), vm.uiState.value.customDice)
    }

    /** Remove, undo, remove again: the second removal must arm its own undo, not reuse a stale one. */
    @Test
    fun givenADieRemovedUndoneAndRemovedAgain_whenUndoneAgain_thenTheCountIsStillRestored() {
        val vm = viewModel()
        vm.addCustomDie(CustomDie(7))
        repeat(2) { vm.incrementCount(CustomDie(7)) }

        vm.removeCustomDie(CustomDie(7))
        vm.undoRemoveCustomDie()
        vm.removeCustomDie(CustomDie(7))
        vm.undoRemoveCustomDie()

        assertEquals(listOf(CustomDie(7)), vm.uiState.value.customDice)
        assertEquals(2, vm.uiState.value.pool[CustomDie(7)])
    }

    // --- Custom dice: persistence and the creator dialog ---

    @Test
    fun givenACustomDieAdded_whenReadingTheStore_thenItWasWrittenThrough() = runTest {
        val store = InMemoryCustomDiceStore()
        val vm = viewModel(customDiceStore = store)

        vm.addCustomDie(CustomDie(7))

        assertEquals(listOf(CustomDie(7)), store.customDice.first())
    }

    @Test
    fun givenStoredCustomDice_whenANewViewModelSharesTheStore_thenTheyAreRestored() {
        val store = InMemoryCustomDiceStore(listOf(CustomDie(7), CustomDie(3)))

        val state = viewModel(customDiceStore = store).uiState.value

        assertEquals(listOf(CustomDie(3), CustomDie(7)), state.customDice)
        assertEquals(0, state.pool[CustomDie(7)])
        assertEquals(0, state.pool[CustomDie(3)])
    }

    @Test
    fun givenACustomDieRemoved_whenReadingTheStore_thenTheRemovalWasWrittenThrough() = runTest {
        val store = InMemoryCustomDiceStore(listOf(CustomDie(7)))
        val vm = viewModel(customDiceStore = store)

        vm.removeCustomDie(CustomDie(7))

        assertEquals(emptyList<CustomDie>(), store.customDice.first())
    }

    @Test
    fun givenARemovalUndone_whenReadingTheStore_thenTheDieIsBackInIt() = runTest {
        val store = InMemoryCustomDiceStore(listOf(CustomDie(7)))
        val vm = viewModel(customDiceStore = store)
        vm.removeCustomDie(CustomDie(7))

        vm.undoRemoveCustomDie()

        assertEquals(listOf(CustomDie(7)), store.customDice.first())
    }

    @Test
    fun givenNewViewModel_whenReadingState_thenTheCreatorIsClosed() {
        assertFalse(viewModel().uiState.value.isCustomDieCreatorVisible)
    }

    @Test
    fun givenTheCreatorIsClosed_whenShown_thenItOpensAndDismissingClosesItAgain() {
        val vm = viewModel()

        vm.showCustomDieCreator()
        assertTrue(vm.uiState.value.isCustomDieCreatorVisible)

        vm.dismissCustomDieCreator()
        assertFalse(vm.uiState.value.isCustomDieCreatorVisible)
    }

    @Test
    fun givenTheCreatorIsOpen_whenADieIsAdded_thenItCloses() {
        val vm = viewModel()
        vm.showCustomDieCreator()

        vm.addCustomDie(CustomDie(7))

        assertFalse(vm.uiState.value.isCustomDieCreatorVisible)
    }

    // --- The About sheet (issue #66) ---

    @Test
    fun givenNewViewModel_whenReadingState_thenTheAboutSheetIsClosed() {
        assertFalse(viewModel().uiState.value.isAboutVisible)
    }

    @Test
    fun givenTheAboutSheetShown_whenItIsDismissed_thenItCloses() {
        val vm = viewModel()

        vm.showAbout()
        assertTrue(vm.uiState.value.isAboutVisible)
        vm.dismissAbout()

        assertFalse(vm.uiState.value.isAboutVisible)
    }

    /**
     * Reading the credit is not a change to what Roll would produce, so unlike a count change it
     * must leave the result and the pool alone — the sheet is a place to look, not an action.
     */
    @Test
    fun givenARolledPool_whenTheAboutSheetIsShown_thenTheResultAndPoolSurvive() {
        val vm = viewModel(seed = 42)
        vm.incrementCount(Dice.D6)
        vm.rollDice()
        val rolled = vm.uiState.value.result

        vm.showAbout()

        assertEquals(rolled, vm.uiState.value.result)
        assertEquals(1, vm.uiState.value.pool[Dice.D6])
    }

    /** Presets are not definitions: nothing the custom-dice actions do may remove one. */
    @Test
    fun givenCustomDiceAddedAndRemoved_whenReadingThePool_thenEveryPresetStillHasAnEntry() {
        val vm = viewModel()

        vm.addCustomDie(CustomDie(7))
        vm.incrementCount(Dice.D6)
        vm.removeCustomDie(CustomDie(7))

        Dice.entries.forEach { dice ->
            assertTrue("missing $dice", vm.uiState.value.pool.containsKey(dice))
        }
    }
}
