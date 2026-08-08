// app/src/test/java/fr/mandarine/diceroller/presentation/DiceRollerViewModelTest.kt
package fr.mandarine.diceroller.presentation

import fr.mandarine.diceroller.MainDispatcherRule
import fr.mandarine.diceroller.domain.Dice
import fr.mandarine.diceroller.domain.DicePool
import fr.mandarine.diceroller.domain.DiceRoller
import fr.mandarine.diceroller.presentation.model.DiceColor
import kotlin.random.Random
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class DiceRollerViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun viewModel(
        seed: Long = 42,
        colorStore: DiceColorStore = InMemoryDiceColorStore(),
    ): DiceRollerViewModel = DiceRollerViewModel(
        diceRoller = DiceRoller(random = Random(seed)),
        colorStore = colorStore,
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
}
