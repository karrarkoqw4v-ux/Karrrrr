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
)

@Composable
fun MainScreen(nav: Nav) {
    val p = pal
    var tab by remember { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize().background(p.bg)) {
        Box(Modifier.weight(1f)) {
            when (tab) {
                0 -> FeedScreen(nav)
                1 -> ExploreScreen(nav)
                3 -> ReelsScreen()
                else -> ProfileScreen(0, nav, onBack = null)
            }
        }
        HorizontalDivider(color = p.line)
        val labels = listOf("الرئيسية", "استكشاف", "نشر", "ريلز", "حسابي")
        Row(Modifier.fillMaxWidth().navigationBarsPadding().height(68.dp), verticalAlignment = Alignment.CenterVertically) {
            listOf(Icons.Outlined.Home, Icons.Outlined.Search, Icons.Outlined.Add, Icons.Outlined.PlayCircleOutline, Icons.Outlined.Person).forEachIndexed { i, ic ->
                Box(Modifier.weight(1f).fillMaxHeight().bounceClick { if (i == 2) nav.create() else tab = i }, contentAlignment = Alignment.Center) {
                    val c by animateColorAsState(if (tab == i) PrimaryDark else p.faint, label = "tab")
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (i == 2) {
                            Box(Modifier.size(width = 44.dp, height = 32.dp).clip(RoundedCornerShape(11.dp)).background(Brush.horizontalGradient(listOf(Primary, PrimaryDark))), contentAlignment = Alignment.Center) {
                                Icon(ic, null, tint = Color.White)
                            }
                        } else Icon(ic, null, tint = c, modifier = Modifier.size(if (tab == i) 29.dp else 27.dp))
                        Spacer(Modifier.height(3.dp))
                        Text(labels[i], style = MaterialTheme.typography.labelSmall, color = if (i == 2) p.muted else c)
                    }
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
                IconBtn(Icons.Outlined.FavoriteBorder, "النشاط") { nav.notifs() }
                IconBtn(Icons.Outlined.Send, "الرسائل") { nav.inbox() }
            }
            HorizontalDivider(color = p.line)
        }
        item {
            LazyRow(Modifier.fillMaxWidth().padding(vertical = 10.dp), contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    Column(Modifier.bounceClick { nav.create() }, horizontalAlignment = Alignment.CenterHorizontally) {
                        Box { Avatar(Repo.me.name, Repo.me.idx, 56.dp, Modifier.padding(3.dp))
                            Box(Modifier.align(Alignment.BottomEnd).size(22.dp).clip(CircleShape).background(p.bg).padding(2.dp).clip(CircleShape).background(Primary), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Add, null, tint = Color.White, modifier = Modifier.size(14.dp)) } }
                        Text("قصتك", style = MaterialTheme.typography.labelSmall, color = p.ink)
                    }
                }
                itemsIndexed(Repo.users.take(7)) { i, u ->
                    Column(Modifier.bounceClick { nav.story(i) }, horizontalAlignment = Alignment.CenterHorizontally) {
                        Avatar(u.name, u.idx, 56.dp, ring = if (i < 5) Ring.New else Ring.Seen)
                        Text(u.name.substringBefore(" "), style = MaterialTheme.typography.labelSmall, color = if (i < 5) p.ink else p.muted)
                    }
                }
            }
            HorizontalDivider(color = p.line)
        }
        Repo.posts.forEachIndexed { i, post ->
            item(key = post.id) { PostCard(post, nav, onComments = { commentsFor = post }, onLikes = { likesFor = post }) }
            if (i == 1) item(key = "suggestions") { Suggestions(nav) }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
    commentsFor?.let { CommentsSheet(it) { commentsFor = null } }
    likesFor?.let { post -> UserListSheet("الإعجابات", Repo.likers(post), onUser = { nav.user(it) }) { likesFor = null } }
}

