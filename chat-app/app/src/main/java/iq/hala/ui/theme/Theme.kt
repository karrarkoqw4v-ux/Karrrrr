package iq.hala.ui.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import iq.hala.R

val Primary = Color(0xFF0FB5A0)
val PrimaryDark = Color(0xFF0A8F7E)
val Accent = Color(0xFFFFB020)
val Like = Color(0xFFFF4D6A)
val Danger = Color(0xFFE5484D)
val Success = Color(0xFF16A34A)

/** ألوان الواجهة (فاتح/داكن) */
data class Palette(val bg: Color, val bg2: Color, val ink: Color, val muted: Color, val faint: Color, val line: Color, val soft: Color, val dark: Boolean)

val LightPalette = Palette(Color.White, Color(0xFFF3F6F8), Color(0xFF0E1A24), Color(0xFF5D6B78), Color(0xFF98A4AF), Color(0xFFE6EBEF), Color(0xFFE3F7F4), false)
val DarkPalette = Palette(Color(0xFF0F171E), Color(0xFF1A242D), Color(0xFFF2F6F8), Color(0xFFA5B2BC), Color(0xFF6F7E8A), Color(0xFF26333D), Color(0xFF0F3B38), true)

val LocalPalette = staticCompositionLocalOf { LightPalette }
val pal: Palette @Composable get() = LocalPalette.current

val Tajawal = FontFamily(
    Font(R.font.tajawal_regular, FontWeight.Normal),
    Font(R.font.tajawal_medium, FontWeight.Medium),
    Font(R.font.tajawal_bold, FontWeight.Bold),
    Font(R.font.tajawal_extrabold, FontWeight.ExtraBold),
)

private fun st(size: Float, w: FontWeight, lh: Float = size * 1.5f) = TextStyle(fontFamily = Tajawal, fontSize = size.sp, fontWeight = w, lineHeight = lh.sp)

val HalaTypography = Typography(
    displaySmall = st(26f, FontWeight.ExtraBold, 36f),
    headlineSmall = st(22f, FontWeight.ExtraBold, 30f),
    titleLarge = st(18f, FontWeight.Bold, 26f),
    titleMedium = st(16f, FontWeight.Bold, 24f),
    bodyLarge = st(15f, FontWeight.Medium, 22f),
    bodyMedium = st(14f, FontWeight.Normal, 21f),
    labelLarge = st(15f, FontWeight.Bold, 22f),
    labelMedium = st(12f, FontWeight.Medium, 18f),
    labelSmall = st(11f, FontWeight.Medium, 16f),
)

@Composable
fun HalaTheme(dark: Boolean, content: @Composable () -> Unit) {
    val d = LocalDensity.current
    val capped = Density(d.density, fontScale = d.fontScale.coerceIn(0.85f, 1.15f))   // تحديد تكبير الخط
    val p = if (dark) DarkPalette else LightPalette
    val scheme = if (dark) darkColorScheme(primary = Primary, background = p.bg, surface = p.bg, onSurface = p.ink)
    else lightColorScheme(primary = Primary, background = p.bg, surface = p.bg, onSurface = p.ink)
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl, LocalDensity provides capped, LocalPalette provides p) {
        MaterialTheme(colorScheme = scheme, typography = HalaTypography) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                Box(Modifier.widthIn(max = 600.dp).fillMaxSize()) { content() }
            }
        }
    }
}
