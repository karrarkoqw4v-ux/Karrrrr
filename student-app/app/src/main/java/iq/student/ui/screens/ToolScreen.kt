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
import iq.student.ui.components.IconTile
import iq.student.ui.components.ScreenHeader
import iq.student.ui.theme.AppColors
import iq.student.ui.theme.Tone
import iq.student.ui.theme.dimens

/** شاشة مؤقتة لكل أداة؛ تُستبدل بتنفيذ الأداة الفعلي حسب التصاميم في design/screens */
@Composable
fun ToolScreen(toolId: String, onBack: () -> Unit) {
    val tool = Tools.firstOrNull { it.id == toolId }
    val title = tool?.title ?: if (toolId == "solver") "حلّال المسائل" else "قريباً"
    Column(Modifier.fillMaxSize().background(AppColors.Bg)) {
        ScreenHeader(title, onBack)
        Column(
            Modifier.fillMaxSize().padding(dimens.pad),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            tool?.let { IconTile(it.icon, it.tone, size = 72.dp) }
            Spacer(Modifier.height(16.dp))
            Text("هذي الأداة قيد البناء", style = MaterialTheme.typography.titleMedium, color = AppColors.Ink)
            Spacer(Modifier.height(6.dp))
            Text("تصميمها جاهز بمجلد design/screens", style = MaterialTheme.typography.bodyMedium,
                color = AppColors.Muted, textAlign = TextAlign.Center)
        }
    }
}
