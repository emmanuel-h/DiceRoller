// app/src/test/java/fr/mandarine/diceroller/presentation/CustomFacesValidationTest.kt
package fr.mandarine.diceroller.presentation

import fr.mandarine.diceroller.R
import fr.mandarine.diceroller.domain.CustomDie
import fr.mandarine.diceroller.domain.Dice
import fr.mandarine.diceroller.domain.DieType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The creator dialog's Add button and its inline message are two readings of one
 * [validateCustomFaces] verdict, so every rule the user can break is pinned here rather than in a
 * UI test — including *which* rule wins when an input breaks more than one.
 *
 * Since issue #68 a verdict names its message by resource rather than spelling it out, so these
 * assertions are on the rule that fired and the arguments it fired with, and survive a translator
 * rewording any of them.
 */
class CustomFacesValidationTest {

    /**
     * A full set, built from the first [MAX_CUSTOM_DICE] face counts that are *not* presets — so
     * raising the cap cannot silently make this fixture ask for a `CustomDie(4)`, which throws.
     */
    private val fullDice: List<CustomDie> = DieType.FACES_RANGE.asSequence()
        .mapNotNull { DieType.ofFaces(it) as? CustomDie }
        .take(MAX_CUSTOM_DICE)
        .toList()

    private fun verdict(input: String, existing: List<CustomDie> = emptyList()) =
        validateCustomFaces(input = input, existing = existing)

    private fun message(input: String, existing: List<CustomDie> = emptyList()): UiText =
        (verdict(input, existing) as CustomFacesResult.Invalid).message

    // --- Accepting ---

    @Test
    fun givenANonPresetFaceCount_whenValidated_thenItIsValid() {
        assertEquals(CustomFacesResult.Valid(CustomDie(7)), verdict("7"))
    }

    @Test
    fun givenSurroundingWhitespace_whenValidated_thenItIsTrimmedAndAccepted() {
        assertEquals(CustomFacesResult.Valid(CustomDie(7)), verdict("  7 "))
    }

    @Test
    fun givenTheRangeBounds_whenValidated_thenBothAreAccepted() {
        assertTrue(verdict("${DieType.FACES_RANGE.first}") is CustomFacesResult.Valid)
        assertTrue(verdict("${DieType.FACES_RANGE.last}") is CustomFacesResult.Valid)
    }

    @Test
    fun givenAnotherDieAlreadyDefined_whenValidatingADifferentOne_thenItIsValid() {
        assertTrue(verdict("7", existing = listOf(CustomDie(3))) is CustomFacesResult.Valid)
    }

    // --- Unfinished input is not an error the user is shown ---

    /**
     * An empty field is the dialog's opening state; complaining about it would mean the form is
     * born wrong. It still cannot be submitted.
     */
    @Test
    fun givenAnEmptyField_whenValidated_thenItIsIncompleteAndShowsTheRangeHint() {
        val result = verdict("") as CustomFacesResult.Invalid

        assertTrue(result.isIncomplete)
        assertEquals(CUSTOM_FACES_HINT, result.message)
    }

    /** The hint names the range rather than restating it in prose, so widening it needs no edit. */
    @Test
    fun givenTheRangeHint_whenRead_thenItCarriesBothBoundsAsArguments() {
        assertEquals(
            UiText.Res(
                R.string.custom_faces_hint,
                listOf(DieType.FACES_RANGE.first, DieType.FACES_RANGE.last),
            ),
            CUSTOM_FACES_HINT,
        )
    }

    @Test
    fun givenBlankInput_whenValidated_thenItIsIncomplete() {
        assertTrue((verdict("   ") as CustomFacesResult.Invalid).isIncomplete)
    }

    /** `"1"` is as often on its way to `"12"` as it is a mistake, so it is not scolded mid-type. */
    @Test
    fun givenAFaceCountBelowTheMinimum_whenValidated_thenItIsIncompleteRatherThanWrong() {
        assertTrue((verdict("1") as CustomFacesResult.Invalid).isIncomplete)
    }

    /** Too *large* cannot become correct by typing more digits, so it is a real error. */
    @Test
    fun givenAFaceCountAboveTheMaximum_whenValidated_thenItIsAnOutrightError() {
        val result = verdict("${DieType.FACES_RANGE.last + 1}") as CustomFacesResult.Invalid

        assertFalse(result.isIncomplete)
        assertEquals(
            UiText.Res(
                R.string.custom_faces_out_of_range,
                listOf(DieType.FACES_RANGE.first, DieType.FACES_RANGE.last),
            ),
            result.message,
        )
    }

    @Test
    fun givenNonNumericInput_whenValidated_thenItIsAnOutrightError() {
        val result = verdict("seven") as CustomFacesResult.Invalid

        assertFalse(result.isIncomplete)
        assertEquals(UiText.Res(R.string.custom_faces_not_a_number), result.message)
    }

    // --- Rejecting, with the reason that actually applies ---

    @Test
    fun givenAPresetsFaceCount_whenValidated_thenItSaysItIsAlreadyStandard() {
        Dice.entries.forEach { dice ->
            val result = verdict("${dice.faces}") as CustomFacesResult.Invalid

            assertFalse(result.isIncomplete)
            assertEquals(
                UiText.Res(R.string.custom_faces_is_preset, listOf(dice.label)),
                result.message,
            )
        }
    }

    @Test
    fun givenADieAlreadyDefined_whenValidated_thenItSaysItIsADuplicate() {
        val result = verdict("7", existing = listOf(CustomDie(7))) as CustomFacesResult.Invalid

        assertEquals(
            UiText.Res(R.string.custom_faces_duplicate, listOf("D7")),
            result.message,
        )
    }

    @Test
    fun givenTheDiceAreFull_whenValidatingANewOne_thenItSaysThereIsNoRoom() {
        val result = verdict("50", existing = fullDice) as CustomFacesResult.Invalid

        assertEquals(UiText.Plural(R.plurals.custom_faces_full, MAX_CUSTOM_DICE), result.message)
    }

    /**
     * Rule precedence, and the reason the fullness check is last: re-typing a die you already have
     * while at the cap is a duplicate, not a capacity problem, and saying "remove one to add
     * another" would send the user to delete a die they did not need to.
     */
    @Test
    fun givenTheDiceAreFullAndTheInputDuplicatesOne_whenValidated_thenTheDuplicateWins() {
        val die = fullDice.first()

        assertEquals(
            UiText.Res(R.string.custom_faces_duplicate, listOf(die.label)),
            message("${die.faces}", existing = fullDice),
        )
    }

    /** Likewise a preset beats fullness: `6` is on screen already, whatever the custom set holds. */
    @Test
    fun givenTheDiceAreFullAndTheInputIsAPreset_whenValidated_thenThePresetReasonWins() {
        assertEquals(
            UiText.Res(R.string.custom_faces_is_preset, listOf(Dice.D6.label)),
            message("6", existing = fullDice),
        )
    }
}
