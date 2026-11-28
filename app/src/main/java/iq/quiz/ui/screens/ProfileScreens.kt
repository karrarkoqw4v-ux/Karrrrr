package iq.quiz.ui.screens

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
import iq.quiz.data.Repo
import iq.quiz.ui.components.*
import iq.quiz.ui.theme.*
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
    var ptab by remember { mutableIntStateOf(0) }
    val allMine = Repo.posts.filter { it.userId == userId && !it.archived }
    val posts = when (ptab) { 1 -> allMine.filter { it.videoPath != null }; 2 -> Repo.posts.filter { it.saved }; else -> allMine.filter { it.videoPath == null } }
    val ctxP = androidx.compose.ui.platform.LocalContext.current
    var sheet by remember { mutableStateOf<Int?>(null) }
    val isF = Repo.isFollowing(userId)
    sheet?.let { which ->
        val list = if (mine) (if (which == 1) Repo.users.filter { it.id in Repo.following } else emptyList()) else Repo.users.filter { it.id != userId }.take(6)
        UserListSheet(if (which == 0) "المتابعون" else "يتابع", list, onUser = { nav.user(it) }) { sheet = null }
    }
    Column(Modifier.fillMaxSize().background(p.bg)) {
        TopBack(if (mine) Repo.me.username else u.username, onBack) {
            if (mine) { IconBtn(Icons.Outlined.Archive, "الأرشيف") { nav.archive() }; IconBtn(Icons.Outlined.Settings, "الإعدادات") { nav.settings() } }
        }
        LazyVerticalGrid(GridCells.Fixed(3), Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(3) }) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Avatar(u.name, u.idx, 80.dp, ring = Ring.New)
                        Spacer(Modifier.width(18.dp))
                        Row(Modifier.weight(1f)) {
                            Stat("${allMine.size}", "منشور", Modifier.weight(1f))
                            Stat(if (mine) "0" else "${Repo.followersOf(u)}", "متابِع", Modifier.weight(1f).bounceClick { sheet = 0 })
                            Stat(if (mine) "${Repo.following.size}" else "${u.following}", "يتابع", Modifier.weight(1f).bounceClick { sheet = 1 })
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(u.name, style = MaterialTheme.typography.bodyLarge, color = p.ink)
                    if (u.bio.isNotBlank()) Text(u.bio, style = MaterialTheme.typography.bodyMedium, color = p.muted)
                    Spacer(Modifier.height(14.dp))
                    if (mine) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(Modifier.weight(1f).height(38.dp).bounceClick { nav.edit() }.clip(RoundedCornerShape(12.dp)).background(p.bg2), contentAlignment = Alignment.Center) { Text("تعديل الملف", style = MaterialTheme.typography.labelLarge, color = p.ink) }
                            Box(Modifier.weight(1f).height(38.dp).bounceClick {
                                val i = android.content.Intent(android.content.Intent.ACTION_SEND).setType("text/plain").putExtra(android.content.Intent.EXTRA_TEXT, "تابعني على كويز تواصلكم: @" + Repo.me.username)
                                ctxP.startActivity(android.content.Intent.createChooser(i, "مشاركة الملف"))
                            }.clip(RoundedCornerShape(12.dp)).background(p.bg2), contentAlignment = Alignment.Center) { Text("مشاركة الملف", style = MaterialTheme.typography.labelLarge, color = p.ink) }
                        }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FollowPill(userId, Modifier.weight(1f), big = true)
                            Box(Modifier.weight(1f).height(38.dp).bounceClick { Repo.chats.firstOrNull { it.userId == userId && !it.group }?.let { nav.chat(it.id) } }.clip(RoundedCornerShape(12.dp)).background(p.bg2), contentAlignment = Alignment.Center) { Text("مراسلة", style = MaterialTheme.typography.labelLarge, color = p.ink) }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    HorizontalDivider(color = p.line)
                    Row(Modifier.fillMaxWidth()) {
                        listOf(Icons.Outlined.GridOn, Icons.Outlined.Movie, Icons.Outlined.BookmarkBorder).forEachIndexed { k, ic ->
                            if (k < 2 || mine) Box(Modifier.weight(1f).height(46.dp).bounceClick { ptab = k }, contentAlignment = Alignment.Center) {
                                Icon(ic, null, tint = if (ptab == k) p.ink else p.faint, modifier = Modifier.size(26.dp))
                                if (ptab == k) Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(2.dp).background(p.ink))
                            }
                        }
                    }
                }
            }
            if (posts.isEmpty()) item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(3) }) {
                Text(if (mine) "ما عندك منشورات بعد. اضغط + حتى تنشر أول صورة" else "لا توجد منشورات", Modifier.fillMaxWidth().padding(32.dp), color = p.muted, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium)
            }
            items(posts, key = { it.id }) { post ->
                Box(Modifier.aspectRatio(1f)) {
                    if (post.imagePath != null) AsyncImage(File(post.imagePath), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    else if (post.videoPath != null) Box(Modifier.fillMaxSize().background(p.bg2), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.PlayCircleOutline, null, tint = p.ink, modifier = Modifier.size(34.dp)) }
                    else PhotoArt(post.kind, Modifier.fillMaxSize())
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
