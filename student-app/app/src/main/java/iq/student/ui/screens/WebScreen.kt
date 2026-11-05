package iq.student.ui.screens

import android.annotation.SuppressLint
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import iq.student.ui.components.ScreenHeader
import iq.student.ui.theme.AppColors

/**
 * متصفح داخل التطبيق لموقع الملازم: يدعم التكبير والتصغير بالإصبعين،
 * وأي رابط PDF يُفتح مباشرة بقارئ التطبيق (بتكبير وتمرير سلس).
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebScreen(url: String, title: String, onBack: () -> Unit, onPdf: (String) -> Unit) {
    var web by remember { mutableStateOf<WebView?>(null) }
    var progress by remember { mutableIntStateOf(0) }
    BackHandler { if (web?.canGoBack() == true) web?.goBack() else onBack() }

    Column(Modifier.fillMaxSize().background(AppColors.Surface)) {
        ScreenHeader(title, onBack)
        if (progress in 1..99) LinearProgressIndicator(progress = { progress / 100f }, modifier = Modifier.fillMaxWidth(), color = AppColors.Primary)
        AndroidView(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.setSupportZoom(true)
                    settings.builtInZoomControls = true
                    settings.displayZoomControls = false
                    settings.loadWithOverviewMode = true
                    settings.useWideViewPort = true
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                            val u = request.url.toString()
                            if (u.substringBefore('?').endsWith(".pdf", ignoreCase = true)) { onPdf(u); return true }
                            return false
                        }
                    }
                    webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView?, newProgress: Int) { progress = newProgress }
                    }
                    setDownloadListener { u, _, _, mime, _ ->
                        if (mime.contains("pdf", true) || u.substringBefore('?').endsWith(".pdf", true)) onPdf(u)
                    }
                    loadUrl(url)
                    web = this
                }
            },
            onRelease = { it.destroy() },
        )
    }
}
