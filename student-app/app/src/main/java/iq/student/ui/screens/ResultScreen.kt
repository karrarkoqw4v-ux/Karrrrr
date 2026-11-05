package iq.student.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import iq.student.data.StudyTask
import iq.student.offline.MathSolver
import iq.student.offline.Ocr
import iq.student.offline.PageExtract
import iq.student.offline.Pronounce
import iq.student.offline.Translate
import iq.student.ui.components.*
import iq.student.ui.theme.AppColors
import iq.student.ui.theme.dimens
import iq.student.util.ScanSession
import iq.student.util.Speaker
import iq.student.util.rememberSpeaker

private sealed interface UiState {
    data class Loading(val message: String) : UiState
    data class Translated(val original: String, val arabic: String) : UiState
    data class Pronounced(val english: String, val arabic: String, val pron: String, val words: List<Triple<String, String, String>>) : UiState
    data class MathRead(val text: String) : UiState
    data class Page(val text: String, val lines: List<String>) : UiState
    data class Failed(val message: String) : UiState
}

/** شاشة النتيجة: كل المعالجة على الجهاز (قراءة الصورة، الترجمة، النطق، الحل) */
@Composable
fun ResultScreen(task: StudyTask, onBack: () -> Unit, onAgain: () -> Unit) {
    val ctx = LocalContext.current
    val speaker = rememberSpeaker()
    var state by remember { mutableStateOf<UiState>(UiState.Loading("نقرا الصورة...")) }
    var attempt by remember { mutableIntStateOf(0) }
    val image = ScanSession.cropped

    LaunchedEffect(attempt) {
        state = UiState.Loading("نقرا الصورة...")
        val bmp = image
        if (bmp == null) { state = UiState.Failed("ما في صورة، ارجع صوّر مرة ثانية"); return@LaunchedEffect }
        state = try {
            when (task) {
                StudyTask.TranslateText -> {
                    val text = Ocr.english(bmp)
                    if (text.isBlank()) return@LaunchedEffect run { state = UiState.Failed("ما قدرنا نقرأ نص إنكليزي بالصورة. جرّب تقص النص بشكل أدق وبإضاءة أحسن") }
                    state = UiState.Loading("نترجم النص... (أول مرة ننزّل نموذج الترجمة)")
                    UiState.Translated(text, Translate.run(text, true))
                }
                StudyTask.TranslatePronounce -> {
                    val text = Ocr.english(bmp)
                    if (text.isBlank()) return@LaunchedEffect run { state = UiState.Failed("ما قدرنا نقرأ نص إنكليزي بالصورة. جرّب تقص النص بشكل أدق وبإضاءة أحسن") }
                    state = UiState.Loading("نترجم ونجهّز اللفظ...")
                    val arabic = Translate.run(text, true)
                    val tokens = text.split(Regex("\\s+")).map { it.filter { c -> c.isLetter() || c == '\'' } }.filter { it.length > 1 }.distinct().take(36)
                    val words = tokens.map { Triple(it, Pronounce.word(ctx, it), "") }
                    UiState.Pronounced(text, arabic, Pronounce.sentence(ctx, text), words)
                }
                StudyTask.MathSimple, StudyTask.MathExplain, StudyTask.ScienceSolve, StudyTask.MathTyped -> {
                    val text = Ocr.english(bmp)
                    UiState.MathRead(text.lines().firstOrNull { it.any { c -> c.isDigit() } && it.contains("=") } ?: text.lines().firstOrNull { it.any { c -> c.isDigit() } } ?: text)
                }
                else -> {
                    state = UiState.Loading("نقرا الصفحة (العربي أبطأ شوية)...")
                    val raw = Ocr.arabic(ctx, bmp)
                    if (raw.isBlank()) return@LaunchedEffect run { state = UiState.Failed("ما قدرنا نقرأ نص بالصورة. جرّب إضاءة أحسن وقص أدق") }
                    UiState.Page(raw, PageExtract.clean(raw))
                }
            }
        } catch (e: Exception) {
            UiState.Failed(
                if (e.message?.contains("download", true) == true || e.javaClass.simpleName.contains("MlKit", true))
                    "تحتاج إنترنت أول مرة حتى ينزل نموذج الترجمة، وبعدها يشتغل بدونه"
                else "صار خطأ: ${e.message ?: e.javaClass.simpleName}",
            )
        }
    }

    val title = when (task) {
        StudyTask.TranslateText -> "الترجمة"
        StudyTask.TranslatePronounce -> "ترجمة مع النطق"
        StudyTask.MathSimple -> "حل مسألة بالصورة"
        StudyTask.MathExplain -> "حل وشرح المسألة"
        StudyTask.ScienceSolve -> "حل المسألة"
        StudyTask.SciencePage -> "نتيجة مسح الصفحة"
        StudyTask.PageAnswers -> "أسئلة الصفحة"
        else -> "قراءة الصفحة"
    }

    Column(Modifier.fillMaxSize().background(AppColors.Bg)) {
        ScreenHeader(title, onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = dimens.pad).padding(top = 8.dp, bottom = 16.dp)) {
            image?.let {
                Image(
                    it.asImageBitmap(), null, contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth().heightIn(max = 150.dp).clip(RoundedCornerShape(14.dp)).background(AppColors.Surface),
                )
                Spacer(Modifier.height(14.dp))
            }
            when (val s = state) {
                is UiState.Loading -> LoadingBox(s.message)
                is UiState.Failed -> {
                    ErrorBox(s.message)
                    Spacer(Modifier.height(12.dp))
                    AppButton("إعادة المحاولة", { attempt++ }, icon = Icons.Outlined.Refresh)
                }
                is UiState.Translated -> Reveal {
                    SectionLabel("النص الإنكليزي")
                    TextCard(s.original, ltr = true)
                    Spacer(Modifier.height(14.dp))
                    SectionLabel("الترجمة العربية")
                    TextCard(s.arabic, highlight = true)
                    Spacer(Modifier.height(12.dp))
                    Actions(s.arabic, speaker, speakLocaleArabic = true)
                    Spacer(Modifier.height(8.dp))
                    ActionChip(Icons.Outlined.VolumeUp, "اسمع الإنكليزي") { speaker.speak(s.original, speaker.english) }
                }
                is UiState.Pronounced -> Reveal { PronounceBody(s, speaker) }
                is UiState.MathRead -> Reveal {
                    MathBody(initial = s.text, readAloud = task == StudyTask.MathExplain, speaker = speaker, fromPhoto = true)
                }
                is UiState.Page -> Reveal { PageBody(s, task, speaker) }
            }
        }
        Row(Modifier.padding(horizontal = dimens.pad).padding(bottom = 16.dp).navigationBarsPadding()) {
            AppButton("صفحة ثانية", onAgain, icon = Icons.Outlined.PhotoCamera)
        }
    }
}