@Composable
private fun Suggestions(nav: Nav) {
    val p = pal
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp).background(p.bg2).padding(vertical = 12.dp)) {
        Row(Modifier.padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("اقتراحات لك", style = MaterialTheme.typography.bodyLarge, color = p.ink, modifier = Modifier.weight(1f))
            Text("عرض الكل", style = MaterialTheme.typography.labelMedium, color = Primary)
        }
        Spacer(Modifier.height(10.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(Repo.users.drop(2).take(6)) { u ->
                Column(
                    Modifier.width(150.dp).clip(RoundedCornerShape(16.dp)).background(p.bg).border(1.dp, p.line, RoundedCornerShape(16.dp)).padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Avatar(u.name, u.idx, 66.dp, Modifier.bounceClick { nav.user(u.id) }, Ring.New)
                    Spacer(Modifier.height(8.dp))
                    Text(u.name, style = MaterialTheme.typography.bodyLarge, color = p.ink, maxLines = 1)
                    Text("يتابعه ${u.followers / 100} من أصدقائك", style = MaterialTheme.typography.labelSmall, color = p.muted, maxLines = 1)
                    Spacer(Modifier.height(10.dp))
                    FollowPill(u.id, Modifier.fillMaxWidth())
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
    val burstScale by animateFloatAsState(if (burst > 0) 1f else 0f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow), label = "burst")
    LaunchedEffect(burst) { if (burst > 0) { delay(650); burst = 0 } }
    
    Column(Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Avatar(u.name, u.idx, 36.dp, Modifier.bounceClick { nav.user(u.id) }, if (u.id % 2 == 0) Ring.New else Ring.None)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(if (u.id == 0) Repo.me.name else u.name, style = MaterialTheme.typography.bodyLarge, color = p.ink)
                Text("بغداد - " + post.time, style = MaterialTheme.typography.labelSmall, color = p.muted)
            }
            if (u.id != 0) { FollowPill(u.id); Spacer(Modifier.width(6.dp)) }
            Icon(Icons.Outlined.MoreHoriz, null, tint = p.ink)
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
            PostAction(if (post.liked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder, "${post.likes}", if (post.liked) Like else p.ink) {
                post.liked = !post.liked; post.likes += if (post.liked) 1 else -1
            }
            PostAction(Icons.Outlined.ChatBubbleOutline, "${post.commentCount.value}", p.ink) { onComments() }
            PostAction(Icons.Outlined.Send, "شارك", p.ink) {}
            Spacer(Modifier.weight(1f))
            PostAction(if (post.saved) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder, if (post.saved) "محفوظ" else "حفظ", if (post.saved) Primary else p.ink) { post.saved = !post.saved }
        }
        val likers = remember(post.id) { Repo.likers(post) }
        Row(Modifier.fillMaxWidth().bounceClick { onLikes() }.padding(horizontal = 14.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(54.dp).height(24.dp)) {
                likers.take(3).forEachIndexed { i, l -> Avatar(l.name, l.idx, 24.dp, Modifier.offset(x = (i * 15).dp)) }
            }
            Text(
                "أعجب بها ${likers[0].name.substringBefore(" ")} و ${(post.likes - 1).coerceAtLeast(0)} آخرون",
                style = MaterialTheme.typography.labelMedium, color = p.ink,
            )
        }
        Text("${u.name}  ${post.caption}", style = MaterialTheme.typography.bodyMedium, color = p.ink, modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp))
        Text("عرض كل ${post.commentCount.value} تعليق", style = MaterialTheme.typography.labelMedium, color = p.muted, modifier = Modifier.bounceClick { onComments() }.padding(horizontal = 14.dp, vertical = 4.dp))
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
            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Avatar(Repo.me.name, Repo.me.idx, 36.dp)
                Spacer(Modifier.width(10.dp))
                HalaField(text, { text = it }, "أضف تعليقاً...", modifier = Modifier.weight(1f))
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
            LazyVerticalGrid(GridCells.Fixed(3), Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                items(Repo.posts.size + 8) { i ->
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
fun ReelsScreen() {
    val pager = rememberPagerState { 6 }
    VerticalPager(pager, Modifier.fillMaxSize()) { page ->
        var liked by remember { mutableStateOf(false) }
        Box(Modifier.fillMaxSize()) {
            PhotoArt(page + 1, Modifier.fillMaxSize())
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xB0000000)), startY = 900f)))
            Column(Modifier.align(Alignment.BottomStart).padding(16.dp).padding(bottom = 12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                IconBtn(if (liked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder, "إعجاب", if (liked) Like else Color.White, 32.dp) { liked = !liked }
                IconBtn(Icons.Outlined.ChatBubbleOutline, "تعليق", Color.White, 30.dp) {}
                IconBtn(Icons.Outlined.Send, "مشاركة", Color.White, 28.dp) {}
            }
            Column(Modifier.align(Alignment.BottomEnd).padding(16.dp).padding(bottom = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Avatar(Repo.users[page % 8].name, Repo.users[page % 8].idx, 34.dp, ring = Ring.New)
                    Spacer(Modifier.width(10.dp))
                    Text(Repo.users[page % 8].name, style = MaterialTheme.typography.bodyLarge, color = Color.White)
                }
                Text("لحظات من العراق - اسحب للأعلى للمزيد", style = MaterialTheme.typography.bodyMedium, color = Color.White, modifier = Modifier.padding(top = 8.dp))
            }
            Text("ريلز", style = MaterialTheme.typography.headlineSmall, color = Color.White, modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(16.dp))
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
    var caption by remember { mutableStateOf("") }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch { runCatching { Repo.importImage(ctx, uri) }.onSuccess { path = it }.onFailure { Toast.makeText(ctx, "تعذر فتح الصورة", Toast.LENGTH_SHORT).show() } }
    }
    Column(Modifier.fillMaxSize().background(p.bg).imePadding()) {
        TopBack("منشور جديد", onBack) {
            Text("نشر", style = MaterialTheme.typography.labelLarge, color = if (path != null) PrimaryDark else p.faint,
                modifier = Modifier.bounceClick(enabled = path != null) { Repo.addPost(path!!, caption.trim()); onBack() }.padding(12.dp))
        }
        Column(Modifier.weight(1f).padding(16.dp)) {
            Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(20.dp)).background(p.bg2).bounceClick { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, contentAlignment = Alignment.Center) {
                if (path != null) AsyncImage(File(path!!), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                else Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.Image, null, tint = Primary, modifier = Modifier.size(54.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("اضغط لاختيار صورة من المعرض", style = MaterialTheme.typography.bodyLarge, color = p.muted)
                }
            }
            Spacer(Modifier.height(14.dp))
            HalaField(caption, { caption = it }, "اكتب وصفاً...", singleLine = false)
        }
    }
}

@Composable
fun NotificationsScreen(onBack: () -> Unit) {
    val p = pal
    val items = listOf(Triple(1, "أعجب بمنشورك", "5 د"), Triple(2, "بدأت بمتابعتك", "20 د"), Triple(4, "علّقت: جميلة جداً!", "1 س"), Triple(3, "ذكرك بتعليق", "2 س"), Triple(6, "أعجبت بتعليقك", "أمس"), Triple(7, "بدأ بمتابعتك", "الاثنين"))
    Column(Modifier.fillMaxSize().background(p.bg)) {
        TopBack("النشاط", onBack)
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
