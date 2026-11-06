package iq.quiz.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import iq.quiz.data.Post
import iq.quiz.data.Repo
import iq.quiz.ui.components.*
import iq.quiz.ui.theme.*
import kotlinx.coroutines.launch
import java.io.File

/** تبويب الريلز: فيديوهات عمودية بملء الشاشة من رفع المستخدم. */
@Composable
fun ReelsScreen(nav: Nav) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val reels = Repo.posts.filter { it.videoPath != null }
    var commentsFor by remember { mutableStateOf<Post?>(null) }
    var pending by remember { mutableStateOf<String?>(null) }
    var caption by remember { mutableStateOf("") }
    var friendsTab by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) scope.launch {
            runCatching { Repo.importVideo(ctx, uri) }.onSuccess { pending = it; caption = "" }
                .onFailure { Toast.makeText(ctx, "تعذر فتح الفيديو", Toast.LENGTH_SHORT).show() }
        }
    }
    fun pick() = picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (reels.isEmpty()) {
            Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Icon(Icons.Outlined.Movie, null, tint = Color(0xFFB0B0B8), modifier = Modifier.size(72.dp))
                Spacer(Modifier.height(14.dp))
                Text("ما في ريلز بعد", style = MaterialTheme.typography.titleMedium, color = Color.White)
                Text("ارفع فيديو قصير من المعرض وينعرض هنا بملء الشاشة", style = MaterialTheme.typography.bodyMedium, color = Color(0xFFB0B0B8), textAlign = TextAlign.Center)
                Spacer(Modifier.height(20.dp))
                GradButton("رفع فيديو", { pick() }, icon = Icons.Outlined.Add)
            }
        } else {
            val st = rememberPagerState { reels.size }
            VerticalPager(st, Modifier.fillMaxSize()) { i ->
                ReelPage(reels[i], active = st.currentPage == i, nav, onComments = { commentsFor = reels[i] })
            }
        }
        // الشريط العلوي
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Add, "رفع فيديو", tint = Color.White, modifier = Modifier.size(34.dp).bounceClick { pick() })
            Spacer(Modifier.weight(1f))
            Text("الأصدقاء", style = MaterialTheme.typography.titleMedium, color = if (friendsTab) Color.White else Color(0xB3FFFFFF), modifier = Modifier.bounceClick { friendsTab = true }.padding(8.dp))
            Text("|", color = Color(0x80FFFFFF))
            Text("ريلز", style = MaterialTheme.typography.titleMedium, color = if (!friendsTab) Color.White else Color(0xB3FFFFFF), modifier = Modifier.bounceClick { friendsTab = false }.padding(8.dp))
        }
    }
    pending?.let { path ->
        AlertDialog(
            onDismissRequest = { pending = null },
            title = { Text("نشر الريل") },
            text = { OutlinedTextField(caption, { caption = it }, placeholder = { Text("اكتب وصفاً...") }) },
            confirmButton = { TextButton({ Repo.addReel(path, caption.trim()); pending = null; Toast.makeText(ctx, "تم النشر", Toast.LENGTH_SHORT).show() }) { Text("نشر") } },
            dismissButton = { TextButton({ pending = null }) { Text("إلغاء") } },
        )
    }
    commentsFor?.let { CommentsSheet(it) { commentsFor = null } }
}

@Composable
private fun ReelPage(post: Post, active: Boolean, nav: Nav, onComments: () -> Unit) {
    val ctx = LocalContext.current
    var paused by remember { mutableStateOf(false) }
    val player = remember(post.id) {
        ExoPlayer.Builder(ctx).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.fromFile(File(post.videoPath!!))))
            repeatMode = Player.REPEAT_MODE_ONE
            prepare()
        }
    }
    DisposableEffect(player) { onDispose { player.release() } }
    LaunchedEffect(active, paused) { player.playWhenReady = active && !paused }
    val u = Repo.user(post.userId)

    Box(Modifier.fillMaxSize().pointerInput(post.id) {
        detectTapGestures(onTap = { paused = !paused }, onDoubleTap = { if (!post.liked) { post.liked = true; post.likes++ } })
    }) {
        AndroidView(factory = { c -> PlayerView(c).apply { this.player = player; useController = false; resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM } }, modifier = Modifier.fillMaxSize())
        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(260.dp).background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC000000)))))
        if (paused) Icon(Icons.Filled.PlayArrow, null, tint = Color(0xCCFFFFFF), modifier = Modifier.align(Alignment.Center).size(84.dp))

        // أزرار الجانب (يسار)
        Column(Modifier.align(Alignment.BottomEnd).padding(start = 8.dp, end = 12.dp, bottom = 24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            ReelAction(if (post.liked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder, "${post.likes}", if (post.liked) Like else Color.White) {
                post.liked = !post.liked; post.likes += if (post.liked) 1 else -1
            }
            ReelAction(Icons.Outlined.ChatBubbleOutline, "${post.commentCount.value}", Color.White) { onComments() }
            ReelAction(Icons.Outlined.Repeat, "", Color.White) { Toast.makeText(ctx, "تمت إعادة النشر لمتابعيك", Toast.LENGTH_SHORT).show() }
            ReelAction(Icons.Outlined.Send, "", Color.White) {
                val i = android.content.Intent(android.content.Intent.ACTION_SEND).setType("text/plain").putExtra(android.content.Intent.EXTRA_TEXT, post.caption)
                ctx.startActivity(android.content.Intent.createChooser(i, "مشاركة"))
            }
            ReelAction(if (post.saved) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder, "", if (post.saved) Primary else Color.White) { post.saved = !post.saved }
            Icon(Icons.Outlined.MoreVert, null, tint = Color.White, modifier = Modifier.size(28.dp))
        }
        // معلومات الناشر (يمين)
        Column(Modifier.align(Alignment.BottomStart).padding(start = 14.dp, end = 80.dp, bottom = 26.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(if (u.id == 0) Repo.me.name else u.name, u.idx, 40.dp, Modifier.bounceClick { nav.user(u.id) }, Ring.New)
                Spacer(Modifier.width(8.dp))
                Text(if (u.id == 0) Repo.me.username else u.username, style = MaterialTheme.typography.bodyLarge, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                if (u.id != 0) { Spacer(Modifier.width(10.dp)); FollowPill(u.id, outlined = true) }
            }
            if (post.caption.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(post.caption, style = MaterialTheme.typography.bodyMedium, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun ReelAction(icon: ImageVector, label: String, tint: Color, onClick: () -> Unit) {
    Column(Modifier.bounceClick(onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(32.dp))
        if (label.isNotEmpty()) Text(label, style = MaterialTheme.typography.labelMedium, color = Color.White)
    }
}