@Composable
private fun Reveal(content: @Composable ColumnScope.() -> Unit) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    AnimatedVisibility(shown, enter = fadeIn() + slideInVertically { it / 8 }) { Column(content = content) }
}

@Composable
internal fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelMedium, color = AppColors.Muted, modifier = Modifier.padding(bottom = 6.dp))
}

@Composable
internal fun LoadingBox(text: String) {
    AppCard(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(color = AppColors.Primary, strokeWidth = 3.dp, modifier = Modifier.size(26.dp))
            Spacer(Modifier.width(14.dp))
            Text(text, style = MaterialTheme.typography.bodyLarge, color = AppColors.Ink)
        }
    }
}

@Composable
internal fun TextCard(text: String, highlight: Boolean = false, ltr: Boolean = false) {
    CompositionLocalProvider(
        androidx.compose.ui.platform.LocalLayoutDirection provides
            if (ltr) androidx.compose.ui.unit.LayoutDirection.Ltr else androidx.compose.ui.unit.LayoutDirection.Rtl,
    ) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                .background(if (highlight) AppColors.PrimarySoft else AppColors.Surface).padding(16.dp),
        ) {
            text.lines().forEach { line ->
                if (line.isBlank()) Spacer(Modifier.height(6.dp))
                else Text(line.trim(), style = MaterialTheme.typography.bodyLarge, color = AppColors.Ink, modifier = Modifier.padding(vertical = 2.dp))
            }
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
        Modifier.height(40.dp).bounceClick(onClick = onClick).clip(RoundedCornerShape(20.dp)).background(AppColors.Surface).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = AppColors.Primary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = AppColors.Primary)
    }
}

