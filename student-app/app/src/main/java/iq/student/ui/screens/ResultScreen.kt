package iq.student.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
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
import iq.student.util.Speaker
import iq.student.util.rememberSpeaker
import org.json.JSONObject

private sealed interface UiState {
    data object Loading : UiState
    data class Done(val text: String) : UiState
    data class Failed(val message: String) : UiState
}

/** شاشة النتيجة: ترسل الصورة المقصوصة للذكاء الاصطناعي وتعرض الناتج حسب نوع المهمة */
@Composable
fun ResultScreen(task: AiTask, onBack: () -> Unit, onAgain: () -> Unit, onSettings: () -> Unit) {
    val speaker = rememberSpeaker()
    var state by remember { mutableStateOf<UiState>(UiState.Loading) }
    var attempt by remember { mutableIntStateOf(0) }
    val image = ScanSession.cropped

    LaunchedEffect(attempt) {
        state = UiState.Loading
        val grade = Catalog.grade(Prefs.gradeId)
        val subject = Catalog.subject(Prefs.gradeId, Prefs.branch, ScanSession.subjectId)
        state = try {
            UiState.Done(AiClient.ask(Prompts.system(grade, subject), Prompts.user(task), image))
        } catch (e: AiException) {
            UiState.Failed(e.message ?: "صار خطأ")
        } catch (e: Exception) {
            UiState.Failed("صار خطأ غير متوقع: ${e.message}")
        }
    }

    // قراءة تلقائية للشرح المفصل (المطلوب: التطبيق يقرأ المسألة ويشرحها بالصوت)
    LaunchedEffect(state) {
        val s = state
        if (task == AiTask.MathExplain && s is UiState.Done) speaker.speak(s.text, speaker.arabic)
    }

    val title = when (task) {
        AiTask.TranslateText -> "الترجمة"
        AiTask.TranslatePronounce -> "ترجمة مع النطق"
        AiTask.MathSimple -> "حل مسألة بالصورة"
        AiTask.MathExplain -> "حل وشرح المسألة"
        AiTask.SciencePage -> "نتيجة مسح الصفحة"
        AiTask.ScienceSolve -> "حل المسألة"
        AiTask.BioExplain -> "شرح الدرس"
        AiTask.BioDiagram -> "الرسومات والتسميات"
        else -> "الجواب"
    }

    Column(Modifier.fillMaxSize().background(AppColors.Bg)) {
        ScreenHeader(title, onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = dimens.pad).padding(top = 8.dp, bottom = 16.dp)) {
            image?.let {
                Image(
                    it.asImageBitmap(), null, contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth().heightIn(max = 160.dp).clip(RoundedCornerShape(14.dp)).background(AppColors.Surface),
                )
                Spacer(Modifier.height(14.dp))
            }
            when (val s = state) {
                UiState.Loading -> LoadingBox(if (task == AiTask.MathExplain) "نقرا المسألة ونحضّر الشرح..." else "نقرا الصورة ونجهّز الجواب...")
                is UiState.Failed -> {
                    if (s.message == "NO_KEY") {
                        ErrorBox("لازم تدخل مفتاح الخدمة من الإعدادات حتى يشتغل الذكاء الاصطناعي")
                        Spacer(Modifier.height(12.dp))
                        AppButton("فتح الإعدادات", onSettings, icon = Icons.Outlined.Settings)
                    } else {
                        ErrorBox(s.message)
                        Spacer(Modifier.height(12.dp))
                        AppButton("إعادة المحاولة", { attempt++ }, icon = Icons.Outlined.Refresh)
                    }
                }
                is UiState.Done -> ResultBody(task, s.text, speaker)
            }
        }
        Row(Modifier.padding(horizontal = dimens.pad).padding(bottom = 16.dp).navigationBarsPadding(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AppButton("مسألة ثانية", onAgain, Modifier.weight(1f), icon = Icons.Outlined.PhotoCamera)
        }
    }
}

@Composable
private fun LoadingBox(text: String) {
    AppCard(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(color = AppColors.Primary, strokeWidth = 3.dp, modifier = Modifier.size(26.dp))
            Spacer(Modifier.width(14.dp))
            Text(text, style = MaterialTheme.typography.bodyLarge, color = AppColors.Ink)
        }
    }
}

@Composable
private fun ResultBody(task: AiTask, text: String, speaker: Speaker) {
    when (task) {
        AiTask.TranslatePronounce -> PronounceBody(text, speaker)
        AiTask.SciencePage -> SciencePageBody(text)
        AiTask.TranslateText -> {
            TextCard(text)
            Spacer(Modifier.height(12.dp))
            Actions(text, speaker, speakLocaleArabic = true)
        }
        else -> {
            TextCard(text)
            Spacer(Modifier.height(12.dp))
            Actions(text, speaker, speakLocaleArabic = true)
        }
    }
}

@Composable
internal fun TextCard(text: String, highlight: Boolean = false) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
            .background(if (highlight) AppColors.PrimarySoft else AppColors.Surface).padding(16.dp),
    ) {
        // عناوين الأقسام ("المسألة:" ...) تظهر بخط عريض
        text.lines().forEach { line ->
            val isHeader = line.trim().endsWith(":") || line.trim().endsWith("؟") && line.length < 30
            if (line.isBlank()) Spacer(Modifier.height(6.dp))
            else Text(
                line.trim(),
                style = if (isHeader) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
                color = if (isHeader) AppColors.Primary else AppColors.Ink,
                modifier = Modifier.padding(vertical = 2.dp),
            )
        }
    }
}

