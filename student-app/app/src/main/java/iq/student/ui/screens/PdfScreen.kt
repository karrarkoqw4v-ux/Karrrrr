package iq.student.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import iq.student.ui.components.ErrorBox
import iq.student.ui.components.ScreenHeader
import iq.student.ui.theme.AppColors
import iq.student.util.Books
import iq.student.util.PdfDoc

/**
 * قارئ PDF داخل التطبيق:
 * - تكبير وتصغير بإصبعين (حتى 5 أضعاف) مع سحب أفقي أثناء التكبير
 * - ضغطتين سريعتين للتكبير/الرجوع
 * - تمرير عمودي سلس بين الصفحات + مؤشر رقم الصفحة
 */
@Composable
fun PdfScreen(title: String, ref: String?, onBack: () -> Unit) {
    val ctx = LocalContext.current
    var doc by remember { mutableStateOf<PdfDoc?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val density = LocalDensityPx()
    val renderWidth = (density.screenWidthPx * 1.6f).toInt()

    LaunchedEffect(ref) {
        if (ref == null) { error = "ما في ملف مضاف"; return@LaunchedEffect }
        try { doc = PdfDoc(Books.resolve(ctx, ref)) } catch (e: Exception) { error = e.message ?: "تعذر فتح الملف" }
    }
    DisposableEffect(Unit) { onDispose { doc?.close() } }

    Column(Modifier.fillMaxSize().background(Color(0xFFEEF0F6))) {
        Box(Modifier.fillMaxWidth().background(AppColors.Surface)) { ScreenHeader(title, onBack) }
        val d = doc
        when {
            error != null -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) { ErrorBox(error!!) }
            d == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = AppColors.Primary) }
            else -> ZoomablePages(d, renderWidth)
        }
    }
}

private class Px(val screenWidthPx: Int)

@Composable
private fun LocalDensityPx(): Px {
    val cfg = androidx.compose.ui.platform.LocalConfiguration.current
    val dens = androidx.compose.ui.platform.LocalDensity.current
    return Px(with(dens) { cfg.screenWidthDp.dp.roundToPx() })
}

@Composable
private fun ZoomablePages(d: PdfDoc, renderWidth: Int) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offX by remember { mutableFloatStateOf(0f) }
    val shownScale by animateFloatAsState(scale, spring(Spring.DampingRatioNoBouncy, Spring.StiffnessHigh), label = "zoom")
    val shownOff by animateFloatAsState(offX, spring(Spring.DampingRatioNoBouncy, Spring.StiffnessHigh), label = "pan")
    val listState = rememberLazyListState()

    BoxWithConstraints(Modifier.fillMaxSize().clipToBounds()) {
        val w = constraints.maxWidth.toFloat()
        fun clampX(x: Float, s: Float) = x.coerceIn(-(w * (s - 1f) / 2f), w * (s - 1f) / 2f)

        Box(
            Modifier.fillMaxSize()
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        do {
                            val ev = awaitPointerEvent(PointerEventPass.Initial)
                            if (ev.changes.count { it.pressed } >= 2) {
                                val z = ev.calculateZoom()
                                val pan = ev.calculatePan()
                                scale = (scale * z).coerceIn(1f, 5f)
                                offX = clampX(offX + pan.x, scale)
                                ev.changes.forEach { if (it.positionChanged()) it.consume() }
                            } else if (scale > 1f) {
                                offX = clampX(offX + ev.calculatePan().x, scale)   // سحب أفقي أثناء التكبير
                            }
                        } while (ev.changes.any { it.pressed })
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures(onDoubleTap = { if (scale > 1f) { scale = 1f; offX = 0f } else scale = 2.5f })
                },
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().graphicsLayer {
                    scaleX = shownScale; scaleY = shownScale; translationX = shownOff
                },
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(List(d.pageCount) { it }, key = { it }) { index ->
                    var bmp by remember(index) { mutableStateOf<android.graphics.Bitmap?>(null) }
                    LaunchedEffect(index) { bmp = runCatching { d.render(index, renderWidth) }.getOrNull() }
                    val b = bmp
                    if (b != null) Image(b.asImageBitmap(), "صفحة ${index + 1}", Modifier.fillMaxWidth(), contentScale = ContentScale.FillWidth)
                    else Box(Modifier.fillMaxWidth().height(420.dp).background(AppColors.Surface), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AppColors.Primary, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                    }
                }
            }
        }
        Box(
            Modifier.align(Alignment.BottomCenter).padding(bottom = 18.dp).clip(RoundedCornerShape(24.dp))
                .background(Color(0xE6101828)).padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text("${listState.firstVisibleItemIndex + 1} / ${d.pageCount}", style = MaterialTheme.typography.labelMedium, color = Color.White)
        }
    }
}
