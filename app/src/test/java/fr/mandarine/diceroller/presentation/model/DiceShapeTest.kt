// app/src/test/java/fr/mandarine/diceroller/presentation/model/DiceShapeTest.kt
package fr.mandarine.diceroller.presentation.model

import fr.mandarine.diceroller.domain.Dice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [DiceShape].
 *
 * Verifies:
 * - Triangle polygons (D4 / D20) receive a non-zero negative [DiceShape.verticalOffsetFraction]
 * - Non-triangle polygons (D6, D8, D12) receive a zero [DiceShape.verticalOffsetFraction]
 * - [DiceShape.fromDice] maps every [Dice] value to the expected subtype
 * - Structural properties (vertexCount, rotationDegrees) match the spec
 */
class DiceShapeTest {

    // -------------------------------------------------------------------------
    // vertexCount
    // -------------------------------------------------------------------------

    @Test
    fun givenTriangle_whenReadingVertexCount_thenItIsThree() {
        val shape = DiceShape.Triangle

        assertEquals(3, shape.vertexCount)
    }

    @Test
    fun givenIcosahedron_whenReadingVertexCount_thenItIsThree() {
        val shape = DiceShape.Icosahedron

        assertEquals(3, shape.vertexCount)
    }

    @Test
    fun givenSquare_whenReadingVertexCount_thenItIsFour() {
        val shape = DiceShape.Square

        assertEquals(4, shape.vertexCount)
    }

    @Test
    fun givenOctagon_whenReadingVertexCount_thenItIsEight() {
        val shape = DiceShape.Octagon

        assertEquals(8, shape.vertexCount)
    }

    @Test
    fun givenPentagon_whenReadingVertexCount_thenItIsFive() {
        val shape = DiceShape.Pentagon

        assertEquals(5, shape.vertexCount)
    }

    // -------------------------------------------------------------------------
    // verticalOffsetFraction — triangle polygons must have a non-zero offset
    // -------------------------------------------------------------------------

    @Test
    fun givenTriangle_whenReadingVerticalOffsetFraction_thenItIsNonZero() {
        val shape = DiceShape.Triangle

        assertTrue(
            "Expected non-zero verticalOffsetFraction for Triangle, got ${shape.verticalOffsetFraction}",
            shape.verticalOffsetFraction != 0f,
        )
    }

    @Test
    fun givenTriangle_whenReadingVerticalOffsetFraction_thenItIsNegative() {
        val shape = DiceShape.Triangle

        assertTrue(
            "Expected negative verticalOffsetFraction for Triangle (upward shift), got ${shape.verticalOffsetFraction}",
            shape.verticalOffsetFraction < 0f,
        )
    }

    @Test
    fun givenIcosahedron_whenReadingVerticalOffsetFraction_thenItIsNonZero() {
        val shape = DiceShape.Icosahedron

        assertTrue(
            "Expected non-zero verticalOffsetFraction for Icosahedron, got ${shape.verticalOffsetFraction}",
            shape.verticalOffsetFraction != 0f,
        )
    }

    @Test
    fun givenIcosahedron_whenReadingVerticalOffsetFraction_thenItIsNegative() {
        val shape = DiceShape.Icosahedron

        assertTrue(
            "Expected negative verticalOffsetFraction for Icosahedron (upward shift), got ${shape.verticalOffsetFraction}",
            shape.verticalOffsetFraction < 0f,
        )
    }

    @Test
    fun givenTriangleAndIcosahedron_whenComparingVerticalOffsetFractions_thenTheyAreEqual() {
        val triangleOffset = DiceShape.Triangle.verticalOffsetFraction
        val icosahedronOffset = DiceShape.Icosahedron.verticalOffsetFraction

        assertEquals(
            "D4 (Triangle) and D20 (Icosahedron) must share the same vertical offset constant",
            triangleOffset,
            icosahedronOffset,
            0.0001f,
        )
    }

    // -------------------------------------------------------------------------
    // verticalOffsetFraction — non-triangle polygons must have zero offset
    // -------------------------------------------------------------------------

    @Test
    fun givenSquare_whenReadingVerticalOffsetFraction_thenItIsZero() {
        val shape = DiceShape.Square

        assertEquals(0f, shape.verticalOffsetFraction, 0f)
    }

    @Test
    fun givenOctagon_whenReadingVerticalOffsetFraction_thenItIsZero() {
        val shape = DiceShape.Octagon

        assertEquals(0f, shape.verticalOffsetFraction, 0f)
    }

    @Test
    fun givenPentagon_whenReadingVerticalOffsetFraction_thenItIsZero() {
        val shape = DiceShape.Pentagon

        assertEquals(0f, shape.verticalOffsetFraction, 0f)
    }

