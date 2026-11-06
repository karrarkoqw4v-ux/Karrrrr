package iq.quiz.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import iq.quiz.data.Post
import iq.quiz.data.Repo
import iq.quiz.ui.components.*
import iq.quiz.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

class Nav(
    val story: (Int) -> Unit, val chat: (Int) -> Unit, val user: (Int) -> Unit, val create: () -> Unit,
    val notifs: () -> Unit, val inbox: () -> Unit, val settings: () -> Unit, val edit: () -> Unit,
    val myStory: () -> Unit, val archive: () -> Unit,
)

@Composable
fun MainScreen(nav: Nav) {
    val p = pal
    val ctx = LocalContext.current
    var tab by remember { mutableIntStateOf(0) }
    val askNotif = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) {
        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            androidx.core.content.ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) askNotif.launch(android.Manifest.permission.POST_NOTIFICATIONS)
    }
    val dark = tab == 1
    Column(Modifier.fillMaxSize().background(if (dark) Color.Black else p.bg)) {
        Box(Modifier.weight(1f)) {
            when (tab) {
                0 -> FeedScreen(nav)
                1 -> ReelsScreen(nav)
                2 -> InboxScreen(onBack = null, onChat = { nav.chat(it) })
                3 -> ExploreScreen(nav)
                else -> ProfileScreen(0, nav, onBack = null)
            }
        }
        Row(Modifier.fillMaxWidth().background(if (dark) Color.Black else p.bg).navigationBarsPadding().height(66.dp), verticalAlignment = Alignment.CenterVertically) {
            (0..4).forEach { i ->
                Box(Modifier.weight(1f).fillMaxHeight().bounceClick { tab = i }, contentAlignment = Alignment.Center) {
                    val sel = tab == i
                    val c = Color.White
                    if (i == 4) {
                        Box {
                            Box(Modifier.border(if (sel) 2.dp else 1.dp, if (sel) Color.White else Color(0x33FFFFFF), CircleShape).padding(2.dp)) { Avatar(Repo.me.name, Repo.me.idx, 34.dp) }
                            if (Repo.chats.any { it.unread > 0 }) Box(Modifier.align(Alignment.BottomEnd).offset(x = (-3).dp, y = 3.dp).size(18.dp).clip(CircleShape).background(if (dark) Color.Black else p.bg).padding(3.dp).clip(CircleShape).background(Color(0xFFFF3040)))
                        }
                    } else NavGlyph(i.let { listOf(0, 1, 2, 3)[it] }, sel, c, if (dark) Color.Black else p.bg)
                }
            }
        }
    }
}

