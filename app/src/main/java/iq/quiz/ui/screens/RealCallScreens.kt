package iq.quiz.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import iq.quiz.data.CallSignal
import iq.quiz.data.Cloud
import iq.quiz.data.RealCall
import iq.quiz.ui.components.bounceClick
import iq.quiz.ui.theme.Danger
import iq.quiz.ui.theme.Primary
import kotlinx.coroutines.launch
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoTrack

/** شاشة مكالمة صوت/فيديو حقيقية: WebRTC فعلي عبر Realtime Database للتفاوض. */
@Composable
fun RealCallScreen(callId: String, peerUid: String, peerNameArg: String, isCaller: Boolean, video: Boolean, onEnd: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var peerName by remember { mutableStateOf(peerNameArg) }
    var accepted by remember { mutableStateOf(isCaller) }
    var connected by remember { mutableStateOf(false) }
    var ended by remember { mutableStateOf(false) }
    var muted by remember { mutableStateOf(false) }
    var camOn by remember { mutableStateOf(video) }
    var remoteHasVideo by remember { mutableStateOf(false) }
    val me = Cloud.uid

    LaunchedEffect(peerUid) { if (peerName.isBlank() || peerName == "...") Cloud.profile(peerUid)?.let { peerName = it.name.ifBlank { it.username } } }

    val localView = remember { SurfaceViewRenderer(ctx) }
    val remoteView = remember { SurfaceViewRenderer(ctx) }
    var call by remember { mutableStateOf<RealCall?>(null) }

    fun endNow() {
        if (ended) return
        ended = true
        me?.let { CallSignal.endCall(callId, it) }
        call?.end()
        onEnd()
    }

    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        if (granted.values.all { it }) {
            val c = RealCall(
                ctx, callId, isCaller, video,
                onRemoteVideo = { t: VideoTrack? -> remoteHasVideo = t != null },
                onState = { s -> if (s == "connected") connected = true else if (s == "ended") endNow() },
            )
            c.start(if (video) localView else null, remoteView)
            call = c
        }
    }

    LaunchedEffect(accepted) {
        if (accepted) {
            val perms = if (video) arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA) else arrayOf(Manifest.permission.RECORD_AUDIO)
            permLauncher.launch(perms)
        }
    }

    DisposableEffect(callId) {
        val l = CallSignal.listenStatus(callId) { s -> if (s == "ended") endNow() }
        onDispose {
            CallSignal.stopListeningStatus(callId, l)
            call?.end()
            localView.release(); remoteView.release()
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (video && connected) AndroidView(factory = { remoteView }, modifier = Modifier.fillMaxSize())
        if (video && accepted) {
            AndroidView(
                factory = { localView },
                modifier = Modifier.size(110.dp, 150.dp).align(Alignment.TopEnd).padding(16.dp).clip(RoundedCornerShape(16.dp)),
            )
        }
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(60.dp))
            if (!video || !connected) {
                Box(Modifier.size(120.dp).clip(CircleShape).background(Color(0xFF2A2E38)), contentAlignment = Alignment.Center) {
                    Text(peerName.take(1), color = Primary, style = MaterialTheme.typography.displaySmall)
                }
                Spacer(Modifier.height(20.dp))
            }
            Text(peerName, style = MaterialTheme.typography.headlineSmall, color = Color.White)
            Text(
                when {
                    !accepted -> if (video) "اتصال فيديو وارد..." else "اتصال وارد..."
                    connected -> "متصل"
                    isCaller -> "يرن..."
                    else -> "جاري الاتصال..."
                },
                style = MaterialTheme.typography.bodyLarge, color = Color(0xFFD6F5F0),
            )
            Spacer(Modifier.weight(1f))
            if (!accepted) {
                Row(horizontalArrangement = Arrangement.spacedBy(40.dp)) {
                    Box(
                        Modifier.size(70.dp).bounceClick { endNow() }.clip(CircleShape).background(Danger),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Outlined.CallEnd, "رفض", tint = Color.White, modifier = Modifier.size(32.dp)) }
                    Box(
                        Modifier.size(70.dp).bounceClick { scope.launch { CallSignal.acceptCall(callId); accepted = true } }.clip(CircleShape).background(Color(0xFF34C759)),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Outlined.Call, "قبول", tint = Color.White, modifier = Modifier.size(30.dp)) }
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    CallIconBtn(if (muted) Icons.Outlined.MicOff else Icons.Outlined.Mic, muted) { muted = !muted; call?.setMuted(muted) }
                    if (video) CallIconBtn(if (camOn) Icons.Outlined.Videocam else Icons.Outlined.VideocamOff, !camOn) { camOn = !camOn; call?.setVideoEnabled(camOn) }
                    if (video) CallIconBtn(Icons.Outlined.Cameraswitch, false) { call?.switchCamera() }
                }
                Spacer(Modifier.height(26.dp))
                Box(Modifier.size(70.dp).bounceClick { endNow() }.clip(CircleShape).background(Danger), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.CallEnd, "إنهاء", tint = Color.White, modifier = Modifier.size(32.dp))
                }
            }
        }
    }
}

@Composable
private fun CallIconBtn(icon: androidx.compose.ui.graphics.vector.ImageVector, active: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(56.dp).bounceClick(onClick = onClick).clip(CircleShape).background(if (active) Color.White else Color(0x40FFFFFF)),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = if (active) Color.Black else Color.White, modifier = Modifier.size(24.dp)) }
}
