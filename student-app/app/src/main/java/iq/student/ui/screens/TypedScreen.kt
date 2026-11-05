package iq.student.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import iq.student.data.StudyTask
import iq.student.offline.Translate
import iq.student.ui.components.*
import iq.student.ui.theme.AppColors
import iq.student.ui.theme.dimens
import iq.student.util.rememberSpeaker
import kotlinx.coroutines.launch

/** إدخال بالكتابة: ترجمة السؤال (إنكليزي/عربي) أو حاسبة معادلات */
@Composable
fun TypedScreen(task: StudyTask, onBack: () -> Unit) {
    val speaker = rememberSpeaker()
    val scope = rememberCoroutineScope()
    val translate = task == StudyTask.TranslateTyped
    var input by remember { mutableStateOf("") }
    var toArabic by remember { mutableStateOf(true) }
    var loading by remember { mutableStateOf(false) }
    var output by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    fun run() {
        if (input.isBlank() || loading) return
        loading = true; error = null; output = null
        scope.launch {
            try {
                output = Translate.run(input, toArabic)
            } catch (e: Exception) {
                error = "تحتاج إنترنت أول مرة حتى ينزل نموذج الترجمة، وبعدها يشتغل بدونه"
            }
            loading = false
        }
    }

    Column(Modifier.fillMaxSize().background(AppColors.Bg).imePadding()) {
        ScreenHeader(if (translate) "كتابة السؤال وترجمته" else "اكتب المسألة", onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = dimens.pad).padding(top = 8.dp, bottom = 16.dp)) {
            if (!translate) {
                MathBody(initial = "", readAloud = false, speaker = speaker, fromPhoto = false)
                return@Column
            }
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color(0xFFEAECF5)).padding(4.dp)) {
                listOf("إنكليزي إلى عربي" to true, "عربي إلى إنكليزي" to false).forEach { (label, dir) ->
                    Box(
                        Modifier.weight(1f).height(38.dp).bounceClick { toArabic = dir; output = null }.clip(RoundedCornerShape(11.dp))
                            .background(if (toArabic == dir) AppColors.Surface else Color.Transparent),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(label, style = MaterialTheme.typography.labelMedium, color = if (toArabic == dir) AppColors.Primary else AppColors.Muted)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = input, onValueChange = { input = it },
                modifier = Modifier.fillMaxWidth().heightIn(min = 140.dp),
                shape = RoundedCornerShape(18.dp),
                placeholder = { Text("اكتب السؤال أو الإنشاء هنا", color = AppColors.Faint) },
                textStyle = MaterialTheme.typography.bodyLarge,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AppColors.Primary, unfocusedBorderColor = AppColors.Line,
                    focusedContainerColor = AppColors.Surface, unfocusedContainerColor = AppColors.Surface,
                ),
            )
            Spacer(Modifier.height(12.dp))
            AppButton("ترجمة", { run() }, icon = Icons.Outlined.Translate)
            Spacer(Modifier.height(16.dp))
            if (loading) LoadingBox("نترجم... (أول مرة ننزّل نموذج الترجمة)")
            error?.let { ErrorBox(it) }
            output?.let { out ->
                TextCard(out, highlight = true, ltr = !toArabic)
                Spacer(Modifier.height(10.dp))
                Actions(out, speaker, speakLocaleArabic = toArabic)
            }
        }
    }
}
