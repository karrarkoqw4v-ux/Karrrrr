package iq.quiz.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import iq.quiz.data.Chat
import iq.quiz.data.Repo
import iq.quiz.ui.components.*
import iq.quiz.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun InboxScreen(onBack: () -> Unit, onChat: (Int) -> Unit) {
    val p = pal
    var q by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().background(p.bg)) {
        TopBack("الرسائل", onBack)
        HalaField(q, { q = it }, "بحث", Icons.Outlined.Search, modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp))
        if (!Repo.demo) Row(Modifier.fillMaxWidth().padding(14.dp).clip(RoundedCornerShape(14.dp)).background(p.bg2).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Info, null, tint = Primary, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Text("المراسلة بين الحسابات تحتاج سيرفر. حالياً تقدر تكتب رسائل لنفسك وتنحفظ بجهازك.", style = MaterialTheme.typography.labelMedium, color = p.muted)
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp)) {
            Text("الرسائل", style = MaterialTheme.typography.bodyLarge, color = p.ink, modifier = Modifier.weight(1f))
        }
        LazyColumn {
            val shown = Repo.chats.filter { q.isBlank() || it.title.contains(q) }
            itemsIndexed(shown, key = { _, c -> c.id }) { i, c ->
                Entrance(i) {
                    val last = c.msgs.lastOrNull()
                    Row(Modifier.fillMaxWidth().bounceClick { c.unread = 0; onChat(c.id) }.padding(horizontal = 14.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                        val u = Repo.user(c.userId)
                        Avatar(c.title, u.idx, 54.dp, online = Repo.demo && !c.group && i % 2 == 0)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(c.title, style = MaterialTheme.typography.bodyLarge, color = p.ink)
                            Text(if (last?.imagePath != null) "أرسلت صورة" else last?.text ?: "", style = MaterialTheme.typography.bodyMedium, color = if (c.unread > 0) p.ink else p.muted, maxLines = 1)
                        }
                        if (c.unread > 0) Box(Modifier.size(22.dp).clip(CircleShape).background(Primary), contentAlignment = Alignment.Center) { Text("${c.unread}", color = Color.White, style = MaterialTheme.typography.labelSmall) }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatScreen(chatId: Int, onBack: () -> Unit, onCall: (Boolean) -> Unit, onUser: () -> Unit) {
    val p = pal
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val chat = Repo.chats.firstOrNull { it.id == chatId } ?: run { onBack(); return }
    val u = Repo.user(chat.userId)
    var text by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            runCatching { Repo.importImage(ctx, uri) }.onSuccess { Repo.send(chat, "", it); Repo.botReply(chat) }.onFailure { Toast.makeText(ctx, "تعذر فتح الصورة", Toast.LENGTH_SHORT).show() }
        }
    }
    LaunchedEffect(chat.msgs.size, chat.typing) { if (chat.msgs.isNotEmpty()) listState.animateScrollToItem(chat.msgs.size) }

    fun send() {
        val t = text.trim()
        if (t.isEmpty()) return
        text = ""
        Repo.send(chat, t)
        scope.launch { Repo.botReply(chat) }
    }

    Column(Modifier.fillMaxSize().background(p.bg).imePadding()) {
        Row(Modifier.fillMaxWidth().statusBarsPadding().height(60.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconBtn(Icons.AutoMirrored.Outlined.ArrowBack, "رجوع") { onBack() }
            Row(Modifier.weight(1f).bounceClick { onUser() }, verticalAlignment = Alignment.CenterVertically) {
                Avatar(chat.title, u.idx, 38.dp, online = !chat.group)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(chat.title, style = MaterialTheme.typography.bodyLarge, color = p.ink)
                    Text(if (chat.typing) "يكتب الآن..." else if (chat.id == 0) "محفوظة لك فقط" else if (chat.group) "15 عضو" else "متصل الآن", style = MaterialTheme.typography.labelSmall, color = Success)
                }
            }
            if (chat.id != 0) { IconBtn(Icons.Outlined.Videocam, "فيديو", size = 26.dp) { onCall(true) }; IconBtn(Icons.Outlined.Call, "اتصال") { onCall(false) } }
        }
        HorizontalDivider(color = p.line)
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), state = listState, contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            itemsIndexed(chat.msgs, key = { i, m -> "${m.id}_$i" }) { _, m ->
                var shown by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) { shown = true }
                val a by animateFloatAsState(if (shown) 1f else 0f, tween(250), label = "m")
                val dy by animateDpAsState(if (shown) 0.dp else 12.dp, tween(250), label = "dy")
                Row(Modifier.fillMaxWidth().graphicsLayer { alpha = a; translationY = dy.toPx() }, horizontalArrangement = if (m.mine) Arrangement.Start else Arrangement.End) {
                    // في RTL بداية الصف = اليمين: رسائلي يمين، رسائلهم يسار
                    Box(
                        Modifier.widthIn(max = 270.dp).clip(RoundedCornerShape(20.dp))
                            .then(if (m.mine) Modifier.background(Brush.horizontalGradient(listOf(Primary, PrimaryDark))) else Modifier.background(p.bg2)),
                    ) {
                        if (m.imagePath != null) AsyncImage(File(m.imagePath), null, Modifier.size(210.dp, 180.dp), contentScale = ContentScale.Crop)
                        else Text(m.text, color = if (m.mine) Color.White else p.ink, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(horizontal = 15.dp, vertical = 10.dp))
                    }
                }
            }
            if (chat.typing) item("typing") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Row(Modifier.clip(RoundedCornerShape(20.dp)).background(p.bg2).padding(horizontal = 16.dp, vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        repeat(3) { i ->
                            val t = rememberInfiniteTransition(label = "d")
                            val s by t.animateFloat(0.5f, 1f, infiniteRepeatable(tween(600, delayMillis = i * 150), RepeatMode.Reverse), label = "s")
                            Box(Modifier.size(8.dp).graphicsLayer { scaleX = s; scaleY = s }.clip(CircleShape).background(p.faint))
                        }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f).clip(RoundedCornerShape(26.dp)).background(p.bg2).padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                IconBtn(Icons.Outlined.Image, "صورة", Primary) { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                BasicTextField(
                    text, { text = it }, Modifier.weight(1f).padding(vertical = 12.dp),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = p.ink), cursorBrush = Brush.linearGradient(listOf(Primary, Primary)),
                    decorationBox = { inner -> if (text.isEmpty()) Text("رسالة...", color = p.faint, style = MaterialTheme.typography.bodyLarge); inner() },
                )
            }
            Spacer(Modifier.width(8.dp))
            Box(Modifier.size(46.dp).bounceClick { send() }.clip(CircleShape).background(Brush.horizontalGradient(listOf(Primary, PrimaryDark))), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Send, "إرسال", tint = Color.White, modifier = Modifier.size(22.dp))
            }
        }
    }
}

