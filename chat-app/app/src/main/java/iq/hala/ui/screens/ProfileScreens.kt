package iq.hala.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import iq.hala.data.Repo
import iq.hala.ui.components.*
import iq.hala.ui.theme.*
import java.io.File

@Composable
private fun Stat(n: String, label: String, modifier: Modifier = Modifier) {
    val p = pal
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(n, style = MaterialTheme.typography.titleLarge, color = p.ink)
        Text(label, style = MaterialTheme.typography.labelMedium, color = p.muted)
    }
}

@Composable
fun ProfileScreen(userId: Int, nav: Nav, onBack: (() -> Unit)?) {
    val p = pal
    val u = Repo.user(userId)
    val mine = userId == 0
    val posts = Repo.posts.filter { it.userId == userId }
    var following by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(p.bg)) {
        TopBack(if (mine) Repo.me.username else u.username, onBack) {
            if (mine) IconBtn(Icons.Outlined.Settings, "الإعدادات") { nav.settings() }
        }
        LazyVerticalGrid(GridCells.Fixed(3), Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(3) }) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Avatar(u.name, u.idx, 80.dp, ring = Ring.New)
                        Spacer(Modifier.width(18.dp))
                        Row(Modifier.weight(1f)) {
                            Stat("${posts.size}", "منشور", Modifier.weight(1f))
                            Stat(if (mine) "0" else "${u.followers + if (following) 1 else 0}", "متابِع", Modifier.weight(1f))
                            Stat(if (mine) "0" else "${u.following}", "يتابع", Modifier.weight(1f))
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(u.name, style = MaterialTheme.typography.bodyLarge, color = p.ink)
                    if (u.bio.isNotBlank()) Text(u.bio, style = MaterialTheme.typography.bodyMedium, color = p.muted)
                    Spacer(Modifier.height(14.dp))
                    if (mine) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(Modifier.weight(1f).height(38.dp).bounceClick { nav.edit() }.clip(RoundedCornerShape(12.dp)).background(p.bg2), contentAlignment = Alignment.Center) { Text("تعديل الملف", style = MaterialTheme.typography.labelLarge, color = p.ink) }
                            Box(Modifier.weight(1f).height(38.dp).bounceClick { nav.settings() }.clip(RoundedCornerShape(12.dp)).background(p.bg2), contentAlignment = Alignment.Center) { Text("الإعدادات", style = MaterialTheme.typography.labelLarge, color = p.ink) }
                        }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(Modifier.weight(1f).height(38.dp).bounceClick { following = !following }.clip(RoundedCornerShape(12.dp))
                                .background(if (following) Brush.horizontalGradient(listOf(p.bg2, p.bg2)) else Brush.horizontalGradient(listOf(Primary, PrimaryDark))), contentAlignment = Alignment.Center) {
                                Text(if (following) "متابَع" else "متابعة", style = MaterialTheme.typography.labelLarge, color = if (following) p.ink else Color.White)
                            }
                            Box(Modifier.weight(1f).height(38.dp).bounceClick { Repo.chats.firstOrNull { it.userId == userId && !it.group }?.let { nav.chat(it.id) } }.clip(RoundedCornerShape(12.dp)).background(p.bg2), contentAlignment = Alignment.Center) { Text("مراسلة", style = MaterialTheme.typography.labelLarge, color = p.ink) }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    HorizontalDivider(color = p.line)
                }
            }
            if (posts.isEmpty()) item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(3) }) {
                Text(if (mine) "ما عندك منشورات بعد. اضغط + حتى تنشر أول صورة" else "لا توجد منشورات", Modifier.fillMaxWidth().padding(32.dp), color = p.muted, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium)
            }
            items(posts, key = { it.id }) { post ->
                Box(Modifier.aspectRatio(1f)) {
                    if (post.imagePath != null) AsyncImage(File(post.imagePath), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) else PhotoArt(post.kind, Modifier.fillMaxSize())
                }
            }
        }
    }
}

