package iq.quiz.nav

import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import iq.quiz.data.CallSignal
import iq.quiz.data.Cloud
import iq.quiz.data.Repo
import iq.quiz.ui.screens.*

private fun callRoute(callId: String, peerUid: String, peerName: String, isCaller: Boolean, video: Boolean) =
    "realcall/$callId/$peerUid/${android.net.Uri.encode(peerName)}/$isCaller/$video"

@Composable
fun AppNav() {
    val nav = rememberNavController()
    val start = if (Repo.loggedIn) "main" else "splash"
    var lastIncomingId by remember { mutableStateOf<String?>(null) }
    DisposableEffect(Repo.loggedIn) {
        val uid = Cloud.uid
        val listener = if (Repo.loggedIn) {
            CallSignal.listenIncoming { incoming ->
                if (incoming != null && incoming.id != lastIncomingId) {
                    lastIncomingId = incoming.id
                    nav.navigate(callRoute(incoming.id, incoming.callerId, "...", isCaller = false, video = incoming.video))
                }
            }
        } else null
        onDispose { if (uid != null && listener != null) CallSignal.stopListeningIncoming(uid, listener) }
    }
    val ease = tween<Float>(320, easing = FastOutSlowInEasing)
    val slide = tween<androidx.compose.ui.unit.IntOffset>(320, easing = FastOutSlowInEasing)
    val actions = Nav(
        story = { nav.navigate("story/$it") }, chat = { nav.navigate("chat/$it") }, user = { nav.navigate("user/$it") },
        create = { nav.navigate("create") }, notifs = { nav.navigate("notifs") }, inbox = { nav.navigate("inbox") },
        settings = { nav.navigate("settings") }, edit = { nav.navigate("edit") },
        myStory = { nav.navigate("mystory") }, archive = { nav.navigate("archive") },
    )
    NavHost(
        nav, startDestination = start,
        enterTransition = { slideInHorizontally(slide) { -it / 3 } + fadeIn(ease) },
        exitTransition = { slideOutHorizontally(slide) { it / 6 } + fadeOut(ease) },
        popEnterTransition = { slideInHorizontally(slide) { it / 6 } + fadeIn(ease) },
        popExitTransition = { slideOutHorizontally(slide) { -it / 3 } + fadeOut(ease) },
    ) {
        composable("splash", enterTransition = { fadeIn() }, exitTransition = { fadeOut() }) {
            SplashScreen { nav.navigate("onboarding") { popUpTo("splash") { inclusive = true } } }
        }
        composable("onboarding") {
            OnboardingScreen(onFinish = { nav.navigate("signup") }, onLogin = { nav.navigate("login") })
        }
        composable("login") {
            LoginScreen(onDone = { nav.navigate("main") { popUpTo(0) { inclusive = true } } }, onSignup = { nav.navigate("signup") }, onForgot = { nav.navigate("forgot") })
        }
        composable("forgot") { ForgotScreen { nav.popBackStack() } }
        composable("signup") {
            SignupScreen(onBack = { nav.popBackStack() }, onDone = { nav.navigate("main") { popUpTo(0) { inclusive = true } } })
        }
        composable("main") { MainScreen(actions) }
        composable("story/{i}", arguments = listOf(navArgument("i") { type = NavType.IntType })) { e ->
            StoryScreen(e.arguments?.getInt("i") ?: 0) { nav.popBackStack() }
        }
        composable("create") { CreatePostScreen { nav.popBackStack() } }
        composable("notifs") { NotificationsScreen { nav.popBackStack() } }
        composable("inbox") { InboxScreen(onBack = { nav.popBackStack() }, onChat = { nav.navigate("chat/$it") }) }
        composable("chat/{id}", arguments = listOf(navArgument("id") { type = NavType.IntType })) { e ->
            val id = e.arguments?.getInt("id") ?: 0
            ChatScreen(id, onBack = { nav.popBackStack() }, onCall = { v -> nav.navigate("call/$id/$v") }, onUser = {
                Repo.chats.firstOrNull { it.id == id }?.let { nav.navigate("user/${it.userId}") }
            })
        }
        composable("call/{id}/{video}", arguments = listOf(navArgument("id") { type = NavType.IntType }, navArgument("video") { type = NavType.BoolType })) { e ->
            CallScreen(e.arguments?.getInt("id") ?: 0, e.arguments?.getBoolean("video") ?: false) { nav.popBackStack() }
        }
        composable("user/{id}", arguments = listOf(navArgument("id") { type = NavType.IntType })) { e ->
            ProfileScreen(e.arguments?.getInt("id") ?: 1, actions, onBack = { nav.popBackStack() })
        }
        composable("mystory") { MyStoryScreen(onClose = { nav.popBackStack() }, onAdd = { nav.popBackStack(); nav.navigate("create") }) }
        composable("archive") { ArchiveScreen { nav.popBackStack() } }
        composable("saved") { SavedScreen(actions) }
        composable("activity") { NotificationsScreen { nav.popBackStack() } }
        composable("edit") { EditProfileScreen { nav.popBackStack() } }
        composable("settings") {
            SettingsScreen(onBack = { nav.popBackStack() }, onLogout = { nav.navigate("login") { popUpTo(0) { inclusive = true } } }, onOpen = { r -> nav.navigate(r) })
        }
        composable("realinbox") {
            RealInboxScreen(onBack = { nav.popBackStack() }, onOpenChat = { peerUid, name -> nav.navigate("realchat/$peerUid/${android.net.Uri.encode(name)}") })
        }
        composable(
            "realchat/{uid}/{name}",
            arguments = listOf(navArgument("uid") { type = NavType.StringType }, navArgument("name") { type = NavType.StringType }),
        ) { e ->
            val peerUid = e.arguments?.getString("uid") ?: ""
            val peerName = android.net.Uri.decode(e.arguments?.getString("name") ?: "...")
            RealChatScreen(
                peerUid = peerUid,
                peerNameArg = peerName,
                onBack = { nav.popBackStack() },
                onCall = { pid, video ->
                    val id = CallSignal.startCall(pid, video)
                    if (id != null) nav.navigate(callRoute(id, pid, peerName, isCaller = true, video = video))
                },
            )
        }
        composable(
            "realcall/{callId}/{uid}/{name}/{isCaller}/{video}",
            arguments = listOf(
                navArgument("callId") { type = NavType.StringType },
                navArgument("uid") { type = NavType.StringType },
                navArgument("name") { type = NavType.StringType },
                navArgument("isCaller") { type = NavType.BoolType },
                navArgument("video") { type = NavType.BoolType },
            ),
        ) { e ->
            RealCallScreen(
                callId = e.arguments?.getString("callId") ?: "",
                peerUid = e.arguments?.getString("uid") ?: "",
                peerNameArg = android.net.Uri.decode(e.arguments?.getString("name") ?: "..."),
                isCaller = e.arguments?.getBoolean("isCaller") ?: true,
                video = e.arguments?.getBoolean("video") ?: false,
                onEnd = { lastIncomingId = null; nav.popBackStack() },
            )
        }
    }
}
