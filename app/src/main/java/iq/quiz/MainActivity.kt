package iq.quiz

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import iq.quiz.data.Repo
import iq.quiz.nav.AppNav
import iq.quiz.ui.theme.AppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Repo.init(this)
        enableEdgeToEdge()
        setContent {
            AppTheme(dark = Repo.dark) { AppNav() }
        }
    }
}
