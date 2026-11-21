package iq.dablik

import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun OneTopBar(title: String, onBack: (() -> Unit)?, actions: @Composable RowScope.() -> Unit = {}) {
    Row(Modifier.fillMaxWidth().statusBarsPadding().height(64.dp).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) Box(Modifier.size(46.dp).oneClick { onBack() }.clip(CircleShape).background(OneUi.Card), contentAlignment = Alignment.Center) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, "رجوع", tint = OneUi.Ink, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, color = OneUi.Ink, modifier = Modifier.weight(1f))
        actions()
    }
}

@Composable
private fun BigTile(title: String, sub: String, icon: androidx.compose.ui.graphics.vector.ImageVector, accent: Color, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.aspectRatio(0.92f).oneClick { onClick() }.clip(RoundedCornerShape(OneUi.RCard)).background(OneUi.Card).padding(20.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Box(Modifier.size(62.dp).clip(CircleShape).background(accent.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) { Icon(icon, null, tint = accent, modifier = Modifier.size(34.dp)) }
        Column {
            Text(title, style = MaterialTheme.typography.titleLarge, color = OneUi.Ink)
            Text(sub, style = MaterialTheme.typography.labelMedium, color = OneUi.Muted)
        }
    }
}

@Composable
fun HomeScreen(onApk: () -> Unit, onZip: () -> Unit, onAbout: () -> Unit) {
    val ctx = LocalContext.current
    val ls = rememberLazyListState()
    val headerPx = with(LocalDensity.current) { 190.dp.toPx() }
    val collapse = if (ls.firstVisibleItemIndex > 0) 1f else (ls.firstVisibleItemScrollOffset / headerPx).coerceIn(0f, 1f)
    Box(Modifier.fillMaxSize().background(OneUi.Bg)) {
        LazyColumn(Modifier.fillMaxSize(), state = ls, contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 90.dp)) {
            // منطقة العنوان الكبيرة (كما في One UI): العنوان بالأسفل ويصغر عند التمرير
            item {
                Box(Modifier.fillMaxWidth().height(250.dp)) {
                    Column(Modifier.align(Alignment.BottomStart).padding(start = 8.dp, bottom = 22.dp).graphicsLayer { alpha = 1f - collapse; translationY = -40f * collapse }) {
                        Text("دبليك", style = MaterialTheme.typography.displaySmall, color = OneUi.Ink)
                        Text("حوّل موقعك إلى تطبيق APK قابل للتثبيت", style = MaterialTheme.typography.bodyMedium, color = OneUi.Muted)
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    BigTile("إنشاء APK", "من صفحة HTML", Icons.Outlined.Android, OneUi.Blue, Modifier.weight(1f), onApk)
                    BigTile("ZIP / HTML", "موقع كامل داخل ملف ZIP", Icons.Outlined.FolderZip, OneUi.Green, Modifier.weight(1f), onZip)
                }
                Spacer(Modifier.height(28.dp))
                Text("التطبيقات التي أنشأتها", style = MaterialTheme.typography.titleMedium, color = OneUi.Ink, modifier = Modifier.padding(start = 8.dp, bottom = 10.dp))
            }
            if (Store.items.isEmpty()) item {
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(OneUi.RCard)).background(OneUi.Card).padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.Inventory2, null, tint = OneUi.Faint, modifier = Modifier.size(48.dp))
                    Text("لا توجد تطبيقات بعد", style = MaterialTheme.typography.titleMedium, color = OneUi.Ink, modifier = Modifier.padding(top = 8.dp))
                    Text("اضغط «إنشاء APK» وسيظهر تطبيقك هنا بعد البناء", style = MaterialTheme.typography.labelMedium, color = OneUi.Muted, textAlign = TextAlign.Center)
                }
            }
            items(Store.items, key = { it.path }) { b ->
                val icon = remember(b.icon) { if (b.icon.isNotBlank() && File(b.icon).exists()) BitmapFactory.decodeFile(b.icon)?.asImageBitmap() else null }
                Row(
                    Modifier.fillMaxWidth().padding(bottom = 10.dp).oneClick { Store.install(ctx, File(b.path)) }.clip(RoundedCornerShape(OneUi.RCard)).background(OneUi.Card).padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(OneUi.Card2), contentAlignment = Alignment.Center) {
                        if (icon != null) Image(icon, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) else Icon(Icons.Outlined.Android, null, tint = OneUi.Blue, modifier = Modifier.size(30.dp))
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(b.name, style = MaterialTheme.typography.bodyLarge, color = OneUi.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${b.pkg} · ${b.version}", style = MaterialTheme.typography.labelSmall, color = OneUi.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Box(Modifier.size(42.dp).oneClick { Store.share(ctx, File(b.path)) }, contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Share, "مشاركة", tint = OneUi.Muted) }
                    Box(Modifier.size(42.dp).oneClick { Store.remove(b) }, contentAlignment = Alignment.Center) { Icon(Icons.Outlined.DeleteOutline, "حذف", tint = OneUi.Danger) }
                }
            }
        }
        // عنوان مصغر مثبت يظهر عند التمرير
        Box(Modifier.fillMaxWidth().statusBarsPadding().height(56.dp).graphicsLayer { alpha = collapse }, contentAlignment = Alignment.CenterStart) {
            Text("دبليك", style = MaterialTheme.typography.titleLarge, color = OneUi.Ink, modifier = Modifier.padding(start = 24.dp))
        }
        EdgeShade(top = 100.dp, bottom = 70.dp)
        Row(Modifier.align(Alignment.TopEnd).statusBarsPadding().height(56.dp).padding(end = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).oneClick { onAbout() }.clip(CircleShape).background(OneUi.Card), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Info, "عن التطبيق", tint = OneUi.Ink, modifier = Modifier.size(24.dp))
            }
        }
    }
}

