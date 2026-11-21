package iq.dablik

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.BitmapFactory
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
private fun SectionTitle(title: String, sub: String = "") {
    Column(Modifier.padding(start = 8.dp, top = 14.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = OneUi.Ink)
        if (sub.isNotEmpty()) Text(sub, style = MaterialTheme.typography.labelSmall, color = OneUi.Muted)
    }
}

@Composable
private fun TypeTile(title: String, sub: String, icon: ImageVector, tint: Color, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.height(150.dp).oneClick { onClick() }.clip(RoundedCornerShape(OneUi.RCard)).background(OneUi.Card).padding(18.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Box(Modifier.size(52.dp).clip(CircleShape).background(tint.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) { Icon(icon, null, tint = tint, modifier = Modifier.size(28.dp)) }
        Column { Text(title, style = MaterialTheme.typography.bodyLarge, color = OneUi.Ink); Text(sub, style = MaterialTheme.typography.labelSmall, color = OneUi.Muted) }
    }
}

@Composable
private fun NavCard(icon: ImageVector, tint: Color, title: String, sub: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().oneClick { onClick() }.clip(RoundedCornerShape(OneUi.RCard)).background(OneUi.Card).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(48.dp).clip(CircleShape).background(tint.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) { Icon(icon, null, tint = tint, modifier = Modifier.size(26.dp)) }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) { Text(title, style = MaterialTheme.typography.bodyLarge, color = OneUi.Ink); Text(sub, style = MaterialTheme.typography.labelMedium, color = OneUi.Muted, maxLines = 2, overflow = TextOverflow.Ellipsis) }
        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, null, tint = OneUi.Muted, modifier = Modifier.size(26.dp))
    }
}

@Composable
private fun NameDialog(title: String, initial: String, ok: String, onOk: (String) -> Unit, onDismiss: () -> Unit) {
    var v by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = OneUi.Card,
        title = { Text(title, color = OneUi.Ink) },
        text = { OutlinedTextField(v, { v = it }, singleLine = true) },
        confirmButton = { TextButton({ if (v.isNotBlank()) onOk(v.trim()) }) { Text(ok, color = OneUi.Blue) } },
        dismissButton = { TextButton(onDismiss) { Text("إلغاء", color = OneUi.Muted) } },
    )
}

