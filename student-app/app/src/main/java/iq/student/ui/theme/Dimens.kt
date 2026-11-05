package iq.student.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * قياسات محددة ومرتبة لكل الأجهزة.
 * المرجع: شاشة 360dp عرضاً. يكبر القياس تدريجياً مع عرض الشاشة (حد أدنى 0.9 وأعلى 1.25)،
 * وعلى التابلت يُحصر المحتوى بعرض أقصى [contentMaxWidth] ويتوسط الشاشة.
 */
@Immutable
class Dimens(private val scale: Float) {
    private fun d(v: Int): Dp = (v * scale).dp

    val pad = d(20)               // هامش الشاشة
    val gap = d(12)
    val gapSmall = d(8)
    val radiusCard = d(16)
    val radiusField = d(14)
    val radiusButton = d(16)
    val buttonHeight = d(56)
    val fieldHeight = d(52)
    val navHeight = d(72)
    val iconNav = d(22)
    val iconTile = d(56)
    val iconTileRadius = d(18)
    val touchMin = 44.dp          // أقل حجم لمس مريح، لا يتأثر بالتكبير
    val contentMaxWidth = 600.dp
}

val LocalDimens = compositionLocalOf { Dimens(1f) }

@Composable
fun ProvideDimens(content: @Composable () -> Unit) {
    val width = LocalConfiguration.current.screenWidthDp
    val scale = (width / 360f).coerceIn(0.9f, 1.25f)
    CompositionLocalProvider(LocalDimens provides Dimens(scale), content = content)
}

val dimens: Dimens
    @Composable get() = LocalDimens.current
