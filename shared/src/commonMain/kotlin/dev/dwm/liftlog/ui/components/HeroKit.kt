package dev.dwm.liftlog.ui.components

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowOutward
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.sp
import dev.dwm.liftlog.ui.Palette
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun ScreenHeading(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(modifier.padding(bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title.uppercase(), style = MaterialTheme.typography.headlineLarge)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** A native, resolution-independent plate illustration shared by the training entry points. */
@Composable
fun TrainingHero(
    title: String,
    subtitle: String,
    eyebrow: String,
    action: String,
    onClick: () -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF22242B)).clickable(role = Role.Button, onClick = onClick),
    ) {
        Box(Modifier.fillMaxWidth().heightIn(min = 188.dp)) {
            Canvas(Modifier.align(Alignment.CenterEnd).size(190.dp)) {
                rotate(-28f) {
                    val center = Offset(size.width * 0.72f, size.height * 0.5f)
                    val radius = size.minDimension * 0.42f
                    drawLine(Color(0xFF646674), Offset(center.x - radius * 1.3f, center.y), Offset(center.x + radius * 1.4f, center.y), 13.dp.toPx())
                    drawCircle(Brush.radialGradient(listOf(Color(0xFF565B64), Color(0xFF15171C)), center, radius), radius, center)
                    drawCircle(Color(0xFF858C83), radius, center, style = Stroke(1.5.dp.toPx()))
                    drawCircle(Palette.Success.copy(alpha = 0.75f), radius * 0.82f, center, style = Stroke(2.dp.toPx()))
                    drawCircle(Color(0xFF0D0E11), radius * 0.32f, center)
                    drawCircle(Color(0xFF626A58), radius * 0.32f, center, style = Stroke(6.dp.toPx()))
                    drawCircle(Color(0xFFBEC5AE), radius * 0.12f, center)
                    repeat(3) { index ->
                        rotate(index * 120f, center) {
                            drawRoundRect(Color(0xFF101216), Offset(center.x - radius * 0.08f, center.y - radius * 0.68f), Size(radius * 0.16f, radius * 0.23f), androidx.compose.ui.geometry.CornerRadius(8f))
                        }
                    }
                }
            }
            Box(Modifier.matchParentSize().background(Brush.horizontalGradient(listOf(Color(0xFF22242B), Color(0xFF22242B).copy(alpha = 0f)))))
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(eyebrow.uppercase(), style = MaterialTheme.typography.labelSmall, color = Palette.Success, letterSpacing = 1.5.sp)
                Text(title.uppercase(), modifier = Modifier.fillMaxWidth(0.83f), style = MaterialTheme.typography.displayMedium.copy(lineHeight = 48.sp), fontStyle = FontStyle.Italic, color = Color(0xFFF5F5EF))
                Text(subtitle, modifier = Modifier.fillMaxWidth(0.75f), style = MaterialTheme.typography.bodySmall, color = Color(0xFFBDC0C8))
            }
            if (onDelete != null) {
                IconButton(onClick = onDelete, modifier = Modifier.align(Alignment.TopEnd)) {
                    Icon(Icons.Default.DeleteOutline, "Remove program", tint = Color(0xFFBDC0C8))
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().background(Palette.Success).padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(action, modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Color(0xFF182006))
            Icon(Icons.Default.ArrowOutward, null, Modifier.size(20.dp), tint = Color(0xFF182006))
        }
    }
}

/** Giant count-up numeral + caption — the "one big number" that leads each screen. */
@Composable
fun HeroNumber(value: Int, caption: String, color: Color, modifier: Modifier = Modifier) {
    val animated by animateIntAsState(value, tween(900))
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "$animated",
            style = MaterialTheme.typography.displayLarge,
            color = color,
        )
        Text(
            caption,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Flat uppercase section label with optional right-aligned value; replaces Card headers. */
@Composable
fun SectionHeader(
    label: String,
    trailing: String? = null,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth().padding(top = 14.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = color,
            fontWeight = FontWeight.Bold,
        )
        if (trailing != null) {
            Text(
                trailing,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

/** Thick rounded progress bar. */
@Composable
fun FlatBar(progress: Float, color: Color, modifier: Modifier = Modifier, height: Int = 10) {
    Row(modifier.fillMaxWidth().height(height.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(height.dp))) {
        val p = if (progress.isFinite()) progress.coerceIn(0f, 1f) else 0f
        if (p > 0f) {
            Row(
                Modifier.fillMaxWidth(p).height(height.dp)
                    .background(color, RoundedCornerShape(height.dp)),
            ) {}
        }
    }
}
