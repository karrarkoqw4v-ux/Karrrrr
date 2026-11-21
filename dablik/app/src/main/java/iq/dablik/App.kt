package iq.dablik

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.*

/** حالة نموذج الإنشاء، محفوظة أثناء التنقل بين الشاشات */
class FormState {
    var name by mutableStateOf("")
    var pkg by mutableStateOf("com.example.app")
    var version by mutableStateOf("1.0.0")
    var iconPath by mutableStateOf<String?>(null)
    val perms = mutableStateListOf(*Perms.defaults.toTypedArray())
    var update by mutableStateOf(false)
    var updateUrl by mutableStateOf("")
    var login by mutableStateOf(false)
    var bridge by mutableStateOf(true)
    var askPerms by mutableStateOf(true)
    var fullscreen by mutableStateOf(false)
    var keepAwake by mutableStateOf(false)
    var orientation by mutableIntStateOf(0)   // 0 تلقائي، 1 عمودي، 2 أفقي
    var rev by mutableIntStateOf(0)           // يزيد عند تغيير ملفات المشروع

    fun configJson(): String = org.json.JSONObject()
        .put("name", name).put("login", login).put("update", update).put("updateUrl", updateUrl.trim())
        .put("bridge", bridge).put("askPerms", askPerms).put("fullscreen", fullscreen).put("keepAwake", keepAwake)
        .put("orientation", orientation).toString()
}

sealed class Screen(val depth: Int) {
    data object Home : Screen(0)
    data object Form : Screen(1)
    data object About : Screen(1)
    data object Assets : Screen(2)
    class Editor(val uri: Uri) : Screen(2)
    data object Console : Screen(2)
    data object Perms : Screen(3)
    data object Files : Screen(3)
    class FileEdit(val path: String) : Screen(4)
    class Preview(val path: String) : Screen(4)
}

@Composable
fun DablikApp() {
    var screen by remember { mutableStateOf<Screen>(Screen.Home) }
    val form = remember { FormState() }
    BackHandler(screen !is Screen.Home) {
        screen = when (val c = screen) {
            is Screen.Editor, Screen.Console, Screen.Assets -> Screen.Form
            Screen.Perms, Screen.Files -> Screen.Assets
            is Screen.FileEdit -> Screen.Files
            is Screen.Preview -> if (c.path.isEmpty()) Screen.Files else Screen.FileEdit(c.path)
            else -> Screen.Home
        }
    }
    val ease = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    AnimatedContent(
        screen,
        transitionSpec = {
            val fwd = targetState.depth > initialState.depth
            if (fwd) (slideInHorizontally(tween(420, easing = ease)) { -it / 3 } + fadeIn(tween(300))) togetherWith (slideOutHorizontally(tween(420, easing = ease)) { it / 6 } + fadeOut(tween(200)))
            else (slideInHorizontally(tween(420, easing = ease)) { it / 6 } + fadeIn(tween(300))) togetherWith (slideOutHorizontally(tween(420, easing = ease)) { -it / 3 } + fadeOut(tween(200)))
        },
        label = "nav",
    ) { s ->
        when (s) {
            Screen.Home -> HomeScreen(onApk = { screen = Screen.Form }, onZip = { screen = Screen.Assets }, onAbout = { screen = Screen.About })
            Screen.About -> AboutScreen(onBack = { screen = Screen.Home })
            Screen.Form -> FormScreen(form, onBack = { screen = Screen.Home }, onEdit = { screen = Screen.Editor(it) }, onAssets = { screen = Screen.Assets }, onBuild = { screen = Screen.Console })
            Screen.Assets -> AssetsScreen(form, onBack = { screen = Screen.Form }, onPerms = { screen = Screen.Perms }, onFiles = { screen = Screen.Files })
            Screen.Perms -> PermsScreen(form, onBack = { screen = Screen.Assets })
            Screen.Files -> FilesScreen(form, onBack = { screen = Screen.Assets }, onOpen = { screen = Screen.FileEdit(it) }, onRun = { screen = Screen.Preview("") })
            is Screen.FileEdit -> FileEditScreen(form, s.path, onBack = { screen = Screen.Files }, onRun = { screen = Screen.Preview(s.path) })
            is Screen.Preview -> PreviewScreen(s.path, onBack = { screen = if (s.path.isEmpty()) Screen.Files else Screen.FileEdit(s.path) })
            is Screen.Editor -> IconEditorScreen(s.uri, onClose = { screen = Screen.Form }, onSaved = { form.iconPath = it; screen = Screen.Form })
            Screen.Console -> ConsoleScreen(form, onBack = { screen = Screen.Form }, onHome = { screen = Screen.Home })
        }
    }
}
