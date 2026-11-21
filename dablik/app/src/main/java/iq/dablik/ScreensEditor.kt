package iq.dablik

import android.graphics.*
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.min
import androidx.compose.ui.graphics.Color as CColor
import androidx.compose.ui.graphics.vector.ImageVector

class TextItem(text: String, val color: Int, size: Float) {
    var text by mutableStateOf(text)
    var size by mutableFloatStateOf(size)
    var x by mutableFloatStateOf(0.5f)
    var y by mutableFloatStateOf(0.5f)
}

class StrokeItem(val color: Int, val width: Float) { val pts = mutableStateListOf<Offset>() }

class EditState {
    var rot by mutableIntStateOf(0)
    var scale by mutableFloatStateOf(1f)
    var ox by mutableFloatStateOf(0f)
    var oy by mutableFloatStateOf(0f)
    var bright by mutableFloatStateOf(0f)
    var contrast by mutableFloatStateOf(1f)
    var sat by mutableFloatStateOf(1f)
    var tint by mutableIntStateOf(Color.WHITE)
    var tintAmt by mutableFloatStateOf(0f)
    var blur by mutableFloatStateOf(0f)
    var outSize by mutableIntStateOf(512)
    val texts = mutableStateListOf<TextItem>()
    val strokes = mutableStateListOf<StrokeItem>()
}

private fun colorMatrix(st: EditState): ColorMatrix {
    val cm = ColorMatrix().apply { setSaturation(st.sat) }
    val c = st.contrast; val off = 128f * (1f - c) + st.bright * 255f
    cm.postConcat(ColorMatrix(floatArrayOf(c, 0f, 0f, 0f, off, 0f, c, 0f, 0f, off, 0f, 0f, c, 0f, off, 0f, 0f, 0f, 1f, 0f)))
    val a = st.tintAmt
    if (a > 0f) {
        val s = floatArrayOf(Color.red(st.tint) / 255f * 1.6f, Color.green(st.tint) / 255f * 1.6f, Color.blue(st.tint) / 255f * 1.6f)
        val l = floatArrayOf(.299f, .587f, .114f)
        val m = FloatArray(20)
        for (k in 0..2) for (j in 0..2) m[k * 5 + j] = (if (k == j) 1f - a else 0f) + a * s[k] * l[j]
        m[18] = 1f
        cm.postConcat(ColorMatrix(m))
    }
    return cm
}

private fun blurPass(src: IntArray, dst: IntArray, w: Int, h: Int, r: Int, horiz: Boolean) {
    val len = if (horiz) w else h; val lines = if (horiz) h else w; val div = 2 * r + 1
    for (line in 0 until lines) {
        var a = 0; var rr = 0; var g = 0; var b = 0
        fun idx(i: Int) = if (horiz) line * w + i else i * w + line
        for (k in -r..r) { val p = src[idx(k.coerceIn(0, len - 1))]; a += p ushr 24; rr += (p shr 16) and 255; g += (p shr 8) and 255; b += p and 255 }
        for (i in 0 until len) {
            dst[idx(i)] = ((a / div) shl 24) or ((rr / div) shl 16) or ((g / div) shl 8) or (b / div)
            val add = src[idx((i + r + 1).coerceAtMost(len - 1))]; val rem = src[idx((i - r).coerceAtLeast(0))]
            a += (add ushr 24) - (rem ushr 24); rr += ((add shr 16) and 255) - ((rem shr 16) and 255)
            g += ((add shr 8) and 255) - ((rem shr 8) and 255); b += (add and 255) - (rem and 255)
        }
    }
}

private fun boxBlur(src: Bitmap, r: Int): Bitmap {
    val w = src.width; val h = src.height
    val a = IntArray(w * h); val b = IntArray(w * h)
    src.getPixels(a, 0, w, 0, 0, w, h)
    repeat(2) { blurPass(a, b, w, h, r, true); blurPass(b, a, w, h, r, false) }
    return Bitmap.createBitmap(a, w, h, Bitmap.Config.ARGB_8888)
}

