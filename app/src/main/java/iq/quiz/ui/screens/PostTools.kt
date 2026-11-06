package iq.quiz.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import iq.quiz.data.Post
import iq.quiz.data.Repo
import iq.quiz.ui.components.*
import iq.quiz.ui.theme.*
import java.io.File

fun sharePost(ctx: Context, post: Post) {
    val i = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, (post.caption.ifBlank { "منشور من كويز تواصلكم" }))
    ctx.startActivity(Intent.createChooser(i, "مشاركة"))
}

@Composable
private fun MenuIcon(icon: ImageVector, label: String, onClick: () -> Unit) {
    val p = pal
    Column(Modifier.bounceClick(onClick = onClick).padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(58.dp).clip(RoundedCornerShape(18.dp)).background(p.bg2), contentAlignment = Alignment.Center) { Icon(icon, null, tint = p.ink, modifier = Modifier.size(28.dp)) }
        Text(label, style = MaterialTheme.typography.labelMedium, color = p.ink, modifier = Modifier.padding(top = 5.dp))
    }
}

@Composable
private fun MenuRow(icon: ImageVector, title: String, color: Color = pal.ink, toggle: Boolean? = null, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().bounceClick(onClick = onClick).padding(horizontal = 20.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = color, modifier = Modifier.size(27.dp))
        Spacer(Modifier.width(16.dp))
        Text(title, style = MaterialTheme.typography.bodyLarge, color = color, modifier = Modifier.weight(1f))
        if (toggle != null) Switch(checked = toggle, onCheckedChange = { onClick() }, colors = SwitchDefaults.colors(checkedTrackColor = Primary))
    }
}

/** قائمة خيارات المنشور مثل إنستغرام: حفظ، مشاركة، نسخ الرابط، تعديل، أرشفة، إخفاء الإعجابات، إيقاف التعليقات، حذف / إخفاء، كتم، إبلاغ */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostMenuSheet(post: Post, onDismiss: () -> Unit) {
    val p = pal
    val ctx = LocalContext.current
    val mine = post.userId == 0
    var edit by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf(post.caption) }
    fun done(msg: String) { Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show(); onDismiss() }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = p.bg) {
        Column(Modifier.navigationBarsPadding().padding(bottom = 12.dp)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                MenuIcon(if (post.saved) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder, if (post.saved) "محفوظ" else "حفظ") { post.saved = !post.saved; onDismiss() }
                MenuIcon(Icons.Outlined.Send, "مشاركة") { sharePost(ctx, post); onDismiss() }
                MenuIcon(Icons.Outlined.Link, "نسخ الرابط") {
                    (ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("link", "quiztawasulkum://post/${post.id}"))
                    done("تم نسخ الرابط")
                }
            }
            HorizontalDivider(color = p.line, modifier = Modifier.padding(vertical = 6.dp))
            if (mine) {
                MenuRow(Icons.Outlined.Edit, "تعديل الوصف") { edit = true }
                MenuRow(Icons.Outlined.Archive, "أرشفة") { post.archived = true; Repo.persistMine(); done("انتقل المنشور إلى الأرشيف") }
                MenuRow(Icons.Outlined.FavoriteBorder, "إخفاء عدد الإعجابات", toggle = post.hideLikes) { post.hideLikes = !post.hideLikes; Repo.persistMine() }
                MenuRow(Icons.Outlined.ChatBubbleOutline, "إيقاف التعليقات", toggle = post.commentsOff) { post.commentsOff = !post.commentsOff; Repo.persistMine() }
                MenuRow(Icons.Outlined.DeleteOutline, "حذف", Danger) { Repo.deletePost(post); done("تم الحذف") }
            } else {
                MenuRow(Icons.Outlined.VisibilityOff, "لا أرغب بهذا المنشور") { Repo.hidePost(post); done("لن نعرض لك مثل هذا المنشور") }
                MenuRow(Icons.Outlined.NotificationsOff, "كتم صاحب المنشور") { Repo.mutedUsers.add(post.userId); done("تم الكتم") }
                MenuRow(Icons.Outlined.Flag, "إبلاغ", Danger) { Repo.hidePost(post); done("شكراً، تم استلام بلاغك") }
            }
        }
    }
    if (edit) AlertDialog(
        onDismissRequest = { edit = false }, title = { Text("تعديل الوصف") },
        text = { OutlinedTextField(text, { text = it }, modifier = Modifier.fillMaxWidth()) },
        confirmButton = { TextButton({ post.caption = text.trim(); Repo.persistMine(); edit = false; onDismiss() }) { Text("حفظ") } },
        dismissButton = { TextButton({ edit = false }) { Text("إلغاء") } },
    )
}

