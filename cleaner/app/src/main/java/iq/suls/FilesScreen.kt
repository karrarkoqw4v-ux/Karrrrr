package iq.suls

import androidx.compose.foundation.background
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

private fun humanSizeF(bytes: Long): String {
    if (bytes <= 0) return "0 كيلو"
    val kb = bytes / 1024.0
    return if (kb < 1024) "%.0f كيلو".format(kb) else "%.1f ميغا".format(kb / 1024.0)
}

@Composable
fun FilesScreen(onBack: () -> Unit) {
    var buckets by remember { mutableStateOf<Map<TimeBucket, List<ScannedFile>>>(emptyMap()) }
    var loading by remember { mutableStateOf(true) }
    val selected = remember { mutableStateMapOf<String, Boolean>() }
    var tick by remember { mutableStateOf(0) }

    LaunchedEffect(tick) {
        loading = true
        buckets = FilesScanner.scanRecentFiles()
        loading = false
    }

    val selectedFiles = remember(buckets, selected.toMap()) {
        buckets.values.flatten().filter { selected[it.file.path] == true }
    }
    val selectedSize = selectedFiles.sumOf { it.sizeBytes }

    Column(Modifier.fillMaxSize()) {
        TopBarSuls("الملفات الأخيرة", onBack)
        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Suls.Accent)
            }
        } else {
            LazyColumn(Modifier.weight(1f).padding(horizontal = 20.dp)) {
                for (bucket in TimeBucket.values()) {
                    val list = buckets[bucket].orEmpty()
                    if (list.isEmpty()) continue
                    item {
                        Text(
                            bucket.label, color = Suls.TextSecondary, fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 18.dp, bottom = 8.dp)
                        )
                    }
                    items(list, key = { it.file.path }) { sf ->
                        FileRow(
                            sf,
                            checked = selected[sf.file.path] == true,
                            onToggle = { selected[sf.file.path] = !(selected[sf.file.path] ?: false) }
                        )
                        Spacer(Modifier.height(10.dp))
                    }
                }
                item { Spacer(Modifier.height(100.dp)) }
            }
        }
        if (selectedFiles.isNotEmpty()) {
            Surface(color = Suls.Card, modifier = Modifier.fillMaxWidth()) {
                Row(
                    Modifier.padding(20.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${selectedFiles.size} ملف • ${humanSizeF(selectedSize)}",
                        color = Suls.TextPrimary, modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            FilesScanner.delete(selectedFiles)
                            selected.clear()
                            tick++
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Suls.Danger)
                    ) { Text("حذف", color = androidx.compose.ui.graphics.Color.White) }
                }
            }
        }
    }
}

@Composable
fun FileRow(sf: ScannedFile, checked: Boolean, onToggle: () -> Unit) {
    Surface(
        onClick = onToggle,
        color = Suls.Card,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = checked, onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(checkedColor = Suls.Accent)
            )
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Default.InsertDriveFile, null, tint = Suls.TextSecondary)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(sf.name, color = Suls.TextPrimary, fontSize = 14.sp, maxLines = 1)
                Text(humanSizeF(sf.sizeBytes), color = Suls.TextSecondary, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun TopBarSuls(title: String, onBack: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) { Icon(Icons.Default.ArrowForward, null, tint = Suls.TextPrimary) }
        Spacer(Modifier.width(8.dp))
        Text(title, color = Suls.TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}
