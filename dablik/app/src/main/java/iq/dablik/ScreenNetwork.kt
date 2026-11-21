package iq.dablik

import android.Manifest
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat

private fun ipToString(ip: Int): String =
    "${ip and 0xFF}.${(ip shr 8) and 0xFF}.${(ip shr 16) and 0xFF}.${(ip shr 24) and 0xFF}"

private val ouiTable = mapOf(
    "EC086B" to "TP-Link", "F4F26D" to "TP-Link", "A42BB0" to "TP-Link", "50C7BF" to "TP-Link",
    "001E42" to "Netgear", "204E7F" to "Netgear", "E0469A" to "Netgear",
    "F8A2D6" to "Huawei", "A4919C" to "Huawei", "48DAF4" to "Huawei", "00E0FC" to "Huawei",
    "D4A1D1" to "Xiaomi/Mi", "7C49EB" to "Xiaomi/Mi", "28E31F" to "Xiaomi/Mi",
    "B0486A" to "ASUS", "1C872C" to "ASUS", "04D4C4" to "ASUS",
    "001BFC" to "D-Link", "14D64D" to "D-Link", "C4A81D" to "D-Link",
    "649EF3" to "MikroTik", "4C5E0C" to "MikroTik", "6C3B6B" to "MikroTik",
    "000FB5" to "ZTE", "ACE87B" to "ZTE", "CC1FC6" to "ZTE",
    "002722" to "Linksys", "48F8B3" to "Linksys",
)
private fun vendorFromMac(mac: String?): String? {
    val oui = mac?.replace(":", "")?.uppercase()?.take(6) ?: return null
    return ouiTable[oui]
}

private class NetInfo(val ssid: String?, val gateway: String?, val bssid: String?, val isWifi: Boolean)

@Suppress("DEPRECATION")
private fun readNetwork(ctx: android.content.Context): NetInfo {
    val cm = ctx.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val caps = cm.activeNetwork?.let { cm.getNetworkCapabilities(it) }
    val isWifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
    if (!isWifi) return NetInfo(null, null, null, false)
    val wm = ctx.applicationContext.getSystemService(android.content.Context.WIFI_SERVICE) as WifiManager
    val info = wm.connectionInfo
    val dhcp = wm.dhcpInfo
    val gw = if (dhcp != null && dhcp.gateway != 0) ipToString(dhcp.gateway) else null
    val ssid = info?.ssid?.removeSurrounding("\"")
    return NetInfo(ssid, gw, info?.bssid, true)
}

@Composable
private fun InfoCard(net: NetInfo) {
    val vendor = remember(net.bssid) { vendorFromMac(net.bssid) }
    val knownHome = net.gateway != null && (net.gateway == "192.168.0.1" || net.gateway == "192.168.1.1" || net.gateway.startsWith("192.168.100.") || net.gateway == "192.168.1.254")
    OneCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("اسم الشبكة (SSID)", style = MaterialTheme.typography.labelSmall, color = OneUi.Muted)
            Text(net.ssid ?: "غير معروف", style = MaterialTheme.typography.titleMedium, color = OneUi.Ink)
            Spacer(Modifier.height(10.dp))
            Text("عنوان الراوتر (Gateway)", style = MaterialTheme.typography.labelSmall, color = OneUi.Muted)
            Text(net.gateway ?: "غير معروف", style = MaterialTheme.typography.titleMedium, color = OneUi.Ink)
            Spacer(Modifier.height(10.dp))
            Text("نوع الشبكة", style = MaterialTheme.typography.labelSmall, color = OneUi.Muted)
            Text(if (knownHome) "شبكة منزلية/محلية معروفة" else "شبكة غير معرّفة (تأكد أنها شبكتك)", style = MaterialTheme.typography.bodyLarge, color = if (knownHome) OneUi.Green else Color(0xFFFFB020))
            if (vendor != null) {
                Spacer(Modifier.height(10.dp))
                Text("الشركة المصنّعة المحتملة", style = MaterialTheme.typography.labelSmall, color = OneUi.Muted)
                Text("$vendor  (تخمين من عنوان الجهاز، غير مؤكد 100٪)", style = MaterialTheme.typography.bodyLarge, color = OneUi.Ink)
            }
        }
    }
}

