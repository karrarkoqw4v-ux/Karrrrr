package iq.quiz.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import iq.quiz.data.Cloud
import iq.quiz.ui.components.IconBtn
import iq.quiz.ui.components.TopBack
import iq.quiz.ui.theme.Primary
import iq.quiz.ui.theme.pal
import kotlinx.coroutines.launch

/** واجهات المراسلة الحقيقية: Firestore فعلي، تشتغل بين أي حسابين حقيقيين مسجّلين. */

@Composable
fun RealInboxScreen(onBack: () -> Unit, onOpenChat: (String, String) -> Unit) {
    val p = pal
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var searching by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }
    var chats by remember { mutableStateOf<List<Cloud.ChatSummary>>(emptyList()) }
    val names = remember { mutableStateMapOf<String, String>() }

    DisposableEffect(Unit) {
        val reg = Cloud.listenMyChats { list ->
            chats = list
            scope.launch {
                list.forEach { c ->
                    if (names[c.peerUid] == null) Cloud.profile(c.peerUid)?.let { names[c.peerUid] = it.name.ifBlank { it.username } }
                }
            }
        }
        onDispose { reg?.remove() }
    }

    Column(Modifier.fillMaxSize().background(p.bg)) {
        TopBack("المراسلات الحقيقية", onBack)
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp).height(46.dp)
                .clip(RoundedCornerShape(23.dp)).background(p.bg2).padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.Search, null, tint = p.muted, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            BasicTextField(
                query, { query = it; searchError = null }, Modifier.weight(1f), singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = p.ink),
                cursorBrush = SolidColor(Primary),
                decorationBox = { inner -> if (query.isEmpty()) Text("ابحث باسم المستخدم لبدء محادثة جديدة", style = MaterialTheme.typography.bodyMedium, color = p.muted); inner() },
            )
            if (query.isNotBlank()) {
                Text(
                    if (searching) "..." else "ابدأ", style = MaterialTheme.typography.labelMedium, color = Primary,
                    modifier = Modifier.clickable(enabled = !searching) {
                        searching = true; searchError = null
                        scope.launch {
                            val target = Cloud.findByUsername(query)
                            searching = false
                            if (target == null) searchError = "ما لقينا حساب بهذا الاسم" else { query = ""; onOpenChat(target.uid, target.name.ifBlank { target.username }) }
                        }
                    }.padding(6.dp),
                )
            }
        }
        if (searchError != null) Text(searchError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 18.dp))

        if (chats.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("ما عندك محادثات حقيقية الحين\nدوّر باسم مستخدم بالأعلى وابدأ وحدة", style = MaterialTheme.typography.bodyMedium, color = p.muted, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        } else {
            LazyColumn {
                items(chats) { c ->
                    val title = names[c.peerUid] ?: "..."
                    Row(
                        Modifier.fillMaxWidth().clickable { onOpenChat(c.peerUid, title) }.padding(horizontal = 18.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(52.dp).clip(CircleShape).background(p.soft), contentAlignment = Alignment.Center) {
                            Text(title.take(1), color = Primary, style = MaterialTheme.typography.titleMedium)
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(title, style = MaterialTheme.typography.bodyLarge, color = p.ink)
                            Text(c.lastMessage, style = MaterialTheme.typography.bodyMedium, color = p.muted, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RealChatScreen(peerUid: String, peerNameArg: String, onBack: () -> Unit) {
    val p = pal
    val scope = rememberCoroutineScope()
    var peerName by remember { mutableStateOf(peerNameArg) }
    var msgs by remember { mutableStateOf<List<Cloud.CloudMsg>>(emptyList()) }
    var text by remember { mutableStateOf("") }
    val me = Cloud.uid
    val listState = rememberLazyListState()

    LaunchedEffect(peerUid) { if (peerName.isBlank() || peerName == "...") Cloud.profile(peerUid)?.let { peerName = it.name.ifBlank { it.username } } }

    DisposableEffect(peerUid) {
        val reg = Cloud.listenMessages(peerUid) { msgs = it }
        onDispose { reg?.remove() }
    }

    LaunchedEffect(msgs.size) { if (msgs.isNotEmpty()) listState.animateScrollToItem(msgs.size - 1) }

    Column(Modifier.fillMaxSize().background(p.bg)) {
        TopBack(peerName, onBack)
        LazyColumn(Modifier.weight(1f).padding(horizontal = 12.dp), state = listState, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(msgs, key = { it.id }) { m ->
                val mine = m.senderId == me
                Row(Modifier.fillMaxWidth(), horizontalArrangement = if (mine) Arrangement.Start else Arrangement.End) {
                    Box(
                        Modifier.widthIn(max = 280.dp).clip(RoundedCornerShape(18.dp))
                            .background(if (mine) Primary else p.bg2).padding(horizontal = 14.dp, vertical = 10.dp),
                    ) { Text(m.text, color = if (mine) androidx.compose.ui.graphics.Color.White else p.ink, style = MaterialTheme.typography.bodyLarge) }
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(10.dp).navigationBarsPadding().imePadding(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.weight(1f).clip(RoundedCornerShape(22.dp)).background(p.bg2).padding(horizontal = 16.dp, vertical = 10.dp)) {
                BasicTextField(
                    text, { text = it }, Modifier.fillMaxWidth(), singleLine = false,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = p.ink), cursorBrush = SolidColor(Primary),
                    decorationBox = { inner -> if (text.isEmpty()) Text("اكتب رسالة...", color = p.faint, style = MaterialTheme.typography.bodyLarge); inner() },
                )
            }
            Spacer(Modifier.width(8.dp))
            IconBtn(Icons.AutoMirrored.Outlined.Send, "إرسال", tint = Primary) {
                val t = text.trim()
                if (t.isNotEmpty()) { Cloud.sendMessage(peerUid, t); text = "" }
            }
        }
    }
}
