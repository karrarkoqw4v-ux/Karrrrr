package iq.student.nav

import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import iq.student.data.Catalog
import iq.student.data.Prefs
import iq.student.data.StudyTask
import iq.student.ui.screens.*

object Route {
    const val Login = "login"
    const val Signup = "signup"
    const val Grades = "grades"
    const val Home = "home"
    const val Subject = "subject/{id}"
    const val Scan = "scan/{task}"
    const val Crop = "crop/{task}"
    const val Result = "result/{task}"
    const val Typed = "typed/{task}"
    const val Library = "library"
    const val Pdf = "pdf/{key}"
    const val PdfUrl = "pdfurl?u={u}"
    const val Ministry = "ministry"
    const val MinistrySubject = "ministry/{subject}"
    const val Qa = "qa/{subject}/{year}/{round}/{index}"
    const val Handout = "handout/{id}"
    const val Web = "web"
    const val Settings = "settings"
    const val Tab = "tab/{name}"
}

private const val SITE = "https://mlazemna.com/"

private fun task(s: String?) = StudyTask.entries.firstOrNull { it.name == s } ?: StudyTask.ReadPage

private fun scanTitle(t: StudyTask) = when (t) {
    StudyTask.TranslateText -> "ترجمة النصوص"
    StudyTask.TranslatePronounce -> "ترجمة مع النطق"
    StudyTask.MathSimple -> "حل مسائل بالصورة"
    StudyTask.MathExplain -> "حل وشرح المسألة"
    StudyTask.ScienceSolve -> "حل المسائل"
    StudyTask.SciencePage -> "مسح الصفحة"
    StudyTask.PageAnswers -> "أسئلة الصفحة"
    else -> "قراءة الصفحة"
}

private val ease = tween<Float>(340, easing = FastOutSlowInEasing)

