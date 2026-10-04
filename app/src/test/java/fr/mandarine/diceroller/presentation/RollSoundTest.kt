// app/src/test/java/fr/mandarine/diceroller/presentation/RollSoundTest.kt
package fr.mandarine.diceroller.presentation

import fr.mandarine.diceroller.domain.CustomDie
import fr.mandarine.diceroller.domain.Dice
import fr.mandarine.diceroller.domain.DiceGroupResult
import fr.mandarine.diceroller.domain.DicePoolResult
import fr.mandarine.diceroller.domain.DieType
import fr.mandarine.diceroller.domain.ValueTally
import org.junit.Assert.assertEquals
import org.junit.Test

class RollSoundTest {

    private fun group(dice: DieType, poolCount: Int) = DiceGroupResult(
        dice = dice,
        poolCount = poolCount,
        tallies = listOf(ValueTally(value = 1, count = poolCount)),
    )

    private fun result(vararg groups: DiceGroupResult) =
        DicePoolResult(groups = groups.toList(), total = groups.sumOf { it.poolCount })

    @Test
    fun givenASingleDie_whenChoosingTheSound_thenItIsOneDie() {
        assertEquals(RollSound.OneDie, RollSound.of(result(group(Dice.D20, 1))))
    }

    @Test
    fun givenASingleCustomDie_whenChoosingTheSound_thenItIsOneDie() {
        assertEquals(RollSound.OneDie, RollSound.of(result(group(CustomDie(7), 1))))
    }

    @Test
    fun givenSeveralDiceOfOneType_whenChoosingTheSound_thenItIsSeveralDice() {
        assertEquals(RollSound.SeveralDice, RollSound.of(result(group(Dice.D6, 2))))
    }

    /** One die of each of two types is still two dice landing at once. */
    @Test
    fun givenOneDieOfEachOfTwoTypes_whenChoosingTheSound_thenItIsSeveralDice() {
        assertEquals(
            RollSound.SeveralDice,
            RollSound.of(result(group(Dice.D6, 1), group(Dice.D8, 1))),
        )
    }
}
