package iq.student.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import iq.student.ui.components.*
import iq.student.ui.theme.AppColors
import iq.student.ui.theme.Tone
import iq.student.ui.theme.dimens

private data class Stage(val icon: ImageVector, val title: String, val sub: String, val tone: Tone)

private val Stages = listOf(
    Stage(Icons.Outlined.Edit, "ابتدائي", "من الأول إلى السادس الابتدائي", Tone.Orange),
    Stage(Icons.Outlined.MenuBook, "متوسط", "من الأول إلى الثالث المتوسط", Tone.Teal),
    Stage(Icons.Outlined.Science, "إعدادي", "من الرابع إلى السادس الإعدادي", Tone.Blue),
    Stage(Icons.Outlined.School, "جامعي", "كل الكليات والاختصاصات", Tone.Violet),
)

@Composable
fun StageScreen(onBack: () -> Unit, onContinue: () -> Unit) {
    var selected by remember { mutableIntStateOf(2) }
    var branch by remember { mutableIntStateOf(0) }

    Column(Modifier.fillMaxSize().background(AppColors.Surface)) {
        ScreenHeader("إنشاء حساب", onBack)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = dimens.pad),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 16.dp)) {
                repeat(3) { i ->
                    Box(Modifier.width(28.dp).height(4.dp).clip(RoundedCornerShape(2.dp))
                        .background(if (i < 2) AppColors.Primary else AppColors.Line))
                }
            }
            Spacer(Modifier.height(18.dp))
            Text("شنو مرحلتك الدراسية؟", style = MaterialTheme.typography.displaySmall, color = AppColors.Ink)
            Spacer(Modifier.height(6.dp))
            Text("نرتّب لك المواد والأدوات حسب منهج وزارة التربية", style = MaterialTheme.typography.bodyMedium,
                color = AppColors.Muted)
            Spacer(Modifier.height(20.dp))
            Stages.forEachIndexed { i, s ->
                val on = i == selected
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = if (on) AppColors.PrimarySoft else AppColors.Surface,
                    border = BorderStroke(if (on) 2.dp else 1.dp, if (on) AppColors.Primary else AppColors.Line),
                    modifier = Modifier.fillMaxWidth().clickable { selected = i },
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconTile(s.icon, s.tone, size = 52.dp)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(s.title, style = MaterialTheme.typography.titleMedium, color = AppColors.Ink)
                            Text(s.sub, style = MaterialTheme.typography.labelMedium, color = AppColors.Muted)
                        }
                        Box(
                            Modifier.size(24.dp).clip(CircleShape)
                                .then(if (on) Modifier.background(AppColors.Primary) else Modifier.border(1.6.dp, AppColors.Line, CircleShape)),
                            contentAlignment = Alignment.Center,
                        ) { if (on) Icon(Icons.Outlined.Check, null, tint = Color.White, modifier = Modifier.size(16.dp)) }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
            Text("الفرع", style = MaterialTheme.typography.labelMedium, color = AppColors.Muted)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("علمي", "أدبي", "مهني").forEachIndexed { i, t -> AppChip(t, i == branch) { branch = i } }
            }
            Spacer(Modifier.height(16.dp))
        }
        Box(Modifier.padding(horizontal = dimens.pad).padding(top = 8.dp, bottom = 16.dp).navigationBarsPadding()) {
            AppButton("متابعة", onContinue, icon = Icons.AutoMirrored.Outlined.ArrowForward)
        }
    }
}
