// app/src/test/java/fr/mandarine/diceroller/presentation/model/DiceColorTest.kt
package fr.mandarine.diceroller.presentation.model

import fr.mandarine.diceroller.domain.Dice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DiceColorTest {

    // --- Enum shape ---

    @Test
    fun givenDiceColorEnum_whenCountingEntries_thenTwelveVariantsAreAvailable() {
        assertEquals(12, DiceColor.entries.size)
    }

    @Test
    fun givenDiceColorEnum_whenReadingDefault_thenDefaultIsAmethyst() {
        assertEquals(DiceColor.Amethyst, DiceColor.Default)
    }

    @Test
    fun givenDiceColorEnum_whenReadingLabels_thenEveryLabelIsUniqueAndLowercase() {
        val labels = DiceColor.entries.map { it.label }

        assertEquals(
            "Labels must be unique: $labels",
            labels.size,
            labels.toSet().size,
        )
        labels.forEach { label ->
            assertEquals("Label '$label' must be lowercase", label.lowercase(), label)
        }
    }

    @Test
    fun givenDiceColorEnum_whenReadingSwatches_thenEverySwatchIsDistinct() {
        val swatches = DiceColor.entries.map { it.swatch }

        assertEquals(
            "Swatch colors must be distinct: $swatches",
            swatches.size,
            swatches.toSet().size,
        )
    }

    // --- drawableFor ---

    @Test
    fun givenEveryColorAndDie_whenResolvingDrawable_thenResourceIsNonZero() {
        DiceColor.entries.forEach { color ->
            Dice.entries.forEach { dice ->
                assertNotEquals(
                    "Missing drawable for ${color.label} $dice",
                    0,
                    color.drawableFor(dice),
                )
            }
        }
    }

    @Test
    fun givenEveryColorAndDie_whenResolvingDrawable_thenAll72ResourcesAreDistinct() {
        val resources = DiceColor.entries.flatMap { color ->
            Dice.entries.map { dice -> color.drawableFor(dice) }
        }

        assertEquals(72, resources.size)
        assertEquals(
            "Every (color, die) pair must map to its own drawable",
            resources.size,
            resources.toSet().size,
        )
    }

    @Test
    fun givenSameColorAndDie_whenResolvingDrawableTwice_thenResourceIsStable() {
        DiceColor.entries.forEach { color ->
            Dice.entries.forEach { dice ->
                assertEquals(color.drawableFor(dice), color.drawableFor(dice))
            }
        }
    }

    // --- fromNameOrDefault ---

    @Test
    fun givenKnownName_whenResolving_thenMatchingVariantIsReturned() {
        DiceColor.entries.forEach { color ->
            assertEquals(color, DiceColor.fromNameOrDefault(color.name))
        }
    }

    @Test
    fun givenUnknownName_whenResolving_thenDefaultIsReturned() {
        assertEquals(DiceColor.Default, DiceColor.fromNameOrDefault("Turquoise"))
    }

    @Test
    fun givenNullName_whenResolving_thenDefaultIsReturned() {
        assertEquals(DiceColor.Default, DiceColor.fromNameOrDefault(null))
    }

    @Test
    fun givenLabelInsteadOfEnumName_whenResolving_thenDefaultIsReturned() {
        // Storage round-trips the enum `name`, not the display `label`
        assertTrue(DiceColor.Ruby.label != DiceColor.Ruby.name)
        assertEquals(DiceColor.Default, DiceColor.fromNameOrDefault(DiceColor.Ruby.label))
    }
}
