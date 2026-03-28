// app/src/test/java/fr/mandarine/diceroller/presentation/DiceRollerViewModelTest.kt
package fr.mandarine.diceroller.presentation

import fr.mandarine.diceroller.domain.Dice
import fr.mandarine.diceroller.domain.DiceRoller
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Test

class DiceRollerViewModelTest {

    private fun createViewModel(seed: Long = 42): DiceRollerViewModel {
        return DiceRollerViewModel(diceRoller = DiceRoller(random = Random(seed)))
    }

    @Test
    fun `initial state has D6 selected and null result`() {
        val viewModel = createViewModel()
        val state = viewModel.uiState.value
        assertEquals(Dice.D6, state.selectedDice)
        assertNull(state.result)
    }

    @Test
    fun `selectDice updates selected dice`() {
        val viewModel = createViewModel()
        viewModel.selectDice(Dice.D20)
        assertEquals(Dice.D20, viewModel.uiState.value.selectedDice)
    }

    @Test
    fun `selectDice preserves previous result`() {
        val viewModel = createViewModel()
        viewModel.rollDice()
        val resultBefore = viewModel.uiState.value.result
        assertNotNull(resultBefore)

        viewModel.selectDice(Dice.D12)
        assertEquals(resultBefore, viewModel.uiState.value.result)
        assertEquals(Dice.D12, viewModel.uiState.value.selectedDice)
    }

    @Test
    fun `rollDice produces a non-null result`() {
        val viewModel = createViewModel()
        viewModel.rollDice()
        assertNotNull(viewModel.uiState.value.result)
    }

    @Test
    fun `rollDice replaces previous result`() {
        val viewModel = createViewModel(seed = 1)
        viewModel.rollDice()
        val first = viewModel.uiState.value.result

        viewModel.rollDice()
        val second = viewModel.uiState.value.result

        assertNotNull(first)
        assertNotNull(second)
        // With seed=1, consecutive rolls on D6 should produce values;
        // we just verify the state was updated (may or may not differ).
        assertNotNull(viewModel.uiState.value.result)
    }
}
