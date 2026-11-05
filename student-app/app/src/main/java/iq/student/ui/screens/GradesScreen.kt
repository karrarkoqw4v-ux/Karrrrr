package iq.student.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import iq.student.data.Catalog
import iq.student.data.Grade
import iq.student.data.Prefs
import iq.student.data.Stage
import iq.student.ui.components.*
import iq.student.ui.theme.AppColors
import iq.student.ui.theme.Tone
import iq.student.ui.theme.dimens

@Composable
fun GradesScreen(onBack: (() -> Unit)?, onDone: () -> Unit) {
    var selected by remember { mutableStateOf(Prefs.gradeId ?: "m3") }
    var branch by remember { mutableStateOf(Prefs.branch) }
    var dev by remember { mutableStateOf(false) }
    val grade = Catalog.grade(selected)

    if (dev) DevDialog(onDismiss = { dev = false })

    Column(Modifier.fillMaxSize().background(AppColors.Surface)) {
        ScreenHeader("اختر صفك", onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = dimens.pad)) {
            Text("المنهج يتخصص حسب صفك", style = MaterialTheme.typography.bodyMedium, color = AppColors.Muted)
            Spacer(Modifier.height(16.dp))

            // الابتدائية مقفلة
            Surface(
                shape = RoundedCornerShape(18.dp), color = Color(0xFFF2F4F7), border = BorderStroke(1.dp, AppColors.Line),
                modifier = Modifier.fillMaxWidth().clickable { dev = true },
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconTile(Icons.Outlined.Edit, Tone.Orange, size = 46.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("المرحلة الابتدائية", style = MaterialTheme.typography.bodyLarge, color = AppColors.Faint)
                        Text("من الأول إلى السادس", style = MaterialTheme.typography.labelMedium, color = AppColors.Faint)
                    }
                    Icon(Icons.Outlined.Lock, null, tint = Color(0xFFB7791F), modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("قيد التطوير", style = MaterialTheme.typography.labelSmall, color = Color(0xFF92400E))
                }
            }
            Spacer(Modifier.height(22.dp))
            GradeRow("المرحلة المتوسطة", Catalog.grades.filter { it.stage == Stage.Middle }, selected, { selected = it.id }) { dev = true }
            Spacer(Modifier.height(20.dp))
            GradeRow("المرحلة الإعدادية", Catalog.grades.filter { it.stage == Stage.Prep }, selected, { selected = it.id }) { dev = true }

            if (grade?.stage == Stage.Prep) {
                Spacer(Modifier.height(20.dp))
                Text("الفرع", style = MaterialTheme.typography.labelMedium, color = AppColors.Muted)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppChip("علمي", branch == "sci") { branch = "sci" }
                    AppChip("أدبي", branch == "lit") { branch = "lit" }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
        Box(Modifier.padding(horizontal = dimens.pad).padding(top = 8.dp, bottom = 16.dp).navigationBarsPadding()) {
            AppButton("متابعة", {
                Prefs.gradeId = selected
                Prefs.branch = branch
                onDone()
            }, icon = Icons.AutoMirrored.Outlined.ArrowForward)
        }
    }
}


@Composable
private fun GradeRow(title: String, grades: List<Grade>, selected: String, onPick: (Grade) -> Unit, onLocked: () -> Unit) {
    Text(title, style = MaterialTheme.typography.titleMedium, color = AppColors.Ink)
    Spacer(Modifier.height(10.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        grades.forEach { g ->
            val on = g.id == selected && g.enabled
            val bg = when { !g.enabled -> Color(0xFFF2F4F7); on -> AppColors.PrimarySoft; else -> AppColors.Surface }
            Surface(
                shape = RoundedCornerShape(16.dp), color = bg,
                border = BorderStroke(if (on) 2.dp else 1.dp, if (on) AppColors.Primary else AppColors.Line),
                modifier = Modifier.weight(1f).height(68.dp).clickable { if (g.enabled) onPick(g) else onLocked() },
            ) {
                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    val short = g.label.substringBefore(" ")
                    Text(short, style = MaterialTheme.typography.bodyLarge, color = if (!g.enabled) AppColors.Faint else if (on) AppColors.Primary else AppColors.Ink)
                    if (!g.enabled) Icon(Icons.Outlined.Lock, null, tint = AppColors.Faint, modifier = Modifier.size(16.dp))
                    else Text(g.label.substringAfter(" "), style = MaterialTheme.typography.labelMedium, color = if (on) AppColors.Primary else AppColors.Muted)
                }
            }
        }
    }
}