@Composable
fun FeedScreen(nav: Nav) {
    val p = pal
    var commentsFor by remember { mutableStateOf<Post?>(null) }
    var likesFor by remember { mutableStateOf<Post?>(null) }
    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Row(Modifier.fillMaxWidth().statusBarsPadding().height(56.dp).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("كويز تواصلكم", style = MaterialTheme.typography.titleLarge.copy(brush = BrandBrush), modifier = Modifier.weight(1f))
                IconBtn(Icons.Outlined.AddBox, "نشر", size = 29.dp) { nav.create() }
                IconBtn(Icons.Outlined.FavoriteBorder, "النشاط", size = 29.dp) { nav.notifs() }
            }
            HorizontalDivider(color = p.line)
        }
        item {
            LazyRow(Modifier.fillMaxWidth().padding(vertical = 10.dp), contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box {
                            Avatar(Repo.me.name, Repo.me.idx, 56.dp, Modifier.bounceClick { if (Repo.myStories.isNotEmpty()) nav.myStory() else nav.create() }, if (Repo.myStories.isNotEmpty()) Ring.New else Ring.None)
                            Box(Modifier.align(Alignment.BottomEnd).size(24.dp).clip(CircleShape).background(p.bg).padding(2.dp).clip(CircleShape).background(Primary).bounceClick { nav.create() }, contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Add, null, tint = Color.White, modifier = Modifier.size(16.dp)) }
                        }
                        Text("قصتك", style = MaterialTheme.typography.labelMedium, color = p.ink)
                    }
                }
                if (Repo.demo) itemsIndexed(Repo.users.take(7)) { i, u ->
                    Column(Modifier.bounceClick { nav.story(i) }, horizontalAlignment = Alignment.CenterHorizontally) {
                        Avatar(u.name, u.idx, 56.dp, ring = if (i < 5) Ring.New else Ring.Seen)
                        Text(u.name.substringBefore(" "), style = MaterialTheme.typography.labelSmall, color = if (i < 5) p.ink else p.muted)
                    }
                }
            }
            HorizontalDivider(color = p.line)
        }
        if (Repo.posts.none { it.videoPath == null && !it.archived }) item(key = "empty") {
            Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Outlined.Image, null, tint = p.faint, modifier = Modifier.size(64.dp))
                Spacer(Modifier.height(12.dp))
                Text("ما في منشورات بعد", style = MaterialTheme.typography.titleMedium, color = p.ink)
                Text("انشر أول صورة لك وتظهر هنا", style = MaterialTheme.typography.bodyMedium, color = p.muted)
                Spacer(Modifier.height(18.dp))
                GradButton("انشر الآن", { nav.create() }, icon = Icons.Outlined.Add)
            }
        }
        Repo.posts.filter { it.videoPath == null && !it.archived && it.id !in Repo.hiddenIds && it.userId !in Repo.mutedUsers }.forEachIndexed { i, post ->
            item(key = post.id) { PostCard(post, nav, onComments = { commentsFor = post }, onLikes = { likesFor = post }) }
            if (i == 1 && Repo.demo && Repo.flag("sugg", true)) item(key = "suggestions") { Suggestions(nav) }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
    commentsFor?.let { CommentsSheet(it) { commentsFor = null } }
    likesFor?.let { post -> UserListSheet("الإعجابات", Repo.likers(post), onUser = { nav.user(it) }) { likesFor = null } }
}

@Composable
private fun Suggestions(nav: Nav) {
    val p = pal
    val hidden = remember { mutableStateListOf<Int>() }
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(Modifier.padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("اقتراحات قد تعجبك", style = MaterialTheme.typography.titleMedium, color = p.ink, modifier = Modifier.weight(1f))
            Text("عرض الكل", style = MaterialTheme.typography.labelMedium, color = Primary)
        }
        Spacer(Modifier.height(10.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(Repo.users.drop(2).take(6).filter { it.id !in hidden }, key = { it.id }) { u ->
                Box(Modifier.width(170.dp).clip(RoundedCornerShape(16.dp)).background(p.bg2).border(1.dp, p.line, RoundedCornerShape(16.dp))) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Avatar(u.name, u.idx, 76.dp, Modifier.bounceClick { nav.user(u.id) })
                        Spacer(Modifier.height(10.dp))
                        Text(u.name, style = MaterialTheme.typography.bodyLarge, color = p.ink, maxLines = 1)
                        Text("صديق واحد مشترك", style = MaterialTheme.typography.labelMedium, color = p.muted, maxLines = 1)
                        Spacer(Modifier.height(12.dp))
                        FollowPill(u.id, Modifier.fillMaxWidth(), big = true)
                    }
                    Icon(Icons.Outlined.Close, "إخفاء", tint = p.muted, modifier = Modifier.align(Alignment.TopStart).padding(6.dp).size(30.dp).bounceClick { hidden.add(u.id) }.padding(5.dp))
                }
            }
        }
    }
}

