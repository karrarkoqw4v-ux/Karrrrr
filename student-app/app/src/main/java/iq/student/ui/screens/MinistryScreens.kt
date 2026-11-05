package iq.student.ui.screens

import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import iq.student.data.Catalog
import iq.student.data.Handout
import iq.student.data.MQuestion
import iq.student.data.Ministry
import iq.student.data.Handouts
import iq.student.ui.components.*
import iq.student.ui.theme.AppColors
import iq.student.ui.theme.Tone
import iq.student.ui.theme.dimens

private val Star = Color(0xFFF59E0B)

@Composable
private fun Stars(rating: Int) {
    Row {
        repeat(5) { i ->
            Icon(Icons.Outlined.Star, null, tint = if (i < rating) Star else AppColors.Line, modifier = Modifier.size(16.dp))
        }
    }
}

private fun toneFor(subject: String) = when {
    subject.contains("رياض") -> Tone.Blue
    subject.contains("إنكليز") -> Tone.Teal
    subject.contains("كيمي") -> Tone.Green
    subject.contains("فيزي") -> Tone.Violet
    subject.contains("أحياء") -> Tone.Orange
    else -> Tone.Rose
}

/** الوزاريات (للثالث المتوسط): تبويب الأسئلة (مادة ثم سنة ودور) وتبويب الملازم */
@Composable
fun MinistryScreen(onBack: () -> Unit, onSubject: (String) -> Unit, onOpenHandout: (String) -> Unit, onSite: () -> Unit) {
    val ctx = LocalContext.current
    val all = remember { Handouts.load(ctx) }
    var tab by remember { mutableIntStateOf(0) }
    val subjects = Catalog.subjectsFor("m3", null)

    Column(Modifier.fillMaxSize().background(AppColors.Bg)) {
        ScreenHeader("الوزاريات", onBack)
        Row(
            Modifier.padding(horizontal = dimens.pad).fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color(0xFFEAECF5)).padding(4.dp),
        ) {
            listOf("الأسئلة الوزارية", "الملازم").forEachIndexed { i, t ->
                Box(
                    Modifier.weight(1f).height(40.dp).bounceClick { tab = i }.clip(RoundedCornerShape(11.dp))
                        .background(androidx.compose.animation.animateColorAsState(if (tab == i) AppColors.Surface else Color.Transparent, label = "tab").value),
                    contentAlignment = Alignment.Center,
                ) { Text(t, style = MaterialTheme.typography.bodyMedium, color = if (tab == i) AppColors.Primary else AppColors.Muted) }
            }
        }
        Spacer(Modifier.height(12.dp))
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = dimens.pad)) {
            if (tab == 0) {
                Text("أسئلة الثالث المتوسط من 2011 إلى 2026 (الدور الأول والثاني والثالث) مع الأجوبة", style = MaterialTheme.typography.labelMedium, color = AppColors.Muted)
                Spacer(Modifier.height(12.dp))
                subjects.chunked(2).forEachIndexed { r, row ->
                    Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        row.forEachIndexed { c, s ->
                            Entrance(r * 2 + c, Modifier.weight(1f)) {
                                SubjectCard(s.icon, s.title, "2011 - 2026", s.tone, Modifier.fillMaxWidth()) { onSubject(s.id) }
                            }
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            } else {
                Entrance(0) {
                    SectionCard(Icons.Outlined.Public, "موقع الملازم", "تصفح ملازم أفضل المدرسين داخل التطبيق، وأي ملزمة PDF تنفتح بقارئ يكبّر ويصغّر", Tone.Blue, action = "فتح الموقع", onClick = onSite)
                }
                Spacer(Modifier.height(16.dp))
                if (all.isNotEmpty()) {
                    Text("ملازم مضافة", style = MaterialTheme.typography.titleMedium, color = AppColors.Ink)
                    Spacer(Modifier.height(10.dp))
                    if (all.any { it.sample }) {
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color(0xFFFFF4D6)).padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Outlined.Lock, null, tint = Color(0xFFB7791F), modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("هذي نماذج للتصميم، الملازم الحقيقية تنضاف لاحقاً", style = MaterialTheme.typography.labelMedium, color = Color(0xFF92400E))
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                    all.forEachIndexed { i, h -> Entrance(i) { HandoutCard(h) { onOpenHandout(h.id) } }; Spacer(Modifier.height(12.dp)) }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

/** اختيار السنة والدور وعرض أسئلتهما */
@Composable
fun MinistrySubjectScreen(subjectId: String, onBack: () -> Unit, onQuestion: (year: Int, round: Int, index: Int) -> Unit) {
    val ctx = LocalContext.current
    val subject = Catalog.subject("m3", null, subjectId)
    var year by remember { mutableIntStateOf(Ministry.years.first()) }
    var round by remember { mutableIntStateOf(1) }
    var items by remember { mutableStateOf<List<MQuestion>?>(null) }

    LaunchedEffect(year, round) {
        items = null
        items = Ministry.load(ctx, subjectId, year, round)
    }

    Column(Modifier.fillMaxSize().background(AppColors.Bg)) {
        ScreenHeader(subject?.title ?: "الوزاري", onBack)
        Text("السنة", style = MaterialTheme.typography.labelMedium, color = AppColors.Muted, modifier = Modifier.padding(horizontal = dimens.pad))
        Spacer(Modifier.height(6.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = dimens.pad), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Ministry.years.forEach { y -> AppChip("$y", y == year) { year = y } }
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.padding(horizontal = dimens.pad).fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color(0xFFEAECF5)).padding(4.dp)) {
            Ministry.rounds.forEach { (n, label) ->
                Box(
                    Modifier.weight(1f).height(38.dp).bounceClick { round = n }.clip(RoundedCornerShape(11.dp))
                        .background(androidx.compose.animation.animateColorAsState(if (round == n) AppColors.Surface else Color.Transparent, label = "round").value),
                    contentAlignment = Alignment.Center,
                ) { Text(label, style = MaterialTheme.typography.labelMedium, color = if (round == n) AppColors.Primary else AppColors.Muted) }
            }
        }
        Spacer(Modifier.height(12.dp))
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = dimens.pad)) {
            val list = items
            when {
                list == null -> LoadingBox("نجلب الأسئلة...")
                list.isEmpty() -> {
                    ErrorBox("ما في أسئلة مضافة لـ ${subject?.title.orEmpty()} $year (${Ministry.rounds[round - 1].second}) بعد")
                    Spacer(Modifier.height(8.dp))
                    Text("تنضاف الأسئلة كملفات JSON داخل التطبيق أو من رابط بنك الأسئلة بالإعدادات.", style = MaterialTheme.typography.labelSmall, color = AppColors.Muted)
                }
                else -> list.forEachIndexed { i, q ->
                    Entrance(i) {
                        AppCard(Modifier.fillMaxWidth().bounceClick { onQuestion(year, round, i) }) {
                            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(AppColors.PrimarySoft), contentAlignment = Alignment.Center) {
                                    Text("${i + 1}", style = MaterialTheme.typography.bodyLarge, color = AppColors.Primary)
                                }
                                Spacer(Modifier.width(12.dp))
                                Text(q.q, style = MaterialTheme.typography.bodyMedium, color = AppColors.Ink, maxLines = 3, modifier = Modifier.weight(1f))
                                Icon(Icons.Outlined.ChevronLeft, null, tint = AppColors.Faint)
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

/** صفحة السؤال: السؤال فوق والجواب تحته، مع التنقل بين الأسئلة */
@Composable
fun MinistryQaScreen(subjectId: String, year: Int, round: Int, startIndex: Int, onBack: () -> Unit) {
    val ctx = LocalContext.current
    var items by remember { mutableStateOf<List<MQuestion>>(emptyList()) }
    var index by remember { mutableIntStateOf(startIndex) }
    LaunchedEffect(Unit) { items = Ministry.load(ctx, subjectId, year, round) }
    val subject = Catalog.subject("m3", null, subjectId)

    Column(Modifier.fillMaxSize().background(AppColors.Bg)) {
        ScreenHeader("${subject?.title.orEmpty()} $year", onBack)
        val q = items.getOrNull(index)
        if (q == null) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { LoadingBox("...") }; return@Column }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = dimens.pad)) {
            Text("${Ministry.rounds[round - 1].second} - سؤال ${index + 1} من ${items.size}", style = MaterialTheme.typography.labelMedium, color = AppColors.Muted)
            Spacer(Modifier.height(10.dp))
            androidx.compose.animation.AnimatedContent(targetState = index, label = "qa", transitionSpec = {
                (androidx.compose.animation.slideInHorizontally { -it / 3 } + androidx.compose.animation.fadeIn()) togetherWith
                    (androidx.compose.animation.slideOutHorizontally { it / 3 } + androidx.compose.animation.fadeOut())
            }) { i ->
                val cur = items.getOrNull(i) ?: q
                Column {
                    AppCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text("السؤال", style = MaterialTheme.typography.labelMedium, color = AppColors.Primary)
                            Spacer(Modifier.height(6.dp))
                            Text(cur.q, style = MaterialTheme.typography.bodyLarge, color = AppColors.Ink)
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(AppColors.SuccessSoft).padding(16.dp)) {
                        Text("الجواب", style = MaterialTheme.typography.labelMedium, color = Color(0xFF166534))
                        Spacer(Modifier.height(6.dp))
                        Text(cur.a.ifBlank { "الجواب غير مضاف" }, style = MaterialTheme.typography.bodyLarge, color = Color(0xFF14532D))
                    }
                }
            }
        }
        Row(Modifier.padding(horizontal = dimens.pad).padding(bottom = 16.dp, top = 8.dp).navigationBarsPadding(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AppButton("السابق", { if (index > 0) index-- }, Modifier.weight(1f), icon = Icons.Outlined.ChevronRight, outlined = true)
            AppButton("التالي", { if (index < items.size - 1) index++ }, Modifier.weight(1f), icon = Icons.Outlined.ChevronLeft)
        }
    }
}

@Composable
private fun HandoutCard(h: Handout, onClick: () -> Unit) {
    val tone = toneFor(h.subject)
    AppCard(Modifier.fillMaxWidth().bounceClick(onClick = onClick)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(width = 84.dp, height = 112.dp).clip(RoundedCornerShape(12.dp)).background(tone.soft), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Description, null, tint = tone.fg, modifier = Modifier.size(34.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(h.title, style = MaterialTheme.typography.bodyLarge, color = AppColors.Ink)
                Text(h.teacher, style = MaterialTheme.typography.labelMedium, color = AppColors.Muted)
                Spacer(Modifier.height(6.dp))
                Stars(h.rating)
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier.height(36.dp).clip(RoundedCornerShape(12.dp)).background(AppColors.Primary).padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.Visibility, null, tint = Color.White, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("مشاهدة", style = MaterialTheme.typography.labelMedium, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun HandoutDetailScreen(id: String, onBack: () -> Unit, onView: (String) -> Unit) {
    val ctx = LocalContext.current
    val h = remember(id) { Handouts.find(ctx, id) }
    var noFile by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(AppColors.Bg)) {
        ScreenHeader("تفاصيل الملزمة", onBack)
        if (h == null) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("الملزمة غير موجودة") }; return@Column }
        val tone = toneFor(h.subject)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = dimens.pad)) {
            Box(Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(22.dp)).background(tone.soft), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.Description, null, tint = tone.fg, modifier = Modifier.size(56.dp))
                    Spacer(Modifier.height(8.dp))
                    Text(h.subject, style = MaterialTheme.typography.titleMedium, color = tone.fg)
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(h.title, style = MaterialTheme.typography.titleLarge, color = AppColors.Ink)
            Spacer(Modifier.height(6.dp))
            Stars(h.rating)
            Spacer(Modifier.height(14.dp))
            listOf(
                Triple(Icons.Outlined.Person, "المدرس", h.teacher),
                Triple(Icons.Outlined.MenuBook, "المادة", h.subject),
                Triple(Icons.Outlined.Description, "عدد الصفحات", if (h.pages > 0) "${h.pages} صفحة" else "-"),
                Triple(Icons.Outlined.CalendarMonth, "السنة", h.year),
            ).forEach { (ic, k, v) ->
                AppCard(Modifier.fillMaxWidth().padding(bottom = 8.dp), ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(ic, null, tint = AppColors.Primary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(k, style = MaterialTheme.typography.bodyMedium, color = AppColors.Muted, modifier = Modifier.weight(1f))
                        Text(v, style = MaterialTheme.typography.bodyLarge, color = AppColors.Ink)
                    }
                }
            }
            if (noFile) { Spacer(Modifier.height(8.dp)); ErrorBox("ملف هذي الملزمة ما انضاف للحين") }
        }
        Box(Modifier.padding(horizontal = dimens.pad).padding(bottom = 16.dp, top = 8.dp).navigationBarsPadding()) {
            AppButton("مشاهدة الملزمة", {
                if (h.pdfUrl.isBlank()) noFile = true else onView(h.pdfUrl)
            }, icon = Icons.Outlined.Visibility)
        }
    }
}
