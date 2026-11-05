package iq.student.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import iq.student.data.Catalog
import iq.student.data.Prefs
import iq.student.data.Section
import iq.student.ui.components.ScreenHeader
import iq.student.ui.components.SectionCard
import iq.student.ui.theme.AppColors
import iq.student.ui.theme.dimens
import iq.student.util.ScanSession

@Composable
fun SubjectScreen(subjectId: String, onBack: () -> Unit, onSection: (Section) -> Unit) {
    val subject = Catalog.subject(Prefs.gradeId, Prefs.branch, subjectId)
    Column(Modifier.fillMaxSize().background(AppColors.Bg)) {
        ScreenHeader(subject?.title ?: "المادة", onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = dimens.pad).padding(top = 12.dp, bottom = 24.dp)) {
            Text("شنو تريد تسوي؟", style = MaterialTheme.typography.titleLarge, color = AppColors.Ink)
            Spacer(Modifier.height(14.dp))
            subject?.let { s ->
                Catalog.sectionsFor(s.kind).forEach { sec ->
                    SectionCard(sec.icon, sec.title, sec.sub, sec.tone, action = if (sec.typed) "اكتب" else "امسح") {
                        ScanSession.subjectId = s.id
                        onSection(sec)
                    }
                    Spacer(Modifier.height(14.dp))
                }
            }
        }
    }
}