@Composable
fun EditProfileScreen(onBack: () -> Unit) {
    val p = pal
    var name by remember { mutableStateOf(Repo.me.name) }
    var bio by remember { mutableStateOf(Repo.me.bio) }
    Column(Modifier.fillMaxSize().background(p.bg).imePadding()) {
        TopBack("تعديل الملف", onBack) {
            Text("حفظ", style = MaterialTheme.typography.labelLarge, color = PrimaryDark, modifier = Modifier.bounceClick { Repo.updateMe(name, Repo.me.username, bio); onBack() }.padding(12.dp))
        }
        Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Avatar(name.ifBlank { "أ" }, Repo.me.idx, 96.dp, ring = Ring.New)
            Spacer(Modifier.height(10.dp))
            Text("@" + Repo.me.username, style = MaterialTheme.typography.bodyMedium, color = p.muted)
            Spacer(Modifier.height(20.dp))
            HalaField(name, { name = it }, "الاسم", Icons.Outlined.Person, label = "الاسم")
            Spacer(Modifier.height(14.dp))
            HalaField(bio, { bio = it }, "نبذة عنك", Icons.Outlined.Edit, label = "النبذة", singleLine = false)
        }
    }
}

@Composable
fun SettingsScreen(onBack: () -> Unit, onLogout: () -> Unit) {
    val p = pal
    var priv by remember { mutableStateOf(Repo.flag("private", false)) }
    var active by remember { mutableStateOf(Repo.flag("active", true)) }
    var read by remember { mutableStateOf(Repo.flag("read", true)) }
    var nLikes by remember { mutableStateOf(Repo.flag("n_likes", true)) }
    var nMsgs by remember { mutableStateOf(Repo.flag("n_msgs", true)) }
    var nCalls by remember { mutableStateOf(Repo.flag("n_calls", true)) }
    var lang by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(p.bg)) {
        TopBack("الإعدادات", onBack)
        Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
            Text("المظهر", style = MaterialTheme.typography.labelMedium, color = p.muted, modifier = Modifier.padding(16.dp, 8.dp))
            SettingRow(Icons.Outlined.DarkMode, "الوضع الداكن", "تغيير ألوان التطبيق", Color(0xFF3B82F6), toggle = Repo.dark, onToggle = { Repo.setDark(it) })
            SettingRow(Icons.Outlined.Language, "اللغة", null, Color(0xFF0891B2), value = "العربية") { lang = true }
            Text("الخصوصية", style = MaterialTheme.typography.labelMedium, color = p.muted, modifier = Modifier.padding(16.dp, 14.dp, 16.dp, 4.dp))
            SettingRow(Icons.Outlined.Lock, "حساب خاص", "فقط من توافق عليه يشوف منشوراتك", Color(0xFF7C5CFF), toggle = priv, onToggle = { priv = it; Repo.setFlag("private", it) })
            SettingRow(Icons.Outlined.Visibility, "حالة النشاط", "اعرض متى كنت متصل", Primary, toggle = active, onToggle = { active = it; Repo.setFlag("active", it) })
            SettingRow(Icons.Outlined.DoneAll, "إشعار القراءة", "علامة المشاهدة بالرسائل", Primary, toggle = read, onToggle = { read = it; Repo.setFlag("read", it) })
            Text("الإشعارات", style = MaterialTheme.typography.labelMedium, color = p.muted, modifier = Modifier.padding(16.dp, 14.dp, 16.dp, 4.dp))
            SettingRow(Icons.Outlined.FavoriteBorder, "الإعجابات والتعليقات", null, Like, toggle = nLikes, onToggle = { nLikes = it; Repo.setFlag("n_likes", it) })
            SettingRow(Icons.Outlined.ChatBubbleOutline, "الرسائل", null, Primary, toggle = nMsgs, onToggle = { nMsgs = it; Repo.setFlag("n_msgs", it) })
            SettingRow(Icons.Outlined.Call, "المكالمات", null, Success, toggle = nCalls, onToggle = { nCalls = it; Repo.setFlag("n_calls", it) })
            Text("الحساب", style = MaterialTheme.typography.labelMedium, color = p.muted, modifier = Modifier.padding(16.dp, 14.dp, 16.dp, 4.dp))
            SettingRow(Icons.Outlined.Logout, "تسجيل الخروج", null, Danger) { Repo.signOut(); onLogout() }
            Text("هلا 0.1 - البيانات محفوظة على جهازك فقط", style = MaterialTheme.typography.labelSmall, color = p.faint, modifier = Modifier.fillMaxWidth().padding(20.dp), textAlign = TextAlign.Center)
        }
    }
    if (lang) AlertDialog(
        onDismissRequest = { lang = false }, confirmButton = { TextButton({ lang = false }) { Text("تمام") } },
        title = { Text("اللغة") }, text = { Text("النسخة الحالية بالعربية فقط. لغات إضافية تأتي بالتحديثات.") },
    )
}
