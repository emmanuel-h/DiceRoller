// app/src/main/java/fr/mandarine/diceroller/domain/Dice.kt
package fr.mandarine.diceroller.domain

/**
 * The standard dice presets available for rolling — the six the Fantasy Dices Pack has artwork
 * for, and therefore the only [DieType]s that render as a drawn die.
 *
 * Kept an `enum` on purpose: it is what makes
 * [fr.mandarine.diceroller.presentation.model.DiceColor.drawableFor] exhaustive at compile time,
 * so adding a preset here is a build error until all 12 colour renders are wired up. User-defined
 * face counts are [CustomDie]s instead and need no artwork.
 *
 * @property faces the number of faces on the die
 */
enum class Dice(override val faces: Int) : DieType {
    D4(faces = 4),
    D6(faces = 6),
    D8(faces = 8),
    D10(faces = 10),
    D12(faces = 12),
    D20(faces = 20),
}
