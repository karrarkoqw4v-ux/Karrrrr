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

/** حالة نموذج الإنشاء، محفوظة أثناء فتح محرر الصور */
class FormState {
    var mode by mutableIntStateOf(0)          // 0: إنشاء APK (HTML)، 1: ZIP
    var name by mutableStateOf("")
    var pkg by mutableStateOf("com.example.app")
    var version by mutableStateOf("1.0.0")
    var iconPath by mutableStateOf<String?>(null)
    var htmlCode by mutableStateOf("")
    var htmlFile by mutableStateOf<Uri?>(null)
    var htmlFileName by mutableStateOf("")
    var zip by mutableStateOf<Uri?>(null)
    var zipName by mutableStateOf("")
}

sealed class Screen(val depth: Int) {
    data object Home : Screen(0)
    data object Form : Screen(1)
    class Editor(val uri: Uri) : Screen(2)
    data object Console : Screen(2)
    data object About : Screen(1)
}

@Composable
fun DablikApp() {
    var screen by remember { mutableStateOf<Screen>(Screen.Home) }
    val form = remember { FormState() }
    BackHandler(screen !is Screen.Home) {
        screen = when (screen) { is Screen.Editor, Screen.Console -> Screen.Form; else -> Screen.Home }
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
            Screen.Home -> HomeScreen(onApk = { form.mode = 0; screen = Screen.Form }, onZip = { form.mode = 1; screen = Screen.Form }, onAbout = { screen = Screen.About })
            Screen.About -> AboutScreen(onBack = { screen = Screen.Home })
            Screen.Form -> FormScreen(form, onBack = { screen = Screen.Home }, onEdit = { screen = Screen.Editor(it) }, onBuild = { screen = Screen.Console })
            is Screen.Editor -> IconEditorScreen(s.uri, onClose = { screen = Screen.Form }, onSaved = { form.iconPath = it; screen = Screen.Form })
            Screen.Console -> ConsoleScreen(form, onBack = { screen = Screen.Form }, onHome = { screen = Screen.Home })
        }
    }
}
