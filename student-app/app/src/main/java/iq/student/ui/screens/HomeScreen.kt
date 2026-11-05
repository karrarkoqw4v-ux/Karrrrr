package iq.student.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import iq.student.ui.components.*
import iq.student.ui.theme.AppColors
import iq.student.ui.theme.Tone
import iq.student.ui.theme.dimens

data class Tool(val id: String, val icon: ImageVector, val title: String, val tone: Tone)

val Tools = listOf(
    Tool("translate", Icons.Outlined.Translate, "الترجمة", Tone.Teal),
    Tool("assistant", Icons.Outlined.ChatBubbleOutline, "المساعد", Tone.Violet),
    Tool("summary", Icons.Outlined.Description, "التلخيص", Tone.Orange),
    Tool("ocr", Icons.Outlined.DocumentScanner, "قراءة صور", Tone.Cyan),
    Tool("exams", Icons.Outlined.Quiz, "وزاري", Tone.Rose),
    Tool("cards", Icons.Outlined.Style, "بطاقات", Tone.Amber),
    Tool("timer", Icons.Outlined.Timer, "المؤقت", Tone.Green),
    Tool("gpa", Icons.Outlined.Calculate, "المعدل", Tone.Blue),
)

@Composable
fun HomeScreen(onTool: (String) -> Unit, onTab: (Int) -> Unit) {
    Column(Modifier.fillMaxSize().background(AppColors.Bg)) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).statusBarsPadding()
                .padding(horizontal = dimens.pad).padding(top = 14.dp),
        ) {
            // الترويسة
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(44.dp).clip(CircleShape).background(AppColors.PrimarySoft), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Person, null, tint = AppColors.Primary)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("هلا بيك", style = MaterialTheme.typography.titleLarge, color = AppColors.Ink)
                    Text("السادس الإعدادي - العلمي", style = MaterialTheme.typography.labelMedium, color = AppColors.Muted)
                }
                Box(Modifier.size(dimens.touchMin).clip(CircleShape).background(AppColors.Surface)
                    .border(1.dp, AppColors.Line, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.NotificationsNone, "الإشعارات")
                }
            }
            Spacer(Modifier.height(16.dp))
            // البحث
            Row(
                Modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(16.dp)).background(AppColors.Surface)
                    .border(1.dp, AppColors.Line, RoundedCornerShape(16.dp)).padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Search, null, tint = AppColors.Faint)
                Spacer(Modifier.width(10.dp))
                Text("دوّر عن درس أو سؤال أو أداة", style = MaterialTheme.typography.bodyMedium, color = AppColors.Faint)
            }
            Spacer(Modifier.height(16.dp))
            // بطاقة حل المسائل
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(AppColors.Primary).padding(20.dp),
            ) {
                Text("حلّ أي مسألة بالصورة", style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp), color = Color.White)
                Spacer(Modifier.height(4.dp))
                Text("صوّرها ونشرح لك الحل خطوة بخطوة", style = MaterialTheme.typography.labelMedium, color = Color(0xFFDCE5FF))
                Spacer(Modifier.height(14.dp))
                Row(
                    Modifier.height(dimens.touchMin).clip(RoundedCornerShape(16.dp)).background(Color.White)
                        .clickable { onTool("solver") }.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.PhotoCamera, null, tint = AppColors.Primary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("صوّر الآن", style = MaterialTheme.typography.bodyLarge, color = AppColors.Primary)
                }
            }
            Spacer(Modifier.height(22.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("أدوات الدراسة", style = MaterialTheme.typography.titleMedium, color = AppColors.Ink, modifier = Modifier.weight(1f))
                Text("عرض الكل", style = MaterialTheme.typography.labelMedium, color = AppColors.Primary,
                    modifier = Modifier.clickable { onTab(1) })
            }
            Spacer(Modifier.height(14.dp))
            // شبكة 4 أعمدة
            Tools.chunked(4).forEach { row ->
                Row(Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                    row.forEach { t ->
                        Column(
                            Modifier.weight(1f).clickable { onTool(t.id) },
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            IconTile(t.icon, t.tone)
                            Spacer(Modifier.height(6.dp))
                            Text(t.title, style = MaterialTheme.typography.labelMedium, color = AppColors.Ink, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
            // يومك
            AppCard(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconTile(Icons.Outlined.LocalFireDepartment, Tone.Orange, size = 48.dp)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("7 أيام متتالية", style = MaterialTheme.typography.titleMedium, color = AppColors.Ink)
                        Text("هدف اليوم: 45 من 60 دقيقة", style = MaterialTheme.typography.labelMedium, color = AppColors.Muted)
                        Spacer(Modifier.height(8.dp))
                        Box(Modifier.fillMaxWidth().height(7.dp).clip(CircleShape).background(AppColors.Line)) {
                            Box(Modifier.fillMaxWidth(0.75f).fillMaxHeight().clip(CircleShape).background(AppColors.Primary))
                        }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }
        AppBottomBar(selected = 0, onSelect = onTab)
    }
}

