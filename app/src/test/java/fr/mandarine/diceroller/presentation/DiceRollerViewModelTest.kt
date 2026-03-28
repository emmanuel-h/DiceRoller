// app/src/test/java/fr/mandarine/diceroller/presentation/DiceRollerViewModelTest.kt
package fr.mandarine.diceroller.presentation

import fr.mandarine.diceroller.domain.Dice
import fr.mandarine.diceroller.domain.DiceRoller
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DiceRollerViewModelTest {

    private fun viewModel(seed: Long = 42): DiceRollerViewModel =
        DiceRollerViewModel(diceRoller = DiceRoller(random = Random(seed)))

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
    fun givenRollPerformed_whenSelectingDifferentDice_thenPreviousResultIsPreserved() {
        val vm = viewModel()
        vm.rollDice()
        val resultBeforeSelection = vm.uiState.value.result
        assertNotNull(resultBeforeSelection)

        vm.selectDice(Dice.D12)

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
}