@Composable
internal fun Actions(text: String, speaker: Speaker, speakLocaleArabic: Boolean) {
    val clip = LocalClipboardManager.current
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        ActionChip(Icons.Outlined.VolumeUp, "اقرأ") { speaker.speak(text, if (speakLocaleArabic) speaker.arabic else speaker.english) }
        ActionChip(Icons.Outlined.ContentCopy, "نسخ") { clip.setText(AnnotatedString(text)) }
        ActionChip(Icons.Outlined.StopCircle, "إيقاف") { speaker.stop() }
    }
}

@Composable
internal fun ActionChip(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        Modifier.height(40.dp).clip(RoundedCornerShape(20.dp)).background(AppColors.Surface).clickable(onClick = onClick).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = AppColors.Primary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = AppColors.Primary)
    }
}

/** تعاريف وأجوبة: تبويبان */
@Composable
private fun SciencePageBody(text: String) {
    val defs = text.substringAfter(Prompts.DEFS, "").substringBefore(Prompts.ANSWERS).trim()
    val answers = text.substringAfter(Prompts.ANSWERS, "").trim()
    if (defs.isEmpty() && answers.isEmpty()) { TextCard(text); return }
    var tab by remember { mutableIntStateOf(0) }
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color2).padding(4.dp)) {
        listOf("التعاريف", "الأجوبة").forEachIndexed { i, t ->
            Box(
                Modifier.weight(1f).height(38.dp).clip(RoundedCornerShape(11.dp))
                    .background(if (tab == i) AppColors.Surface else Color2).clickable { tab = i },
                contentAlignment = Alignment.Center,
            ) { Text(t, style = MaterialTheme.typography.bodyMedium, color = if (tab == i) AppColors.Primary else AppColors.Muted) }
        }
    }
    Spacer(Modifier.height(12.dp))
    val shown = if (tab == 0) defs else answers
    (shown.ifBlank { "لا يوجد" }).lines().filter { it.isNotBlank() }.forEach { line ->
        AppCard(Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
            Text(line.trim(), style = MaterialTheme.typography.bodyMedium, color = AppColors.Ink, modifier = Modifier.padding(14.dp))
        }
    }
}

private val Color2 = androidx.compose.ui.graphics.Color(0xFFEAECF5)

/** ترجمة مع النطق: JSON من الموديل → بطاقات كلمات + قراءة */
@Composable
private fun PronounceBody(raw: String, speaker: Speaker) {
    val json = remember(raw) {
        runCatching { JSONObject(raw.substring(raw.indexOf('{'), raw.lastIndexOf('}') + 1)) }.getOrNull()
    }
    if (json == null) { TextCard(raw); return }
    val english = json.optString("english")
    val arabic = json.optString("arabic")
    val pron = json.optString("pronunciation")
    val words = json.optJSONArray("words")

    AppCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("English", style = MaterialTheme.typography.labelMedium, color = AppColors.Faint)
            Spacer(Modifier.height(6.dp))
            Text(english, style = MaterialTheme.typography.titleMedium, color = AppColors.Ink, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = AppColors.Line)
            Spacer(Modifier.height(10.dp))
            Text(arabic, style = MaterialTheme.typography.bodyLarge, color = AppColors.Primary)
        }
    }
    Spacer(Modifier.height(14.dp))
    Text("اللفظ بحروف عربية", style = MaterialTheme.typography.titleMedium, color = AppColors.Ink)
    Spacer(Modifier.height(8.dp))
    TextCard(pron, highlight = true)
    Spacer(Modifier.height(10.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        ActionChip(Icons.Outlined.VolumeUp, "اسمع بالإنكليزي") { speaker.speak(english, speaker.english, rate = 0.85f) }
        ActionChip(Icons.Outlined.Mic, "اقرأ وياي") { speaker.speak(english, speaker.english, rate = 0.5f) }
        ActionChip(Icons.Outlined.Translate, "عربي") { speaker.speak(arabic, speaker.arabic) }
    }
    if (words != null && words.length() > 0) {
        Spacer(Modifier.height(16.dp))
        Text("اضغط على أي كلمة حتى تسمعها", style = MaterialTheme.typography.labelMedium, color = AppColors.Muted)
        Spacer(Modifier.height(8.dp))
        val items = (0 until words.length()).map { words.getJSONObject(it) }
        items.chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { w ->
                    AppCard(Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).clickable { speaker.speak(w.optString("en"), speaker.english, rate = 0.8f) }) {
                        Column(Modifier.padding(10.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(w.optString("en"), style = MaterialTheme.typography.labelMedium, color = AppColors.Muted)
                            Text(w.optString("say"), style = MaterialTheme.typography.titleMedium, color = AppColors.Primary)
                            Text(w.optString("ar"), style = MaterialTheme.typography.labelSmall, color = AppColors.Muted)
                        }
                    }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}
