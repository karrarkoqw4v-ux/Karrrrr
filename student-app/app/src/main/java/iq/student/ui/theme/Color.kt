package iq.student.ui.theme

import androidx.compose.ui.graphics.Color

/** ألوان الواجهة الفاتحة — نفس قيم design/tokens.py */
object AppColors {
    val Bg = Color(0xFFF6F7FB)
    val Surface = Color(0xFFFFFFFF)
    val Ink = Color(0xFF101828)
    val Muted = Color(0xFF667085)
    val Faint = Color(0xFF98A2B3)
    val Line = Color(0xFFE4E7EC)
    val Primary = Color(0xFF2F5BEA)
    val PrimarySoft = Color(0xFFE8EEFF)
    val Success = Color(0xFF16A34A)
    val SuccessSoft = Color(0xFFE3F6EA)
    val Danger = Color(0xFFD92D4A)
}

/** لون أيقونة الأداة + خلفيتها الفاتحة */
enum class Tone(val fg: Color, val soft: Color) {
    Blue(Color(0xFF2F5BEA), Color(0xFFE8EEFF)),
    Teal(Color(0xFF0E9384), Color(0xFFDFF5F1)),
    Violet(Color(0xFF7A5AF8), Color(0xFFEEEAFE)),
    Orange(Color(0xFFE8590C), Color(0xFFFFEEE0)),
    Rose(Color(0xFFD92D6A), Color(0xFFFDE7EF)),
    Green(Color(0xFF16A34A), Color(0xFFE3F6EA)),
    Amber(Color(0xFFB7791F), Color(0xFFFFF4D6)),
    Cyan(Color(0xFF0891B2), Color(0xFFDDF4FA)),
}
