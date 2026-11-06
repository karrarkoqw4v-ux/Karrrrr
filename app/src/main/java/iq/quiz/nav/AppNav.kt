package iq.quiz.nav

import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import iq.quiz.data.Repo
import iq.quiz.ui.screens.*

@Composable
fun AppNav() {
    val nav = rememberNavController()
    val start = if (Repo.loggedIn) "main" else "splash"
    val ease = tween<Float>(320, easing = FastOutSlowInEasing)
    val slide = tween<androidx.compose.ui.unit.IntOffset>(320, easing = FastOutSlowInEasing)
    val actions = Nav(
        story = { nav.navigate("story/$it") }, chat = { nav.navigate("chat/$it") }, user = { nav.navigate("user/$it") },
        create = { nav.navigate("create") }, notifs = { nav.navigate("notifs") }, inbox = { nav.navigate("inbox") },
        settings = { nav.navigate("settings") }, edit = { nav.navigate("edit") },
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
        composable("edit") { EditProfileScreen { nav.popBackStack() } }
        composable("settings") {
            SettingsScreen(onBack = { nav.popBackStack() }, onLogout = { nav.navigate("login") { popUpTo(0) { inclusive = true } } })
        }
    }
}
