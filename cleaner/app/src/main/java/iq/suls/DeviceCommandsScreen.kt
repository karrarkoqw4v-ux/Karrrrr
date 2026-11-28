package iq.suls

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class CommandLog(val cmd: String, val output: String, val exitCode: Int, val viaShizuku: Boolean)

@Composable
fun DeviceCommandsScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    var input by remember { mutableStateOf("") }
    val logs = remember { mutableStateListOf<CommandLog>() }
    var running by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val binderAlive = ShizukuBridge.binderAlive
    val granted = ShizukuBridge.permissionGranted

    Column(Modifier.fillMaxSize()) {
        TopBarSuls("تصحيح الأخطاء اللاسلكي (Shizuku)", onBack)

        LazyColumn(Modifier.weight(1f).padding(horizontal = 20.dp)) {
            item {
                StatusRow(
                    title = "اتصال Shizuku",
                    value = if (binderAlive) "مفعل" else "غير مفعل"
                )
                Spacer(Modifier.height(10.dp))
                if (!binderAlive) {
                    SulsCard {
                        Text(
                            "ما لقيت Shizuku شغّال. ثبّت تطبيق Shizuku الرسمي وشغّله بنفسك عبر إعدادات تصحيح الأخطاء اللاسلكي (خطوات أندرويد الحقيقية، بدون أي إشعار مزيّف مني):",
                            color = Suls.TextSecondary, fontSize = 13.sp
                        )
                        Spacer(Modifier.height(10.dp))
                        Button(
                            onClick = { ShizukuBridge.openDeveloperSettings(ctx) },
                            colors = ButtonDefaults.buttonColors(containerColor = Suls.Accent)
                        ) { Text("فتح إعدادات تصحيح الأخطاء اللاسلكي", color = Suls.Bg) }
                    }
                } else {
                    Spacer(Modifier.height(4.dp))
                    StatusRow(title = "صلاحية سلس داخل Shizuku", value = if (granted) "مفعل" else "غير مفعل")
                    Spacer(Modifier.height(10.dp))
                    if (!granted) {
                        Button(
                            onClick = { ShizukuBridge.requestPermission() },
                            colors = ButtonDefaults.buttonColors(containerColor = Suls.Accent),
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("طلب الصلاحية من Shizuku", color = Suls.Bg) }
                    }
                }
                Spacer(Modifier.height(18.dp))
                Text(
                    if (granted) "الأوامر الحين تنفّذ بهوية شل حقيقية (نفس صلاحية ADB) — أوامر مثل settings put global أو am force-stop راح تشتغل فعلياً."
                    else "بدون Shizuku، الأوامر تنفّذ بصلاحية التطبيق العادية فقط — أوامر النظام (settings put global, am force-stop لتطبيق ثاني) راح ترجع Permission denied.",
                    color = Suls.TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(bottom = 14.dp)
                )
            }
        }

        Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = input, onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("مثال: settings put global window_animation_scale 0.5") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Suls.TextPrimary, unfocusedTextColor = Suls.TextPrimary,
                    focusedBorderColor = Suls.Accent, unfocusedBorderColor = Suls.TextSecondary
                )
            )
            Spacer(Modifier.width(10.dp))
            IconButton(
                onClick = {
                    val cmd = input.trim()
                    if (cmd.isNotEmpty() && !running) {
                        running = true
                        val useShizuku = granted && ShizukuBridge.service != null
                        scope.launch {
                            val output = withContext(Dispatchers.IO) {
                                if (useShizuku) {
                                    try { ShizukuBridge.service?.exec(cmd) ?: "خدمة Shizuku غير مربوطة بعد" }
                                    catch (e: Exception) { "فشل التنفيذ عبر Shizuku: ${e.message}" }
                                } else {
                                    ShellRunner.run(cmd).output
                                }
                            }
                            logs.add(0, CommandLog(cmd, output, 0, useShizuku))
                            running = false
                        }
                        input = ""
                    }
                },
                enabled = !running
            ) {
                if (running) CircularProgressIndicator(Modifier.size(22.dp), color = Suls.Accent, strokeWidth = 2.dp)
                else Icon(Icons.Default.PlayArrow, null, tint = Suls.Accent)
            }
        }

        Spacer(Modifier.height(16.dp))
        LazyColumn(Modifier.weight(1f).padding(horizontal = 20.dp)) {
            items(logs) { log ->
                Surface(color = Suls.Card, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("$ ${log.cmd}", color = Suls.Accent, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Text(if (log.viaShizuku) "Shizuku" else "عادي", color = Suls.TextSecondary, fontSize = 10.sp)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(log.output, color = Suls.TextPrimary, fontSize = 12.sp)
                    }
                }
                Spacer(Modifier.height(10.dp))
            }
            item { Spacer(Modifier.height(40.dp)) }
        }
    }
}

@Composable
private fun StatusRow(title: String, value: String) {
    SulsCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = Suls.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            if (value == "مفعل") Icon(Icons.Default.CheckCircle, null, tint = Suls.Accent, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(value, color = if (value == "مفعل") Suls.Accent else Suls.TextSecondary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}
