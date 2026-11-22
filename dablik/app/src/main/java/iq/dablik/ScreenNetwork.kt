package iq.dablik

import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

fun ipToString(ip: Int): String =
    "${ip and 0xFF}.${(ip shr 8) and 0xFF}.${(ip shr 16) and 0xFF}.${(ip shr 24) and 0xFF}"

val ouiTable = mapOf(
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
fun vendorFromMac(mac: String?): String? {
    val oui = mac?.replace(":", "")?.uppercase()?.take(6) ?: return null
    return ouiTable[oui]
}

class NetInfo(val ssid: String?, val gateway: String?, val bssid: String?, val isWifi: Boolean)

@Suppress("DEPRECATION")
fun readNetwork(ctx: android.content.Context): NetInfo {
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
fun InfoCard(net: NetInfo) {
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

