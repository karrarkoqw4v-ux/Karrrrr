package iq.student.nav

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import iq.student.ui.screens.HomeScreen
import iq.student.ui.screens.LoginScreen
import iq.student.ui.screens.StageScreen
import iq.student.ui.screens.ToolScreen

object Routes {
    const val Login = "login"
    const val Stage = "stage"
    const val Home = "home"
    const val Tool = "tool/{id}"
    fun tool(id: String) = "tool/$id"
}

@Composable
fun AppNav() {
    val nav = rememberNavController()
    NavHost(nav, startDestination = Routes.Login) {
        composable(Routes.Login) {
            LoginScreen(
                onLogin = { nav.navigate(Routes.Home) { popUpTo(Routes.Login) { inclusive = true } } },
                onCreateAccount = { nav.navigate(Routes.Stage) },
            )
        }
        composable(Routes.Stage) {
            StageScreen(
                onBack = { nav.popBackStack() },
                onContinue = { nav.navigate(Routes.Home) { popUpTo(Routes.Login) { inclusive = true } } },
            )
        }
        composable(Routes.Home) {
            HomeScreen(
                onTool = { nav.navigate(Routes.tool(it)) },
                onTab = { if (it != 0) nav.navigate(Routes.tool(listOf("", "tools", "assistant", "progress", "account")[it])) },
            )
        }
        composable(Routes.Tool) { entry ->
            ToolScreen(entry.arguments?.getString("id").orEmpty(), onBack = { nav.popBackStack() })
        }
    }
}
