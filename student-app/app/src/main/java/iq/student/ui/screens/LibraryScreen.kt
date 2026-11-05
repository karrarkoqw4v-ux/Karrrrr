package iq.student.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import iq.student.data.Catalog
import iq.student.data.Prefs
import iq.student.ui.components.*
import iq.student.ui.theme.AppColors
import iq.student.ui.theme.dimens
import iq.student.util.Books
import iq.student.util.PdfDoc
import iq.student.util.bookKey
import kotlinx.coroutines.launch

/** مكتبة الكتب: كل صف وكل مادة لها خانة؛ الطالب يضيف الكتاب مرة وحدة ثم يفتحه داخل التطبيق */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryScreen(onBack: () -> Unit, onOpen: (String) -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var gradeId by remember { mutableStateOf(Prefs.gradeId?.takeIf { Catalog.grade(it)?.enabled == true } ?: "m3") }
    var refresh by remember { mutableIntStateOf(0) }
    var addFor by remember { mutableStateOf<String?>(null) }
    var url by remember { mutableStateOf("") }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val key = addFor
        if (uri != null && key != null) scope.launch {
            runCatching { Books.importFile(ctx, key, uri) }
                .onSuccess { addFor = null; refresh++; Toast.makeText(ctx, "انضاف الكتاب", Toast.LENGTH_SHORT).show() }
                .onFailure { Toast.makeText(ctx, it.message ?: "فشل الإضافة", Toast.LENGTH_SHORT).show() }
        }
    }
    val branchForList = if (Catalog.grade(gradeId)?.id?.startsWith("p") == true) Prefs.branch else null
    val subjects = Catalog.subjectsFor(gradeId, branchForList)

    Column(Modifier.fillMaxSize().background(AppColors.Bg)) {
        ScreenHeader("مكتبة الكتب", onBack)
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = dimens.pad), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Catalog.enabledGrades.forEach { g -> AppChip(g.label.substringBefore(" "), g.id == gradeId) { gradeId = g.id } }
        }
        Text(
            Catalog.grade(gradeId)?.label.orEmpty() + " - اضغط مطولاً على الكتاب لتغييره",
            style = MaterialTheme.typography.labelMedium, color = AppColors.Muted,
            modifier = Modifier.padding(horizontal = dimens.pad, vertical = 10.dp),
        )
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = dimens.pad)) {
            subjects.chunked(3).forEach { row ->
                Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { s ->
                        val key = bookKey(gradeId, s.id)
                        val has = remember(refresh, gradeId) { Books.has(key) }
                        AppCard(
                            Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).combinedClickable(
                                onClick = { if (has) onOpen(key) else addFor = key },
                                onLongClick = { addFor = key },
                            ),
                        ) {
                            Column(Modifier.padding(10.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                                IconTile(s.icon, s.tone, size = 64.dp, modifier = Modifier.fillMaxWidth().height(72.dp))
                                Spacer(Modifier.height(8.dp))
                                Text(s.title, style = MaterialTheme.typography.bodyMedium, color = AppColors.Ink, maxLines = 1)
                                Text(if (has) "افتح" else "أضف الكتاب", style = MaterialTheme.typography.labelSmall,
                                    color = if (has) AppColors.Primary else androidx.compose.ui.graphics.Color(0xFFB7791F))
                            }
                        }
                    }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    addFor?.let { key ->
        AlertDialog(
            onDismissRequest = { addFor = null },
            title = { Text("إضافة كتاب", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column {
                    Text("الكتاب يُحفظ على جهازك وتفتحه بدون نت.", style = MaterialTheme.typography.bodyMedium, color = AppColors.Muted)
                    Spacer(Modifier.height(12.dp))
                    AppButton("رفع ملف PDF من الجهاز", { picker.launch(arrayOf("application/pdf")) }, icon = Icons.Outlined.Description)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = url, onValueChange = { url = it }, singleLine = true,
                        placeholder = { Text("أو الصق رابط PDF مباشر") }, modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (url.startsWith("http")) { Books.saveUrl(key, url); url = ""; addFor = null; refresh++ }
                    else Toast.makeText(ctx, "الرابط لازم يبدأ بـ http", Toast.LENGTH_SHORT).show()
                }) { Text("حفظ الرابط") }
            },
            dismissButton = { TextButton(onClick = { addFor = null }) { Text("إلغاء") } },
        )
    }
}

