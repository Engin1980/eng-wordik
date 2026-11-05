package cz.wordik.app.ui.results

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cz.wordik.app.ui.theme.WordikTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.delay

/**
 * Odměna za dokončený test: tři hvězdičky, [filled] jich je zlatých. Zlaté se postupně
 * "vyskočí" krátkou animací. Hvězdičky se kreslí, aby nezáviselo na glyfech písma.
 */
@Composable
fun StarRating(filled: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(3) { index ->
            val on = index < filled
            val scale = remember { Animatable(if (on) 0f else 1f) }
            LaunchedEffect(on) {
                if (on) {
                    delay(250L * index + 150)
                    scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
                }
            }
            Star(on, Modifier.size(56.dp).scale(scale.value))
        }
    }
}

@Composable
private fun Star(filled: Boolean, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val outer = size.minDimension / 2
        val inner = outer * 0.45f
        val center = Offset(size.width / 2, size.height / 2)
        val path = Path()
        for (i in 0 until 10) {
            val radius = if (i % 2 == 0) outer else inner
            val angle = -PI / 2 + i * PI / 5
            val point = Offset(center.x + (radius * cos(angle)).toFloat(), center.y + (radius * sin(angle)).toFloat())
            if (i == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
        }
        path.close()
        if (filled) {
            drawPath(path, Color(0xFFFFB300))
            drawPath(path, Color(0xFFE08A00), style = Stroke(width = 3f))
        } else {
            drawPath(path, Color(0xFFE6E0D0))
            drawPath(path, Color(0xFFB5AE9B), style = Stroke(width = 3f))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun StarRatingPreview() {
    WordikTheme { StarRating(filled = 2) }
}
