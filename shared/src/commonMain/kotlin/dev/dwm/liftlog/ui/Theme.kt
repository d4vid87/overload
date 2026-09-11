package dev.dwm.liftlog.ui

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.Shapes
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import org.jetbrains.compose.resources.Font
import overload.shared.generated.resources.Res
import overload.shared.generated.resources.barlow_condensed_bold
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp

// Editorial sportswear: ink, chalk, acid yellow, and violet data accents.
object Palette {
    val Success = Color(0xFFD9FF70)
    val Protein = Color(0xFFE8AD94)
    val Fat = Color(0xFFE5CD91)
    val Carbs = Color(0xFFABBCE8)
    val Calories = Color(0xFFD9FF70)
    val Pr = Color(0xFFE5CD91)
    val Boost = Success
    val Volt = Color(0xFFBDB4FF)
    val Trend = Color(0xFFB8ADF0)
}

private val Scheme = darkColorScheme(
    primary = Palette.Success,
    onPrimary = Color(0xFF182006),
    primaryContainer = Color(0xFF29311C),
    onPrimaryContainer = Palette.Success,
    secondary = Palette.Volt,
    onSecondary = Color(0xFF102B32),
    secondaryContainer = Color(0xFF2D3422),
    onSecondaryContainer = Palette.Success,
    tertiary = Palette.Trend,
    background = Color(0xFF0D0E11),
    surface = Color(0xFF141519),
    surfaceDim = Color(0xFF0D0E11),
    surfaceBright = Color(0xFF303940),
    surfaceVariant = Color(0xFF33363F),
    surfaceContainerLowest = Color(0xFF0B0F12),
    surfaceContainerLow = Color(0xFF171E23),
    surfaceContainer = Color(0xFF1B2329),
    surfaceContainerHigh = Color(0xFF232C32),
    surfaceContainerHighest = Color(0xFF1B1D23),
    onSurface = Color(0xFFF0F3F1),
    onSurfaceVariant = Color(0xFFA6A7B2),
    onBackground = Color(0xFFF0F3F1),
    outline = Color(0xFF52616A),
    outlineVariant = Color(0xFF2B363D),
)

// numbers are the hero: heavier display/headline weights, tighter labels
private val OverloadTypography = Typography().run {
    copy(
        displayLarge = displayLarge.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-2).sp),
        displayMedium = displayMedium.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-1).sp),
        displaySmall = displaySmall.copy(fontWeight = FontWeight.SemiBold),
        headlineLarge = headlineLarge.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-1).sp),
        headlineMedium = headlineMedium.copy(fontWeight = FontWeight.SemiBold),
        headlineSmall = headlineSmall.copy(fontWeight = FontWeight.Bold),
        titleLarge = titleLarge.copy(fontWeight = FontWeight.Bold),
        titleMedium = titleMedium.copy(fontWeight = FontWeight.Bold),
        labelMedium = labelMedium.copy(letterSpacing = 0.2.sp),
        labelSmall = labelSmall.copy(letterSpacing = 0.3.sp),
    )
}

@Composable
fun LiftLogTheme(content: @Composable () -> Unit) {
    val display = FontFamily(Font(Res.font.barlow_condensed_bold, FontWeight.Bold))
    MaterialTheme(
        colorScheme = Scheme,
        typography = OverloadTypography.run {
            copy(
                displayLarge = displayLarge.copy(fontFamily = display, fontSize = 64.sp, letterSpacing = (-1).sp),
                displayMedium = displayMedium.copy(fontFamily = display, fontSize = 52.sp),
                displaySmall = displaySmall.copy(fontFamily = display, fontSize = 40.sp),
                headlineLarge = headlineLarge.copy(fontFamily = display, fontSize = 40.sp, letterSpacing = 0.sp),
                headlineMedium = headlineMedium.copy(fontFamily = display, fontSize = 32.sp),
                headlineSmall = headlineSmall.copy(fontFamily = display, fontSize = 28.sp),
            )
        },
        shapes = Shapes(
            extraSmall = RoundedCornerShape(8.dp), small = RoundedCornerShape(12.dp),
            medium = RoundedCornerShape(16.dp), large = RoundedCornerShape(20.dp),
            extraLarge = RoundedCornerShape(24.dp),
        ),
    ) {
        // Scaffold/Box use containerColor=Transparent, and contentColorFor(Transparent) is
        // Unspecified — which Text renders as BLACK on our dark background. Every heading outside
        // a Card was invisible until this. Cards override it themselves with onSurface.
        CompositionLocalProvider(LocalContentColor provides Scheme.onBackground, content = content)
    }
}