/** واجهة المكالمة (صوتية/فيديو): واجهة تجريبية بدون اتصال حقيقي (يحتاج WebRTC وسيرفر) */
@Composable
fun CallScreen(chatId: Int, video: Boolean, onEnd: () -> Unit) {
    val chat = Repo.chats.firstOrNull { it.id == chatId }
    val u = Repo.user(chat?.userId ?: 1)
    var seconds by remember { mutableIntStateOf(-1) }
    var muted by remember { mutableStateOf(false) }
    var speaker by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(2200); seconds = 0; while (true) { delay(1000); seconds++ } }
    val t = rememberInfiniteTransition(label = "pulse")
    val pulse by t.animateFloat(1f, 1.35f, infiniteRepeatable(tween(1400), RepeatMode.Reverse), label = "p")
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF3B1275), Color(0xFF090512))))) {
        if (video) PhotoArt(5, Modifier.fillMaxSize())
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(if (video) 24.dp else 70.dp))
            if (!video) {
                Box(contentAlignment = Alignment.Center) {
                    Box(Modifier.size(150.dp).graphicsLayer { scaleX = pulse; scaleY = pulse; alpha = 0.18f }.clip(CircleShape).background(Color.White))
                    Avatar(u.name, u.idx, 120.dp, ring = Ring.New)
                }
                Spacer(Modifier.height(24.dp))
            }
            Text(chat?.title ?: u.name, style = MaterialTheme.typography.headlineSmall, color = Color.White)
            Text(if (seconds < 0) "جاري الاتصال..." else "%02d:%02d".format(seconds / 60, seconds % 60), style = MaterialTheme.typography.bodyLarge, color = Color(0xFFD6F5F0))
            Spacer(Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                CallBtn(if (muted) Icons.Outlined.MicOff else Icons.Outlined.Mic, if (muted) "مكتوم" else "كتم", muted) { muted = !muted }
                CallBtn(Icons.Outlined.VolumeUp, "السماعة", speaker) { speaker = !speaker }
                CallBtn(Icons.Outlined.Videocam, "فيديو", video) {}
            }
            Spacer(Modifier.height(26.dp))
            Box(Modifier.size(70.dp).bounceClick { onEnd() }.clip(CircleShape).background(Danger), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.CallEnd, "إنهاء", tint = Color.White, modifier = Modifier.size(32.dp)) }
        }
    }
}

@Composable
private fun CallBtn(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, active: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(58.dp).bounceClick(onClick = onClick).clip(CircleShape).background(if (active) Color.White else Color(0x40FFFFFF)), contentAlignment = Alignment.Center) {
            Icon(icon, label, tint = if (active) PrimaryDark else Color.White, modifier = Modifier.size(26.dp))
        }
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color(0xFFE3F8F5), modifier = Modifier.padding(top = 6.dp))
    }
}