@Composable
fun PostCard(post: Post, nav: Nav, onComments: () -> Unit, onLikes: () -> Unit) {
    val p = pal
    val u = Repo.user(post.userId)
    var burst by remember { mutableStateOf(0) }
    val ctxP = LocalContext.current
    var menu by remember { mutableStateOf(false) }
    if (menu) PostMenuSheet(post) { menu = false }
    val burstScale by animateFloatAsState(if (burst > 0) 1f else 0f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow), label = "burst")
    LaunchedEffect(burst) { if (burst > 0) { delay(650); burst = 0 } }
    
    Column(Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Avatar(u.name, u.idx, 36.dp, Modifier.bounceClick { nav.user(u.id) }, if (u.id % 2 == 0) Ring.New else Ring.None)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (u.id == 0) Repo.me.name else u.name, style = MaterialTheme.typography.bodyLarge, color = p.ink)
                    if (u.id % 3 == 0) { Spacer(Modifier.width(4.dp)); Icon(Icons.Filled.Verified, null, tint = Primary, modifier = Modifier.size(16.dp)) }
                }
                Text(if (post.location.isNotBlank()) post.location else if (u.id != 0 && !Repo.isFollowing(u.id)) "مقترح لك" else post.time, style = MaterialTheme.typography.labelMedium, color = p.muted)
            }
            if (u.id != 0) { FollowPill(u.id, outlined = true); Spacer(Modifier.width(4.dp)) }
            IconBtn(Icons.Outlined.MoreVert, "خيارات", p.ink, 26.dp) { menu = true }
        }
        Box(
            Modifier.fillMaxWidth().aspectRatio(1.08f).pointerInput(post.id) {
                detectTapGestures(onDoubleTap = { if (!post.liked) { post.liked = true; post.likes++ }; burst++ })
            },
            contentAlignment = Alignment.Center,
        ) {
            if (post.imagePath != null) AsyncImage(File(post.imagePath), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            else PhotoArt(post.kind, Modifier.fillMaxSize())
            Icon(Icons.Filled.Favorite, null, tint = Color.White, modifier = Modifier.size(96.dp).graphicsLayer { scaleX = burstScale; scaleY = burstScale; alpha = burstScale })
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            PostAction(if (post.liked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder, if (post.hideLikes) "" else "${post.likes}", if (post.liked) Like else p.ink) {
                post.liked = !post.liked; post.likes += if (post.liked) 1 else -1
            }
            PostAction(Icons.Outlined.ChatBubbleOutline, if (post.commentsOff) "" else "${post.commentCount.value}", p.ink) { if (!post.commentsOff) onComments() }
            PostAction(Icons.Outlined.Send, "شارك", p.ink) { sharePost(ctxP, post) }
            Spacer(Modifier.weight(1f))
            PostAction(if (post.saved) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder, if (post.saved) "محفوظ" else "حفظ", if (post.saved) Primary else p.ink) { post.saved = !post.saved }
        }
        val likers = remember(post.id) { Repo.likers(post) }
        if (post.userId != 0 && !post.hideLikes) Row(Modifier.fillMaxWidth().bounceClick { onLikes() }.padding(horizontal = 14.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(54.dp).height(24.dp)) {
                likers.take(3).forEachIndexed { i, l -> Avatar(l.name, l.idx, 24.dp, Modifier.offset(x = (i * 15).dp)) }
            }
            Text(
                "أعجب بها ${likers[0].name.substringBefore(" ")} و ${(post.likes - 1).coerceAtLeast(0)} آخرون",
                style = MaterialTheme.typography.labelMedium, color = p.ink,
            )
        }
        if (post.caption.isNotBlank()) Text(hashtagText(if (u.id == 0) Repo.me.name else u.name, post.caption, Primary), style = MaterialTheme.typography.bodyMedium, color = p.ink, modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp))
        if (post.commentsOff) Text("التعليقات مغلقة", style = MaterialTheme.typography.labelMedium, color = p.faint, modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp))
        else Text("عرض كل ${post.commentCount.value} تعليق", style = MaterialTheme.typography.labelMedium, color = p.muted, modifier = Modifier.bounceClick { onComments() }.padding(horizontal = 14.dp, vertical = 4.dp))
    }
}

