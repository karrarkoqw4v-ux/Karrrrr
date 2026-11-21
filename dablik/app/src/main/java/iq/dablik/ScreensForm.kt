package iq.dablik

import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun FormScreen(f: FormState, onBack: () -> Unit, onEdit: (Uri) -> Unit, onAssets: () -> Unit, onBuild: () -> Unit) {
    val ctx = LocalContext.current
    var tried by remember { mutableStateOf(false) }
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> if (uri != null) onEdit(uri) }
    val summary = remember(f.rev) { Project.summary(ctx) }
    val hasIndex = remember(f.rev) { File(Project.dir(ctx), "index.html").exists() }

    val nameErr = if (tried && f.name.isBlank()) "اكتب اسم التطبيق" else null
    val pkgErr = if (tried && !ApkBuilder.validPackage(f.pkg.trim())) "مثال صحيح: com.fff.app (أحرف إنكليزية صغيرة ونقطة على الأقل)" else null
    val verErr = if (tried && !Regex("^[0-9]+(\\.[0-9]+){0,3}$").matches(f.version.trim())) "مثال: 1.1.0" else null
    val filesErr = if (tried && !hasIndex) "أضف ملفات التطبيق (يجب وجود index.html)" else null

    Box(Modifier.fillMaxSize().background(OneUi.Bg)) {
        Column(Modifier.fillMaxSize()) {
            OneTopBar("إنشاء APK", onBack)
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(top = 6.dp, bottom = 130.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Entrance(0) { Text("معلومات التطبيق", style = MaterialTheme.typography.titleMedium, color = OneUi.Muted, modifier = Modifier.padding(start = 8.dp)) }
                Entrance(1) { OneField(f.name, { f.name = it }, "اسم التطبيق", hint = "مثال: متجري", error = nameErr) }
                Entrance(2) { OneField(f.pkg, { f.pkg = it.lowercase().replace(" ", "") }, "حزمة التطبيق", hint = "com.fff.app", keyboard = KeyboardType.Uri, error = pkgErr) }
                Entrance(3) { OneField(f.version, { f.version = it }, "إصدار التطبيق", hint = "1.1.0", keyboard = KeyboardType.Decimal, error = verErr) }
                // الأيقونة
                Entrance(4) {
                    Row(
                        Modifier.fillMaxWidth().oneClick { pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                            .clip(RoundedCornerShape(OneUi.RCard)).background(OneUi.Card).padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val bmp = remember(f.iconPath) { f.iconPath?.let { p -> BitmapFactory.decodeFile(p)?.asImageBitmap() } }
                        if (bmp != null) Image(bmp, null, Modifier.size(92.dp).clip(RoundedCornerShape(24.dp)), contentScale = ContentScale.Crop)
                        else Box(Modifier.size(92.dp).clip(RoundedCornerShape(24.dp)).background(OneUi.Card2), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.AddPhotoAlternate, null, tint = OneUi.Blue, modifier = Modifier.size(40.dp)) }
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text("أيقونة التطبيق", style = MaterialTheme.typography.bodyLarge, color = OneUi.Ink)
                            Text(if (bmp != null) "اضغط لاختيار صورة أخرى وتعديلها" else "اختر صورة من المعرض ثم عدّل القص والألوان والنص", style = MaterialTheme.typography.labelMedium, color = OneUi.Muted)
                        }
                    }
                }
                Entrance(5) { Text("المحتوى", style = MaterialTheme.typography.titleMedium, color = OneUi.Muted, modifier = Modifier.padding(start = 8.dp, top = 8.dp)) }
                // زر: ملفات التطبيق والصلاحيات
                Entrance(6) {
                    Column {
                        Row(
                            Modifier.fillMaxWidth().oneClick { onAssets() }.clip(RoundedCornerShape(OneUi.RCard)).background(OneUi.Card)
                                .then(if (filesErr != null) Modifier.border(1.5.dp, OneUi.Danger, RoundedCornerShape(OneUi.RCard)) else Modifier).padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(Modifier.size(52.dp).clip(CircleShape).background(OneUi.BlueSoft), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.FolderZip, null, tint = OneUi.Blue, modifier = Modifier.size(28.dp)) }
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text("ملفات التطبيق والصلاحيات", style = MaterialTheme.typography.bodyLarge, color = OneUi.Ink)
                                Text("$summary · ${f.perms.size} صلاحية", style = MaterialTheme.typography.labelMedium, color = OneUi.Muted)
                            }
                            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, null, tint = OneUi.Muted, modifier = Modifier.size(28.dp))
                        }
                        if (filesErr != null) Text(filesErr, style = MaterialTheme.typography.labelSmall, color = OneUi.Danger, modifier = Modifier.padding(start = 18.dp, top = 6.dp))
                    }
                }
            }
        }
        EdgeShade(top = 90.dp, bottom = 120.dp)
        Box(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(horizontal = 16.dp, vertical = 14.dp)) {
            OneButton("إنشاء", {
                tried = true
                if (f.name.isNotBlank() && ApkBuilder.validPackage(f.pkg.trim()) && verErr == null && hasIndex) onBuild()
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
    var step by remember { mutableIntStateOf(0) }
    val scroll = rememberScrollState()
    val prog by animateFloatAsState(if (state == 1) 1f else step / 7f, tween(500), label = "prog")
    LaunchedEffect(Unit) {
        val icon = f.iconPath?.let { p -> runCatching { File(p).readBytes() }.getOrNull() }
        val req = BuildRequest(f.name.trim(), f.pkg.trim(), f.version.trim(), icon, Project.dir(ctx), f.perms.toList(), f.configJson())
        logs.add("$ dablik build --name \"${req.name}\" --package ${req.pkg} --version ${req.version}")
        try {
            val out = withContext(Dispatchers.Default) {
                ApkBuilder.buildSuspend(ctx, req) { line ->
                    withContext(Dispatchers.Main) {
                        logs.add(line)
                        Regex("^\\[(\\d)/7\\]").find(line)?.let { step = it.groupValues[1].toInt() }
                    }
                    delay(350)
                }
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
            Box(Modifier.padding(horizontal = 20.dp, vertical = 4.dp).fillMaxWidth().height(6.dp).clip(CircleShape).background(OneUi.Card2)) {
                Box(Modifier.fillMaxWidth(prog.coerceIn(0f, 1f)).fillMaxHeight().clip(CircleShape).background(if (state == 2) OneUi.Danger else if (state == 1) OneUi.Green else OneUi.Blue))
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
