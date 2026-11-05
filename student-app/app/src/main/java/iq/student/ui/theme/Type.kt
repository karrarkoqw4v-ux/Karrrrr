package iq.student.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import iq.student.R

val Tajawal = FontFamily(
    Font(R.font.tajawal_regular, FontWeight.Normal),
    Font(R.font.tajawal_medium, FontWeight.Medium),
    Font(R.font.tajawal_bold, FontWeight.Bold),
    Font(R.font.tajawal_extrabold, FontWeight.ExtraBold),
)

private fun style(size: Float, weight: FontWeight, line: Float = size * 1.5f) =
    TextStyle(fontFamily = Tajawal, fontSize = size.sp, fontWeight = weight, lineHeight = line.sp)

/** أحجام الخط محددة بالـ sp: display 26 / title 18 / body 14 / label 12 / caption 11 */
val AppTypography = Typography(
    displaySmall = style(26f, FontWeight.ExtraBold, 36f),
    titleLarge = style(18f, FontWeight.Bold, 26f),
    titleMedium = style(16f, FontWeight.Bold, 24f),
    bodyLarge = style(15f, FontWeight.Medium, 22f),
    bodyMedium = style(14f, FontWeight.Normal, 22f),
    labelLarge = style(16f, FontWeight.Bold, 22f),
    labelMedium = style(12f, FontWeight.Medium, 18f),
    labelSmall = style(11f, FontWeight.Medium, 16f),
)