@Composable
private fun PostAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, tint: Color, onClick: () -> Unit) {
    Row(Modifier.height(44.dp).bounceClick(onClick = onClick).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, label, tint = tint, modifier = Modifier.size(26.dp))
        Spacer(Modifier.width(5.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = tint)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsSheet(post: Post, onDismiss: () -> Unit) {
    val p = pal
    var text by remember { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = p.bg) {
        Column(Modifier.fillMaxWidth().heightIn(max = 520.dp).navigationBarsPadding().imePadding()) {
            Text("التعليقات", style = MaterialTheme.typography.titleMedium, color = p.ink, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp))
            HorizontalDivider(color = p.line)
            LazyColumn(Modifier.weight(1f, fill = false)) {
                items(post.comments.size) { i ->
                    val c = post.comments[i]
                    val u = Repo.user(c.userId)
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Avatar(u.name, u.idx, 38.dp)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Row { Text(u.name, style = MaterialTheme.typography.bodyLarge, color = p.ink); Spacer(Modifier.width(8.dp)); Text(c.time, style = MaterialTheme.typography.labelSmall, color = p.faint) }
                            Text(c.text, style = MaterialTheme.typography.bodyMedium, color = p.ink)
                            Text("رد", style = MaterialTheme.typography.labelMedium, color = p.muted, modifier = Modifier.padding(top = 2.dp))
                        }
                        Column(Modifier.bounceClick { c.liked = !c.liked; c.likes += if (c.liked) 1 else -1 }.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(if (c.liked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder, "إعجاب", tint = if (c.liked) Like else p.faint, modifier = Modifier.size(20.dp))
                            if (c.likes > 0) Text("${c.likes}", style = MaterialTheme.typography.labelSmall, color = p.muted)
                        }
                    }
                }
            }
            HorizontalDivider(color = p.line)
            Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf("❤", "🙌", "🔥", "👏", "😢", "😍", "😮", "😂").forEach { e ->
                    Text(e, fontSize = androidx.compose.ui.unit.TextUnit(27f, androidx.compose.ui.unit.TextUnitType.Sp), modifier = Modifier.bounceClick { text += e }.padding(4.dp))
                }
            }
            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Avatar(Repo.me.name, Repo.me.idx, 36.dp)
                Spacer(Modifier.width(10.dp))
                HalaField(text, { text = it }, "ما رأيك في هذا؟", modifier = Modifier.weight(1f))
                IconBtn(Icons.Outlined.Send, "نشر", Primary) { if (text.isNotBlank()) { Repo.addComment(post, text.trim()); text = "" } }
            }
        }
    }
}

@Composable
fun ExploreScreen(nav: Nav) {
    val p = pal
    var q by remember { mutableStateOf("") }
    var cat by remember { mutableIntStateOf(0) }
    val people = Repo.users.filter { q.isNotBlank() && (it.name.contains(q) || it.username.contains(q, true)) }
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        HalaField(q, { q = it }, "ابحث عن ناس، وسوم، أماكن", Icons.Outlined.Search, modifier = Modifier.padding(12.dp))
        if (q.isNotBlank()) {
            LazyColumn {
                items(people) { u ->
                    Row(Modifier.fillMaxWidth().bounceClick { nav.user(u.id) }.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Avatar(u.name, u.idx, 46.dp, ring = Ring.New)
                        Spacer(Modifier.width(12.dp))
                        Column { Text(u.name, style = MaterialTheme.typography.bodyLarge, color = p.ink); Text("@" + u.username, style = MaterialTheme.typography.labelMedium, color = p.muted) }
                    }
                }
                if (people.isEmpty()) item { Text("لا توجد نتائج", Modifier.padding(24.dp), color = p.muted, style = MaterialTheme.typography.bodyMedium) }
            }
        } else {
            LazyRow(contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(listOf("لك", "رحلات", "طعام", "فن", "رياضة", "موسيقى")) { i, t -> Chip(t, cat == i) { cat = i } }
            }
            Spacer(Modifier.height(10.dp))
            if (Repo.posts.isEmpty()) Text("لا توجد منشورات للاستكشاف بعد. انشر صورك وتظهر هنا", Modifier.fillMaxWidth().padding(32.dp), color = p.muted, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium)
            LazyVerticalGrid(GridCells.Fixed(3), Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                items(if (Repo.demo) Repo.posts.size + 8 else Repo.posts.size) { i ->
                    val post = Repo.posts.getOrNull(i)
                    Box(Modifier.aspectRatio(1f).bounceClick {}) {
                        if (post?.imagePath != null) AsyncImage(File(post.imagePath), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        else PhotoArt((post?.kind ?: i) + cat, Modifier.fillMaxSize())
                    }
                }
            }
        }
    }
}