@Composable
fun AssetsScreen(f: FormState, onBack: () -> Unit, onPerms: () -> Unit, onFiles: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val summary = remember(f.rev) { Project.summary(ctx) }
    val count = remember(f.rev) { Project.files(ctx).size }
    var htmlChoice by remember { mutableStateOf(false) }
    var codeDlg by remember { mutableStateOf(false) }
    var code by remember { mutableStateOf("") }
    var clearDlg by remember { mutableStateOf(false) }
    var adDlg by remember { mutableStateOf(false) }
    var ghToken by remember { mutableStateOf(Publisher.token(ctx)) }
    val pubLog = remember { mutableStateListOf<String>() }
    var publishing by remember { mutableStateOf(false) }
    // رابط التحديث يُملأ تلقائياً عند ربط مستودع GitHub
    LaunchedEffect(f.ghRepo, f.ghBranch, f.pkg) { if (Publisher.validRepo(f.ghRepo) && ApkBuilder.validPackage(f.pkg.trim())) f.updateUrl = Publisher.rawBase(f.ghRepo, f.ghBranch, f.pkg.trim()) + "/update.json" }

    val pickZip = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            runCatching { withContext(Dispatchers.IO) { Project.importZip(ctx, uri) } }
                .onSuccess { n -> f.rev++; Toast.makeText(ctx, if (n == 0) "الـ ZIP فارغ" else "تم فك $n ملف", Toast.LENGTH_SHORT).show() }
                .onFailure { e -> Toast.makeText(ctx, e.message ?: "تعذر فتح الـ ZIP", Toast.LENGTH_LONG).show() }
        }
    }
    val pickFiles = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) scope.launch {
            val n = withContext(Dispatchers.IO) { runCatching { Project.importFiles(ctx, uris) }.getOrDefault(0) }
            f.rev++; Toast.makeText(ctx, "تمت إضافة $n ملف", Toast.LENGTH_SHORT).show()
        }
    }

    val hasSizes = remember(f.rev) { File(Project.dir(ctx), Responsive.FILE).exists() }
    val pickSizes = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            val txt = withContext(Dispatchers.IO) { runCatching { ctx.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) } }.getOrNull() }
            if (txt != null && Responsive.isValid(txt)) { File(Project.dir(ctx), Responsive.FILE).writeText(txt); f.rev++; Toast.makeText(ctx, "تم رفع ملف القياسات", Toast.LENGTH_SHORT).show() }
            else Toast.makeText(ctx, "الملف ليس JSON صالحاً", Toast.LENGTH_LONG).show()
        }
    }
    Box(Modifier.fillMaxSize().background(OneUi.Bg)) {
        Column(Modifier.fillMaxSize()) {
            OneTopBar("ملفات التطبيق والصلاحيات", onBack)
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 120.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Entrance(0) { SectionTitle("ملفات التطبيق", "اختر نوع الملفات التي تريد رفعها") }
                Entrance(1) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TypeTile("ملفات - ZIP", "موقع كامل مضغوط", Icons.Outlined.FolderZip, OneUi.Green, Modifier.weight(1f)) { pickZip.launch(arrayOf("*/*")) }
                        TypeTile("ملفات - HTML", "صفحات وCSS وJS وصور", Icons.Outlined.Html, OneUi.Blue, Modifier.weight(1f)) { htmlChoice = true }
                    }
                }
                AnimatedVisibility(count > 0, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        NavCard(Icons.Outlined.EditNote, Color(0xFFFF9F1C), "تعديل الملفات", "$summary · عدّل أو احذف أو غيّر الاسم أو شغّل") { onFiles() }
                        Box(Modifier.align(Alignment.End).oneClick { clearDlg = true }.padding(horizontal = 12.dp, vertical = 4.dp)) { Text("مسح كل الملفات", style = MaterialTheme.typography.labelMedium, color = OneUi.Danger) }
                    }
                }
                Entrance(2) { SectionTitle("الصلاحيات", "فعّل أي صلاحيات يحتاجها تطبيقك") }
                Entrance(3) {
                    val on = f.perms.mapNotNull { id -> Perms.groups.flatMap { it.items }.firstOrNull { it.id == id }?.title }
                    NavCard(Icons.Outlined.Shield, OneUi.Blue, "صلاحيات التطبيق", "${f.perms.size} مفعّلة من ${Perms.total} · " + on.take(3).joinToString("، ")) { onPerms() }
                }
                Entrance(3) { SectionTitle("قياسات الأجهزة", "يطابق النصوص والأحجام على كل الجوالات والتابلت") }
                Entrance(4) {
                    Column(Modifier.clip(RoundedCornerShape(OneUi.RCard)).background(OneUi.Card)) {
                        ToggleRow(Icons.Outlined.PhoneAndroid, OneUi.Blue, "ضبط القياسات تلقائياً", "يضيف viewport وحجم خط يتدرج مع الشاشة", f.autoFit) { f.autoFit = it }
                        ToggleRow(Icons.Outlined.TextFields, Color(0xFFFF9F1C), "ثبات حجم الخط", "يتجاهل تكبير الخط في إعدادات الجهاز", f.fixText) { f.fixText = it }
                        Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(if (hasSizes) Icons.Outlined.CheckCircle else Icons.Outlined.AutoFixHigh, null, tint = if (hasSizes) OneUi.Green else OneUi.Blue, modifier = Modifier.size(24.dp))
                                Spacer(Modifier.width(10.dp))
                                Text(if (hasSizes) "وُجد ملف ${Responsive.FILE} داخل ملفاتك وسيُستخدم" else "لا يوجد ملف قياسات: يحدد دبليك القياسات تلقائياً حسب الجهاز", style = MaterialTheme.typography.labelMedium, color = OneUi.Muted)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(Modifier.waterClick(RoundedCornerShape(18.dp)) { pickSizes.launch(arrayOf("*/*")) }.glass(RoundedCornerShape(18.dp), OneUi.Blue, 0.28f, 0.14f).padding(horizontal = 16.dp, vertical = 10.dp)) { Text("رفع ملف القياسات", style = MaterialTheme.typography.labelMedium, color = OneUi.Blue) }
                                Box(Modifier.oneClick { File(Project.dir(ctx), Responsive.FILE).writeText(Responsive.templateText()); f.rev++; Toast.makeText(ctx, "تم إنشاء ${Responsive.FILE}، عدّله من «تعديل الملفات»", Toast.LENGTH_LONG).show() }.glass(RoundedCornerShape(18.dp)).padding(horizontal = 16.dp, vertical = 10.dp)) { Text("إنشاء ملف جاهز", style = MaterialTheme.typography.labelMedium, color = OneUi.Ink) }
                            }
                            Text("الملف اختياري: إذا كتبته أو رفعته يُستخدم، وإذا لم يوجد يطبّق دبليك قيماً افتراضية.", style = MaterialTheme.typography.labelSmall, color = OneUi.Faint)
                        }
                    }
                }
                Entrance(4) { SectionTitle("التحديث عن بعد", "يحدّث التطبيق محتواه من رابط دون إعادة تثبيت") }
                Entrance(5) {
                    Column(Modifier.clip(RoundedCornerShape(OneUi.RCard)).background(OneUi.Card)) {
                        ToggleRow(Icons.Outlined.CloudSync, OneUi.Green, "تفعيل التحديث عن بعد", "يفحص الرابط عند كل تشغيل", f.update) { f.update = it }
                        AnimatedVisibility(f.update, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                            Column(Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                OneField(f.updateUrl, { f.updateUrl = it.trim() }, "رابط التحديث", hint = "https://example.com/update.json", keyboard = androidx.compose.ui.text.input.KeyboardType.Uri)
                                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color(0xFF050505)).padding(14.dp)) {
                                    Text("ضع هذا الملف على أي استضافة، وارفع موقعك كملف ZIP على رابط مباشر:", style = MaterialTheme.typography.labelSmall, color = OneUi.Muted)
                                    Text("{\"version\": 2, \"zip\": \"https://example.com/site.zip\"}", fontFamily = FontFamily.Monospace, fontSize = 12.5.sp, color = OneUi.Green, modifier = Modifier.padding(vertical = 8.dp))
                                    Text("لتحديث التطبيق ارفع ZIP جديداً وزد رقم version. يمكنك أيضاً وضع رابط ZIP مباشر بدل JSON.", style = MaterialTheme.typography.labelSmall, color = OneUi.Muted)
                                }
                                Box(Modifier.oneClick {
                                    (ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("update", "{\"version\": 2, \"zip\": \"https://example.com/site.zip\"}"))
                                    Toast.makeText(ctx, "تم نسخ المثال", Toast.LENGTH_SHORT).show()
                                }.glass(RoundedCornerShape(18.dp)).padding(horizontal = 16.dp, vertical = 10.dp)) { Text("نسخ المثال", style = MaterialTheme.typography.labelMedium, color = OneUi.Blue) }
                            }
                        }
                    }
                }
                Entrance(5) { SectionTitle("نشر التحديثات للجميع", "ارفع ملفاتك الجديدة وتصل لكل من ثبّت تطبيقك") }
                Entrance(6) {
                    Column(Modifier.clip(RoundedCornerShape(OneUi.RCard)).background(OneUi.Card).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("يرفع دبليك ملفات المشروع الحالية إلى مستودع GitHub عام تملكه، والتطبيقات المثبتة تفحص التحديث عند كل تشغيل وتحمّله تلقائياً. أنشئ مستودعاً عاماً وتوكن بصلاحية Contents (قراءة وكتابة).", style = MaterialTheme.typography.labelMedium, color = OneUi.Muted)
                        OneField(f.ghRepo, { f.ghRepo = it.trim() }, "المستودع (owner/name)", hint = "karrar/my-apps", keyboard = androidx.compose.ui.text.input.KeyboardType.Uri)
                        OneField(f.ghBranch, { f.ghBranch = it.trim() }, "الفرع", hint = "main")
                        OneField(ghToken, { ghToken = it.trim(); Publisher.saveToken(ctx, it) }, "GitHub Token", hint = "github_pat_...", keyboard = androidx.compose.ui.text.input.KeyboardType.Password)
                        Text("آخر إصدار منشور: ${if (f.remoteVer == 0) "لم يُنشر بعد" else "v${f.remoteVer}"}", style = MaterialTheme.typography.labelMedium, color = OneUi.Blue)
                        OneButton(if (publishing) "جارٍ النشر..." else "نشر الملفات كتحديث للجميع", {
                            if (!publishing) { publishing = true; pubLog.clear()
                                scope.launch {
                                    runCatching { withContext(Dispatchers.IO) { Publisher.publish(ctx, f) { l -> pubLog.add(l) } } }
                                        .onSuccess { try { Store.saveProject(ctx, f) } catch (_: Exception) {} }
                                        .onFailure { e -> pubLog.add("[ERR ] ${e.message}") }
                                    publishing = false
                                }
                            }
                        }, enabled = !publishing, icon = Icons.Outlined.CloudUpload)
                        if (pubLog.isNotEmpty()) Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color(0xFF050505)).padding(12.dp)) {
                            pubLog.forEach { l -> Text(l, fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = if (l.startsWith("[ERR")) OneUi.Danger else if (l.startsWith("[ OK")) OneUi.Green else Color(0xFFD0D0D0)) }
                        }
                        Text("أول مرة: فعّل «التحديث عن بعد» وابنِ التطبيق مرة واحدة، ثم انشر التحديثات متى شئت بدون إعادة بناء.", style = MaterialTheme.typography.labelSmall, color = OneUi.Faint)
                    }
                }
                Entrance(7) { SectionTitle("تسجيل الدخول", "حسابات محلية أو ربط مباشر مع خدمة اتصال") }
                Entrance(7) {
                    Column(Modifier.clip(RoundedCornerShape(OneUi.RCard)).background(OneUi.Card)) {
                        ToggleRow(Icons.Outlined.Lock, Color(0xFFAF52DE), "إضافة تسجيل دخول وحساب جديد", "شاشة حساب قبل فتح التطبيق. يقرأ موقعك الاسم عبر Dablik.user() والتوكن عبر Dablik.token()", f.login) { f.login = it }
                        AnimatedVisibility(f.login, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                            Column(Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf("محلي على الجهاز", "Firebase مباشر", "API خاص").forEachIndexed { i, t ->
                                        Box(Modifier.waterClick(RoundedCornerShape(18.dp)) { f.loginMode = i }.glass(RoundedCornerShape(18.dp), if (f.loginMode == i) OneUi.Blue else Color.White, if (f.loginMode == i) 0.32f else 0.12f, if (f.loginMode == i) 0.18f else 0.06f).padding(horizontal = 16.dp, vertical = 10.dp)) {
                                            Text(t, style = MaterialTheme.typography.labelMedium, color = if (f.loginMode == i) OneUi.Blue else OneUi.Ink)
                                        }
                                    }
                                }
                                when (f.loginMode) {
                                    1 -> {
                                        OneField(f.fbKey, { f.fbKey = it.trim() }, "Firebase Web API Key", hint = "AIza...", keyboard = androidx.compose.ui.text.input.KeyboardType.Uri)
                                        Text("اتصال مباشر مع Firebase Authentication (بريد وكلمة مرور). فعّل Email/Password من لوحة Firebase.", style = MaterialTheme.typography.labelSmall, color = OneUi.Muted)
                                    }
                                    2 -> {
                                        OneField(f.loginUrl, { f.loginUrl = it.trim() }, "رابط تسجيل الدخول (POST)", hint = "https://api.example.com/login", keyboard = androidx.compose.ui.text.input.KeyboardType.Uri)
                                        OneField(f.registerUrl, { f.registerUrl = it.trim() }, "رابط إنشاء حساب (POST)", hint = "https://api.example.com/register", keyboard = androidx.compose.ui.text.input.KeyboardType.Uri)
                                        Text("يرسل {\"username\",\"email\",\"password\"} بصيغة JSON، وينجح إذا رجع الخادم 200 (ويقرأ token إن وُجد).", style = MaterialTheme.typography.labelSmall, color = OneUi.Muted)
                                    }
                                    else -> Text("حسابات تُحفظ بتشفير على جهاز المستخدم فقط.", style = MaterialTheme.typography.labelSmall, color = OneUi.Muted)
                                }
                            }
                        }
                    }
                }
                Entrance(7) { SectionTitle("الإعلانات (تجربة شهر)", "إعلانات خاصة بك داخل التطبيق: شريط أو نافذة") }
                Entrance(8) {
                    Column(Modifier.clip(RoundedCornerShape(OneUi.RCard)).background(OneUi.Card)) {
                        ToggleRow(Icons.Outlined.Campaign, Color(0xFFFF6BAA), "تفعيل نظام الإعلانات", "يعمل 30 يوماً من لحظة البناء ثم يتوقف تلقائياً", f.adsOn) { f.adsOn = it }
                        AnimatedVisibility(f.adsOn, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                            Column(Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                f.ads.forEachIndexed { i, ad ->
                                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(OneUi.Card2).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(if (ad.popup) Icons.Outlined.WebAsset else Icons.Outlined.ViewAgenda, null, tint = OneUi.Blue, modifier = Modifier.size(24.dp))
                                        Spacer(Modifier.width(10.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text(ad.title, style = MaterialTheme.typography.bodyLarge, color = OneUi.Ink, maxLines = 1)
                                            Text((if (ad.popup) "نافذة" else "شريط") + " · " + ad.text, style = MaterialTheme.typography.labelSmall, color = OneUi.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                        Box(Modifier.size(36.dp).oneClick { f.ads.removeAt(i) }, contentAlignment = Alignment.Center) { Icon(Icons.Outlined.DeleteOutline, "حذف", tint = OneUi.Danger) }
                                    }
                                }
                                Box(Modifier.waterClick(RoundedCornerShape(18.dp)) { adDlg = true }.glass(RoundedCornerShape(18.dp), OneUi.Blue, 0.30f, 0.16f).padding(horizontal = 16.dp, vertical = 10.dp)) { Text("إضافة إعلان", style = MaterialTheme.typography.labelMedium, color = OneUi.Blue) }
                                Text("يمكن تغيير الإعلانات عن بعد: تُرسَل مع كل «نشر تحديث». هذه إعلانات ترويجية خاصة بك، وليست شبكة إعلانات مدفوعة مثل AdMob.", style = MaterialTheme.typography.labelSmall, color = OneUi.Faint)
                            }
                        }
                    }
                }
                Entrance(8) { SectionTitle("أدوات التطبيق", "ما يحتاجه تطبيقك ليعمل كتطبيق حقيقي") }
                Entrance(9) {
                    Column(Modifier.clip(RoundedCornerShape(OneUi.RCard)).background(OneUi.Card)) {
                        ToggleRow(Icons.Outlined.Build, OneUi.Blue, "أدوات JavaScript", "toast وshare وcopy وvibrate وnotify وinfo وopenUrl...", f.bridge) { f.bridge = it }
                        ToggleRow(Icons.Outlined.Shield, OneUi.Green, "طلب الصلاحيات عند التشغيل", "يطلب الصلاحيات الخطرة المفعّلة من المستخدم", f.askPerms) { f.askPerms = it }
                        ToggleRow(Icons.Outlined.Fullscreen, Color(0xFFFF9F1C), "ملء الشاشة", "إخفاء شريط الحالة", f.fullscreen) { f.fullscreen = it }
                        ToggleRow(Icons.Outlined.LightMode, Color(0xFFFFD60A), "إبقاء الشاشة مضاءة", "لا ينطفئ العرض أثناء الاستخدام", f.keepAwake) { f.keepAwake = it }
                        ToggleRow(Icons.Outlined.Wifi, Color(0xFF34A853), "التحكم في شبكتي", "زر Dablik.network() بموقعك يفتح لوحة الراوتر الحقيقية ويحفظ جلسة الدخول", f.networkTool) { f.networkTool = it }
                        Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.ScreenRotation, null, tint = OneUi.Muted, modifier = Modifier.size(26.dp))
                            Spacer(Modifier.width(14.dp))
                            Text("اتجاه الشاشة", style = MaterialTheme.typography.bodyLarge, color = OneUi.Ink, modifier = Modifier.weight(1f))
                            listOf("تلقائي", "عمودي", "أفقي").forEachIndexed { i, t ->
                                Box(Modifier.padding(start = 6.dp).oneClick { f.orientation = i }.glass(RoundedCornerShape(16.dp), if (f.orientation == i) OneUi.Blue else Color.White, if (f.orientation == i) 0.30f else 0.12f, if (f.orientation == i) 0.16f else 0.06f).padding(horizontal = 12.dp, vertical = 8.dp)) {
                                    Text(t, style = MaterialTheme.typography.labelMedium, color = if (f.orientation == i) OneUi.Blue else OneUi.Muted)
                                }
                            }
                        }
                    }
                }
                Entrance(10) { SectionTitle("خيارات المطور", "إعدادات إضافية يحتاجها التطبيق") }
                Entrance(11) {
                    Column(Modifier.clip(RoundedCornerShape(OneUi.RCard)).background(OneUi.Card)) {
                        ToggleRow(Icons.Outlined.RocketLaunch, Color(0xFFFF6BAA), "شاشة بداية باسم التطبيق", "تظهر ثانية ونصف عند الفتح", f.splash) { f.splash = it }
                        ToggleRow(Icons.Outlined.ExitToApp, OneUi.Muted, "الضغط مرتين للخروج", "يمنع الخروج بالخطأ", f.doubleBack) { f.doubleBack = it }
                        ToggleRow(Icons.Outlined.WifiOff, OneUi.Blue, "صفحة عند انقطاع الإنترنت", "رسالة وزر إعادة المحاولة", f.offline) { f.offline = it }
                        ToggleRow(Icons.Outlined.ZoomIn, OneUi.Green, "السماح بالتكبير بالأصابع", "", f.zoom) { f.zoom = it }
                        ToggleRow(Icons.Outlined.OpenInBrowser, Color(0xFFFF9F1C), "فتح الروابط الخارجية بالمتصفح", "روابط http تفتح خارج التطبيق", f.external) { f.external = it }
                        ToggleRow(Icons.Outlined.BugReport, Color(0xFFAF52DE), "وضع المطور", "فحص الصفحة من Chrome على الكمبيوتر", f.debug) { f.debug = it }
                        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                            Text("لون شريط الحالة وشاشة البداية", style = MaterialTheme.typography.bodyLarge, color = OneUi.Ink)
                            Row(Modifier.horizontalScroll(rememberScrollState()).padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                listOf("" to Color(0xFF2A2A2D), "#000000" to Color.Black, "#3E91FF" to OneUi.Blue, "#AF52DE" to Color(0xFFAF52DE), "#30D158" to Color(0xFF30D158), "#FF5A5A" to Color(0xFFFF5A5A), "#FF9F1C" to Color(0xFFFF9F1C), "#FFFFFF" to Color.White).forEach { (hex, col) ->
                                    Box(Modifier.size(40.dp).oneClick { f.statusColor = hex }.clip(CircleShape).background(col).border(if (f.statusColor == hex) 3.dp else 1.dp, if (f.statusColor == hex) OneUi.Blue else OneUi.Line, CircleShape), contentAlignment = Alignment.Center) {
                                        if (hex.isEmpty()) Text("تلقائي", style = MaterialTheme.typography.labelSmall, color = OneUi.Muted)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        EdgeShade(top = 90.dp, bottom = 110.dp)
        Box(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(horizontal = 16.dp, vertical = 14.dp)) { OneButton("تم", onBack, icon = Icons.Outlined.Done) }
    }

    if (adDlg) {
        var t by remember { mutableStateOf("") }; var tx by remember { mutableStateOf("") }; var u by remember { mutableStateOf("") }; var pop by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { adDlg = false }, containerColor = OneUi.Card,
            title = { Text("إعلان جديد", color = OneUi.Ink) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(t, { t = it }, singleLine = true, label = { Text("العنوان") })
                    OutlinedTextField(tx, { tx = it }, label = { Text("النص") })
                    OutlinedTextField(u, { u = it.trim() }, singleLine = true, label = { Text("رابط عند الضغط (اختياري)") })
                    Row(verticalAlignment = Alignment.CenterVertically) { Text("نافذة منبثقة بدل الشريط", color = OneUi.Ink, modifier = Modifier.weight(1f)); OneSwitch(pop) { pop = it } }
                }
            },
            confirmButton = { TextButton({ if (t.isNotBlank()) { f.ads.add(Ad(t.trim(), tx.trim(), u, pop)); adDlg = false } }) { Text("إضافة", color = OneUi.Blue) } },
            dismissButton = { TextButton({ adDlg = false }) { Text("إلغاء", color = OneUi.Muted) } },
        )
    }
    if (htmlChoice) AlertDialog(
        onDismissRequest = { htmlChoice = false }, containerColor = OneUi.Card,
        title = { Text("ملفات - HTML", color = OneUi.Ink) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                NavCard(Icons.Outlined.UploadFile, OneUi.Blue, "اختيار ملفات", "HTML وCSS وJS وصور من جهازك") { htmlChoice = false; pickFiles.launch(arrayOf("*/*")) }
                NavCard(Icons.Outlined.Code, OneUi.Green, "كتابة كود HTML", "اكتب أو الصق صفحتك مباشرة") { htmlChoice = false; codeDlg = true }
            }
        },
        confirmButton = {}, dismissButton = { TextButton({ htmlChoice = false }) { Text("إغلاق", color = OneUi.Muted) } },
    )
    if (codeDlg) AlertDialog(
        onDismissRequest = { codeDlg = false }, containerColor = OneUi.Card,
        title = { Text("كود HTML", color = OneUi.Ink) },
        text = { OutlinedTextField(code, { code = it }, modifier = Modifier.fillMaxWidth().heightIn(min = 160.dp), placeholder = { Text("<h1>مرحباً</h1>") }) },
        confirmButton = { TextButton({ if (code.isNotBlank()) { Project.writeHtml(ctx, code); f.rev++; codeDlg = false } }) { Text("حفظ كـ index.html", color = OneUi.Blue) } },
        dismissButton = { TextButton({ codeDlg = false }) { Text("إلغاء", color = OneUi.Muted) } },
    )
    if (clearDlg) AlertDialog(
        onDismissRequest = { clearDlg = false }, containerColor = OneUi.Card,
        title = { Text("مسح كل الملفات؟", color = OneUi.Ink) }, text = { Text("سيتم حذف ملفات المشروع الحالية.", color = OneUi.Muted) },
        confirmButton = { TextButton({ Project.clear(ctx); f.rev++; clearDlg = false }) { Text("مسح", color = OneUi.Danger) } },
        dismissButton = { TextButton({ clearDlg = false }) { Text("إلغاء", color = OneUi.Muted) } },
    )
}