private fun nameOf(ctx: android.content.Context, uri: Uri): String =
    runCatching { ctx.contentResolver.query(uri, null, null, null, null)?.use { c -> if (c.moveToFirst()) c.getString(c.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME)) else null } }.getOrNull() ?: "ملف"

@Composable
fun FormScreen(f: FormState, onBack: () -> Unit, onEdit: (Uri) -> Unit, onBuild: () -> Unit) {
    val ctx = LocalContext.current
    var tried by remember { mutableStateOf(false) }
    var htmlMode by remember { mutableIntStateOf(if (f.htmlFile != null) 1 else 0) } // 0 كتابة، 1 ملف
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> if (uri != null) onEdit(uri) }
    val pickZip = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) { f.zip = uri; f.zipName = nameOf(ctx, uri) } }
    val pickHtml = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) { f.htmlFile = uri; f.htmlFileName = nameOf(ctx, uri) } }

    val nameErr = if (tried && f.name.isBlank()) "اكتب اسم التطبيق" else null
    val pkgErr = if (tried && !ApkBuilder.validPackage(f.pkg.trim())) "مثال صحيح: com.fff.app (أحرف إنكليزية صغيرة ونقطتان)" else null
    val verErr = if (tried && !Regex("^[0-9]+(\\.[0-9]+){0,3}$").matches(f.version.trim())) "مثال: 1.1.0" else null
    val srcOk = if (f.mode == 1) f.zip != null else if (htmlMode == 0) f.htmlCode.isNotBlank() else f.htmlFile != null
    val srcErr = if (tried && !srcOk) (if (f.mode == 1) "اختر ملف ZIP" else "أضف ملف HTML أو اكتب الكود") else null

    Box(Modifier.fillMaxSize().background(OneUi.Bg)) {
        Column(Modifier.fillMaxSize()) {
            OneTopBar(if (f.mode == 1) "ZIP / HTML" else "إنشاء APK", onBack)
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(top = 8.dp, bottom = 120.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OneField(f.name, { f.name = it }, "اسم التطبيق", hint = "مثال: متجري", error = nameErr)
                OneField(f.pkg, { f.pkg = it.lowercase().replace(" ", "") }, "حزمة التطبيق", hint = "com.fff.app", keyboard = androidx.compose.ui.text.input.KeyboardType.Uri, error = pkgErr)
                OneField(f.version, { f.version = it }, "إصدار التطبيق", hint = "1.1.0", keyboard = androidx.compose.ui.text.input.KeyboardType.Decimal, error = verErr)
                // الصف السفلي: صندوق الأيقونة + المصدر
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(
                        Modifier.weight(1f).height(170.dp).oneClick { pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                            .clip(RoundedCornerShape(OneUi.RCard)).background(OneUi.Card),
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
                    ) {
                        val bmp = remember(f.iconPath) { f.iconPath?.let { p -> BitmapFactory.decodeFile(p)?.asImageBitmap() } }
                        if (bmp != null) Image(bmp, null, Modifier.size(84.dp).clip(RoundedCornerShape(22.dp)), contentScale = ContentScale.Crop)
                        else Box(Modifier.size(84.dp).clip(RoundedCornerShape(22.dp)).background(OneUi.Card2), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.AddPhotoAlternate, null, tint = OneUi.Blue, modifier = Modifier.size(38.dp)) }
                        Text(if (bmp != null) "تعديل الأيقونة" else "أيقونة التطبيق", style = MaterialTheme.typography.labelMedium, color = OneUi.Ink, modifier = Modifier.padding(top = 10.dp))
                        Text("من المعرض + تعديل", style = MaterialTheme.typography.labelSmall, color = OneUi.Muted)
                    }
                    Column(
                        Modifier.weight(1f).height(170.dp).clip(RoundedCornerShape(OneUi.RCard)).background(OneUi.Card)
                            .then(if (srcErr != null) Modifier.border(1.5.dp, OneUi.Danger, RoundedCornerShape(OneUi.RCard)) else Modifier)
                            .padding(14.dp),
                        verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        if (f.mode == 1) {
                            Icon(Icons.Outlined.FolderZip, null, tint = OneUi.Green, modifier = Modifier.size(40.dp))
                            Text(if (f.zip != null) f.zipName else "ملف ZIP للموقع", style = MaterialTheme.typography.labelMedium, color = OneUi.Ink, maxLines = 2, textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 8.dp))
                            Box(Modifier.oneClick { pickZip.launch(arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream")) }.clip(RoundedCornerShape(18.dp)).background(OneUi.Card2).padding(horizontal = 18.dp, vertical = 10.dp)) {
                                Text(if (f.zip != null) "تغيير" else "اختيار ZIP", style = MaterialTheme.typography.labelMedium, color = OneUi.Blue)
                            }
                        } else {
                            Icon(Icons.Outlined.Html, null, tint = OneUi.Blue, modifier = Modifier.size(40.dp))
                            Text(if (htmlMode == 1 && f.htmlFile != null) f.htmlFileName else if (htmlMode == 0 && f.htmlCode.isNotBlank()) "${f.htmlCode.length} حرف" else "صفحة HTML", style = MaterialTheme.typography.labelMedium, color = OneUi.Ink, maxLines = 1, modifier = Modifier.padding(vertical = 8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(Modifier.oneClick { htmlMode = 1; pickHtml.launch(arrayOf("text/html", "application/xhtml+xml", "text/*")) }.clip(RoundedCornerShape(16.dp)).background(if (htmlMode == 1) OneUi.BlueSoft else OneUi.Card2).padding(horizontal = 12.dp, vertical = 9.dp)) { Text("ملف", style = MaterialTheme.typography.labelMedium, color = OneUi.Blue) }
                                Box(Modifier.oneClick { htmlMode = 0 }.clip(RoundedCornerShape(16.dp)).background(if (htmlMode == 0) OneUi.BlueSoft else OneUi.Card2).padding(horizontal = 12.dp, vertical = 9.dp)) { Text("كتابة", style = MaterialTheme.typography.labelMedium, color = OneUi.Blue) }
                            }
                        }
                    }
                }
                if (srcErr != null) Text(srcErr, style = MaterialTheme.typography.labelSmall, color = OneUi.Danger, modifier = Modifier.padding(start = 18.dp))
                if (f.mode == 0 && htmlMode == 0) OneField(f.htmlCode, { f.htmlCode = it }, "كود HTML", hint = "<h1>مرحباً</h1>", singleLine = false, minHeight = 160)
            }
        }
        EdgeShade(top = 90.dp, bottom = 110.dp)
        Box(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(horizontal = 16.dp, vertical = 14.dp)) {
            OneButton("إنشاء", {
                tried = true
                if (f.name.isNotBlank() && ApkBuilder.validPackage(f.pkg.trim()) && verErr == null && srcOk) onBuild()
                else Toast.makeText(ctx, "أكمل الحقول المطلوبة", Toast.LENGTH_SHORT).show()
            }, icon = Icons.Outlined.Build)
        }
    }
}

