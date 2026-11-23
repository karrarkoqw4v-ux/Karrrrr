package iq.suls

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object Suls {
    val Bg = Color(0xFF0E1512)
    val Card = Color(0xFF162420)
    val Accent = Color(0xFF16A085)
    val AccentSoft = Color(0xFF1E8C73)
    val Warn = Color(0xFFE2A33B)
    val Danger = Color(0xFFE2593B)
    val TextPrimary = Color(0xFFEFFBF7)
    val TextSecondary = Color(0xFF8FA69D)
}

@Composable
fun SulsCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Suls.Card, RoundedCornerShape(24.dp))
            .padding(20.dp),
        content = content
    )
}
