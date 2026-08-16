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

    /**
     * The names are strings now (issue #68), so what can be checked off-device is that each
     * variant has its own — a copy-pasted `labelRes` would silently make two swatches announce
     * themselves identically. That they read as lowercase colour words is a translator's business
     * and is asserted against the shipped resources in `FantasyDiceArtUiTest`.
     */
    @Test
    fun givenDiceColorEnum_whenReadingLabels_thenEveryVariantHasItsOwnStringResource() {
        val labels = DiceColor.entries.map { it.labelRes }

        assertEquals(
            "Label resources must be unique: $labels",
            labels.size,
            labels.toSet().size,
        )
        assertTrue("Every variant must name a real string resource", labels.none { it == 0 })
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
                    "Missing drawable for ${color.name} $dice",
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

    /**
     * Storage round-trips the enum `name`, which is exactly why the display name moved to a
     * resource in issue #68: a colour saved yesterday must still resolve after the phone changes
     * language, and it can only do that if what was written down was never the translated word.
     */
    @Test
    fun givenATranslatableLookingName_whenResolving_thenDefaultIsReturned() {
        assertEquals(DiceColor.Default, DiceColor.fromNameOrDefault("rubis"))
        assertEquals(DiceColor.Default, DiceColor.fromNameOrDefault("ruby"))
        assertEquals(DiceColor.Ruby, DiceColor.fromNameOrDefault("Ruby"))
    }
}