@Composable
fun ConsoleScreen(f: FormState, onBack: () -> Unit, onHome: () -> Unit) {
    val ctx = LocalContext.current
    val logs = remember { mutableStateListOf<String>() }
    var state by remember { mutableIntStateOf(0) } // 0 يعمل، 1 نجح، 2 فشل
    var result by remember { mutableStateOf<File?>(null) }
    val scroll = rememberScrollState()
    LaunchedEffect(Unit) {
        val src = if (f.mode == 1) Source.Zip(f.zip!!) else if (f.htmlFile != null && f.htmlCode.isBlank()) Source.HtmlFile(f.htmlFile!!) else if (f.htmlCode.isNotBlank()) Source.Html(f.htmlCode) else Source.HtmlFile(f.htmlFile!!)
        val icon = f.iconPath?.let { p -> runCatching { File(p).readBytes() }.getOrNull() }
        val req = BuildRequest(f.name.trim(), f.pkg.trim(), f.version.trim(), icon, src)
        logs.add("$ dablik build --name \"${req.name}\" --package ${req.pkg} --version ${req.version}")
        try {
            val out = withContext(Dispatchers.Default) {
                ApkBuilder.buildSuspend(ctx, req) { line -> withContext(Dispatchers.Main) { logs.add(line) }; delay(350) }
            }
            result = out
            Store.add(Built(req.name, req.pkg, req.version, out.absolutePath, f.iconPath ?: "", System.currentTimeMillis()))
            logs.add("تم إنشاء ${out.name}")
            state = 1
        } catch (e: Throwable) {
            logs.add("فشل: ${e.message ?: e.javaClass.simpleName}")
            state = 2
        }
    }
    LaunchedEffect(logs.size) { scroll.animateScrollTo(scroll.maxValue) }

    Box(Modifier.fillMaxSize().background(OneUi.Bg)) {
        Column(Modifier.fillMaxSize()) {
            OneTopBar("لوحة التحكم", if (state != 0) onBack else null)
            Row(Modifier.padding(horizontal = 20.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                val c = when (state) { 0 -> OneUi.Blue; 1 -> OneUi.Green; else -> OneUi.Danger }
                Box(Modifier.size(10.dp).clip(CircleShape).background(c))
                Spacer(Modifier.width(8.dp))
                Text(when (state) { 0 -> "جارٍ البناء..."; 1 -> "اكتمل البناء"; else -> "فشل البناء" }, style = MaterialTheme.typography.labelMedium, color = c)
            }
            Box(
                Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).clip(RoundedCornerShape(OneUi.RCard)).background(Color(0xFF050505))
                    .border(1.dp, OneUi.Line, RoundedCornerShape(OneUi.RCard)),
            ) {
                Column(Modifier.fillMaxSize().verticalScroll(scroll).padding(18.dp)) {
                    logs.forEach { l ->
                        Text(l, fontFamily = FontFamily.Monospace, fontSize = 13.5.sp, lineHeight = 21.sp,
                            color = if (l.startsWith("فشل") || l.contains("تحذير")) OneUi.Danger else if (l.startsWith("تم إنشاء")) OneUi.Green else if (l.startsWith("$")) OneUi.Blue else Color(0xFFD0D0D0))
                    }
                    if (state == 0) Text("_", fontFamily = FontFamily.Monospace, color = OneUi.Green)
                }
            }
            Column(Modifier.navigationBarsPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (state == 1 && result != null) {
                    OneButton("تثبيت التطبيق", { Store.install(ctx, result!!) }, icon = Icons.Outlined.InstallMobile)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OneButton("مشاركة", { Store.share(ctx, result!!) }, Modifier.weight(1f), primary = false, icon = Icons.Outlined.Share)
                        OneButton("الرئيسية", onHome, Modifier.weight(1f), primary = false, icon = Icons.Outlined.Home)
                    }
                } else if (state == 2) OneButton("رجوع وتعديل", onBack, primary = false)
            }
        }
    }
}
