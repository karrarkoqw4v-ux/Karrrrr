package iq.hala

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import iq.hala.data.Repo
import iq.hala.nav.AppNav
import iq.hala.ui.theme.HalaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Repo.init(this)
        enableEdgeToEdge()
        setContent {
            HalaTheme(dark = Repo.dark) { AppNav() }
        }
    }
}
