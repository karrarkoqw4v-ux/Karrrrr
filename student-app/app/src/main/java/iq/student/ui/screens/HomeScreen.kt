package iq.student.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import iq.student.data.Catalog
import iq.student.data.Prefs
import iq.student.data.SubjectKind
import iq.student.ui.components.*
import iq.student.ui.theme.AppColors
import iq.student.ui.theme.Tone
import iq.student.ui.theme.dimens

@Composable
fun HomeScreen(
    onSubject: (String) -> Unit,
    onMinistry: () -> Unit,
    onLibrary: () -> Unit,
    onChangeGrade: () -> Unit,
    onTab: (Int) -> Unit,
) {
    val grade = Catalog.grade(Prefs.gradeId)
    val subjects = Catalog.subjectsFor(Prefs.gradeId, Prefs.branch)

    Column(Modifier.fillMaxSize().background(AppColors.Bg)) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).statusBarsPadding()
                .padding(horizontal = dimens.pad).padding(top = 14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("هلا بيك ${Prefs.userName}".trim(), style = MaterialTheme.typography.titleLarge, color = AppColors.Ink)
                    Text("اختر المادة الي تريد تدرسها", style = MaterialTheme.typography.labelMedium, color = AppColors.Muted)
                }
                Box(
                    Modifier.height(36.dp).clip(RoundedCornerShape(18.dp)).background(AppColors.PrimarySoft)
                        .clickable(onClick = onChangeGrade).padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center,
                ) { Text(grade?.label ?: "اختر الصف", style = MaterialTheme.typography.labelMedium, color = AppColors.Primary) }
            }
            Spacer(Modifier.height(16.dp))

            data class Entry(val key: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val title: String, val sub: String, val tone: Tone, val action: () -> Unit)
            val entries = buildList {
                subjects.forEach { s ->
                    val n = Catalog.sectionsFor(s.kind).size
                    add(Entry(s.id, s.icon, s.title, if (n == 1) "قسم واحد" else "$n أقسام", s.tone) { onSubject(s.id) })
                }
                if (grade?.id == "m3") add(Entry("ministry", Icons.Outlined.Quiz, "الوزاريات", "ملازم المدرسين", Tone.Rose, onMinistry))
                add(Entry("library", Icons.Outlined.MenuBook, "مكتبة الكتب", "كتب المنهج PDF", Tone.Blue, onLibrary))
            }
            entries.chunked(2).forEach { row ->
                Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { e -> SubjectCard(e.icon, e.title, e.sub, e.tone, Modifier.weight(1f), e.action) }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        AppBottomBar(selected = 0, onSelect = onTab)
    }
}
