package iq.quiz.ui.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import iq.quiz.R

/** الأزرار والروابط: أزرق واضح */
val Primary = Color(0xFF2F8CFF)
val PrimaryDark = Color(0xFF1B6FE0)

/** تدرج الهوية (حلقات القصص والشعار): بنفسجي - وردي - كهرماني */
val Brand1 = Color(0xFF7C3AED)
val Brand2 = Color(0xFFEC4899)
val Brand3 = Color(0xFFF59E0B)
val BrandBrush = Brush.linearGradient(listOf(Brand1, Brand2, Brand3))

val Accent = Brand3
val Like = Color(0xFFFF3B5C)
val Danger = Color(0xFFFF4D4F)
val Success = Color(0xFF34C759)

/** ألوان الواجهة (داكن افتراضي، وفاتح اختياري) */
data class Palette(val bg: Color, val bg2: Color, val ink: Color, val muted: Color, val faint: Color, val line: Color, val soft: Color, val dark: Boolean)

val LightPalette = Palette(Color.White, Color(0xFFF2F2F7), Color(0xFF111114), Color(0xFF65656F), Color(0xFF9A9AA4), Color(0xFFE5E5EA), Color(0xFFE6F0FF), false)
val DarkPalette = Palette(Color(0xFF000000), Color(0xFF1C1C1E), Color(0xFFF5F5F7), Color(0xFFA1A1AA), Color(0xFF6B6B73), Color(0xFF2C2C2E), Color(0xFF0F2A4A), true)

val LocalPalette = staticCompositionLocalOf { DarkPalette }
val pal: Palette @Composable get() = LocalPalette.current

/** خط سميك ونظيف قريب من خط آيفون (IBM Plex Sans Arabic) */
val AppFont = FontFamily(
    Font(R.font.plex_regular, FontWeight.Normal),
    Font(R.font.plex_medium, FontWeight.Medium),
    Font(R.font.plex_semibold, FontWeight.SemiBold),
    Font(R.font.plex_bold, FontWeight.Bold),
    Font(R.font.plex_bold, FontWeight.ExtraBold),
)

private fun st(size: Float, w: FontWeight, lh: Float = size * 1.5f) = TextStyle(fontFamily = AppFont, fontSize = size.sp, fontWeight = w, lineHeight = lh.sp)

val AppTypography = Typography(
    displaySmall = st(28f, FontWeight.Bold, 38f),
    headlineSmall = st(24f, FontWeight.Bold, 33f),
    titleLarge = st(20f, FontWeight.Bold, 28f),
    titleMedium = st(17f, FontWeight.Bold, 25f),
    bodyLarge = st(16f, FontWeight.SemiBold, 24f),
    bodyMedium = st(15f, FontWeight.Medium, 23f),
    labelLarge = st(16f, FontWeight.Bold, 24f),
    labelMedium = st(13.5f, FontWeight.SemiBold, 20f),
    labelSmall = st(12.5f, FontWeight.Medium, 18f),
)

@Composable
fun AppTheme(dark: Boolean, content: @Composable () -> Unit) {
    val d = LocalDensity.current
    val capped = Density(d.density, fontScale = d.fontScale.coerceIn(0.85f, 1.15f))   // تحديد تكبير الخط
    val p = if (dark) DarkPalette else LightPalette
    val scheme = if (dark) darkColorScheme(primary = Primary, background = p.bg, surface = p.bg, onSurface = p.ink)
    else lightColorScheme(primary = Primary, background = p.bg, surface = p.bg, onSurface = p.ink)
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl, LocalDensity provides capped, LocalPalette provides p) {
        MaterialTheme(colorScheme = scheme, typography = AppTypography) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                Box(Modifier.widthIn(max = 600.dp).fillMaxSize()) { content() }
            }
        }
    }
}