@Composable
fun SavedScreen(nav: Nav) {
    val p = pal
    val saved = Repo.posts.filter { it.saved }
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Text("المحفوظات", style = MaterialTheme.typography.headlineSmall, color = p.ink, modifier = Modifier.padding(16.dp))
        if (saved.isEmpty()) {
            Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Outlined.BookmarkBorder, null, tint = p.faint, modifier = Modifier.size(64.dp))
                Spacer(Modifier.height(12.dp))
                Text("ما حفظت منشورات", style = MaterialTheme.typography.titleMedium, color = p.ink)
                Text("اضغط على علامة الحفظ تحت أي منشور وتظهر هنا", style = MaterialTheme.typography.bodyMedium, color = p.muted, textAlign = TextAlign.Center)
            }
        } else LazyVerticalGrid(GridCells.Fixed(3), Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            items(saved, key = { it.id }) { post ->
                Box(Modifier.aspectRatio(1f)) {
                    if (post.imagePath != null) AsyncImage(File(post.imagePath), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) else PhotoArt(post.kind, Modifier.fillMaxSize())
                }
            }
        }
    }
}

@Composable
fun StoryScreen(startUser: Int, onClose: () -> Unit) {
    var user by remember { mutableIntStateOf(startUser) }
    var seg by remember { mutableIntStateOf(0) }
    val prog = remember { Animatable(0f) }
    val count = 3
    LaunchedEffect(user, seg) {
        prog.snapTo(0f)
        prog.animateTo(1f, tween(4500, easing = LinearEasing))
        if (seg < count - 1) seg++ else if (user < Repo.users.size - 1) { user++; seg = 0 } else onClose()
    }
    val u = Repo.users[user]
    var reply by remember { mutableStateOf("") }
    Box(Modifier.fillMaxSize().background(Color.Black).pointerInput(user, seg) {
        detectTapGestures(onTap = { o -> if (o.x < size.width / 2) { if (seg > 0) seg-- else if (user > 0) { user--; seg = 0 } } else { if (seg < count - 1) seg++ else if (user < Repo.users.size - 1) { user++; seg = 0 } else onClose() } })
    }) {
        PhotoArt(user * 2 + seg, Modifier.fillMaxSize())
        Box(Modifier.fillMaxWidth().height(160.dp).background(Brush.verticalGradient(listOf(Color(0x99000000), Color.Transparent))))
        Column(Modifier.statusBarsPadding().padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(count) { i ->
                    Box(Modifier.weight(1f).height(3.dp).clip(RoundedCornerShape(2.dp)).background(Color(0x66FFFFFF))) {
                        Box(Modifier.fillMaxHeight().fillMaxWidth(if (i < seg) 1f else if (i == seg) prog.value else 0f).background(Color.White))
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(u.name, u.idx, 34.dp, ring = Ring.New)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) { Text(u.name, style = MaterialTheme.typography.bodyLarge, color = Color.White); Text("قبل ساعتين", style = MaterialTheme.typography.labelSmall, color = Color(0xFFE8F5F3)) }
                IconBtn(Icons.Outlined.Close, "إغلاق", Color.White) { onClose() }
            }
        }
        Row(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().imePadding().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                reply, { reply = it }, Modifier.weight(1f), shape = RoundedCornerShape(26.dp), singleLine = true,
                placeholder = { Text("أرسل رسالة...", color = Color(0xFFE8F5F3)) },
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color.White, unfocusedBorderColor = Color(0xCCFFFFFF), cursorColor = Color.White),
            )
            IconBtn(Icons.Outlined.FavoriteBorder, "إعجاب", Color.White, 28.dp) {}
            IconBtn(Icons.Outlined.Send, "إرسال", Color.White, 26.dp) { reply = "" }
        }
    }
}

