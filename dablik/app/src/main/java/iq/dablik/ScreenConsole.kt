package iq.dablik

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
private fun LogView(lines: List<String>, modifier: Modifier = Modifier) {
    val scroll = rememberScrollState()
    LaunchedEffect(lines.size) { scroll.animateScrollTo(scroll.maxValue) }
    Column(
        modifier.clip(RoundedCornerShape(OneUi.RCard)).background(Color(0xFF050505)).padding(14.dp).verticalScroll(scroll),
    ) {
        if (lines.isEmpty()) Text("لا يوجد إخراج بعد.", style = MaterialTheme.typography.labelMedium, color = OneUi.Faint)
        lines.forEach { l ->
            Text(l, fontFamily = FontFamily.Monospace, fontSize = 13.sp, lineHeight = 20.sp,
                color = if (l.startsWith("[خطأ]") || l.startsWith("[Error")) OneUi.Danger else if (l.startsWith("$")) OneUi.Blue else Color(0xFFD8D8D8))
        }
    }
}

/** قسم مستقل: تنفيذ أوامر شل حقيقية على ملفات مشروعك، وتنفيذ أكواد جافاسكربت معزولة — بدون أي وصول خارج صندوق التطبيق */
@Composable
fun ConsoleScreenHome(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var tab by remember { mutableIntStateOf(0) } // 0 أوامر الملفات، 1 كود جافاسكربت، 2 بايثون عبر Termux
    var granted by remember {
        mutableStateOf(Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)
    }
    val askNotif = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    LaunchedEffect(Unit) { if (!granted) askNotif.launch(Manifest.permission.POST_NOTIFICATIONS) }

    // ---- أوامر الشل ----
    var cmd by remember { mutableStateOf("") }
    val shellLog = remember { mutableStateListOf<String>() }
    var running by remember { mutableStateOf(false) }

    // ---- الكود ----
    var code by remember { mutableStateOf("// مثال: بوت تيليجرام بسيط\n// ضع التوكن هنا ثم اضغط تشغيل، أو «تشغيل بالخلفية» ليستمر\nvar TOKEN = \"ضع_التوكن_هنا\";\nlog(\"تم البدء\");\nvar r = http.get(\"https://api.telegram.org/bot\" + TOKEN + \"/getMe\");\nlog(r);") }
    val codeLog = remember { mutableStateListOf<String>() }
    var bgRunning by remember { mutableStateOf(ScriptService.running) }
    DisposableEffect(Unit) {
        val l: (String) -> Unit = { line -> codeLog.add(line) }
        ScriptService.addListener(l)
        onDispose { ScriptService.removeListener(l) }
    }

    Box(Modifier.fillMaxSize().background(OneUi.Bg)) {
        Column(Modifier.fillMaxSize()) {
            OneTopBar("منفّذ الأوامر", onBack)
            Row(Modifier.padding(horizontal = 16.dp, vertical = 6.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("أوامر الملفات" to 0, "كود جافاسكربت" to 1, "بايثون (عبر Termux)" to 2).forEach { (t, i) ->
                    Box(Modifier.oneClick { tab = i }.glass(RoundedCornerShape(18.dp), if (tab == i) OneUi.Blue else Color.White, if (tab == i) 0.30f else 0.12f, if (tab == i) 0.16f else 0.06f).padding(horizontal = 18.dp, vertical = 10.dp)) {
                        Text(t, style = MaterialTheme.typography.labelMedium, color = if (tab == i) OneUi.Blue else OneUi.Ink)
                    }
                }
            }
            Text(
                if (tab == 0) "تشتغل داخل مجلد مشروعك فقط (نفس ملفات ZIP/HTML). أمثلة: ls، mkdir assets، mv a.html b.html، grep -r \"foo\" ."
                else "جافاسكربت معزول: log(نص)، sleep(مللي ثانية)، http.get(رابط)، http.post(رابط, نص). بلا وصول لملفات النظام أو تطبيقات ثانية.",
                style = MaterialTheme.typography.labelSmall, color = OneUi.Muted, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )

            if (tab == 0) {
                Column(Modifier.weight(1f).padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    LogView(shellLog, Modifier.weight(1f).fillMaxWidth())
                    Row(Modifier.fillMaxWidth().height(54.dp).clip(RoundedCornerShape(27.dp)).background(OneUi.Card).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        BasicTextField(
                            cmd, { cmd = it }, Modifier.weight(1f), singleLine = true,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = OneUi.Ink, fontFamily = FontFamily.Monospace),
                            cursorBrush = SolidColor(OneUi.Blue),
                            decorationBox = { inner -> if (cmd.isEmpty()) Text("اكتب أمراً...", style = MaterialTheme.typography.bodyLarge, color = OneUi.Faint); inner() },
                        )
                        Box(Modifier.size(38.dp).oneClick(enabled = !running && cmd.isNotBlank()) {
                            val c = cmd.trim(); shellLog.add("$ $c"); cmd = ""; running = true
                            scope.launch {
                                val (code2, out) = withContext(Dispatchers.IO) { ShellRunner.run(c, Project.dir(ctx)) }
                                if (out.isNotBlank()) shellLog.add(out.trimEnd())
                                if (code2 != 0) shellLog.add("[خطأ] رمز الخروج: $code2")
                                running = false
                            }
                        }, contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Send, "تنفيذ", tint = if (!running && cmd.isNotBlank()) OneUi.Blue else OneUi.Faint) }
                    }
                }
            } else if (tab == 1) {
                Column(Modifier.weight(1f).padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.weight(0.42f).fillMaxWidth().clip(RoundedCornerShape(OneUi.RCard)).background(Color(0xFF1E1E1E))) {
                        BasicTextField(
                            code, { code = it }, Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFFD4D4D4), fontFamily = FontFamily.Monospace, fontSize = 13.sp, lineHeight = 20.sp),
                            cursorBrush = SolidColor(Color.White),
                        )
                    }
                    LogView(codeLog, Modifier.weight(0.58f).fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OneButton("تشغيل الآن", {
                            codeLog.clear(); codeLog.add("$ تشغيل مرة واحدة")
                            scope.launch {
                                val err = withContext(Dispatchers.Default) { CodeRunner.runOnce(code) { line -> codeLog.add(line) } }
                                if (err != null) codeLog.add("[خطأ] $err")
                            }
                        }, Modifier.weight(1f), icon = Icons.Outlined.PlayArrow)
                        if (!bgRunning) OneButton("تشغيل بالخلفية", {
                            ScriptRunner.start(ctx, code); bgRunning = true; codeLog.clear(); codeLog.add("$ بدء التشغيل بالخلفية (إشعار دائم يبيّنه)")
                        }, Modifier.weight(1f), primary = false, icon = Icons.Outlined.CloudSync)
                        else OneButton("إيقاف الخلفية", { ScriptRunner.stop(ctx); bgRunning = false; codeLog.add("$ تم طلب الإيقاف") }, Modifier.weight(1f), primary = false, icon = Icons.Outlined.Stop)
                    }
                    if (bgRunning) Text("شغّال بالخلفية الآن — شوف الإشعار لإيقافه من هناك أيضاً.", style = MaterialTheme.typography.labelSmall, color = OneUi.Green, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                }
            } else {
                var tInstalled by remember { mutableStateOf(TermuxBridge.installed(ctx)) }
                var tGranted by remember { mutableStateOf(TermuxBridge.granted(ctx)) }
                val askTermux = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { tGranted = it }
                var libs by remember { mutableStateOf("") }
                var pyCode by remember { mutableStateOf("print(\"أهلاً من بايثون\")") }
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (!tInstalled) {
                        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(OneUi.RCard)).background(OneUi.Card).padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Outlined.Terminal, null, tint = OneUi.Faint, modifier = Modifier.size(40.dp))
                            Text("تطبيق Termux غير مثبّت على جهازك", style = MaterialTheme.typography.bodyLarge, color = OneUi.Ink, modifier = Modifier.padding(top = 8.dp))
                            Text("ثبّته أول مرة (من F-Droid أو GitHub الرسمي)، ثم ارجع هنا واضغط تحديث.", style = MaterialTheme.typography.labelMedium, color = OneUi.Muted, textAlign = TextAlign.Center)
                            Spacer(Modifier.height(10.dp))
                            OneButton("تحديث الحالة", { tInstalled = TermuxBridge.installed(ctx); tGranted = TermuxBridge.granted(ctx) }, primary = false, icon = Icons.Outlined.Refresh)
                        }
                    } else if (!tGranted) {
                        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(OneUi.RCard)).background(OneUi.Card).padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Outlined.Lock, null, tint = OneUi.Faint, modifier = Modifier.size(40.dp))
                            Text("نحتاج إذنك لإرسال أوامر إلى Termux", style = MaterialTheme.typography.bodyLarge, color = OneUi.Ink, modifier = Modifier.padding(top = 8.dp))
                            Spacer(Modifier.height(10.dp))
                            OneButton("السماح", { askTermux.launch(TermuxBridge.PERM) }, icon = Icons.Outlined.Login)
                        }
                    } else {
                        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(OneUi.RCard)).background(OneUi.Card).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("أول مرة فقط: ثبّت بايثون داخل Termux", style = MaterialTheme.typography.titleMedium, color = OneUi.Ink)
                            OneButton("تثبيت بايثون", { TermuxBridge.installSystemTool(ctx, "python") }, primary = false, icon = Icons.Outlined.Download)
                        }
                        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(OneUi.RCard)).background(OneUi.Card).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("تثبيت مكتبات بايثون (pip)", style = MaterialTheme.typography.titleMedium, color = OneUi.Ink)
                            OneField(libs, { libs = it }, "أسماء المكتبات", hint = "requests flask numpy")
                            OneButton("تثبيت المكتبات", { if (libs.isNotBlank()) TermuxBridge.installPackages(ctx, libs) }, icon = Icons.Outlined.Download)
                        }
                        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(OneUi.RCard)).background(OneUi.Card).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("كود بايثون", style = MaterialTheme.typography.titleMedium, color = OneUi.Ink)
                            Box(Modifier.fillMaxWidth().heightIn(min = 140.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFF1E1E1E)).padding(12.dp)) {
                                BasicTextField(pyCode, { pyCode = it }, Modifier.fillMaxSize(),
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFFD4D4D4), fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                                    cursorBrush = SolidColor(Color.White))
                            }
                            OneButton("تشغيل في Termux", { TermuxBridge.runPython(ctx, pyCode) }, icon = Icons.Outlined.PlayArrow)
                            Text("سيفتح Termux ويشغّل الكود هناك فعلياً — شوف النتيجة بجلسته مباشرة.", style = MaterialTheme.typography.labelSmall, color = OneUi.Muted)
                        }
                    }
                }
            }
        }
    }
}
