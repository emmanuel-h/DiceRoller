// app/src/main/java/fr/mandarine/diceroller/presentation/component/D6Pips.kt
package fr.mandarine.diceroller.presentation.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import fr.mandarine.diceroller.presentation.model.D6PipLayout
import fr.mandarine.diceroller.presentation.model.PipPosition
import fr.mandarine.diceroller.ui.theme.DiceRollerTheme

/** Fraction of the face size used as the pip dot radius. */
private const val PIP_RADIUS_FRACTION = 0.08f

/** Stroke width used for the square outline, in dp. */
private const val OUTLINE_STROKE_WIDTH_DP = 2f

/**
 * Renders a D6 die face with traditional pip dots for the given [result].
 *
 * The composable draws a square outline and places filled circles at the
 * positions defined by [D6PipLayout] for the specified result value.
 * Both the outline and pip colors are sourced from the Material 3 color
 * scheme so they adapt to light/dark theme automatically.
 *
 * @param result the roll result to display, must be in the range 1..6
 * @param modifier optional [Modifier] applied to the root container
 * @throws IllegalArgumentException if [result] is outside the range 1..6
 */
@Composable
fun D6Pips(
    result: Int,
    modifier: Modifier = Modifier,
) {
    require(result in 1..6) { "D6 result must be between 1 and 6, was $result" }

    val pipPositions: List<PipPosition> = D6PipLayout.positions.getValue(result)
    val outlineColor = MaterialTheme.colorScheme.outline
    val pipColor = MaterialTheme.colorScheme.onSurface

    Box(
        modifier = modifier.aspectRatio(1f),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawSquareOutline(outlineColor)
            drawPips(pipPositions, pipColor)
        }
    }
}

/**
 * Draws a square outline that fills the current [DrawScope] bounds.
 */
private fun DrawScope.drawSquareOutline(color: Color) {
    val strokeWidth = OUTLINE_STROKE_WIDTH_DP * density
    drawRect(
        color = color,
        style = Stroke(width = strokeWidth),
    )
}

/**
 * Draws filled circles at each [PipPosition] within the current [DrawScope].
 *
 * Positions are converted from unit-square coordinates to pixel offsets
 * by multiplying by the canvas dimensions.
 */
private fun DrawScope.drawPips(
    positions: List<PipPosition>,
    color: Color,
) {
    val pipRadius = size.minDimension * PIP_RADIUS_FRACTION
    positions.forEach { pip ->
        drawCircle(
            color = color,
            radius = pipRadius,
            center = Offset(
                x = pip.x * size.width,
                y = pip.y * size.height,
            ),
        )
    }
}

// -- Previews -----------------------------------------------------------------

/**
 * Provides result values 1 through 6 for Compose previews.
 */
private class D6ResultProvider : PreviewParameterProvider<Int> {
    override val values: Sequence<Int> = (1..6).asSequence()
}

@Preview(showBackground = true, name = "D6 Pips - All Values")
@Composable
private fun D6PipsPreview(
    @PreviewParameter(D6ResultProvider::class) result: Int,
) {
    DiceRollerTheme {
        D6Pips(
            result = result,
            modifier = Modifier.size(120.dp),
        )
    }
}
