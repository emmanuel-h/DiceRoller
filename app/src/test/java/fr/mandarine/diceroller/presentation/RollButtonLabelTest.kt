// app/src/test/java/fr/mandarine/diceroller/presentation/RollButtonLabelTest.kt
package fr.mandarine.diceroller.presentation

import fr.mandarine.diceroller.R
import fr.mandarine.diceroller.domain.Dice
import fr.mandarine.diceroller.domain.DicePool
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * What the Roll button says, asserted as *which* string is chosen and what notation goes into it
 * (issue #68). The English wording lives in `values/strings.xml` and belongs to translators; the
 * decision this function makes — placeholder or verb, and the notation for the pool — does not.
 */
class RollButtonLabelTest {

    /** The notation argument of a non-empty label, i.e. what the button will interpolate. */
    private fun notationOf(pool: DicePool): String {
        val label = rollButtonLabel(pool) as UiText.Res

        assertEquals(R.string.roll_button, label.id)
        return label.args.single() as String
    }

    // --- Empty pool ---

    @Test
    fun givenEmptyPool_whenFormattingLabel_thenTheAddDicePlaceholderIsChosen() {
        assertEquals(UiText.Res(R.string.roll_button_empty), rollButtonLabel(DicePool()))
    }

    @Test
    fun givenPoolWithOnlyZeroCounts_whenFormattingLabel_thenTheAddDicePlaceholderIsChosen() {
        val label = rollButtonLabel(DicePool(mapOf(Dice.D6 to 0, Dice.D8 to 0)))

        assertEquals(UiText.Res(R.string.roll_button_empty), label)
    }

    // --- Single die type ---

    @Test
    fun givenSingleDieType_whenFormattingLabel_thenNoPlusSignIsIncluded() {
        assertEquals("4D6", notationOf(DicePool(mapOf(Dice.D6 to 4))))
    }

    @Test
    fun givenSingleDieTypeWithCountOfOne_whenFormattingLabel_thenReadsSingularCountPrefix() {
        assertEquals("1D20", notationOf(DicePool(mapOf(Dice.D20 to 1))))
    }

    // --- Mixed pool: ordering ---

    @Test
    fun givenMixedPool_whenFormattingLabel_thenOrderedSmallestToLargest() {
        assertEquals("4D6 + 2D8", notationOf(DicePool(mapOf(Dice.D8 to 2, Dice.D6 to 4))))
    }

    @Test
    fun givenMixedPoolWithReversedInsertionOrder_whenFormattingLabel_thenStillOrderedByFaces() {
        val notation = notationOf(
            DicePool(mapOf(Dice.D20 to 1, Dice.D4 to 3, Dice.D12 to 2, Dice.D6 to 1)),
        )

        assertEquals("3D4 + 1D6 + 2D12 + 1D20", notation)
    }

    @Test
    fun givenAllSixDiceTypesPopulated_whenFormattingLabel_thenAllGroupsAppearInOrder() {
        val pool = DicePool(
            mapOf(
                Dice.D4 to 1,
                Dice.D6 to 2,
                Dice.D8 to 3,
                Dice.D10 to 4,
                Dice.D12 to 5,
                Dice.D20 to 6,
            ),
        )

        assertEquals("1D4 + 2D6 + 3D8 + 4D10 + 5D12 + 6D20", notationOf(pool))
    }

    @Test
    fun givenMixedPoolWithZeroCountEntries_whenFormattingLabel_thenZeroCountsAreExcluded() {
        val pool = DicePool(mapOf(Dice.D6 to 4, Dice.D8 to 0, Dice.D20 to 2))

        assertEquals("4D6 + 2D20", notationOf(pool))
    }

    // --- Cap ---

    @Test
    fun givenCountAtMax_whenFormattingLabel_thenFullCapValueIsShown() {
        val pool = DicePool(mapOf(Dice.D20 to DicePool.MAX_DICE_PER_TYPE))

        assertEquals("20D20", notationOf(pool))
    }

    // --- The notation is not translated ---

    /**
     * The whole point of passing the notation in as an argument rather than baking it into the
     * string: `4D6 + 2D8` is a domain notation, so the only thing a locale may change around it is
     * the verb.
     */
    @Test
    fun givenANonEmptyPool_whenFormattingLabel_thenTheNotationIsTheSoleArgument() {
        val label = rollButtonLabel(DicePool(mapOf(Dice.D6 to 4))) as UiText.Res

        assertEquals(listOf<Any>("4D6"), label.args)
    }
}
