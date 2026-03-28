// app/src/main/java/fr/mandarine/diceroller/presentation/model/PipPosition.kt
package fr.mandarine.diceroller.presentation.model

/**
 * Represents a single pip position within the unit square of a D6 face.
 *
 * Coordinates are expressed as fractions in the range `0f..1f`, where
 * `(0f, 0f)` is the top-left corner and `(1f, 1f)` is the bottom-right.
 * The rendering layer multiplies these values by the actual face size.
 *
 * @property x horizontal position in unit-square coordinates
 * @property y vertical position in unit-square coordinates
 */
data class PipPosition(val x: Float, val y: Float)
