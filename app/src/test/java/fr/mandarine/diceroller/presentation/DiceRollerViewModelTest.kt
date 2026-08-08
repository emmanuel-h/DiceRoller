// app/src/test/java/fr/mandarine/diceroller/presentation/DiceRollerViewModelTest.kt
package fr.mandarine.diceroller.presentation

import fr.mandarine.diceroller.MainDispatcherRule
import fr.mandarine.diceroller.domain.Dice
import fr.mandarine.diceroller.domain.DiceRoller
import fr.mandarine.diceroller.presentation.model.DiceColor
import kotlin.random.Random
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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

    // --- Initial state ---

    @Test
    fun givenNewViewModel_whenReadingState_thenSelectedDiceIsD6() {
        val state = viewModel().uiState.value

        assertEquals(Dice.D6, state.selectedDice)
    }

    @Test
    fun givenNewViewModel_whenReadingState_thenResultIsNull() {
        val state = viewModel().uiState.value

        assertNull(state.result)
    }

    // --- selectDice ---

    @Test
    fun givenD6Selected_whenSelectingD4_thenSelectedDiceChangesToD4() {
        val vm = viewModel()

        vm.selectDice(Dice.D4)

        assertEquals(Dice.D4, vm.uiState.value.selectedDice)
    }

    @Test
    fun givenD6Selected_whenSelectingD20_thenSelectedDiceChangesToD20() {
        val vm = viewModel()

        vm.selectDice(Dice.D20)

        assertEquals(Dice.D20, vm.uiState.value.selectedDice)
    }

    @Test
    fun givenNoRollYet_whenSelectingDice_thenResultRemainsNull() {
        val vm = viewModel()

        vm.selectDice(Dice.D12)

        assertNull(vm.uiState.value.result)
    }

    @Test
    fun givenRollPerformed_whenSelectingDifferentDice_thenResultIsClearedToNull() {
        val vm = viewModel()
        vm.rollDice()
        assertNotNull(vm.uiState.value.result)

        vm.selectDice(Dice.D12)

        assertNull(vm.uiState.value.result)
    }

    @Test
    fun givenRollPerformed_whenReselectingSameDice_thenResultIsPreserved() {
        val vm = viewModel()
        vm.rollDice()
        val resultBeforeSelection = vm.uiState.value.result
        assertNotNull(resultBeforeSelection)

        vm.selectDice(Dice.D6) // same die type

        assertEquals(resultBeforeSelection, vm.uiState.value.result)
    }

    @Test
    fun givenRollPerformed_whenSelectingDifferentDice_thenNewDiceIsReflectedInState() {
        val vm = viewModel()
        vm.rollDice()

        vm.selectDice(Dice.D12)

        assertEquals(Dice.D12, vm.uiState.value.selectedDice)
    }

    // --- rollDice ---

    @Test
    fun givenNullResult_whenRolling_thenResultIsNotNull() {
        val vm = viewModel()

        vm.rollDice()

        assertNotNull(vm.uiState.value.result)
    }

    @Test
    fun givenD6Selected_whenRolling_thenResultIsWithinD6Range() {
        val vm = viewModel()

        vm.rollDice()

        val result = vm.uiState.value.result!!
        assertTrue("Expected 1..6, got $result", result in 1..6)
    }

    @Test
    fun givenD4Selected_whenRolling_thenResultIsWithinD4Range() {
        val vm = viewModel()
        vm.selectDice(Dice.D4)

        vm.rollDice()

        val result = vm.uiState.value.result!!
        assertTrue("Expected 1..4, got $result", result in 1..4)
    }

    @Test
    fun givenD8Selected_whenRolling_thenResultIsWithinD8Range() {
        val vm = viewModel()
        vm.selectDice(Dice.D8)

        vm.rollDice()

        val result = vm.uiState.value.result!!
        assertTrue("Expected 1..8, got $result", result in 1..8)
    }

    @Test
    fun givenD12Selected_whenRolling_thenResultIsWithinD12Range() {
        val vm = viewModel()
        vm.selectDice(Dice.D12)

        vm.rollDice()

        val result = vm.uiState.value.result!!
        assertTrue("Expected 1..12, got $result", result in 1..12)
    }

    @Test
    fun givenD20Selected_whenRolling_thenResultIsWithinD20Range() {
        val vm = viewModel()
        vm.selectDice(Dice.D20)

        vm.rollDice()

        val result = vm.uiState.value.result!!
        assertTrue("Expected 1..20, got $result", result in 1..20)
    }

    @Test
    fun givenFirstRollDone_whenRollingAgain_thenResultIsUpdated() {
        // seed=1 produces two consecutive D6 values: 2 then 6 (verified empirically)
        val vm = DiceRollerViewModel(diceRoller = DiceRoller(random = Random(seed = 1)))

        vm.rollDice()
        val firstResult = vm.uiState.value.result

        vm.rollDice()
        val secondResult = vm.uiState.value.result

        assertNotNull(firstResult)
        assertNotNull(secondResult)
        assertNotEquals(
            "Two consecutive seeded rolls must produce different values for seed=1 on D6",
            firstResult,
            secondResult,
        )
    }

    @Test
    fun givenRollDoneWithD6_whenSwitchingToD20AndRolling_thenResultIsWithinD20Range() {
        val vm = viewModel()
        vm.rollDice()

        vm.selectDice(Dice.D20)
        vm.rollDice()

        val result = vm.uiState.value.result!!
        assertTrue("Expected 1..20 after switching to D20, got $result", result in 1..20)
    }

    @Test
    fun givenRollDoneWithD20_whenSwitchingToD4AndRolling_thenResultIsWithinD4Range() {
        val vm = viewModel()
        vm.selectDice(Dice.D20)
        vm.rollDice()

        vm.selectDice(Dice.D4)
        vm.rollDice()

        val result = vm.uiState.value.result!!
        assertTrue("Expected 1..4 after switching to D4, got $result", result in 1..4)
    }

    @Test
    fun givenRolling_whenRollingAgain_thenSelectedDiceIsUnchanged() {
        val vm = viewModel()
        vm.selectDice(Dice.D8)
        vm.rollDice()

        vm.rollDice()

        assertEquals(Dice.D8, vm.uiState.value.selectedDice)
    }

    @Test
    fun givenD10Selected_whenRolling_thenResultIsWithinD10Range() {
        val vm = viewModel()
        vm.selectDice(Dice.D10)

        vm.rollDice()

        val result = vm.uiState.value.result!!
        assertTrue("Expected 1..10, got $result", result in 1..10)
    }

    // --- selectColor ---

    @Test
    fun givenNewViewModel_whenReadingState_thenSelectedColorIsTheDefault() {
        val state = viewModel().uiState.value

        assertEquals(DiceColor.Default, state.selectedColor)
    }

    @Test
    fun givenDefaultColor_whenSelectingRuby_thenSelectedColorChangesToRuby() {
        val vm = viewModel()

        vm.selectColor(DiceColor.Ruby)

        assertEquals(DiceColor.Ruby, vm.uiState.value.selectedColor)
    }

    @Test
    fun givenRollPerformed_whenSelectingColor_thenResultIsPreserved() {
        val vm = viewModel()
        vm.rollDice()
        val resultBeforeSelection = vm.uiState.value.result
        assertNotNull(resultBeforeSelection)

        vm.selectColor(DiceColor.Jade)

        assertEquals(resultBeforeSelection, vm.uiState.value.result)
    }

    @Test
    fun givenRollPerformed_whenSelectingColor_thenSelectedDiceIsUnchanged() {
        val vm = viewModel()
        vm.selectDice(Dice.D20)
        vm.rollDice()

        vm.selectColor(DiceColor.Moss)

        assertEquals(Dice.D20, vm.uiState.value.selectedDice)
    }

    @Test
    fun givenColorSelected_whenSelectingDifferentDice_thenResultIsClearedButColorRemains() {
        val vm = viewModel()
        vm.selectColor(DiceColor.Indigo)
        vm.rollDice()
        assertNotNull(vm.uiState.value.result)

        vm.selectDice(Dice.D10)

        assertNull(vm.uiState.value.result)
        assertEquals(DiceColor.Indigo, vm.uiState.value.selectedColor)
    }

    @Test
    fun givenEveryColor_whenSelected_thenStateReflectsIt() {
        val vm = viewModel()

        DiceColor.entries.forEach { color ->
            vm.selectColor(color)
            assertEquals(color, vm.uiState.value.selectedColor)
        }
    }

    // --- Color persistence ---

    @Test
    fun givenColorSelected_whenReadingTheStore_thenTheChoiceWasWrittenThrough() = runTest {
        val store = InMemoryDiceColorStore()
        val vm = viewModel(colorStore = store)

        vm.selectColor(DiceColor.Bronze)

        assertEquals(DiceColor.Bronze, store.selectedColor.first())
    }

    @Test
    fun givenStoreHoldingAColor_whenViewModelIsCreated_thenThatColorIsRestored() {
        val store = InMemoryDiceColorStore(initial = DiceColor.Sapphire)

        val vm = viewModel(colorStore = store)

        assertEquals(DiceColor.Sapphire, vm.uiState.value.selectedColor)
    }

    @Test
    fun givenColorSelected_whenANewViewModelSharesTheStore_thenTheChoiceSurvives() {
        val store = InMemoryDiceColorStore()
        viewModel(colorStore = store).selectColor(DiceColor.Orchid)

        val restarted = viewModel(colorStore = store)

        assertEquals(DiceColor.Orchid, restarted.uiState.value.selectedColor)
    }

    @Test
    fun givenStoreHoldingAColor_whenViewModelIsCreated_thenDiceAndResultAreUntouched() {
        val store = InMemoryDiceColorStore(initial = DiceColor.Smoke)

        val vm = viewModel(colorStore = store)

        assertEquals(Dice.D6, vm.uiState.value.selectedDice)
        assertNull(vm.uiState.value.result)
    }
}