@Composable
fun CreatePostScreen(onBack: () -> Unit) {
    val p = pal
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var path by remember { mutableStateOf<String?>(null) }
    var isVideo by remember { mutableStateOf(false) }
    var caption by remember { mutableStateOf("") }
    var mode by remember { mutableIntStateOf(1) } // 0 ريلز، 1 منشور، 2 قصة، 3 قوالب
    var target by remember { mutableIntStateOf(0) } // 0 منشور، 1 قصة
    var location by remember { mutableStateOf("") }
    var hideLikes by remember { mutableStateOf(false) }
    var commentsOff by remember { mutableStateOf(false) }
    val imgPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch { runCatching { Repo.importImage(ctx, uri) }.onSuccess { path = it; isVideo = false; target = if (mode == 2) 1 else 0 }.onFailure { Toast.makeText(ctx, "تعذر فتح الصورة", Toast.LENGTH_SHORT).show() } }
    }
    val vidPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch { runCatching { Repo.importVideo(ctx, uri) }.onSuccess { path = it; isVideo = true }.onFailure { Toast.makeText(ctx, "تعذر فتح الفيديو", Toast.LENGTH_SHORT).show() } }
    }
    fun pickMedia() = if (mode == 0) vidPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
    else imgPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))

    if (path != null) {
        // خطوة الوصف والنشر
        fun publish() {
            when {
                isVideo -> Repo.addReel(path!!, caption.trim(), location.trim(), hideLikes, commentsOff)
                target == 1 -> Repo.addStory(path!!)
                else -> Repo.addPost(path!!, caption.trim(), location.trim(), hideLikes, commentsOff)
            }
            Toast.makeText(ctx, if (target == 1 && !isVideo) "تمت إضافة القصة" else "تم النشر", Toast.LENGTH_SHORT).show(); onBack()
        }
        Column(Modifier.fillMaxSize().background(p.bg).imePadding()) {
            TopBack(if (isVideo) "ريل جديد" else if (target == 1) "قصة جديدة" else "منشور جديد", { path = null }) {
                Text("نشر", style = MaterialTheme.typography.labelLarge, color = PrimaryDark, modifier = Modifier.bounceClick { publish() }.padding(12.dp))
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp).navigationBarsPadding()) {
                Box(Modifier.fillMaxWidth().aspectRatio(if (target == 1 && !isVideo) 0.8f else 1f).clip(RoundedCornerShape(20.dp)).background(p.bg2), contentAlignment = Alignment.Center) {
                    if (!isVideo) AsyncImage(File(path!!), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    else Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.Movie, null, tint = Primary, modifier = Modifier.size(64.dp))
                        Text("الفيديو جاهز للنشر", style = MaterialTheme.typography.bodyLarge, color = p.muted)
                    }
                }
                if (!isVideo) {
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Chip("منشور", target == 0) { target = 0 }; Chip("قصتك (24 ساعة)", target == 1) { target = 1 } }
                }
                if (target == 0 || isVideo) {
                    Spacer(Modifier.height(14.dp))
                    HalaField(caption, { caption = it }, "اكتب وصفاً... استخدم # للوسوم", singleLine = false)
                    Spacer(Modifier.height(10.dp))
                    HalaField(location, { location = it }, "إضافة موقع", Icons.Outlined.Place)
                    Spacer(Modifier.height(6.dp))
                    SettingRow(Icons.Outlined.FavoriteBorder, "إخفاء عدد الإعجابات", null, Like, toggle = hideLikes, onToggle = { hideLikes = it })
                    SettingRow(Icons.Outlined.ChatBubbleOutline, "إيقاف التعليقات", null, Primary, toggle = commentsOff, onToggle = { commentsOff = it })
                }
                Spacer(Modifier.height(16.dp))
                GradButton(if (target == 1 && !isVideo) "شارك في قصتك" else "نشر الآن", { publish() }, icon = Icons.Outlined.Send)
            }
        }
        return
    }

    // شاشة الكاميرا
    var granted by remember { mutableStateOf(androidx.core.content.ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED) }
    val askCam = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    LaunchedEffect(Unit) { if (!granted) askCam.launch(android.Manifest.permission.CAMERA) }
    var front by remember { mutableStateOf(false) }
    var flash by remember { mutableStateOf(false) }
    var capture by remember { mutableStateOf<androidx.camera.core.ImageCapture?>(null) }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (granted) CameraView(front, flash, Modifier.fillMaxSize()) { capture = it }
        else Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(Icons.Outlined.CameraAlt, null, tint = Color(0xFFB0B0B8), modifier = Modifier.size(60.dp))
            Text("نحتاج إذن الكاميرا", style = MaterialTheme.typography.titleMedium, color = Color.White, modifier = Modifier.padding(top = 10.dp))
            Spacer(Modifier.height(14.dp))
            GradButton("السماح", { askCam.launch(android.Manifest.permission.CAMERA) })
        }
        // أعلى
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            IconBtn(Icons.Outlined.Close, "إغلاق", Color.White, 30.dp) { onBack() }
            Spacer(Modifier.weight(1f))
            Row(Modifier.clip(RoundedCornerShape(20.dp)).background(Color(0x66000000)).bounceClick { Toast.makeText(ctx, "إضافة صوت قريباً", Toast.LENGTH_SHORT).show() }.padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.MusicNote, null, tint = Color.White, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
                Text("إضافة صوت", style = MaterialTheme.typography.labelMedium, color = Color.White)
            }
            Spacer(Modifier.weight(1f))
            IconBtn(if (flash) Icons.Outlined.FlashOn else Icons.Outlined.FlashOff, "فلاش", Color.White, 28.dp) { flash = !flash }
        }
        // أدوات جانبية
        Column(Modifier.align(Alignment.CenterEnd).padding(end = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            IconBtn(Icons.Outlined.FlipCameraAndroid, "قلب الكاميرا", Color.White, 28.dp) { front = !front }
            IconBtn(Icons.Outlined.MusicNote, "موسيقى", Color.White, 28.dp) { Toast.makeText(ctx, "قريباً", Toast.LENGTH_SHORT).show() }
            IconBtn(Icons.Outlined.AutoAwesome, "مؤثرات", Color.White, 28.dp) { Toast.makeText(ctx, "قريباً", Toast.LENGTH_SHORT).show() }
            IconBtn(Icons.Outlined.GridView, "قوالب", Color.White, 28.dp) { Toast.makeText(ctx, "قريباً", Toast.LENGTH_SHORT).show() }
            IconBtn(Icons.Outlined.Timer, "مؤقت", Color.White, 28.dp) { Toast.makeText(ctx, "قريباً", Toast.LENGTH_SHORT).show() }
        }
        // أسفل
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().padding(bottom = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 28.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Box(Modifier.size(52.dp).clip(RoundedCornerShape(12.dp)).background(Color(0x55FFFFFF)).border(2.dp, Color.White, RoundedCornerShape(12.dp)).bounceClick { pickMedia() }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.PhotoLibrary, null, tint = Color.White)
                }
                Box(Modifier.size(82.dp).border(5.dp, Color.White, CircleShape).padding(8.dp).clip(CircleShape).background(Color.White).bounceClick {
                    if (mode == 0) { Toast.makeText(ctx, "التسجيل المباشر قريباً: اختر فيديو من المعرض", Toast.LENGTH_SHORT).show(); pickMedia() }
                    else if (mode == 3) Toast.makeText(ctx, "القوالب قريباً", Toast.LENGTH_SHORT).show()
                    else capture?.let { cap ->
                        val f = Repo.newMediaFile("jpg")
                        cap.takePicture(androidx.camera.core.ImageCapture.OutputFileOptions.Builder(f).build(), androidx.core.content.ContextCompat.getMainExecutor(ctx),
                            object : androidx.camera.core.ImageCapture.OnImageSavedCallback {
                                override fun onImageSaved(r: androidx.camera.core.ImageCapture.OutputFileResults) { path = f.absolutePath; isVideo = false; target = if (mode == 2) 1 else 0 }
                                override fun onError(e: androidx.camera.core.ImageCaptureException) { Toast.makeText(ctx, "تعذر التقاط الصورة", Toast.LENGTH_SHORT).show() }
                            })
                    }
                })
                Spacer(Modifier.size(52.dp))
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                listOf("ريلز" to 0, "منشور" to 1, "قصة" to 2, "القوالب" to 3).forEach { (t, i) ->
                    Text(t, style = MaterialTheme.typography.titleMedium, color = if (mode == i) Color.White else Color(0x99FFFFFF), modifier = Modifier.bounceClick { mode = i })
                }
            }
        }
    }
}

