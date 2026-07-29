package dev.dwm.liftlog.ui

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Overload neon-athletic palette: dark navy base, electric accents
object Palette {
    val Success = Color(0xFF00E676) // completed sets, finish button — electric green
    val Protein = Color(0xFFFF8A50)
    val Fat = Color(0xFFFFD54F)
    val Carbs = Color(0xFFC6FF00)
    val Calories = Color(0xFF40C4FF)
    val Pr = Color(0xFFFFD740)
    val Boost = Color(0xFF00E676)  // workout accents — electric green
    val Volt = Color(0xFF22D3EE)   // cyan secondary accent
    val Trend = Color(0xFF9B8CFF)  // MacroFactor purple — trend lines
}

private val Scheme = darkColorScheme(
    primary = Color(0xFF00E676),
    onPrimary = Color(0xFF00310F),
    secondary = Color(0xFF22D3EE),
    onSecondary = Color(0xFF002B33),
    background = Color(0xFF0A0E14),
    surface = Color(0xFF111826),
    surfaceVariant = Color(0xFF1C2536),
    surfaceContainer = Color(0xFF141C2B),
    surfaceContainerHigh = Color(0xFF1C2536),
    onSurface = Color(0xFFE9EDF5),
    onSurfaceVariant = Color(0xFF93A0B4),
    onBackground = Color(0xFFE9EDF5),
    outline = Color(0xFF2E3A50),
)

// numbers are the hero: heavier display/headline weights, tighter labels
private val OverloadTypography = Typography().run {
    copy(
        displayLarge = displayLarge.copy(fontWeight = FontWeight.ExtraBold),
        displayMedium = displayMedium.copy(fontWeight = FontWeight.ExtraBold),
        displaySmall = displaySmall.copy(fontWeight = FontWeight.ExtraBold),
        headlineLarge = headlineLarge.copy(fontWeight = FontWeight.ExtraBold),
        headlineMedium = headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
        headlineSmall = headlineSmall.copy(fontWeight = FontWeight.Bold),
        titleLarge = titleLarge.copy(fontWeight = FontWeight.Bold),
        titleMedium = titleMedium.copy(fontWeight = FontWeight.Bold),
        labelMedium = labelMedium.copy(letterSpacing = 0.8.sp),
        labelSmall = labelSmall.copy(letterSpacing = 0.8.sp),
    )
}

@Composable
fun LiftLogTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, typography = OverloadTypography) {
        // Scaffold/Box use containerColor=Transparent, and contentColorFor(Transparent) is
        // Unspecified — which Text renders as BLACK on our dark background. Every heading outside
        // a Card was invisible until this. Cards override it themselves with onSurface.
        CompositionLocalProvider(LocalContentColor provides Scheme.onBackground, content = content)
    }
}