@Composable
fun PermsScreen(f: FormState, onBack: () -> Unit) {
    var q by remember { mutableStateOf("") }
    val groups = remember(q) {
        Perms.groups.map { g -> PermGroup(g.title, g.items.filter { q.isBlank() || it.title.contains(q) || it.id.contains(q, true) }) }.filter { it.items.isNotEmpty() }
    }
    Box(Modifier.fillMaxSize().background(OneUi.Bg)) {
        Column(Modifier.fillMaxSize()) {
            OneTopBar("الصلاحيات", onBack)
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(25.dp)).background(OneUi.Card).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Search, null, tint = OneUi.Muted, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(10.dp))
                    BasicTextField(q, { q = it }, Modifier.weight(1f), singleLine = true, textStyle = MaterialTheme.typography.bodyLarge.copy(color = OneUi.Ink), cursorBrush = SolidColor(OneUi.Blue),
                        decorationBox = { inner -> if (q.isEmpty()) Text("ابحث عن صلاحية", style = MaterialTheme.typography.bodyLarge, color = OneUi.Faint); inner() })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("${f.perms.size} مفعّلة من ${Perms.total}", style = MaterialTheme.typography.labelMedium, color = OneUi.Muted, modifier = Modifier.weight(1f))
                    Box(Modifier.oneClick { f.perms.clear(); f.perms.addAll(Perms.groups.flatMap { g -> g.items.map { it.id } }) }.glass(RoundedCornerShape(16.dp), OneUi.Blue, 0.30f, 0.16f).padding(horizontal = 14.dp, vertical = 8.dp)) { Text("تفعيل الكل", style = MaterialTheme.typography.labelMedium, color = OneUi.Blue) }
                    Box(Modifier.oneClick { f.perms.clear(); f.perms.addAll(Perms.defaults) }.glass(RoundedCornerShape(16.dp)).padding(horizontal = 14.dp, vertical = 8.dp)) { Text("الأساسية فقط", style = MaterialTheme.typography.labelMedium, color = OneUi.Ink) }
                }
            }
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 100.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(groups, key = { it.title }) { g ->
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(OneUi.RCard)).background(OneUi.Card)) {
                        val allOn = g.items.all { it.id in f.perms }
                        Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(g.title, style = MaterialTheme.typography.titleMedium, color = OneUi.Ink, modifier = Modifier.weight(1f))
                            Text("${g.items.count { it.id in f.perms }}/${g.items.size}", style = MaterialTheme.typography.labelMedium, color = OneUi.Muted)
                            Spacer(Modifier.width(10.dp))
                            Box(Modifier.oneClick { if (allOn) f.perms.removeAll(g.items.map { it.id }.toSet()) else g.items.forEach { if (it.id !in f.perms) f.perms.add(it.id) } }.glass(RoundedCornerShape(14.dp)).padding(horizontal = 12.dp, vertical = 6.dp)) {
                                Text(if (allOn) "إلغاء المجموعة" else "تفعيل المجموعة", style = MaterialTheme.typography.labelSmall, color = OneUi.Blue)
                            }
                        }
                        g.items.forEach { p ->
                            val on = p.id in f.perms
                            Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(p.title, style = MaterialTheme.typography.bodyLarge, color = OneUi.Ink)
                                    Text(p.id.removePrefix("android.permission."), fontFamily = FontFamily.Monospace, fontSize = 11.5.sp, color = OneUi.Faint, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    if (p.danger) Text("تحتاج موافقة المستخدم عند التشغيل", style = MaterialTheme.typography.labelSmall, color = Color(0xFFFF9F1C))
                                }
                                OneSwitch(on) { v -> if (v) { if (p.id !in f.perms) f.perms.add(p.id) } else f.perms.remove(p.id) }
                            }
                        }
                    }
                }
            }
        }
        EdgeShade(top = 150.dp, bottom = 70.dp)
    }
}

