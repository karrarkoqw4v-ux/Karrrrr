package iq.student.ui.screens

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
import iq.student.data.Handout
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

/** الوزاريات (للثالث المتوسط فقط): ملازم أفضل المدرسين */
@Composable
fun MinistryScreen(onBack: () -> Unit, onOpen: (String) -> Unit) {
    val ctx = LocalContext.current
    val all = remember { Handouts.load(ctx) }
    var filter by remember { mutableStateOf("كل المواد") }
    val subjects = listOf("كل المواد") + all.map { it.subject }.distinct()
    val list = all.filter { filter == "كل المواد" || it.subject == filter }

    Column(Modifier.fillMaxSize().background(AppColors.Bg)) {
        ScreenHeader("الوزاريات", onBack)
        Row(
            Modifier.padding(horizontal = dimens.pad).fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(AppColors.PrimarySoft).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.WorkspacePremium, null, tint = AppColors.Primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text("ملازم أفضل مدرسين العراق - الثالث المتوسط", style = MaterialTheme.typography.labelMedium, color = AppColors.Primary)
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = dimens.pad), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            subjects.forEach { s -> AppChip(s, s == filter) { filter = s } }
        }
        Spacer(Modifier.height(12.dp))
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = dimens.pad)) {
            if (all.any { it.sample }) {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color(0xFFFFF4D6)).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.Lock, null, tint = Color(0xFFB7791F), modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("إضافة الملازم الحقيقية قيد التطوير، هذي نماذج للتصميم فقط", style = MaterialTheme.typography.labelMedium, color = Color(0xFF92400E))
                }
                Spacer(Modifier.height(12.dp))
            }
            list.forEach { h -> HandoutCard(h) { onOpen(h.id) }; Spacer(Modifier.height(12.dp)) }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun HandoutCard(h: Handout, onClick: () -> Unit) {
    val tone = toneFor(h.subject)
    AppCard(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).clickable(onClick = onClick)) {
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
