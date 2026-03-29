// app/src/main/java/fr/mandarine/diceroller/presentation/model/DiceShape.kt
package fr.mandarine.diceroller.presentation.model

import fr.mandarine.diceroller.domain.Dice

/**
 * Maps each [Dice] enum value to its polygon rendering parameters.
 *
 * Each subclass carries the vertex count and rotation offset needed to
 * draw the die's outline as a regular polygon. The sealed hierarchy
 * ensures exhaustive handling at compile time and allows individual
 * subtypes to gain additional properties in the future (e.g. corner
 * clipping for [Icosahedron]).
 *
 * @property vertexCount number of vertices in the regular polygon
 * @property rotationDegrees clockwise rotation in degrees applied before drawing,
 *           used to orient the polygon (e.g. point-up for triangles)
 * @property verticalOffsetFraction fraction of the polygon radius applied as a
 *           vertical translation to correct optical misalignment. Negative values
 *           shift the polygon upward. Non-triangle shapes use `0f`.
 */
sealed class DiceShape(
    val vertexCount: Int,
    val rotationDegrees: Float,
    val verticalOffsetFraction: Float = 0f,
) {

    /** D4 -- equilateral triangle, point-up orientation. */
    data object Triangle : DiceShape(
        vertexCount = 3,
        rotationDegrees = -90f,
        verticalOffsetFraction = TRIANGLE_VERTICAL_OFFSET,
    )

    /** D6 -- square rotated 45 degrees so sides are horizontal/vertical. */
    data object Square : DiceShape(vertexCount = 4, rotationDegrees = 45f)

    /** D8 -- regular octagon, flat-top orientation. */
    data object Octagon : DiceShape(vertexCount = 8, rotationDegrees = 0f)

    /** D12 -- regular pentagon, point-up orientation. */
    data object Pentagon : DiceShape(vertexCount = 5, rotationDegrees = -90f)

    /** D20 -- rendered as a triangle (like D4); may gain corner-clipping later. */
    data object Icosahedron : DiceShape(
        vertexCount = 3,
        rotationDegrees = -90f,
        verticalOffsetFraction = TRIANGLE_VERTICAL_OFFSET,
    )

    companion object {
        /**
         * Vertical offset fraction for triangle polygons.
         *
         * Equilateral triangles appear optically top-heavy when centered
         * by their circumscribed circle. Shifting the polygon upward by
         * one-sixth of the radius corrects the visual imbalance so
         * triangles align with other shapes in a row.
         */
        private const val TRIANGLE_VERTICAL_OFFSET = -1f / 6f

        /**
         * Returns the [DiceShape] corresponding to the given [dice] value.
         *
         * The `when` expression is exhaustive -- the compiler will flag
         * any missing branch if a new [Dice] variant is added.
         */
        fun fromDice(dice: Dice): DiceShape = when (dice) {
            Dice.D4 -> Triangle
            Dice.D6 -> Square
            Dice.D8 -> Octagon
            Dice.D12 -> Pentagon
            Dice.D20 -> Icosahedron
        }
    }
}