@Composable
private fun PronounceBody(s: UiState.Pronounced, speaker: Speaker) {
    AppCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("English", style = MaterialTheme.typography.labelMedium, color = AppColors.Faint)
            Spacer(Modifier.height(6.dp))
            TextCard(s.english, ltr = true)
            Spacer(Modifier.height(10.dp))
            Text(s.arabic, style = MaterialTheme.typography.bodyLarge, color = AppColors.Primary)
        }
    }
    Spacer(Modifier.height(14.dp))
    Text("اللفظ بحروف عربية", style = MaterialTheme.typography.titleMedium, color = AppColors.Ink)
    Spacer(Modifier.height(8.dp))
    TextCard(s.pron, highlight = true)
    Spacer(Modifier.height(10.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        ActionChip(Icons.Outlined.VolumeUp, "اسمع بالإنكليزي") { speaker.speak(s.english, speaker.english, rate = 0.85f) }
        ActionChip(Icons.Outlined.RecordVoiceOver, "اقرأ وياي") { speaker.speak(s.english, speaker.english, rate = 0.5f) }
        ActionChip(Icons.Outlined.Translate, "عربي") { speaker.speak(s.arabic, speaker.arabic) }
    }
    if (s.words.isNotEmpty()) {
        Spacer(Modifier.height(16.dp))
        Text("اضغط على أي كلمة حتى تسمعها", style = MaterialTheme.typography.labelMedium, color = AppColors.Muted)
        Spacer(Modifier.height(8.dp))
        s.words.chunked(3).forEachIndexed { r, row ->
            Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEachIndexed { i, (en, say, _) ->
                    Entrance(r * 3 + i, Modifier.weight(1f)) {
                        AppCard(Modifier.fillMaxWidth().bounceClick { speaker.speak(en, speaker.english, rate = 0.8f) }) {
                            Column(Modifier.padding(10.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(en, style = MaterialTheme.typography.labelMedium, color = AppColors.Muted)
                                Text(say, style = MaterialTheme.typography.titleMedium, color = AppColors.Primary)
                            }
                        }
                    }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/** حلّال الرياضيات: يقرأ المسألة (من الصورة أو الكتابة) ويحلها بالخطوات محلياً */
@Composable
internal fun MathBody(initial: String, readAloud: Boolean, speaker: Speaker, fromPhoto: Boolean) {
    var text by remember { mutableStateOf(initial) }
    var result by remember { mutableStateOf<MathSolver.Result?>(null) }

    fun solve() {
        val r = MathSolver.safe(text)
        result = r
        if (readAloud && r.steps.isNotEmpty()) {
            speaker.speak("المسألة: $text. " + r.steps.joinToString(". ") + ". " + r.answer, speaker.arabic)
        }
    }

    if (fromPhoto) {
        SectionLabel("هذا الي قريناه من الصورة (عدّله إذا فيه خطأ)")
    }
    OutlinedTextField(
        value = text, onValueChange = { text = it; result = null },
        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        textStyle = MaterialTheme.typography.titleMedium,
        placeholder = { Text("مثال: 3x - 7 = 2x + 5", color = AppColors.Faint) },
        singleLine = false,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AppColors.Primary, unfocusedBorderColor = AppColors.Line,
            focusedContainerColor = AppColors.Surface, unfocusedContainerColor = AppColors.Surface,
        ),
    )
    Spacer(Modifier.height(10.dp))
    // أزرار رموز سريعة للكتابة
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf("x", "²", "=", "(", ")", "+", "-", "×", "÷", "^").forEach { sym ->
            Box(
                Modifier.weight(1f).height(40.dp).bounceClick { text += sym; result = null }.clip(RoundedCornerShape(10.dp)).background(AppColors.Surface),
                contentAlignment = Alignment.Center,
            ) { Text(sym, style = MaterialTheme.typography.titleMedium, color = AppColors.Primary) }
        }
    }
    Spacer(Modifier.height(12.dp))
    AppButton("حل المسألة", { solve() }, icon = Icons.Outlined.Calculate)
    result?.let { r ->
        Spacer(Modifier.height(16.dp))
        if (r.steps.isEmpty()) ErrorBox(r.answer)
        else {
            Text("الحل خطوة خطوة", style = MaterialTheme.typography.titleMedium, color = AppColors.Ink)
            Spacer(Modifier.height(8.dp))
            r.steps.forEachIndexed { i, step ->
                Entrance(i) {
                    AppCard(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(28.dp).clip(androidx.compose.foundation.shape.CircleShape).background(AppColors.PrimarySoft), contentAlignment = Alignment.Center) {
                                Text("${i + 1}", style = MaterialTheme.typography.labelMedium, color = AppColors.Primary)
                            }
                            Spacer(Modifier.width(10.dp))
                            Text(step, style = MaterialTheme.typography.bodyMedium, color = AppColors.Ink)
                        }
                    }
                }
            }
            Entrance(r.steps.size) {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(AppColors.SuccessSoft).padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.CheckCircle, null, tint = AppColors.Success, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("الجواب النهائي", style = MaterialTheme.typography.labelMedium, color = androidx.compose.ui.graphics.Color(0xFF166534))
                        Text(r.answer, style = MaterialTheme.typography.titleLarge, color = androidx.compose.ui.graphics.Color(0xFF166534))
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Actions(r.steps.joinToString("\n") + "\n" + r.answer, speaker, speakLocaleArabic = true)
        }
    }
}

/** نص الصفحة + التعاريف + الأسئلة (استخراج تقريبي بدون ذكاء اصطناعي) */
@Composable
private fun PageBody(s: UiState.Page, task: StudyTask, speaker: Speaker) {
    val defs = remember(s) { PageExtract.definitions(s.lines) }
    val questions = remember(s) { PageExtract.questions(s.lines) }
    val tabs = listOf("النص", "التعاريف", "الأسئلة")
    var tab by remember { mutableIntStateOf(if (task == StudyTask.PageAnswers) 2 else if (task == StudyTask.SciencePage) 1 else 0) }
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(androidx.compose.ui.graphics.Color(0xFFEAECF5)).padding(4.dp)) {
        tabs.forEachIndexed { i, t ->
            Box(
                Modifier.weight(1f).height(38.dp).bounceClick { tab = i }.clip(RoundedCornerShape(11.dp))
                    .background(if (tab == i) AppColors.Surface else androidx.compose.ui.graphics.Color.Transparent),
                contentAlignment = Alignment.Center,
            ) { Text(t, style = MaterialTheme.typography.bodyMedium, color = if (tab == i) AppColors.Primary else AppColors.Muted) }
        }
    }
    Spacer(Modifier.height(12.dp))
    when (tab) {
        0 -> { TextCard(s.text); Spacer(Modifier.height(12.dp)); Actions(s.text, speaker, speakLocaleArabic = true) }
        1 -> ListOrEmpty(defs, "ما لقينا تعاريف واضحة بالصفحة")
        else -> ListOrEmpty(questions, "ما لقينا أسئلة بالصفحة")
    }
    Spacer(Modifier.height(10.dp))
    Text("الاستخراج تقريبي: التطبيق يعرض الي مكتوب بالصفحة ولا يكتب أجوبة من عنده.", style = MaterialTheme.typography.labelSmall, color = AppColors.Faint)
}

@Composable
private fun ListOrEmpty(items: List<String>, empty: String) {
    if (items.isEmpty()) { Text(empty, style = MaterialTheme.typography.bodyMedium, color = AppColors.Muted); return }
    items.forEachIndexed { i, line ->
        Entrance(i) {
            AppCard(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                Text(line, style = MaterialTheme.typography.bodyMedium, color = AppColors.Ink, modifier = Modifier.padding(14.dp))
            }
        }
    }
}
