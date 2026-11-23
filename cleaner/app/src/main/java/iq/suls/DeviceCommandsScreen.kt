package iq.suls

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class CommandLog(val cmd: String, val output: String, val exitCode: Int)

@Composable
fun DeviceCommandsScreen(onBack: () -> Unit) {
    var input by remember { mutableStateOf("") }
    val logs = remember { mutableStateListOf<CommandLog>() }
    var running by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize()) {
        TopBarSuls("أوامر خاص الجهاز", onBack)

        Surface(color = Suls.Card, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            Text(
                "ينفّذ أي أمر شل بصلاحية التطبيق نفسه (بدون روت). أوامر تحتاج صلاحية نظام مثل settings put global أو am force-stop لتطبيق ثاني راح ترجع \"Permission denied\" فعلياً — هذا تصرف أندرويد الحقيقي نفسه، مو شي أنا مانعه.",
                color = Suls.TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(16.dp)
            )
        }
        Spacer(Modifier.height(14.dp))

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
                        scope.launch {
                            val r = withContext(Dispatchers.IO) { ShellRunner.run(cmd) }
                            logs.add(0, CommandLog(cmd, r.output, r.exitCode))
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
                        Text("$ ${log.cmd}", color = Suls.Accent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            log.output, color = if (log.exitCode == 0) Suls.TextPrimary else Suls.Danger,
                            fontSize = 12.sp
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
            }
            item { Spacer(Modifier.height(40.dp)) }
        }
    }
}