    // -------------------------------------------------------------------------
    // fromDice — correct subtype mapping
    // -------------------------------------------------------------------------

    @Test
    fun givenD4_whenCallingFromDice_thenReturnsTriangle() {
        val shape = DiceShape.fromDice(Dice.D4)

        assertTrue("Expected Triangle for D4, got $shape", shape is DiceShape.Triangle)
    }

    @Test
    fun givenD6_whenCallingFromDice_thenReturnsSquare() {
        val shape = DiceShape.fromDice(Dice.D6)

        assertTrue("Expected Square for D6, got $shape", shape is DiceShape.Square)
    }

    @Test
    fun givenD8_whenCallingFromDice_thenReturnsOctagon() {
        val shape = DiceShape.fromDice(Dice.D8)

        assertTrue("Expected Octagon for D8, got $shape", shape is DiceShape.Octagon)
    }

    @Test
    fun givenD12_whenCallingFromDice_thenReturnsPentagon() {
        val shape = DiceShape.fromDice(Dice.D12)

        assertTrue("Expected Pentagon for D12, got $shape", shape is DiceShape.Pentagon)
    }

    @Test
    fun givenD20_whenCallingFromDice_thenReturnsIcosahedron() {
        val shape = DiceShape.fromDice(Dice.D20)

        assertTrue("Expected Icosahedron for D20, got $shape", shape is DiceShape.Icosahedron)
    }

    // -------------------------------------------------------------------------
    // fromDice — verticalOffsetFraction propagation
    // -------------------------------------------------------------------------

    @Test
    fun givenD4_whenCallingFromDice_thenVerticalOffsetFractionIsNonZero() {
        val shape = DiceShape.fromDice(Dice.D4)

        assertTrue(
            "Expected non-zero verticalOffsetFraction for D4, got ${shape.verticalOffsetFraction}",
            shape.verticalOffsetFraction != 0f,
        )
    }

    @Test
    fun givenD20_whenCallingFromDice_thenVerticalOffsetFractionIsNonZero() {
        val shape = DiceShape.fromDice(Dice.D20)

        assertTrue(
            "Expected non-zero verticalOffsetFraction for D20, got ${shape.verticalOffsetFraction}",
            shape.verticalOffsetFraction != 0f,
        )
    }

    @Test
    fun givenD6_whenCallingFromDice_thenVerticalOffsetFractionIsZero() {
        val shape = DiceShape.fromDice(Dice.D6)

        assertEquals(
            "D6 (Square) must have zero verticalOffsetFraction",
            0f,
            shape.verticalOffsetFraction,
            0f,
        )
    }

    @Test
    fun givenD8_whenCallingFromDice_thenVerticalOffsetFractionIsZero() {
        val shape = DiceShape.fromDice(Dice.D8)

        assertEquals(
            "D8 (Octagon) must have zero verticalOffsetFraction",
            0f,
            shape.verticalOffsetFraction,
            0f,
        )
    }

    @Test
    fun givenD12_whenCallingFromDice_thenVerticalOffsetFractionIsZero() {
        val shape = DiceShape.fromDice(Dice.D12)

        assertEquals(
            "D12 (Pentagon) must have zero verticalOffsetFraction",
            0f,
            shape.verticalOffsetFraction,
            0f,
        )
    }

    // -------------------------------------------------------------------------
    // fromDice — all triangle dice share the same negative offset value
    // -------------------------------------------------------------------------

    @Test
    fun givenD4AndD20_whenCallingFromDice_thenBothHaveIdenticalVerticalOffsetFraction() {
        val d4Shape = DiceShape.fromDice(Dice.D4)
        val d20Shape = DiceShape.fromDice(Dice.D20)

        assertEquals(
            "D4 and D20 must share the same verticalOffsetFraction constant",
            d4Shape.verticalOffsetFraction,
            d20Shape.verticalOffsetFraction,
            0.0001f,
        )
    }

    @Test
    fun givenD4_whenCallingFromDice_thenVerticalOffsetFractionIsNegative() {
        val shape = DiceShape.fromDice(Dice.D4)

        assertTrue(
            "Expected negative verticalOffsetFraction for D4, got ${shape.verticalOffsetFraction}",
            shape.verticalOffsetFraction < 0f,
        )
    }

    @Test
    fun givenD20_whenCallingFromDice_thenVerticalOffsetFractionIsNegative() {
        val shape = DiceShape.fromDice(Dice.D20)

        assertTrue(
            "Expected negative verticalOffsetFraction for D20, got ${shape.verticalOffsetFraction}",
            shape.verticalOffsetFraction < 0f,
        )
    }
}
