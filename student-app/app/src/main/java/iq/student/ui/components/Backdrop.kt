package iq.student.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color

/** خلفية متحركة هادئة: دوائر ملونة شفافة تتحرك ببطء خلف شاشات الدخول والتسجيل */
@Composable
fun AnimatedBackdrop(modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "backdrop")
    val a by t.animateFloat(0f, 1f, infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Reverse), label = "a")
    val b by t.animateFloat(1f, 0f, infiniteRepeatable(tween(12000, easing = LinearEasing), RepeatMode.Reverse), label = "b")
    Canvas(modifier.fillMaxSize()) {
        val m = size.minDimension
        drawCircle(Color(0x182F5BEA), m * 0.55f, Offset(size.width * (0.12f + 0.22f * a), size.height * (0.08f + 0.06f * b)))
        drawCircle(Color(0x1C0E9384), m * 0.42f, Offset(size.width * (0.92f - 0.26f * b), size.height * (0.30f + 0.05f * a)))
        drawCircle(Color(0x167A5AF8), m * 0.50f, Offset(size.width * (0.45f + 0.22f * a), size.height * (0.97f - 0.08f * b)))
    }
}
