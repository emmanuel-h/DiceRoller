// app/src/main/java/fr/mandarine/diceroller/presentation/model/D6PipLayout.kt
package fr.mandarine.diceroller.presentation.model

/**
 * Canonical pip positions for the six faces of a standard D6.
 *
 * Each entry maps a roll result (1 through 6) to a list of [PipPosition]
 * values expressed in unit-square coordinates (`0f..1f`). The rendering
 * layer scales these positions to the actual face size.
 */
object D6PipLayout {

    /** Pip arrangements keyed by roll result. */
    val positions: Map<Int, List<PipPosition>> = mapOf(
        1 to listOf(
            PipPosition(0.5f, 0.5f),
        ),
        2 to listOf(
            PipPosition(0.25f, 0.25f),
            PipPosition(0.75f, 0.75f),
        ),
        3 to listOf(
            PipPosition(0.25f, 0.25f),
            PipPosition(0.5f, 0.5f),
            PipPosition(0.75f, 0.75f),
        ),
        4 to listOf(
            PipPosition(0.25f, 0.25f),
            PipPosition(0.75f, 0.25f),
            PipPosition(0.25f, 0.75f),
            PipPosition(0.75f, 0.75f),
        ),
        5 to listOf(
            PipPosition(0.25f, 0.25f),
            PipPosition(0.75f, 0.25f),
            PipPosition(0.5f, 0.5f),
            PipPosition(0.25f, 0.75f),
            PipPosition(0.75f, 0.75f),
        ),
        6 to listOf(
            PipPosition(0.25f, 0.25f),
            PipPosition(0.75f, 0.25f),
            PipPosition(0.25f, 0.5f),
            PipPosition(0.75f, 0.5f),
            PipPosition(0.25f, 0.75f),
            PipPosition(0.75f, 0.75f),
        ),
    )
}
