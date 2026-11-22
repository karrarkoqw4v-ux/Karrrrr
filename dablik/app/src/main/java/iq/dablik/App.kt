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

data class Ad(val title: String, val text: String, val url: String, val popup: Boolean)

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
    // قياسات الأجهزة وخيارات المطور
    var autoFit by mutableStateOf(true)
    var fixText by mutableStateOf(true)
    var zoom by mutableStateOf(false)
    var doubleBack by mutableStateOf(false)
    var offline by mutableStateOf(true)
    var debug by mutableStateOf(false)
    var external by mutableStateOf(false)
    var splash by mutableStateOf(false)
    var statusColor by mutableStateOf("")     // "" تلقائي
    // تسجيل الدخول المتصل
    var loginMode by mutableIntStateOf(0)     // 0 محلي، 1 Firebase، 2 API خاص
    var fbKey by mutableStateOf("")
    var loginUrl by mutableStateOf("")
    var registerUrl by mutableStateOf("")
    // الإعلانات (تجربة 30 يوماً)
    var adsOn by mutableStateOf(false)
    val ads = mutableStateListOf<Ad>()
    // النشر عن بعد عبر GitHub
    var remoteVer by mutableIntStateOf(0)
    var ghRepo by mutableStateOf("")
    var ghBranch by mutableStateOf("main")

    fun toJson(): org.json.JSONObject = org.json.JSONObject()
        .put("name", name).put("pkg", pkg).put("version", version).put("perms", org.json.JSONArray(perms.toList()))
        .put("update", update).put("updateUrl", updateUrl).put("login", login).put("bridge", bridge).put("askPerms", askPerms)
        .put("fullscreen", fullscreen).put("keepAwake", keepAwake).put("orientation", orientation).put("autoFit", autoFit).put("fixText", fixText)
        .put("zoom", zoom).put("doubleBack", doubleBack).put("offline", offline).put("debug", debug).put("external", external).put("splash", splash)
        .put("statusColor", statusColor).put("loginMode", loginMode).put("fbKey", fbKey).put("loginUrl", loginUrl).put("registerUrl", registerUrl)
        .put("adsOn", adsOn).put("remoteVer", remoteVer).put("ghRepo", ghRepo).put("ghBranch", ghBranch)
        .put("ads", org.json.JSONArray().also { arr -> ads.forEach { arr.put(org.json.JSONObject().put("title", it.title).put("text", it.text).put("url", it.url).put("popup", it.popup)) } })

    fun fromJson(j: org.json.JSONObject, icon: String?) {
        name = j.optString("name"); pkg = j.optString("pkg", pkg); version = j.optString("version", version); iconPath = icon
        perms.clear(); j.optJSONArray("perms")?.let { for (i in 0 until it.length()) perms.add(it.getString(i)) }
        update = j.optBoolean("update"); updateUrl = j.optString("updateUrl"); login = j.optBoolean("login"); bridge = j.optBoolean("bridge", true)
        askPerms = j.optBoolean("askPerms", true); fullscreen = j.optBoolean("fullscreen"); keepAwake = j.optBoolean("keepAwake"); orientation = j.optInt("orientation")
        autoFit = j.optBoolean("autoFit", true); fixText = j.optBoolean("fixText", true); zoom = j.optBoolean("zoom"); doubleBack = j.optBoolean("doubleBack")
        offline = j.optBoolean("offline", true); debug = j.optBoolean("debug"); external = j.optBoolean("external"); splash = j.optBoolean("splash")
        statusColor = j.optString("statusColor"); loginMode = j.optInt("loginMode"); fbKey = j.optString("fbKey"); loginUrl = j.optString("loginUrl"); registerUrl = j.optString("registerUrl")
        adsOn = j.optBoolean("adsOn"); remoteVer = j.optInt("remoteVer"); ghRepo = j.optString("ghRepo"); ghBranch = j.optString("ghBranch", "main")
        ads.clear(); j.optJSONArray("ads")?.let { for (i in 0 until it.length()) { val o = it.getJSONObject(i); ads.add(Ad(o.optString("title"), o.optString("text"), o.optString("url"), o.optBoolean("popup"))) } }
        rev++
    }

    fun configJson(): String = org.json.JSONObject()
        .put("name", name).put("login", login).put("update", update).put("updateUrl", updateUrl.trim())
        .put("bridge", bridge).put("askPerms", askPerms).put("fullscreen", fullscreen).put("keepAwake", keepAwake)
        .put("orientation", orientation).put("textZoom", fixText).put("zoom", zoom).put("doubleBack", doubleBack)
        .put("offlinePage", offline).put("debug", debug).put("external", external)
        .put("loginMode", loginMode).put("fbKey", fbKey.trim()).put("loginUrl", loginUrl.trim()).put("registerUrl", registerUrl.trim())
        .put("adsEnabled", adsOn).put("adsUntil", System.currentTimeMillis() + 30L * 24 * 3600 * 1000)
        .put("ads", org.json.JSONArray().also { arr -> ads.forEach { arr.put(org.json.JSONObject().put("title", it.title).put("text", it.text).put("url", it.url).put("popup", it.popup)) } })
        .put("statusBar", statusColor).put("navBar", statusColor).put("splash", splash).put("splashColor", statusColor).put("splashMs", 1200)
        .toString()
}

sealed class Screen(val depth: Int) {
    data object Home : Screen(0)
    data object Form : Screen(1)
    data object About : Screen(1)
    data object Assets : Screen(2)
    data object Console : Screen(1)
    class Editor(val uri: Uri) : Screen(2)
    data object Console : Screen(2)
    data object Perms : Screen(3)
    data object Files : Screen(3)
    class FileEdit(val path: String) : Screen(4)
    class Preview(val path: String) : Screen(4)
}

@Composable
fun DablikApp() {
    val ctx = androidx.compose.ui.platform.LocalContext.current
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
            Screen.Home -> HomeScreen(onApk = { screen = Screen.Form }, onZip = { screen = Screen.Assets }, onAbout = { screen = Screen.About },
                onEditApp = { b -> Store.loadProject(ctx, b.pkg, form); screen = Screen.Form }, onConsole = { screen = Screen.Console })
            Screen.Console -> ConsoleScreenHome(onBack = { screen = Screen.Home })
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
