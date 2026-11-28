package iq.suls

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private fun humanSizeJ(bytes: Long): String {
    if (bytes <= 0) return "0 كيلو"
    val kb = bytes / 1024.0
    return if (kb < 1024) "%.0f كيلو".format(kb) else "%.1f ميغا".format(kb / 1024.0)
}

@Composable
fun JunkScreen(onBack: () -> Unit) {
    var result by remember { mutableStateOf<JunkResult?>(null) }
    var loading by remember { mutableStateOf(true) }
    var tick by remember { mutableStateOf(0) }
    val selected = remember { mutableStateMapOf<String, Boolean>() }

    LaunchedEffect(tick) {
        loading = true
        result = FilesScanner.scanJunk()
        loading = false
    }

    Column(Modifier.fillMaxSize()) {
        TopBarSuls("ملفات غير مرغوبة", onBack)
        if (loading || result == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Suls.Accent)
                    Spacer(Modifier.height(12.dp))
                    Text("جاري فحص الملفات فعلياً...", color = Suls.TextSecondary, fontSize = 13.sp)
                }
            }
        } else {
            val r = result!!
            val all = r.tmpLogFiles + r.oldInstallers + r.duplicateImages
            if (all.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.CheckCircle, null, tint = Suls.Accent, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("ما لقيت شي يستاهل يتحذف، جهازك نظيف", color = Suls.TextPrimary)
                    }
                }
            } else {
                LazyColumn(Modifier.weight(1f).padding(horizontal = 20.dp)) {
                    if (r.tmpLogFiles.isNotEmpty()) junkSection(this, "ملفات مؤقتة وسجلات", r.tmpLogFiles, selected)
                    if (r.oldInstallers.isNotEmpty()) junkSection(this, "حزم تثبيت APK قديمة", r.oldInstallers, selected)
                    if (r.duplicateImages.isNotEmpty()) junkSection(this, "صور مكررة فعلياً (نفس المحتوى)", r.duplicateImages, selected)
                    item { Spacer(Modifier.height(100.dp)) }
                }
                val selFiles = all.filter { selected[it.file.path] == true }
                if (selFiles.isNotEmpty()) {
                    Surface(color = Suls.Card, modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(20.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "${selFiles.size} ملف • ${humanSizeJ(selFiles.sumOf { it.sizeBytes })}",
                                color = Suls.TextPrimary, modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = { FilesScanner.delete(selFiles); selected.clear(); tick++ },
                                colors = ButtonDefaults.buttonColors(containerColor = Suls.Danger)
                            ) { Text("حذف", color = androidx.compose.ui.graphics.Color.White) }
                        }
                    }
                }
            }
        }
    }
}

private fun junkSection(
    scope: androidx.compose.foundation.lazy.LazyListScope,
    title: String,
    files: List<ScannedFile>,
    selected: androidx.compose.runtime.snapshots.SnapshotStateMap<String, Boolean>
) {
    scope.item {
        Text(
            "$title (${files.size})", color = Suls.TextSecondary, fontSize = 14.sp,
            fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 18.dp, bottom = 8.dp)
        )
    }
    scope.items(files, key = { it.file.path }) { sf ->
        FileRow(
            sf,
            checked = selected[sf.file.path] == true,
            onToggle = { selected[sf.file.path] = !(selected[sf.file.path] ?: false) }
        )
        androidx.compose.foundation.layout.Spacer(Modifier.height(10.dp))
    }
}
