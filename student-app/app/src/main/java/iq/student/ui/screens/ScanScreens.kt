package iq.student.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import iq.student.ui.components.*
import iq.student.ui.theme.AppColors
import iq.student.ui.theme.dimens
import iq.student.util.ScanSession
import iq.student.util.cropFractions
import iq.student.util.loadBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** شاشة الكاميرا: تصوير مباشر أو اختيار من المعرض */
@Composable
fun CaptureScreen(title: String, hint: String, onBack: () -> Unit, onCaptured: () -> Unit) {
    val ctx = LocalContext.current
    val owner = ctx as LifecycleOwner
    val scope = rememberCoroutineScope()
    var granted by remember { mutableStateOf(ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) }
    var busy by remember { mutableStateOf(false) }
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    val imageCapture = remember { ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build() }

    fun useUri(uri: Uri) {
        busy = true
        scope.launch {
            val bmp = withContext(Dispatchers.IO) { loadBitmap(ctx, uri) }
            busy = false
            if (bmp == null) Toast.makeText(ctx, "تعذر فتح الصورة", Toast.LENGTH_SHORT).show()
            else { ScanSession.raw = bmp; onCaptured() }
        }
    }

    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> if (uri != null) useUri(uri) }
    LaunchedEffect(Unit) { if (!granted) permLauncher.launch(Manifest.permission.CAMERA) }

    Box(Modifier.fillMaxSize().background(Color(0xFFE9ECF3))) {
        if (granted) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { c ->
                    PreviewView(c).also { pv ->
                        val future = ProcessCameraProvider.getInstance(c)
                        future.addListener({
                            val provider = future.get()
                            val preview = Preview.Builder().build().also { it.setSurfaceProvider(pv.surfaceProvider) }
                            runCatching {
                                provider.unbindAll()
                                provider.bindToLifecycle(owner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture)
                            }
                        }, ContextCompat.getMainExecutor(c))
                    }
                },
            )
        } else {
            Column(Modifier.align(Alignment.Center).padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("نحتاج إذن الكاميرا حتى نصوّر الصفحة", style = MaterialTheme.typography.bodyLarge, color = AppColors.Ink)
                Spacer(Modifier.height(12.dp))
                AppButton("السماح بالكاميرا", { permLauncher.launch(Manifest.permission.CAMERA) })
            }
        }

        // أعلى
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = dimens.pad, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            RoundBtn(Icons.Outlined.Close, "إغلاق", onBack)
            Spacer(Modifier.weight(1f))
            Pill(title)
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.size(dimens.touchMin))
        }
        Box(Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 66.dp)) { Pill(hint, small = true) }

        // أسفل
        Surface(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp), color = AppColors.Surface, shadowElevation = 12.dp,
        ) {
            Row(
                Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 28.dp, vertical = 22.dp),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable {
                    gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }) {
                    Box(Modifier.size(52.dp).clip(CircleShape).background(AppColors.Bg), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Image, "المعرض")
                    }
                    Text("المعرض", style = MaterialTheme.typography.labelSmall, color = AppColors.Muted)
                }
                Box(
                    Modifier.size(80.dp).border(3.dp, AppColors.Primary, CircleShape).padding(7.dp).clip(CircleShape)
                        .background(AppColors.Primary).clickable(enabled = granted && !busy) {
                            busy = true
                            val file = File(ctx.cacheDir, "cap_${System.currentTimeMillis()}.jpg")
                            imageCapture.takePicture(
                                ImageCapture.OutputFileOptions.Builder(file).build(),
                                ContextCompat.getMainExecutor(ctx),
                                object : ImageCapture.OnImageSavedCallback {
                                    override fun onImageSaved(r: ImageCapture.OutputFileResults) { busy = false; useUri(Uri.fromFile(file)) }
                                    override fun onError(e: ImageCaptureException) {
                                        busy = false
                                        Toast.makeText(ctx, "فشل التصوير، جرّب مرة ثانية", Toast.LENGTH_SHORT).show()
                                    }
                                },
                            )
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    if (busy) CircularProgressIndicator(color = Color.White, strokeWidth = 3.dp, modifier = Modifier.size(28.dp))
                    else Icon(Icons.Outlined.PhotoCamera, "تصوير", tint = Color.White, modifier = Modifier.size(30.dp))
                }
                Spacer(Modifier.size(52.dp))
            }
        }
    }
}

@Composable
private fun RoundBtn(icon: androidx.compose.ui.graphics.vector.ImageVector, desc: String, onClick: () -> Unit) {
    Box(
        Modifier.size(dimens.touchMin).clip(CircleShape).background(AppColors.Surface).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, desc, modifier = Modifier.size(22.dp)) }
}

@Composable
private fun Pill(text: String, small: Boolean = false) {
    Box(
        Modifier.clip(RoundedCornerShape(50)).background(AppColors.Surface).padding(horizontal = 16.dp, vertical = if (small) 7.dp else 10.dp),
    ) {
        Text(text, style = if (small) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodyLarge,
            color = if (small) AppColors.Muted else AppColors.Ink)
    }
}

