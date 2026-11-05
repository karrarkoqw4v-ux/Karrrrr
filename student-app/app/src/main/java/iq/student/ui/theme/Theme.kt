package iq.student.ui.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

private val Scheme = lightColorScheme(
    primary = AppColors.Primary,
    onPrimary = AppColors.Surface,
    background = AppColors.Bg,
    surface = AppColors.Surface,
    onSurface = AppColors.Ink,
    outline = AppColors.Line,
)

@Composable
fun StudentTheme(content: @Composable () -> Unit) {
    val density = LocalDensity.current
    // تحديد تكبير الخط: لا يتجاوز 1.15 حتى لا تنكسر الواجهة عند خط النظام الكبير
    val capped = Density(density.density, fontScale = density.fontScale.coerceIn(0.85f, 1.15f))
    CompositionLocalProvider(
        LocalLayoutDirection provides LayoutDirection.Rtl,
        LocalDensity provides capped,
    ) {
        ProvideDimens {
            MaterialTheme(colorScheme = Scheme, typography = AppTypography) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                    Box(Modifier.widthIn(max = dimens.contentMaxWidth).fillMaxSize()) { content() }
                }
            }
        }
    }
}
