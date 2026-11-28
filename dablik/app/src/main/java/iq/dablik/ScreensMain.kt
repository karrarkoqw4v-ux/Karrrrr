package iq.dablik

import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun OneTopBar(title: String, onBack: (() -> Unit)?, actions: @Composable RowScope.() -> Unit = {}) {
    Row(Modifier.fillMaxWidth().statusBarsPadding().height(64.dp).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) Box(Modifier.size(46.dp).waterClick(CircleShape) { onBack() }.glass(CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, "رجوع", tint = OneUi.Ink, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, color = OneUi.Ink, modifier = Modifier.weight(1f))
        actions()
    }
}

@Composable
private fun BigTile(title: String, sub: String, icon: androidx.compose.ui.graphics.vector.ImageVector, accent: Color, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.aspectRatio(0.92f).oneClick { onClick() }.clip(RoundedCornerShape(OneUi.RCard)).background(OneUi.Card).padding(20.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Box(Modifier.size(62.dp).clip(CircleShape).background(accent.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) { Icon(icon, null, tint = accent, modifier = Modifier.size(34.dp)) }
        Column {
            Text(title, style = MaterialTheme.typography.titleLarge, color = OneUi.Ink)
            Text(sub, style = MaterialTheme.typography.labelMedium, color = OneUi.Muted)
        }
    }
}

@Composable
fun HomeScreen(onApk: () -> Unit, onZip: () -> Unit, onAbout: () -> Unit, onEditApp: (Built) -> Unit, onConsole: () -> Unit) {
    val ctx = LocalContext.current
    val ls = rememberLazyListState()
    val headerPx = with(LocalDensity.current) { 190.dp.toPx() }
    var selected by remember { mutableStateOf<Built?>(null) }
    val collapse = if (ls.firstVisibleItemIndex > 0) 1f else (ls.firstVisibleItemScrollOffset / headerPx).coerceIn(0f, 1f)
    Box(Modifier.fillMaxSize().background(OneUi.Bg)) {
        AuroraBackground(Modifier.fillMaxWidth().height(440.dp).graphicsLayer { alpha = 1f - collapse * 0.8f })
        LazyColumn(Modifier.fillMaxSize(), state = ls, contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 90.dp)) {
            // منطقة العنوان الكبيرة (كما في One UI): العنوان بالأسفل ويصغر عند التمرير
            item {
                Box(Modifier.fillMaxWidth().height(250.dp)) {
                    Column(Modifier.align(Alignment.BottomStart).padding(start = 8.dp, bottom = 22.dp).graphicsLayer { alpha = 1f - collapse; translationY = -40f * collapse }) {
                        Text("دبليك", style = MaterialTheme.typography.displaySmall, color = OneUi.Ink)
                        Text("حوّل موقعك إلى تطبيق APK قابل للتثبيت", style = MaterialTheme.typography.bodyMedium, color = OneUi.Muted)
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Entrance(0, Modifier.weight(1f)) { BigTile("إنشاء APK", "اسم وأيقونة وصلاحيات وملفات", Icons.Outlined.Android, OneUi.Blue, Modifier.fillMaxWidth(), onApk) }
                    Entrance(1, Modifier.weight(1f)) { BigTile("ZIP / HTML", "ارفع الملفات وعدّلها", Icons.Outlined.FolderZip, OneUi.Green, Modifier.fillMaxWidth(), onZip) }
                }
                Spacer(Modifier.height(12.dp))
                Entrance(2) {
                    Row(Modifier.fillMaxWidth().oneClick { onConsole() }.clip(RoundedCornerShape(OneUi.RCard)).background(OneUi.Card).padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(54.dp).clip(CircleShape).background(Color(0xFFAF52DE).copy(alpha = 0.18f)), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Terminal, null, tint = Color(0xFFAF52DE), modifier = Modifier.size(28.dp)) }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text("منفّذ الأوامر", style = MaterialTheme.typography.titleMedium, color = OneUi.Ink)
                            Text("أوامر على ملفات مشروعك + تشغيل أكواد جافاسكربت", style = MaterialTheme.typography.labelMedium, color = OneUi.Muted)
                        }
                        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, null, tint = OneUi.Muted, modifier = Modifier.size(26.dp))
                    }
                }
                Spacer(Modifier.height(28.dp))
                Text("التطبيقات التي أنشأتها", style = MaterialTheme.typography.titleMedium, color = OneUi.Ink, modifier = Modifier.padding(start = 8.dp, bottom = 10.dp))
            }
            if (Store.items.isEmpty()) item {
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(OneUi.RCard)).background(OneUi.Card).padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.Inventory2, null, tint = OneUi.Faint, modifier = Modifier.size(48.dp))
                    Text("لا توجد تطبيقات بعد", style = MaterialTheme.typography.titleMedium, color = OneUi.Ink, modifier = Modifier.padding(top = 8.dp))
                    Text("اضغط «إنشاء APK» وسيظهر تطبيقك هنا بعد البناء", style = MaterialTheme.typography.labelMedium, color = OneUi.Muted, textAlign = TextAlign.Center)
                }
            }
            items(Store.items, key = { it.path }) { b ->
              Entrance(Store.items.indexOf(b) + 2) {
                val icon = remember(b.icon) { if (b.icon.isNotBlank() && File(b.icon).exists()) BitmapFactory.decodeFile(b.icon)?.asImageBitmap() else null }
                Row(
                    Modifier.fillMaxWidth().padding(bottom = 10.dp).oneClick { selected = b }.clip(RoundedCornerShape(OneUi.RCard)).background(OneUi.Card).padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(OneUi.Card2), contentAlignment = Alignment.Center) {
                        if (icon != null) Image(icon, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) else Icon(Icons.Outlined.Android, null, tint = OneUi.Blue, modifier = Modifier.size(30.dp))
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(b.name, style = MaterialTheme.typography.bodyLarge, color = OneUi.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${b.pkg} · ${b.version}", style = MaterialTheme.typography.labelSmall, color = OneUi.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Icon(Icons.Outlined.MoreVert, "خيارات", tint = OneUi.Muted, modifier = Modifier.size(26.dp))
                }
              }
            }
        }
        // عنوان مصغر مثبت يظهر عند التمرير
        Box(Modifier.fillMaxWidth().statusBarsPadding().height(56.dp).graphicsLayer { alpha = collapse }, contentAlignment = Alignment.CenterStart) {
            Text("دبليك", style = MaterialTheme.typography.titleLarge, color = OneUi.Ink, modifier = Modifier.padding(start = 24.dp))
        }
        EdgeShade(top = 100.dp, bottom = 70.dp)
        selected?.let { b ->
            androidx.compose.ui.window.Dialog(onDismissRequest = { selected = null }) {
                Column(Modifier.clip(RoundedCornerShape(OneUi.RCard)).background(Color(0xF01C1C1E)).padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(b.name, style = MaterialTheme.typography.titleLarge, color = OneUi.Ink)
                    Text("${b.pkg} · ${b.version}", style = MaterialTheme.typography.labelMedium, color = OneUi.Muted, modifier = Modifier.padding(bottom = 6.dp))
                    OneButton("تثبيت", { Store.install(ctx, File(b.path)); selected = null }, icon = Icons.Outlined.InstallMobile)
                    OneButton("تعديل التطبيق", { onEditApp(b); selected = null }, primary = false, icon = Icons.Outlined.Edit)
                    OneButton("مشاركة", { Store.share(ctx, File(b.path)); selected = null }, primary = false, icon = Icons.Outlined.Share)
                    OneButton("حذف", { Store.remove(b); selected = null }, primary = false, icon = Icons.Outlined.DeleteOutline)
                }
            }
        }
        Row(Modifier.align(Alignment.TopEnd).statusBarsPadding().height(56.dp).padding(end = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).waterClick(CircleShape) { onAbout() }.glass(CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Info, "عن التطبيق", tint = OneUi.Ink, modifier = Modifier.size(24.dp))
            }
        }
    }
}
