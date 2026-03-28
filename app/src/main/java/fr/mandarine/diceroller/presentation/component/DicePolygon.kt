// app/src/main/java/fr/mandarine/diceroller/presentation/component/DicePolygon.kt
package fr.mandarine.diceroller.presentation.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import fr.mandarine.diceroller.domain.Dice
import fr.mandarine.diceroller.presentation.model.DiceShape
import fr.mandarine.diceroller.ui.theme.DiceRollerTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Draws a polygon outline representing the given [dice] type, with a
 * content slot centered inside the shape.
 *
 * The polygon geometry is derived from [DiceShape.fromDice]. Stroke and
 * fill colors are sourced from the current Material 3 color scheme so
 * the component renders correctly in both light and dark mode.
 *
 * @param dice the die type whose polygon shape to draw
 * @param sizeVariant controls the physical dimensions and stroke width
 * @param modifier optional [Modifier] applied to the root container
 * @param isSelected whether the die is in a selected state (fills the polygon background)
 * @param content composable lambda rendered centered inside the polygon
 */
@Composable
fun DicePolygon(
    dice: Dice,
    sizeVariant: DicePolygonSize,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    content: @Composable () -> Unit,
) {
    val shape = DiceShape.fromDice(dice)
    val outlineColor = MaterialTheme.colorScheme.primary
    val fillColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        Color.Transparent
    }
    val strokeWidthPx = with(LocalDensity.current) { sizeVariant.strokeWidthDp.toPx() }

    Box(
        modifier = modifier.size(sizeVariant.sizeDp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val path = buildPolygonPath(
                vertexCount = shape.vertexCount,
                rotationDegrees = shape.rotationDegrees,
            )
            drawPath(path = path, color = fillColor)
            drawPath(path = path, color = outlineColor, style = Stroke(width = strokeWidthPx))
        }
        content()
    }
}

/**
 * Computes a [Path] for a regular polygon inscribed in the current
 * [DrawScope] bounds.
 *
 * The polygon is centered in the draw area. A small inset is applied
 * so that the stroke does not clip against the canvas edges.
 *
 * @param vertexCount number of vertices (e.g. 3 for triangle, 4 for square)
 * @param rotationDegrees initial rotation offset in degrees
 */
private fun DrawScope.buildPolygonPath(
    vertexCount: Int,
    rotationDegrees: Float,
): Path {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val strokeInset = 4f // pixels to keep the stroke fully inside the canvas
    val radius = min(cx, cy) - strokeInset
    val angleStep = 2.0 * PI / vertexCount
    val startAngle = rotationDegrees * PI / 180.0

    return Path().apply {
        for (i in 0 until vertexCount) {
            val angle = startAngle + i * angleStep
            val x = cx + (radius * cos(angle)).toFloat()
            val y = cy + (radius * sin(angle)).toFloat()
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
}

// -- Previews -----------------------------------------------------------------

@Preview(name = "D4 Small", showBackground = true)
@Composable
private fun DicePolygonD4SmallPreview() {
    DiceRollerTheme(dynamicColor = false) {
        DicePolygon(dice = Dice.D4, sizeVariant = DicePolygonSize.Small) {
            Text(text = "D4", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Preview(name = "D6 Small", showBackground = true)
@Composable
private fun DicePolygonD6SmallPreview() {
    DiceRollerTheme(dynamicColor = false) {
        DicePolygon(dice = Dice.D6, sizeVariant = DicePolygonSize.Small, isSelected = true) {
            Text(text = "D6", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Preview(name = "D8 Large", showBackground = true)
@Composable
private fun DicePolygonD8LargePreview() {
    DiceRollerTheme(dynamicColor = false) {
        DicePolygon(dice = Dice.D8, sizeVariant = DicePolygonSize.Large) {
            Text(text = "5", style = MaterialTheme.typography.displayLarge)
        }
    }
}

@Preview(name = "D12 Large", showBackground = true)
@Composable
private fun DicePolygonD12LargePreview() {
    DiceRollerTheme(dynamicColor = false) {
        DicePolygon(dice = Dice.D12, sizeVariant = DicePolygonSize.Large) {
            Text(text = "11", style = MaterialTheme.typography.displayLarge)
        }
    }
}

@Preview(name = "D20 Large", showBackground = true)
@Composable
private fun DicePolygonD20LargePreview() {
    DiceRollerTheme(dynamicColor = false) {
        DicePolygon(dice = Dice.D20, sizeVariant = DicePolygonSize.Large) {
            Text(text = "17", style = MaterialTheme.typography.displayLarge)
        }
    }
}