private fun iconFor(file: File): Pair<ImageVector, Color> = when (file.extension.lowercase()) {
    "html", "htm", "xhtml", "vue" -> Icons.Outlined.Html to OneUi.Blue
    "css", "scss", "less" -> Icons.Outlined.Css to Color(0xFFAF52DE)
    "js", "mjs", "jsx", "ts", "tsx" -> Icons.Outlined.Javascript to Color(0xFFFFD60A)
    "json", "webmanifest", "yml", "yaml", "toml", "xml" -> Icons.Outlined.DataObject to OneUi.Green
    "py" -> Icons.Outlined.Code to Color(0xFF4B8BBE)
    "kt", "kts", "java", "swift", "dart", "go", "rs", "c", "h", "cpp", "hpp", "cc", "cs", "php", "rb", "lua", "r" -> Icons.Outlined.Code to Color(0xFFFF9F1C)
    "sh", "bash", "zsh", "bat", "ps1" -> Icons.Outlined.Terminal to OneUi.Green
    "sql" -> Icons.Outlined.Storage to Color(0xFF64D2FF)
    "md", "markdown", "txt", "log" -> Icons.Outlined.Description to OneUi.Muted
    "png", "jpg", "jpeg", "webp", "gif", "bmp", "ico", "svg" -> Icons.Outlined.Image to Color(0xFFFF6BAA)
    else -> Icons.Outlined.Description to OneUi.Muted
}

