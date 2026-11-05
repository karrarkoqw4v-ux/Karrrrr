package iq.student.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import iq.student.ai.AiClient
import iq.student.ai.AiException
import iq.student.ai.Prompts
import iq.student.data.AiTask
import iq.student.data.Catalog
import iq.student.data.Prefs
import iq.student.ui.components.*
import iq.student.ui.theme.AppColors
import iq.student.ui.theme.dimens
import iq.student.util.ScanSession
import iq.student.util.rememberSpeaker
import kotlinx.coroutines.launch

/**
 * إدخال بالكتابة: "كتابة السؤال وترجمته" (إنكليزي/عربي) و"اسأل المساعد".
 */
@Composable
fun TypedScreen(task: AiTask, onBack: () -> Unit, onSettings: () -> Unit) {
    val speaker = rememberSpeaker()
    val scope = rememberCoroutineScope()
    val translate = task == AiTask.TranslateTyped
    var input by remember { mutableStateOf("") }
    var toArabic by remember { mutableStateOf(true) }
    var loading by remember { mutableStateOf(false) }
    var output by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    fun run() {
        if (input.isBlank() || loading) return
        loading = true; error = null; output = null
        scope.launch {
            val grade = Catalog.grade(Prefs.gradeId)
            val subject = Catalog.subject(Prefs.gradeId, Prefs.branch, ScanSession.subjectId)
            val prompt = if (translate) {
                (if (toArabic) "Translate this English text into Arabic. Output only the translation.\n\n" else "Translate this Arabic text into English. Output only the translation.\n\n") + input
            } else Prompts.user(AiTask.Ask) + "\n\nStudent question:\n" + input
            try {
                output = AiClient.ask(Prompts.system(grade, subject), prompt)
            } catch (e: AiException) {
                error = e.message
            } catch (e: Exception) {
                error = "صار خطأ غير متوقع: ${e.message}"
            }
            loading = false
        }
    }

    Column(Modifier.fillMaxSize().background(AppColors.Bg).imePadding()) {
        ScreenHeader(if (translate) "كتابة السؤال وترجمته" else "اسأل المساعد", onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = dimens.pad).padding(top = 8.dp, bottom = 16.dp)) {
            if (translate) {
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(androidx.compose.ui.graphics.Color(0xFFEAECF5)).padding(4.dp)) {
                    listOf("إنكليزي إلى عربي" to true, "عربي إلى إنكليزي" to false).forEach { (label, dir) ->
                        Box(
                            Modifier.weight(1f).height(38.dp).clip(RoundedCornerShape(11.dp))
                                .background(if (toArabic == dir) AppColors.Surface else androidx.compose.ui.graphics.Color.Transparent)
                                .clickable { toArabic = dir; output = null },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(label, style = MaterialTheme.typography.labelMedium, color = if (toArabic == dir) AppColors.Primary else AppColors.Muted)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
            OutlinedTextField(
                value = input, onValueChange = { input = it },
                modifier = Modifier.fillMaxWidth().heightIn(min = 140.dp),
                shape = RoundedCornerShape(18.dp),
                placeholder = { Text(if (translate) "اكتب السؤال أو الإنشاء هنا" else "اكتب سؤالك هنا", color = AppColors.Faint) },
                textStyle = MaterialTheme.typography.bodyLarge,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AppColors.Primary, unfocusedBorderColor = AppColors.Line,
                    focusedContainerColor = AppColors.Surface, unfocusedContainerColor = AppColors.Surface,
                ),
            )
            Spacer(Modifier.height(12.dp))
            AppButton(if (translate) "ترجمة" else "اسأل", { run() }, icon = if (translate) Icons.Outlined.Translate else Icons.Outlined.Send)
            Spacer(Modifier.height(16.dp))
            if (loading) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(color = AppColors.Primary, strokeWidth = 3.dp, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("لحظة...", style = MaterialTheme.typography.bodyLarge, color = AppColors.Ink)
                }
            }
            error?.let {
                if (it == "NO_KEY") {
                    ErrorBox("لازم تدخل مفتاح الخدمة من الإعدادات حتى يشتغل الذكاء الاصطناعي")
                    Spacer(Modifier.height(10.dp))
                    AppButton("فتح الإعدادات", onSettings, icon = Icons.Outlined.Settings)
                } else ErrorBox(it)
            }
            output?.let { out ->
                TextCard(out, highlight = true)
                Spacer(Modifier.height(10.dp))
                val outIsArabic = !translate || toArabic
                Actions(out, speaker, speakLocaleArabic = outIsArabic)
            }
        }
    }
}