/** يرسم الأيقونة النهائية (مربعة) من الصورة والتعديلات */
fun renderIcon(src: Bitmap, st: EditState, out: Int): Bitmap {
    val layer = Bitmap.createBitmap(out, out, Bitmap.Config.ARGB_8888)
    val c = Canvas(layer)
    val w = src.width.toFloat(); val h = src.height.toFloat()
    val sideways = st.rot % 2 != 0
    val base = out / min(if (sideways) h else w, if (sideways) w else h) * st.scale
    val m = Matrix().apply {
        postTranslate(-w / 2, -h / 2); postRotate(st.rot * 90f); postScale(base, base)
        postTranslate(out / 2f + st.ox * out, out / 2f + st.oy * out)
    }
    val p = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG).apply { colorFilter = ColorMatrixColorFilter(colorMatrix(st)) }
    c.drawBitmap(src, m, p)
    val res = if (st.blur > 0.5f) boxBlur(layer, (st.blur * out / 512f).toInt().coerceAtLeast(1)) else layer
    val c2 = Canvas(res)
    for (s in st.strokes) {
        if (s.pts.size < 2) continue
        val sp = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND; color = s.color; strokeWidth = s.width * out }
        val path = Path(); path.moveTo(s.pts[0].x * out, s.pts[0].y * out)
        for (i in 1 until s.pts.size) path.lineTo(s.pts[i].x * out, s.pts[i].y * out)
        c2.drawPath(path, sp)
    }
    for (t in st.texts) {
        val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = t.color; textSize = t.size * out; textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD; setShadowLayer(out * 0.01f, 0f, 0f, Color.argb(160, 0, 0, 0)) }
        c2.drawText(t.text, t.x * out, t.y * out + tp.textSize * 0.35f, tp)
    }
    return res
}

private fun loadBitmap(ctx: android.content.Context, uri: Uri): Pair<Bitmap, Int>? = runCatching {
    val bo = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bo) }
    var sample = 1
    while (bo.outWidth / sample > 1600 || bo.outHeight / sample > 1600) sample *= 2
    val bmp = ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample }) }!!
    val rot = runCatching {
        ctx.contentResolver.openInputStream(uri)?.use { s ->
            when (android.media.ExifInterface(s).getAttributeInt(android.media.ExifInterface.TAG_ORIENTATION, 1)) { 6 -> 1; 3 -> 2; 8 -> 3; else -> 0 }
        } ?: 0
    }.getOrDefault(0)
    bmp to rot
}.getOrNull()

private enum class Tool(val label: String, val icon: ImageVector) {
    CROP("قص", Icons.Outlined.Crop), ADJUST("توضيح", Icons.Outlined.Tune), TINT("تلوين", Icons.Outlined.Palette),
    BLUR("تغويش", Icons.Outlined.BlurOn), TEXT("خط", Icons.Outlined.TextFields), DRAW("رسم", Icons.Outlined.Brush),
}

private val palette = listOf(Color.WHITE, Color.BLACK, 0xFFFF4D4D.toInt(), 0xFFFF9F1C.toInt(), 0xFFFFD60A.toInt(), 0xFF30D158.toInt(), 0xFF3E91FF.toInt(), 0xFFAF52DE.toInt(), 0xFFFF6BAA.toInt())

@Composable
private fun Swatches(selected: Int, onPick: (Int) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        palette.forEach { c ->
            Box(Modifier.size(38.dp).oneClick { onPick(c) }.clip(CircleShape).background(CColor(c)).border(if (selected == c) 3.dp else 1.dp, if (selected == c) OneUi.Blue else OneUi.Line, CircleShape))
        }
    }
}

@Composable
private fun LabeledSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = OneUi.Muted)
        Slider(value, onChange, valueRange = range, colors = SliderDefaults.colors(thumbColor = OneUi.Blue, activeTrackColor = OneUi.Blue, inactiveTrackColor = OneUi.Card2))
    }
}