@Composable
fun FilesScreen(f: FormState, onBack: () -> Unit, onOpen: (String) -> Unit, onRun: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val base = Project.dir(ctx)
    val files = remember(f.rev) { Project.files(ctx) }
    var newDlg by remember { mutableStateOf(false) }
    var renameFor by remember { mutableStateOf<File?>(null) }
    var deleteFor by remember { mutableStateOf<File?>(null) }
    val pickFiles = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) scope.launch {
            val n = withContext(Dispatchers.IO) { runCatching { Project.importFiles(ctx, uris) }.getOrDefault(0) }
            f.rev++; Toast.makeText(ctx, "تمت إضافة $n ملف", Toast.LENGTH_SHORT).show()
        }
    }
    Box(Modifier.fillMaxSize().background(OneUi.Bg)) {
        Column(Modifier.fillMaxSize()) {
            OneTopBar("تعديل الملفات", onBack) {
                Box(Modifier.size(44.dp).oneClick { onRun() }, contentAlignment = Alignment.Center) { Icon(Icons.Outlined.PlayArrow, "تشغيل", tint = OneUi.Green, modifier = Modifier.size(28.dp)) }
                Box(Modifier.size(44.dp).oneClick { pickFiles.launch(arrayOf("*/*")) }, contentAlignment = Alignment.Center) { Icon(Icons.Outlined.UploadFile, "استيراد", tint = OneUi.Ink) }
                Box(Modifier.size(44.dp).oneClick { newDlg = true }, contentAlignment = Alignment.Center) { Icon(Icons.Outlined.NoteAdd, "ملف جديد", tint = OneUi.Blue) }
            }
            Text("${files.size} ملف · اضغط على ملف لتعديله، أو شغّل الموقع بزر التشغيل", style = MaterialTheme.typography.labelMedium, color = OneUi.Muted, modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp))
            if (files.isEmpty()) Text("لا توجد ملفات. أضف ملفاً جديداً أو استورد من جهازك.", Modifier.fillMaxWidth().padding(32.dp), color = OneUi.Muted, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium)
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 40.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(files, key = { it.path }) { file ->
                    val rel = Project.rel(base, file)
                    val (ic, tint) = iconFor(file)
                    Row(Modifier.fillMaxWidth().oneClick { onOpen(rel) }.clip(RoundedCornerShape(22.dp)).background(OneUi.Card).padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(44.dp).clip(CircleShape).background(tint.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) { Icon(ic, null, tint = tint, modifier = Modifier.size(24.dp)) }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(rel, style = MaterialTheme.typography.bodyLarge, color = OneUi.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(if (file.length() < 1024) "${file.length()} B" else "${file.length() / 1024} KB", style = MaterialTheme.typography.labelSmall, color = OneUi.Muted)
                        }
                        Box(Modifier.size(40.dp).oneClick { renameFor = file }, contentAlignment = Alignment.Center) { Icon(Icons.Outlined.DriveFileRenameOutline, "إعادة تسمية", tint = OneUi.Muted) }
                        Box(Modifier.size(40.dp).oneClick { deleteFor = file }, contentAlignment = Alignment.Center) { Icon(Icons.Outlined.DeleteOutline, "حذف", tint = OneUi.Danger) }
                    }
                }
            }
        }
        EdgeShade(top = 90.dp, bottom = 50.dp)
    }
    if (newDlg) NameDialog("ملف جديد", "page.html", "إنشاء", { n -> Project.safe(ctx, n)?.let { it.parentFile?.mkdirs(); if (!it.exists()) it.writeText(""); f.rev++ }; newDlg = false }, { newDlg = false })
    renameFor?.let { old ->
        NameDialog("إعادة تسمية", Project.rel(base, old), "حفظ", { n ->
            val dst = Project.safe(ctx, n)
            if (dst != null && !dst.exists()) { dst.parentFile?.mkdirs(); old.renameTo(dst); f.rev++ } else Toast.makeText(ctx, "الاسم غير صالح أو موجود", Toast.LENGTH_SHORT).show()
            renameFor = null
        }, { renameFor = null })
    }
    deleteFor?.let { file ->
        AlertDialog(
            onDismissRequest = { deleteFor = null }, containerColor = OneUi.Card,
            title = { Text("حذف الملف؟", color = OneUi.Ink) }, text = { Text(Project.rel(base, file), color = OneUi.Muted) },
            confirmButton = { TextButton({ file.delete(); f.rev++; deleteFor = null }) { Text("حذف", color = OneUi.Danger) } },
            dismissButton = { TextButton({ deleteFor = null }) { Text("إلغاء", color = OneUi.Muted) } },
        )
    }
}

