package iq.student.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import iq.student.ui.theme.AppColors
import iq.student.ui.theme.Tone
import iq.student.ui.theme.dimens

/** بطاقة مادة بالشاشة الرئيسية (عمودين) */
@Composable
fun SubjectCard(icon: ImageVector, title: String, sub: String, tone: Tone, modifier: Modifier = Modifier, onClick: () -> Unit) {
    AppCard(modifier.bounceClick(onClick = onClick)) {
        Row(Modifier.padding(12.dp).heightIn(min = 62.dp), verticalAlignment = Alignment.CenterVertically) {
            IconTile(icon, tone, size = 46.dp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, color = AppColors.Ink, maxLines = 1)
                Text(sub, style = MaterialTheme.typography.labelSmall, color = AppColors.Muted, maxLines = 2)
            }
        }
    }
}

/** بطاقة قسم كبيرة داخل المادة */
@Composable
fun SectionCard(icon: ImageVector, title: String, sub: String, tone: Tone, action: String = "ابدأ", onClick: () -> Unit) {
    AppCard(Modifier.fillMaxWidth().bounceClick(onClick = onClick)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconTile(icon, tone, size = 56.dp)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, color = AppColors.Ink)
                    Spacer(Modifier.height(4.dp))
                    Text(sub, style = MaterialTheme.typography.labelMedium, color = AppColors.Muted)
                }
            }
            Spacer(Modifier.height(14.dp))
            Box(
                Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(12.dp)).background(tone.soft),
                contentAlignment = Alignment.Center,
            ) { Text(action, style = MaterialTheme.typography.bodyLarge, color = tone.fg) }
        }
    }
}

/** نافذة "قيد التطوير" للمراحل المقفلة */
@Composable
fun DevDialog(onDismiss: () -> Unit, text: String = "هذي المرحلة ما انفتحت للحين،\nراح تنفتح بالتحديثات الجاية.") {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), color = AppColors.Surface) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(60.dp).clip(RoundedCornerShape(20.dp)).background(Color(0xFFFFF4D6)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Lock, null, tint = Color(0xFFB7791F), modifier = Modifier.size(30.dp))
                }
                Spacer(Modifier.height(14.dp))
                Text("قيد التطوير", style = MaterialTheme.typography.titleLarge, color = AppColors.Ink)
                Spacer(Modifier.height(6.dp))
                Text(text, style = MaterialTheme.typography.bodyMedium, color = AppColors.Muted, textAlign = TextAlign.Center)
                Spacer(Modifier.height(18.dp))
                AppButton("تمام", onDismiss, Modifier.height(48.dp))
            }
        }
    }
}

@Composable
fun ErrorBox(message: String) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color(0xFFFDE7EA)).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) { Text(message, style = MaterialTheme.typography.bodyMedium, color = AppColors.Danger) }
}