@Composable
fun IconEditorScreen(uri: Uri, onClose: () -> Unit, onSaved: (String) -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val st = remember { EditState() }
    var src by remember { mutableStateOf<Bitmap?>(null) }
    var failed by remember { mutableStateOf(false) }
    var tool by remember { mutableStateOf(Tool.CROP) }
    var drawColor by remember { mutableIntStateOf(Color.WHITE) }
    var drawWidth by remember { mutableFloatStateOf(0.03f) }
    var textColor by remember { mutableIntStateOf(Color.WHITE) }
    var textSize by remember { mutableFloatStateOf(0.14f) }
    var selText by remember { mutableIntStateOf(-1) }
    var textDlg by remember { mutableStateOf(false) }
    var textIn by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }

    LaunchedEffect(uri) {
        val r = withContext(Dispatchers.IO) { loadBitmap(ctx, uri) }
        if (r == null) failed = true else { src = r.first; st.rot = r.second }
    }

    Box(Modifier.fillMaxSize().background(OneUi.Bg)) {
        Column(Modifier.fillMaxSize()) {
            OneTopBar("تعديل الأيقونة", onClose) {
                Box(Modifier.oneClick(src != null && !saving) {
                    saving = true
                    scope.launch {
                        val path = withContext(Dispatchers.Default) {
                            val bmp = renderIcon(src!!, st, st.outSize)
                            val dir = File(ctx.filesDir, "icons").apply { mkdirs() }
                            val f = File(dir, "icon_${System.currentTimeMillis()}.png")
                            f.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
                            f.absolutePath
                        }
                        onSaved(path)
                    }
                }.glass(RoundedCornerShape(22.dp), OneUi.Blue, 0.9f, 0.75f).padding(horizontal = 22.dp, vertical = 10.dp)) {
                    Text(if (saving) "..." else "حفظ", style = MaterialTheme.typography.labelLarge, color = CColor.White)
                }
            }
            val s = src
            if (failed) Text("تعذر فتح الصورة", Modifier.padding(32.dp), color = OneUi.Danger)
            if (s != null) {
                BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 6.dp), contentAlignment = Alignment.Center) {
                    val vp = min(maxWidth.value, 340f).dp
                    val vpPx = with(LocalDensity.current) { vp.toPx() }
                    val preview = renderIcon(s, st, 420).asImageBitmap()
                    Box(
                        Modifier.size(vp).clip(RoundedCornerShape(26.dp)).background(OneUi.Card2).pointerInput(tool, selText) {
                            when (tool) {
                                Tool.CROP -> detectTransformGestures { _, pan, zoom, _ ->
                                    st.scale = (st.scale * zoom).coerceIn(0.4f, 8f); st.ox += pan.x / vpPx; st.oy += pan.y / vpPx
                                }
                                Tool.DRAW -> {
                                    var cur: StrokeItem? = null
                                    detectDragGestures(
                                        onDragStart = { o -> cur = StrokeItem(drawColor, drawWidth).also { it.pts.add(Offset(o.x / vpPx, o.y / vpPx)); st.strokes.add(it) } },
                                        onDrag = { ch, _ -> cur?.pts?.add(Offset(ch.position.x / vpPx, ch.position.y / vpPx)) },
                                    )
                                }
                                Tool.TEXT -> detectDragGestures { _, d ->
                                    st.texts.getOrNull(selText)?.let { it.x += d.x / vpPx; it.y += d.y / vpPx }
                                }
                                else -> {}
                            }
                        },
                    ) { Image(preview, null, Modifier.fillMaxSize()) }
                }
                // لوحة الأداة
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    when (tool) {
                        Tool.CROP -> {
                            Text("حرّك الصورة بإصبع واحد وكبّرها بإصبعين", style = MaterialTheme.typography.labelMedium, color = OneUi.Muted)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.oneClick { st.rot = (st.rot + 1) % 4 }.clip(RoundedCornerShape(18.dp)).background(OneUi.Card).padding(horizontal = 16.dp, vertical = 10.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Outlined.RotateRight, null, tint = OneUi.Blue); Spacer(Modifier.width(6.dp)); Text("تدوير", color = OneUi.Ink, style = MaterialTheme.typography.labelMedium) }
                                }
                                Box(Modifier.oneClick { st.scale = 1f; st.ox = 0f; st.oy = 0f }.clip(RoundedCornerShape(18.dp)).background(OneUi.Card).padding(horizontal = 16.dp, vertical = 10.dp)) { Text("إعادة ضبط", color = OneUi.Ink, style = MaterialTheme.typography.labelMedium) }
                            }
                            Text("قياس الأيقونة", style = MaterialTheme.typography.labelMedium, color = OneUi.Muted)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(128, 192, 512).forEach { n ->
                                    Box(Modifier.oneClick { st.outSize = n }.clip(RoundedCornerShape(18.dp)).background(if (st.outSize == n) OneUi.BlueSoft else OneUi.Card).padding(horizontal = 18.dp, vertical = 10.dp)) {
                                        Text("$n px", color = if (st.outSize == n) OneUi.Blue else OneUi.Ink, style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                        }
                        Tool.ADJUST -> {
                            LabeledSlider("السطوع", st.bright, -0.5f..0.5f) { st.bright = it }
                            LabeledSlider("التباين (الوضوح)", st.contrast, 0.5f..1.6f) { st.contrast = it }
                            LabeledSlider("تشبّع الألوان", st.sat, 0f..2f) { st.sat = it }
                        }
                        Tool.TINT -> {
                            Text("اختر لون التلوين", style = MaterialTheme.typography.labelMedium, color = OneUi.Muted)
                            Swatches(st.tint) { st.tint = it; if (st.tintAmt == 0f) st.tintAmt = 0.6f }
                            LabeledSlider("شدة اللون", st.tintAmt, 0f..1f) { st.tintAmt = it }
                        }
                        Tool.BLUR -> LabeledSlider("قوة التغويش", st.blur, 0f..24f) { st.blur = it }
                        Tool.TEXT -> {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.oneClick { textIn = ""; textDlg = true }.clip(RoundedCornerShape(18.dp)).background(OneUi.Blue).padding(horizontal = 18.dp, vertical = 10.dp)) { Text("إضافة نص", color = CColor.White, style = MaterialTheme.typography.labelMedium) }
                                if (selText in st.texts.indices) Box(Modifier.oneClick { st.texts.removeAt(selText); selText = st.texts.lastIndex }.clip(RoundedCornerShape(18.dp)).background(OneUi.Card).padding(horizontal = 18.dp, vertical = 10.dp)) { Text("حذف النص", color = OneUi.Danger, style = MaterialTheme.typography.labelMedium) }
                            }
                            Text("اسحب النص على الصورة لتحريكه", style = MaterialTheme.typography.labelMedium, color = OneUi.Muted)
                            Swatches(textColor) { c -> textColor = c }
                            LabeledSlider("حجم الخط", textSize, 0.06f..0.3f) { v -> textSize = v; st.texts.getOrNull(selText)?.size = v }
                        }
                        Tool.DRAW -> {
                            Swatches(drawColor) { drawColor = it }
                            LabeledSlider("سمك القلم", drawWidth, 0.01f..0.1f) { drawWidth = it }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(Modifier.oneClick { if (st.strokes.isNotEmpty()) st.strokes.removeAt(st.strokes.lastIndex) }.clip(RoundedCornerShape(18.dp)).background(OneUi.Card).padding(horizontal = 18.dp, vertical = 10.dp)) { Text("تراجع", color = OneUi.Ink, style = MaterialTheme.typography.labelMedium) }
                                Box(Modifier.oneClick { st.strokes.clear() }.clip(RoundedCornerShape(18.dp)).background(OneUi.Card).padding(horizontal = 18.dp, vertical = 10.dp)) { Text("مسح الرسم", color = OneUi.Danger, style = MaterialTheme.typography.labelMedium) }
                            }
                        }
                    }
                }
                // شريط الأدوات السفلي
                Row(Modifier.fillMaxWidth().navigationBarsPadding().background(OneUi.Card).padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Tool.entries.forEach { t ->
                        Column(Modifier.oneClick { tool = t }.padding(horizontal = 8.dp, vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(Modifier.size(46.dp).clip(CircleShape).background(if (tool == t) OneUi.BlueSoft else CColor.Transparent), contentAlignment = Alignment.Center) {
                                Icon(t.icon, t.label, tint = if (tool == t) OneUi.Blue else OneUi.Muted, modifier = Modifier.size(26.dp))
                            }
                            Text(t.label, style = MaterialTheme.typography.labelSmall, color = if (tool == t) OneUi.Blue else OneUi.Muted)
                        }
                    }
                }
            } else if (!failed) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = OneUi.Blue) }
        }
    }
    if (textDlg) AlertDialog(
        onDismissRequest = { textDlg = false }, containerColor = OneUi.Card,
        title = { Text("النص", color = OneUi.Ink) },
        text = { OutlinedTextField(textIn, { textIn = it }, singleLine = true) },
        confirmButton = { TextButton({ if (textIn.isNotBlank()) { st.texts.add(TextItem(textIn.trim(), textColor, textSize)); selText = st.texts.lastIndex }; textDlg = false }) { Text("إضافة", color = OneUi.Blue) } },
        dismissButton = { TextButton({ textDlg = false }) { Text("إلغاء", color = OneUi.Muted) } },
    )
}
