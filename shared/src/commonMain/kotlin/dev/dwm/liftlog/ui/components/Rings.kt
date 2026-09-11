package dev.dwm.liftlog.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.dwm.liftlog.ui.Palette

@Composable
fun MacroTile(label: String, grams: Double, target: Double, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier.background(color.copy(alpha = 0.10f), RoundedCornerShape(12.dp)).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = color)
        Text("${grams.toInt()}g", style = MaterialTheme.typography.headlineMedium)
        FlatBar(goalProgress(grams, target), color, height = 3)
        Text("of ${target.toInt()}g", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
internal fun goalProgress(value: Double, target: Double): Float =
    if (!value.isFinite() || !target.isFinite() || target <= 0.0) 0f
    else (value / target).coerceIn(0.0, 1.0).toFloat()

/** MFP-style ring: Remaining = target − eaten. */
@Composable
fun CalorieRing(eaten: Double, target: Double, modifier: Modifier = Modifier) {
    // number counts up/down to its value — small dopamine hit
    val remaining by androidx.compose.animation.core.animateIntAsState(
        (target - eaten).toInt(),
        animationSpec = tween(900),
    )
    val track = MaterialTheme.colorScheme.surfaceVariant
    val over = remaining < 0
    // sweep animates in on load/update — small dopamine hit
    val sweepTarget = goalProgress(eaten, target) * 360f
    val sweep by animateFloatAsState(sweepTarget, animationSpec = tween(900))
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round)
            val inset = 5.dp.toPx()
            val arcSize = Size(size.width - 2 * inset, size.height - 2 * inset)
            val topLeft = Offset(inset, inset)
            drawArc(track, -90f, 360f, false, topLeft, arcSize, style = stroke)
            val brush = Brush.sweepGradient(
                if (over) listOf(Palette.Protein, Color(0xFFFF6E40), Palette.Protein)
                else listOf(Palette.Calories, Color(0xFF7BC6FF), Palette.Calories)
            )
            rotate(-90f) {
                drawArc(brush, 0f, sweep, false, topLeft, arcSize, style = stroke)
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("${kotlin.math.abs(remaining.toLong())}", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.SemiBold)
            Text(
                if (remaining >= 0) "kcal left" else "kcal over",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun MacroBar(label: String, grams: Double, targetGrams: Double, color: Color) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text(
                "${grams.toInt()} / ${targetGrams.toInt()} g",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        FlatBar(goalProgress(grams, targetGrams), color, Modifier.padding(top = 6.dp), height = 5)
    }
}