/** شاشة مستقلة في دبليك: تكتشف شبكة الواي فاي الحالية وتفتح لوحة إدارة الراوتر الحقيقية */
@Composable
fun NetworkScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    var granted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
    }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    LaunchedEffect(Unit) { if (!granted) ask.launch(Manifest.permission.ACCESS_FINE_LOCATION) }
    var tick by remember { mutableIntStateOf(0) }
    val net = remember(granted, tick) { if (granted) readNetwork(ctx) else NetInfo(null, null, null, false) }
    var showPanel by remember { mutableStateOf(false) }

    if (showPanel && net.gateway != null) {
        RouterPanelScreen(net.gateway, onBack = { showPanel = false })
        return
    }

    Box(Modifier.fillMaxSize().background(OneUi.Bg)) {
        Column(Modifier.fillMaxSize()) {
            OneTopBar("التحكم في شبكتي", onBack) {
                Box(Modifier.size(44.dp).oneClick { tick++ }, contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Refresh, "تحديث", tint = OneUi.Ink) }
            }
            Column(Modifier.weight(1f).padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                when {
                    !granted -> Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.LocationOff, null, tint = OneUi.Faint, modifier = Modifier.size(56.dp))
                        Text("نحتاج إذن الموقع لقراءة اسم الشبكة المتصلة (مطلب من أندرويد نفسه)", style = MaterialTheme.typography.bodyMedium, color = OneUi.Muted, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 10.dp))
                        Spacer(Modifier.height(14.dp))
                        OneButton("السماح", { ask.launch(Manifest.permission.ACCESS_FINE_LOCATION) })
                    }
                    !net.isWifi -> Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.WifiOff, null, tint = OneUi.Faint, modifier = Modifier.size(56.dp))
                        Text("جهازك غير متصل بشبكة واي فاي حالياً. اتصل بالراوتر ثم اضغط تحديث.", style = MaterialTheme.typography.bodyMedium, color = OneUi.Muted, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 10.dp))
                    }
                    else -> {
                        InfoCard(net)
                        Text(
                            "سيُفتح موقع الراوتر الحقيقي. سجّل دخولك باسم المستخدم وكلمة السر الخاصين بشبكتك (المطبوعين غالباً على ملصق أسفل الراوتر)، ومن هناك تقدر تغيّر اسم الشبكة وكلمة السر، وتدير الأجهزة المسموح لها بالاتصال.",
                            style = MaterialTheme.typography.labelMedium, color = OneUi.Muted, modifier = Modifier.padding(horizontal = 16.dp),
                        )
                        if (net.gateway != null) Box(Modifier.padding(horizontal = 16.dp)) {
                            OneButton("فتح لوحة الراوتر  ${net.gateway}", { showPanel = true }, icon = Icons.Outlined.Router)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RouterPanelScreen(gateway: String, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val sp = remember { ctx.getSharedPreferences("dablik", android.content.Context.MODE_PRIVATE) }
    var progress by remember { mutableFloatStateOf(0f) }
    Box(Modifier.fillMaxSize().background(OneUi.Bg)) {
        Column(Modifier.fillMaxSize()) {
            OneTopBar("لوحة الراوتر  $gateway", onBack)
            if (progress in 0f..0.999f) LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth(), color = OneUi.Blue, trackColor = OneUi.Card2)
            AndroidView(factory = { c ->
                WebView(c).apply {
                    CookieManager.getInstance().setAcceptCookie(true)
                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                    val s: WebSettings = settings
                    s.javaScriptEnabled = true
                    s.domStorageEnabled = true
                    s.loadWithOverviewMode = true
                    s.useWideViewPort = true
                    s.setSupportZoom(true)
                    s.builtInZoomControls = true
                    s.displayZoomControls = false
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) { if (url != null) sp.edit().putString("last_gateway_$gateway", url).apply() }
                    }
                    webChromeClient = object : android.webkit.WebChromeClient() {
                        override fun onProgressChanged(view: WebView?, newProgress: Int) { progress = newProgress / 100f }
                    }
                    val last = sp.getString("last_gateway_$gateway", null)
                    loadUrl(if (last != null && last.contains(gateway)) last else "http://$gateway/")
                }
            }, modifier = Modifier.weight(1f).fillMaxWidth())
        }
    }
}
