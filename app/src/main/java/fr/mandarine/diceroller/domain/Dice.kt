// app/src/main/java/fr/mandarine/diceroller/domain/Dice.kt
package fr.mandarine.diceroller.domain

/**
 * Represents the standard dice types available for rolling.
 *
 * @property faces the number of faces on the die
 */
enum class Dice(val faces: Int) {
    D4(faces = 4),
    D6(faces = 6),
    D8(faces = 8),
    D12(faces = 12),
    D20(faces = 20),
}
