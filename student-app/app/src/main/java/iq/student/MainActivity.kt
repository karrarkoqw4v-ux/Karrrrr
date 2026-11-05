package iq.student

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import iq.student.data.Prefs
import iq.student.nav.AppNav
import iq.student.ui.theme.StudentTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Prefs.init(this)
        enableEdgeToEdge()
        setContent {
            StudentTheme {
                AppNav()
            }
        }
    }
}