/** تحديد النص: إطار قابل للسحب بـ 8 مقابض. الإطار بنسب 0..1 من الصورة. */
@Composable
fun CropScreen(onBack: () -> Unit, onRetake: () -> Unit, onNext: () -> Unit) {
    val bmp = ScanSession.raw
    if (bmp == null) { LaunchedEffect(Unit) { onRetake() }; return }

    var l by remember { mutableFloatStateOf(0.04f) }
    var t by remember { mutableFloatStateOf(0.04f) }
    var r by remember { mutableFloatStateOf(0.96f) }
    var b by remember { mutableFloatStateOf(0.96f) }

    Column(Modifier.fillMaxSize().background(AppColors.Bg)) {
        ScreenHeader("حدد النص", onBack)
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().padding(horizontal = dimens.pad, vertical = 8.dp)) {
            val density = androidx.compose.ui.platform.LocalDensity.current
            val boxW = with(density) { maxWidth.toPx() }
            val boxH = with(density) { maxHeight.toPx() }
            val scale = minOf(boxW / bmp.width, boxH / bmp.height)
            val imgW = bmp.width * scale
            val imgH = bmp.height * scale
            val offX = (boxW - imgW) / 2f
            val offY = (boxH - imgH) / 2f
            val handle = with(density) { 30.dp.toPx() }
            val minFrac = 0.08f

            Image(
                bitmap = bmp.asImageBitmap(), contentDescription = null, contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
            Canvas(
                Modifier.fillMaxSize().pointerInput(imgW, imgH) {
                    var fx = 0; var fy = 0; var move = false
                    detectDragGestures(
                        onDragStart = { o ->
                            val x0 = offX + l * imgW; val x1 = offX + r * imgW
                            val y0 = offY + t * imgH; val y1 = offY + b * imgH
                            val inX = o.x in (x0 - handle)..(x1 + handle)
                            val inY = o.y in (y0 - handle)..(y1 + handle)
                            fx = when {
                                inY && kotlin.math.abs(o.x - x0) <= handle -> -1
                                inY && kotlin.math.abs(o.x - x1) <= handle -> 1
                                else -> 0
                            }
                            fy = when {
                                inX && kotlin.math.abs(o.y - y0) <= handle -> -1
                                inX && kotlin.math.abs(o.y - y1) <= handle -> 1
                                else -> 0
                            }
                            move = fx == 0 && fy == 0 && o.x in x0..x1 && o.y in y0..y1
                        },
                        onDrag = { change, d ->
                            change.consume()
                            val dx = d.x / imgW; val dy = d.y / imgH
                            if (move) {
                                val w = r - l; val h = b - t
                                l = (l + dx).coerceIn(0f, 1f - w); r = l + w
                                t = (t + dy).coerceIn(0f, 1f - h); b = t + h
                            } else {
                                if (fx == -1) l = (l + dx).coerceIn(0f, r - minFrac)
                                if (fx == 1) r = (r + dx).coerceIn(l + minFrac, 1f)
                                if (fy == -1) t = (t + dy).coerceIn(0f, b - minFrac)
                                if (fy == 1) b = (b + dy).coerceIn(t + minFrac, 1f)
                            }
                        },
                    )
                },
            ) {
                val x0 = offX + l * imgW; val x1 = offX + r * imgW
                val y0 = offY + t * imgH; val y1 = offY + b * imgH
                // تعتيم خارج الإطار
                val dim = Color(0x66101828)
                drawRect(dim, Offset(offX, offY), Size(imgW, y0 - offY))
                drawRect(dim, Offset(offX, y1), Size(imgW, offY + imgH - y1))
                drawRect(dim, Offset(offX, y0), Size(x0 - offX, y1 - y0))
                drawRect(dim, Offset(x1, y0), Size(offX + imgW - x1, y1 - y0))
                drawRect(AppColors.Primary, Offset(x0, y0), Size(x1 - x0, y1 - y0), style = Stroke(width = 3.dp.toPx()))
                for (px in listOf(x0, (x0 + x1) / 2, x1)) for (py in listOf(y0, (y0 + y1) / 2, y1)) {
                    if (px == (x0 + x1) / 2 && py == (y0 + y1) / 2) continue
                    drawCircle(Color.White, 8.dp.toPx(), Offset(px, py))
                    drawCircle(AppColors.Primary, 8.dp.toPx(), Offset(px, py), style = Stroke(width = 2.2.dp.toPx()))
                }
            }
        }
        Row(
            Modifier.padding(horizontal = dimens.pad).padding(bottom = 12.dp).fillMaxWidth().clip(RoundedCornerShape(14.dp))
                .background(AppColors.PrimarySoft).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.Info, null, tint = AppColors.Primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text("اسحب النقاط حتى يغطي الإطار النص المطلوب", style = MaterialTheme.typography.labelMedium, color = AppColors.Primary)
        }
        Row(
            Modifier.padding(horizontal = dimens.pad).padding(bottom = 16.dp).navigationBarsPadding(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AppButton("التالي", {
                ScanSession.cropped = cropFractions(bmp, l, t, r, b)
                onNext()
            }, Modifier.weight(1f), icon = Icons.AutoMirrored.Outlined.ArrowForward)
            AppButton("إعادة التصوير", onRetake, Modifier.weight(1f), icon = Icons.Outlined.PhotoCamera, outlined = true)
        }
    }
}