@Composable
private fun CameraView(front: Boolean, flash: Boolean, modifier: Modifier, onCapture: (androidx.camera.core.ImageCapture) -> Unit) {
    val ctx = LocalContext.current
    val owner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    val view = remember { androidx.camera.view.PreviewView(ctx) }
    val cap = remember { androidx.camera.core.ImageCapture.Builder().build() }
    cap.flashMode = if (flash) androidx.camera.core.ImageCapture.FLASH_MODE_ON else androidx.camera.core.ImageCapture.FLASH_MODE_OFF
    DisposableEffect(front) {
        val fut = androidx.camera.lifecycle.ProcessCameraProvider.getInstance(ctx)
        fut.addListener({
            runCatching {
                val prov = fut.get()
                val prev = androidx.camera.core.Preview.Builder().build().also { it.setSurfaceProvider(view.surfaceProvider) }
                val sel = if (front) androidx.camera.core.CameraSelector.DEFAULT_FRONT_CAMERA else androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA
                prov.unbindAll()
                prov.bindToLifecycle(owner, sel, prev, cap)
                onCapture(cap)
            }
        }, androidx.core.content.ContextCompat.getMainExecutor(ctx))
        onDispose { runCatching { fut.get().unbindAll() } }
    }
    AndroidView({ view }, modifier)
}

