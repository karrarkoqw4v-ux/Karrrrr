package iq.student.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * ضغطة سلسة: ينكمش العنصر بنطّة نابضة ويميل ميلاناً خفيفاً (تأثير ثلاثي الأبعاد) ثم يرجع.
 */
fun Modifier.bounceClick(enabled: Boolean = true, onClick: () -> Unit): Modifier = composed {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.93f else 1f, spring(dampingRatio = 0.42f, stiffness = 520f), label = "scale")
    val tilt by animateFloatAsState(if (pressed) 6f else 0f, spring(dampingRatio = 0.5f, stiffness = 420f), label = "tilt")
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
            rotationX = tilt
            cameraDistance = 14f * density
        }
        .clickable(interactionSource = source, indication = null, enabled = enabled, onClick = onClick)
}

/** دخول متتابع: يظهر العنصر بتلاشي وصعود بعد تأخير حسب ترتيبه */
@Composable
fun Entrance(index: Int, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(index * 55L); shown = true }
    val alpha by animateFloatAsState(if (shown) 1f else 0f, tween(420), label = "a")
    val lift by animateDpAsState(if (shown) 0.dp else 28.dp, spring(dampingRatio = 0.72f, stiffness = 300f), label = "y")
    val grow by animateFloatAsState(if (shown) 1f else 0.94f, tween(420), label = "g")
    Box(modifier.graphicsLayer {
        this.alpha = alpha
        translationY = lift.toPx()
        scaleX = grow
        scaleY = grow
    }) { content() }
}

/** حركة خفيفة مستمرة (طفو) للشعارات */
@Composable
fun rememberFloat(amplitudeDp: Float = 6f, periodMs: Int = 2600): Float {
    val t = rememberInfiniteTransition(label = "float")
    val v by t.animateFloat(-amplitudeDp, amplitudeDp, infiniteRepeatable(tween(periodMs, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "f")
    return v
}