@Composable
fun AppNav() {
    val nav = rememberNavController()
    val start = when {
        Prefs.currentEmail == null -> Route.Login
        Prefs.gradeId == null -> Route.Grades
        else -> Route.Home
    }
    NavHost(
        nav, startDestination = start,
        // انتقال سلس: الشاشة الجديدة تنزلق من اليسار (اتجاه RTL) مع تلاشي، والقديمة تتراجع قليلاً
        enterTransition = { slideInHorizontally(tween(340, easing = FastOutSlowInEasing)) { -it / 3 } + fadeIn(ease) + scaleIn(ease, initialScale = 0.96f) },
        exitTransition = { slideOutHorizontally(tween(340, easing = FastOutSlowInEasing)) { it / 6 } + fadeOut(ease) },
        popEnterTransition = { slideInHorizontally(tween(340, easing = FastOutSlowInEasing)) { it / 6 } + fadeIn(ease) },
        popExitTransition = { slideOutHorizontally(tween(340, easing = FastOutSlowInEasing)) { -it / 3 } + fadeOut(ease) + scaleOut(ease, targetScale = 0.96f) },
    ) {
        composable(Route.Login) {
            LoginScreen(
                onDone = { needsGrade -> nav.navigate(if (needsGrade) Route.Grades else Route.Home) { popUpTo(Route.Login) { inclusive = true } } },
                onCreateAccount = { nav.navigate(Route.Signup) },
            )
        }
        composable(Route.Signup) {
            SignupScreen(onBack = { nav.popBackStack() }, onDone = { nav.navigate(Route.Grades) { popUpTo(Route.Login) { inclusive = true } } })
        }
        composable(Route.Grades) {
            GradesScreen(
                onBack = if (nav.previousBackStackEntry != null) ({ nav.popBackStack() }) else null,
                onDone = { nav.navigate(Route.Home) { popUpTo(0) { inclusive = true } } },
            )
        }
        composable(Route.Home) {
            HomeScreen(
                onSubject = { nav.navigate("subject/$it") },
                onMinistry = { nav.navigate(Route.Ministry) },
                onLibrary = { nav.navigate(Route.Library) },
                onChangeGrade = { nav.navigate(Route.Grades) },
                onTab = { tabNav(nav, it) },
            )
        }
        composable(Route.Subject) { e ->
            SubjectScreen(e.arguments?.getString("id").orEmpty(), onBack = { nav.popBackStack() }) { sec ->
                nav.navigate(if (sec.typed) "typed/${sec.task.name}" else "scan/${sec.task.name}")
            }
        }
        composable(Route.Scan) { e ->
            val t = task(e.arguments?.getString("task"))
            CaptureScreen(
                title = scanTitle(t),
                hint = "صوّر الصفحة أو المسألة بوضوح وإضاءة جيدة",
                onBack = { nav.popBackStack() },
                onCaptured = { nav.navigate("crop/${t.name}") },
            )
        }
        composable(Route.Crop) { e ->
            val t = task(e.arguments?.getString("task"))
            CropScreen(onBack = { nav.popBackStack() }, onRetake = { nav.popBackStack() }, onNext = { nav.navigate("result/${t.name}") })
        }
        composable(Route.Result) { e ->
            val t = task(e.arguments?.getString("task"))
            ResultScreen(t, onBack = { nav.popBackStack() }, onAgain = { nav.popBackStack(Route.Scan, inclusive = false) })
        }
        composable(Route.Typed) { e ->
            TypedScreen(task(e.arguments?.getString("task")), onBack = { nav.popBackStack() })
        }
        composable(Route.Library) {
            LibraryScreen(onBack = { nav.popBackStack() }, onOpen = { nav.navigate("pdf/$it") })
        }
        composable(Route.Pdf) { e ->
            val key = e.arguments?.getString("key").orEmpty()
            val title = Catalog.grade(key.substringBefore("_"))?.label.orEmpty() + " - " +
                (Catalog.subject(key.substringBefore("_"), Prefs.branch, key.substringAfter("_"))?.title ?: "كتاب")
            PdfScreen(title, Prefs.bookRef(key), onBack = { nav.popBackStack() })
        }
        composable(Route.PdfUrl, arguments = listOf(navArgument("u") { type = NavType.StringType; defaultValue = "" })) { e ->
            val u = android.net.Uri.decode(e.arguments?.getString("u").orEmpty())
            PdfScreen("الملزمة", if (u.isBlank()) null else "url:$u", onBack = { nav.popBackStack() })
        }
        composable(Route.Ministry) {
            MinistryScreen(
                onBack = { nav.popBackStack() },
                onSubject = { nav.navigate("ministry/$it") },
                onOpenHandout = { nav.navigate("handout/$it") },
                onSite = { nav.navigate(Route.Web) },
            )
        }
        composable(Route.MinistrySubject) { e ->
            val sub = e.arguments?.getString("subject").orEmpty()
            MinistrySubjectScreen(sub, onBack = { nav.popBackStack() }, onQuestion = { y, r, i -> nav.navigate("qa/$sub/$y/$r/$i") })
        }
        composable(
            Route.Qa,
            arguments = listOf(
                navArgument("year") { type = NavType.IntType }, navArgument("round") { type = NavType.IntType },
                navArgument("index") { type = NavType.IntType },
            ),
        ) { e ->
            MinistryQaScreen(
                e.arguments?.getString("subject").orEmpty(), e.arguments?.getInt("year") ?: 2026,
                e.arguments?.getInt("round") ?: 1, e.arguments?.getInt("index") ?: 0, onBack = { nav.popBackStack() },
            )
        }
        composable(Route.Handout) { e ->
            HandoutDetailScreen(e.arguments?.getString("id").orEmpty(), onBack = { nav.popBackStack() },
                onView = { url -> nav.navigate("pdfurl?u=${android.net.Uri.encode(url)}") })
        }
        composable(Route.Web) {
            WebScreen(SITE, "ملازم", onBack = { nav.popBackStack() }, onPdf = { url -> nav.navigate("pdfurl?u=${android.net.Uri.encode(url)}") })
        }
        composable(Route.Settings) {
            SettingsScreen(
                onBack = { nav.popBackStack() },
                onChangeGrade = { nav.navigate(Route.Grades) },
                onLogout = { nav.navigate(Route.Login) { popUpTo(0) { inclusive = true } } },
            )
        }
        composable(Route.Tab) { e -> ToolScreen(e.arguments?.getString("name").orEmpty(), onBack = { nav.popBackStack() }) }
    }
}

private fun tabNav(nav: NavHostController, index: Int) {
    when (index) {
        0 -> {}
        1 -> nav.navigate(Route.Library)
        2 -> nav.navigate(Route.Web)
        3 -> nav.navigate("tab/progress")
        4 -> nav.navigate(Route.Settings)
    }
}