@Composable
fun FileEditScreen(f: FormState, relPath: String, onBack: () -> Unit, onRun: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val file = remember(relPath) { Project.safe(ctx, relPath) }
    if (file == null || !file.exists()) { LaunchedEffect(Unit) { onBack() }; return }
    val isText = remember(file) { Project.isText(file) }
    val ext = remember(file) { file.extension.lowercase() }
    var tf by remember(file) { mutableStateOf(androidx.compose.ui.text.input.TextFieldValue(if (isText) runCatching { file.readText() }.getOrDefault("") else "")) }
    var saved by remember(file) { mutableStateOf(tf.text) }
    var renameDlg by remember { mutableStateOf(false) }
    var deleteDlg by remember { mutableStateOf(false) }
    val dirty = isText && tf.text != saved
    val transform = remember(ext) { SyntaxTransformation(ext) }
    fun save() { if (isText) { file.writeText(tf.text); saved = tf.text; f.rev++; Toast.makeText(ctx, "تم الحفظ", Toast.LENGTH_SHORT).show() } }
    val pickReplace = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            withContext(Dispatchers.IO) { ctx.contentResolver.openInputStream(uri)?.use { i -> file.outputStream().use { o -> i.copyTo(o) } } }
            f.rev++; Toast.makeText(ctx, "تم استبدال الملف", Toast.LENGTH_SHORT).show()
        }
    }
    // إدخال رمز عند المؤشر. back = كم حرفاً نرجع المؤشر (لوضعه بين القوسين)
    fun insert(sn: String, back: Int = 0) {
        val st = tf.selection.min; val en = tf.selection.max
        val t = tf.text.substring(0, st) + sn + tf.text.substring(en)
        val pos = st + sn.length - back
        tf = androidx.compose.ui.text.input.TextFieldValue(t, androidx.compose.ui.text.TextRange(pos))
    }
    // ترتيب تلقائي: عند الضغط على Enter يحافظ على إزاحة السطر السابق (ويزيد بعد { أو :)
    fun onEdit(nv: androidx.compose.ui.text.input.TextFieldValue) {
        val old = tf.text
        if (nv.text.length == old.length + 1 && nv.selection.collapsed && nv.selection.start > 0 && nv.text[nv.selection.start - 1] == '\n') {
            val pos = nv.selection.start
            val prevLineStart = nv.text.lastIndexOf('\n', pos - 2) + 1
            val prev = nv.text.substring(prevLineStart, pos - 1)
            var indent = prev.takeWhile { it == ' ' || it == '\t' }
            if (prev.trimEnd().let { it.endsWith("{") || it.endsWith(":") || it.endsWith("(") || it.endsWith("[") }) indent += "  "
            val t = nv.text.substring(0, pos) + indent + nv.text.substring(pos)
            tf = androidx.compose.ui.text.input.TextFieldValue(t, androidx.compose.ui.text.TextRange(pos + indent.length))
        } else tf = nv
    }
    val lines = tf.text.count { it == '\n' } + 1

    Box(Modifier.fillMaxSize().background(OneUi.Bg)) {
        Column(Modifier.fillMaxSize().imePadding()) {
            OneTopBar(file.name, onBack) {
                if (dirty) Box(Modifier.size(10.dp).clip(CircleShape).background(OneUi.Blue))
                Box(Modifier.size(44.dp).oneClick { if (dirty) save() }, contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Save, "حفظ", tint = if (dirty) OneUi.Blue else OneUi.Faint) }
                Box(Modifier.size(44.dp).oneClick { if (dirty) save(); onRun() }, contentAlignment = Alignment.Center) { Icon(Icons.Outlined.PlayArrow, "تشغيل", tint = OneUi.Green, modifier = Modifier.size(28.dp)) }
            }
            Row(Modifier.padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(relPath, style = MaterialTheme.typography.labelSmall, color = OneUi.Muted, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
                if (isText) Box(Modifier.glass(RoundedCornerShape(12.dp), OneUi.Blue, 0.28f, 0.14f).padding(horizontal = 10.dp, vertical = 3.dp)) { Text(Syntax.name(ext) + " · $lines سطر", style = MaterialTheme.typography.labelSmall, color = OneUi.Blue) }
            }
            if (isText) {
                Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).clip(RoundedCornerShape(OneUi.RCard)).background(Color(0xFF1E1E1E)).border(1.dp, OneUi.Line, RoundedCornerShape(OneUi.RCard))) {
                    Row(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = 14.dp)) {
                        // أرقام الأسطر
                        Column(Modifier.background(Color(0xFF252526)).padding(start = 12.dp, end = 10.dp), horizontalAlignment = Alignment.End) {
                            for (i in 1..lines) Text("$i", fontFamily = FontFamily.Monospace, fontSize = 13.sp, lineHeight = 21.sp, color = Color(0xFF6E7681))
                        }
                        Box(Modifier.weight(1f).horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp)) {
                            BasicTextField(
                                tf, { onEdit(it) }, Modifier.widthIn(min = 300.dp),
                                textStyle = TextStyle(color = Color(0xFFD4D4D4), fontFamily = FontFamily.Monospace, fontSize = 13.sp, lineHeight = 21.sp, textDirection = TextDirection.Ltr),
                                cursorBrush = SolidColor(Color.White), visualTransformation = transform,
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(autoCorrect = false, keyboardType = androidx.compose.ui.text.input.KeyboardType.Ascii),
                            )
                        }
                    }
                }
                // رموز سريعة للكتابة من الجوال
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Tab" to "  ", "{ }" to "{}", "( )" to "()", "[ ]" to "[]", "< >" to "<>", "\" \"" to "\"\"", "' '" to "''", ";" to ";", ":" to ":", "=" to "=", "/" to "/", "#" to "#", "$" to "$", "!" to "!", "_" to "_", "-" to "-").forEach { (label, ins) ->
                        Box(Modifier.waterClick(RoundedCornerShape(12.dp)) { insert(ins, if (ins.length == 2 && label != "Tab") 1 else 0) }.glass(RoundedCornerShape(12.dp)).padding(horizontal = 14.dp, vertical = 8.dp)) {
                            Text(label, fontFamily = FontFamily.Monospace, fontSize = 14.sp, color = OneUi.Ink)
                        }
                    }
                }
            } else {
                Column(Modifier.weight(1f).fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    if (Project.isImage(file)) {
                        val bmp = remember(file, f.rev) { BitmapFactory.decodeFile(file.path)?.asImageBitmap() }
                        if (bmp != null) Image(bmp, null, Modifier.fillMaxWidth().heightIn(max = 320.dp).clip(RoundedCornerShape(OneUi.RCard)), contentScale = ContentScale.Fit)
                    } else Icon(Icons.Outlined.Description, null, tint = OneUi.Faint, modifier = Modifier.size(64.dp))
                    Text(if (Project.isImage(file)) "صورة" else "ملف غير نصي لا يمكن تعديله هنا", style = MaterialTheme.typography.bodyMedium, color = OneUi.Muted, modifier = Modifier.padding(top = 12.dp))
                    Text("${file.length() / 1024} KB", style = MaterialTheme.typography.labelSmall, color = OneUi.Faint)
                }
            }
            Row(Modifier.navigationBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isText) OneButton("حفظ", { save() }, Modifier.weight(1f), enabled = dirty, icon = Icons.Outlined.Save)
                else OneButton("استبدال", { pickReplace.launch(arrayOf("*/*")) }, Modifier.weight(1f), icon = Icons.Outlined.SwapHoriz)
                OneButton("تسمية", { renameDlg = true }, Modifier.weight(1f), primary = false, icon = Icons.Outlined.DriveFileRenameOutline)
                OneButton("حذف", { deleteDlg = true }, Modifier.weight(1f), primary = false, icon = Icons.Outlined.DeleteOutline)
            }
        }
    }
    if (renameDlg) NameDialog("إعادة تسمية", relPath, "حفظ", { n ->
        val dst = Project.safe(ctx, n)
        if (dst != null && !dst.exists()) { dst.parentFile?.mkdirs(); file.renameTo(dst); f.rev++; renameDlg = false; onBack() } else Toast.makeText(ctx, "الاسم غير صالح أو موجود", Toast.LENGTH_SHORT).show()
    }, { renameDlg = false })
    if (deleteDlg) AlertDialog(
        onDismissRequest = { deleteDlg = false }, containerColor = OneUi.Card,
        title = { Text("حذف الملف؟", color = OneUi.Ink) }, text = { Text(relPath, color = OneUi.Muted) },
        confirmButton = { TextButton({ file.delete(); f.rev++; deleteDlg = false; onBack() }) { Text("حذف", color = OneUi.Danger) } },
        dismissButton = { TextButton({ deleteDlg = false }) { Text("إلغاء", color = OneUi.Muted) } },
    )
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PreviewScreen(path: String, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val dir = Project.dir(ctx)
    val target = remember(path) {
        val sel = if (path.isNotEmpty()) Project.safe(ctx, path) else null
        if (sel != null && sel.exists() && sel.extension.lowercase() in setOf("html", "htm")) sel else File(dir, "index.html")
    }
    var web by remember { mutableStateOf<WebView?>(null) }
    Box(Modifier.fillMaxSize().background(OneUi.Bg)) {
        Column(Modifier.fillMaxSize()) {
            OneTopBar("تشغيل: ${target.name}", onBack) {
                Box(Modifier.size(44.dp).oneClick { web?.reload() }, contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Refresh, "إعادة التحميل", tint = OneUi.Ink) }
            }
            if (!target.exists()) Text("لا يوجد index.html لتشغيله", Modifier.padding(32.dp), color = OneUi.Danger)
            else Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp).padding(bottom = 12.dp).clip(RoundedCornerShape(OneUi.RCard))) {
                AndroidView(factory = { c ->
                    WebView(c).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.allowFileAccess = true
                        @Suppress("DEPRECATION") run { settings.allowFileAccessFromFileURLs = true; settings.allowUniversalAccessFromFileURLs = true }
                        settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        webViewClient = WebViewClient()
                        loadUrl("file://" + target.absolutePath)
                        web = this
                    }
                }, modifier = Modifier.fillMaxSize(), onRelease = { it.destroy() })
            }
        }
    }
}