@Composable
fun NotificationsScreen(onBack: () -> Unit) {
    val p = pal
    val items = if (!Repo.demo) emptyList() else listOf(Triple(1, "أعجب بمنشورك", "5 د"), Triple(2, "بدأت بمتابعتك", "20 د"), Triple(4, "علّقت: جميلة جداً!", "1 س"), Triple(3, "ذكرك بتعليق", "2 س"), Triple(6, "أعجبت بتعليقك", "أمس"), Triple(7, "بدأ بمتابعتك", "الاثنين"))
    Column(Modifier.fillMaxSize().background(p.bg)) {
        TopBack("النشاط", onBack)
        if (items.isEmpty()) Column(Modifier.fillMaxWidth().padding(40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Outlined.NotificationsNone, null, tint = p.faint, modifier = Modifier.size(64.dp))
            Spacer(Modifier.height(10.dp))
            Text("ما في نشاط جديد", style = MaterialTheme.typography.titleMedium, color = p.ink)
            Text("الإعجابات والتعليقات تظهر هنا", style = MaterialTheme.typography.bodyMedium, color = p.muted, textAlign = TextAlign.Center)
        }
        LazyColumn {
            itemsIndexed(items) { i, (uid, t, ago) ->
                val u = Repo.user(uid)
                Entrance(i) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Avatar(u.name, u.idx, 46.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) { Text("${u.name} $t", style = MaterialTheme.typography.bodyMedium, color = p.ink); Text(ago, style = MaterialTheme.typography.labelSmall, color = p.faint) }
                        if (t.contains("متابعتك")) Box(Modifier.clip(RoundedCornerShape(15.dp)).background(Brush.horizontalGradient(listOf(Primary, PrimaryDark))).padding(horizontal = 16.dp, vertical = 7.dp)) { Text("متابعة", color = Color.White, style = MaterialTheme.typography.labelMedium) }
                        else Box(Modifier.size(46.dp).clip(RoundedCornerShape(8.dp))) { PhotoArt(i, Modifier.fillMaxSize()) }
                    }
                }
            }
        }
    }
}
