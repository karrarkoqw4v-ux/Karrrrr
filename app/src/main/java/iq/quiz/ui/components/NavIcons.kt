package iq.quiz.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** أيقونات الشريط السفلي مرسومة يدوياً بنفس شكل إنستغرام. kind: 0 رئيسية، 1 ريلز، 2 رسائل، 3 بحث */
@Composable
fun NavGlyph(kind: Int, selected: Boolean, tint: Color, cut: Color, size: Dp = 34.dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val sw = w * (if (selected) 0.095f else 0.085f)
        val st = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round)
        fun p(x: Float, y: Float) = Offset(x * w, y * w)
        when (kind) {
            0 -> {
                val path = Path().apply {
                    moveTo(.50f * w, .10f * w); lineTo(.92f * w, .46f * w); lineTo(.80f * w, .46f * w); lineTo(.80f * w, .88f * w)
                    lineTo(.60f * w, .88f * w); lineTo(.60f * w, .64f * w); lineTo(.40f * w, .64f * w); lineTo(.40f * w, .88f * w)
                    lineTo(.20f * w, .88f * w); lineTo(.20f * w, .46f * w); lineTo(.08f * w, .46f * w); close()
                }
                if (selected) drawPath(path, tint, style = Fill)
                drawPath(path, tint, style = st)
            }
            1 -> {
                val tl = p(.10f, .10f); val sz = Size(.80f * w, .80f * w); val r = CornerRadius(.22f * w)
                if (selected) drawRoundRect(tint, tl, sz, r, style = Fill)
                drawRoundRect(tint, tl, sz, r, style = st)
                val tri = Path().apply { moveTo(.41f * w, .32f * w); lineTo(.68f * w, .50f * w); lineTo(.41f * w, .68f * w); close() }
                drawPath(tri, if (selected) cut else tint, style = if (selected) Fill else st)
                if (selected) drawPath(tri, cut, style = st)
            }
            2 -> {
                val plane = Path().apply {
                    moveTo(.90f * w, .10f * w); lineTo(.10f * w, .44f * w); lineTo(.42f * w, .58f * w); lineTo(.56f * w, .90f * w); close()
                }
                if (selected) drawPath(plane, tint, style = Fill)
                drawPath(plane, tint, style = st)
                drawLine(if (selected) cut else tint, p(.42f, .58f), p(.90f, .10f), strokeWidth = sw, cap = StrokeCap.Round)
            }
            else -> {
                drawCircle(tint, radius = .30f * w, center = p(.45f, .45f), style = st)
                drawLine(tint, p(.68f, .68f), p(.90f, .90f), strokeWidth = sw, cap = StrokeCap.Round)
            }
        }
    }
}