@Composable
fun ArchiveScreen(onBack: () -> Unit) {
    val p = pal
    val list = Repo.posts.filter { it.userId == 0 && it.archived }
    Column(Modifier.fillMaxSize().background(p.bg)) {
        TopBack("الأرشيف", onBack)
        if (list.isEmpty()) Column(Modifier.fillMaxWidth().padding(40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Outlined.Archive, null, tint = p.faint, modifier = Modifier.size(64.dp))
            Text("الأرشيف فارغ", style = MaterialTheme.typography.titleMedium, color = p.ink, modifier = Modifier.padding(top = 10.dp))
            Text("المنشورات التي تؤرشفها تختفي من الرئيسية وتبقى هنا. اضغط على المنشور لإرجاعه.", style = MaterialTheme.typography.bodyMedium, color = p.muted, textAlign = TextAlign.Center)
        } else LazyVerticalGrid(GridCells.Fixed(3), Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            items(list, key = { it.id }) { post ->
                Box(Modifier.aspectRatio(1f).bounceClick { post.archived = false; Repo.persistMine() }) {
                    if (post.imagePath != null) AsyncImage(File(post.imagePath), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    else Box(Modifier.fillMaxSize().background(p.bg2), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Movie, null, tint = p.muted) }
                    Icon(Icons.Outlined.Unarchive, null, tint = Color.White, modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp).size(22.dp))
                }
            }
        }
    }
}

private fun ago(t: Long): String {
    val m = ((System.currentTimeMillis() - t) / 60000).toInt()
    return if (m < 1) "الآن" else if (m < 60) "قبل $m د" else "قبل ${m / 60} س"
}

/** عرض قصصي: صور تنتهي بعد 24 ساعة، تقدّم تلقائي، حذف وإضافة */
@Composable
fun MyStoryScreen(onClose: () -> Unit, onAdd: () -> Unit) {
    val stories = Repo.myStories
    var idx by remember { mutableIntStateOf(0) }
    val prog = remember { Animatable(0f) }
    if (stories.isEmpty()) { LaunchedEffect(Unit) { onClose() }; return }
    val i = idx.coerceIn(0, stories.size - 1)
    LaunchedEffect(i, stories.size) {
        prog.snapTo(0f)
        prog.animateTo(1f, tween(5000, easing = LinearEasing))
        if (i < stories.size - 1) idx = i + 1 else onClose()
    }
    val s = stories[i]
    Box(Modifier.fillMaxSize().background(Color.Black).pointerInput(i, stories.size) {
        detectTapGestures(onTap = { o -> if (o.x < size.width / 3) { if (i > 0) idx = i - 1 } else { if (i < stories.size - 1) idx = i + 1 else onClose() } })
    }) {
        AsyncImage(File(s.path), null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
        Box(Modifier.fillMaxWidth().height(150.dp).background(Brush.verticalGradient(listOf(Color(0x99000000), Color.Transparent))))
        Column(Modifier.statusBarsPadding().padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                stories.forEachIndexed { k, _ ->
                    Box(Modifier.weight(1f).height(3.dp).clip(RoundedCornerShape(2.dp)).background(Color(0x66FFFFFF))) {
                        Box(Modifier.fillMaxHeight().fillMaxWidth(if (k < i) 1f else if (k == i) prog.value else 0f).background(Color.White))
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(Repo.me.name, Repo.me.idx, 34.dp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) { Text("قصتك", style = MaterialTheme.typography.bodyLarge, color = Color.White); Text(ago(s.time), style = MaterialTheme.typography.labelMedium, color = Color(0xCCFFFFFF)) }
                IconBtn(Icons.Outlined.Close, "إغلاق", Color.White) { onClose() }
            }
        }
        Row(Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Visibility, null, tint = Color.White, modifier = Modifier.size(24.dp))
            Text("  0 مشاهدة", style = MaterialTheme.typography.labelMedium, color = Color.White, modifier = Modifier.weight(1f))
            IconBtn(Icons.Outlined.DeleteOutline, "حذف", Color.White, 28.dp) { Repo.deleteStory(s) }
            IconBtn(Icons.Outlined.Add, "إضافة", Color.White, 30.dp) { onAdd() }
        }
    }
}
