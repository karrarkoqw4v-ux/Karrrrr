package iq.suls

import android.os.Environment
import android.os.StatFs
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private fun humanSize(bytes: Long): String {
    if (bytes <= 0) return "0 ميغا"
    val mb = bytes / (1024.0 * 1024.0)
    return if (mb < 1024) "%.0f ميغا".format(mb) else "%.1f غيغا".format(mb / 1024.0)
}

@Composable
fun HomeScreen(onOpenFiles: () -> Unit, onOpenApps: () -> Unit, onOpenJunk: () -> Unit) {
    val stat = remember {
        val s = StatFs(Environment.getExternalStorageDirectory().path)
        val total = s.totalBytes
        val free = s.availableBytes
        Pair(total, free)
    }
    val usedPercent = remember {
        val (total, free) = stat
        if (total <= 0) 0 else (((total - free).toDouble() / total) * 100).toInt()
    }

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Spacer(Modifier.height(24.dp))
        Text("سلس", color = Suls.TextPrimary, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text("خلي جهازك خفيف وسريع", color = Suls.TextSecondary, fontSize = 14.sp)
        Spacer(Modifier.height(20.dp))

        SulsCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(64.dp).background(Suls.Accent.copy(alpha = .15f), RoundedCornerShape(18.dp)),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Default.Storage, null, tint = Suls.Accent, modifier = Modifier.size(32.dp)) }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("مساحة التخزين المستخدمة", color = Suls.TextSecondary, fontSize = 13.sp)
                    Text("$usedPercent%", color = Suls.TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    Text("متاح: ${humanSize(stat.second)}", color = Suls.TextSecondary, fontSize = 12.sp)
                }
            }
        }

        Spacer(Modifier.height(18.dp))
        HomeTile(Icons.Default.FolderOpen, "الملفات الأخيرة", "حسب آخر يوم / أسبوع / شهر", onOpenFiles)
        Spacer(Modifier.height(14.dp))
        HomeTile(Icons.Default.Apps, "تطبيقات ما تستخدمها", "مرتبة من الأقل استخداماً فعلياً", onOpenApps)
        Spacer(Modifier.height(14.dp))
        HomeTile(Icons.Default.DeleteSweep, "ملفات غير مرغوبة", "مؤقتة، تثبيت قديمة، صور مكررة", onOpenJunk)
    }
}

@Composable
private fun HomeTile(icon: ImageVector, title: String, desc: String, onClick: () -> Unit) {
    androidx.compose.material3.Surface(
        onClick = onClick,
        color = Suls.Card,
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Suls.Accent, modifier = Modifier.size(30.dp))
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = Suls.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Text(desc, color = Suls.TextSecondary, fontSize = 12.sp)
            }
            Icon(Icons.Default.ChevronLeft, null, tint = Suls.TextSecondary)
        }
    }
}
