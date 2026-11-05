package iq.student.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import iq.student.ui.components.ScreenHeader
import iq.student.ui.theme.AppColors
import iq.student.ui.theme.dimens

/** شاشة مؤقتة للأقسام الي لسه قيد التطوير (مثل تقدّمي) */
@Composable
fun ToolScreen(name: String, onBack: () -> Unit) {
    val title = if (name == "progress") "تقدمي" else "قريباً"
    Column(Modifier.fillMaxSize().background(AppColors.Bg)) {
        ScreenHeader(title, onBack)
        Column(
            Modifier.fillMaxSize().padding(dimens.pad),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
        ) {
            Text("قيد التطوير", style = MaterialTheme.typography.titleLarge, color = AppColors.Ink)
            Spacer(Modifier.height(6.dp))
            Text("هذا القسم راح ينفتح بالتحديثات الجاية", style = MaterialTheme.typography.bodyMedium, color = AppColors.Muted, textAlign = TextAlign.Center)
        }
    }
}
