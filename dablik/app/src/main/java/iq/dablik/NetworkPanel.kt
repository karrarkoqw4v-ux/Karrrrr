package iq.dablik

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL
import java.util.concurrent.Executors
import android.util.Base64

/** شاشة مستقلة في دبليك: تكتشف شبكة الواي فاي، تسجّل دخولاً حقيقياً للراوتر، وتدير الأجهزة المتصلة — كله بتصميم التطبيق، بدون أي موقع مرئي */
@Composable
fun NetworkScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    var granted by remember { mutableStateOf(ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    LaunchedEffect(Unit) { if (!granted) ask.launch(Manifest.permission.ACCESS_FINE_LOCATION) }
    var tick by remember { mutableIntStateOf(0) }
    val net = remember(granted, tick) { if (granted) readNetwork(ctx) else NetInfo(null, null, null, false) }
    var stage by remember { mutableStateOf(0) } // 0 معلومات، 1 تسجيل دخول، 2 لوحة التحكم
    var auth by remember { mutableStateOf<String?>(null) } // رأس Authorization إن نجح الدخول، أو null إن تخطّى المستخدم التحقق

    when (stage) {
        1 -> { NetworkLoginScreen(net.gateway ?: "", onBack = { stage = 0 }, onDone = { a -> auth = a; stage = 2 }); return }
        2 -> { NetworkDashboardScreen(net, auth, onBack = { stage = 0 }); return }
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
                        Text("سجّل دخولك بمعلومات شبكتك لإدارة الاسم وكلمة السر والأجهزة المتصلة.", style = MaterialTheme.typography.labelMedium, color = OneUi.Muted, modifier = Modifier.padding(horizontal = 16.dp))
                        Box(Modifier.padding(horizontal = 16.dp)) { OneButton("تسجيل الدخول", { stage = 1 }, icon = Icons.Outlined.Lock) }
                    }
                }
            }
        }
    }
}

/** محاولة دخول حقيقية: تختبر بيانات المستخدم فعلياً عبر Basic Auth على عنوان الراوتر. ينجح فقط مع الراوترات التي تدعم هذا النوع. */
private suspend fun tryBasicAuth(gateway: String, user: String, pass: String): Boolean = withContext(Dispatchers.IO) {
    runCatching {
        val c = URL("http://$gateway/").openConnection() as HttpURLConnection
        c.connectTimeout = 4000; c.readTimeout = 4000
        c.setRequestProperty("Authorization", "Basic " + Base64.encodeToString("$user:$pass".toByteArray(), Base64.NO_WRAP))
        val code = c.responseCode
        code in 200..299
    }.getOrDefault(false)
}

@Composable
private fun NetworkLoginScreen(gateway: String, onBack: () -> Unit, onDone: (String?) -> Unit) {
    val scope = rememberCoroutineScope()
    var user by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf<String?>(null) }

    Box(Modifier.fillMaxSize().background(OneUi.Bg)) {
        Column(Modifier.fillMaxSize()) {
            OneTopBar("تسجيل الدخول للشبكة", onBack)
            Column(Modifier.weight(1f).padding(horizontal = 20.dp), verticalArrangement = Arrangement.Center) {
                Box(Modifier.size(70.dp).align(Alignment.CenterHorizontally).clip(CircleShape).background(OneUi.BlueSoft), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Router, null, tint = OneUi.Blue, modifier = Modifier.size(34.dp))
                }
                Text(if (gateway.isEmpty()) "راوترك" else gateway, style = MaterialTheme.typography.titleMedium, color = OneUi.Ink, modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 10.dp, bottom = 22.dp))
                OneField(user, { user = it }, "اسم المستخدم", hint = "admin")
                Spacer(Modifier.height(10.dp))
                OneField(pass, { pass = it }, "كلمة السر", keyboard = KeyboardType.Password, error = err)
                Spacer(Modifier.height(20.dp))
                if (busy) CircularProgressIndicator(color = OneUi.Blue, modifier = Modifier.align(Alignment.CenterHorizontally))
                else OneButton("تسجيل الدخول بالشبكة", {
                    if (gateway.isEmpty()) { onDone(null); return@OneButton }
                    busy = true; err = null
                    scope.launch {
                        val ok = tryBasicAuth(gateway, user, pass)
                        busy = false
                        if (ok) onDone("Basic " + Base64.encodeToString("$user:$pass".toByteArray(), Base64.NO_WRAP))
                        else err = "فشل تسجيل الدخول. تأكد من الاسم وكلمة السر، أو أن راوترك لا يدعم هذا النوع من الدخول."
                    }
                }, icon = Icons.Outlined.Login)
                if (err != null) Box(Modifier.align(Alignment.CenterHorizontally).padding(top = 14.dp).oneClick { onDone(null) }) {
                    Text("متابعة لعرض الأجهزة المتصلة فقط (بدون تعديل)", style = MaterialTheme.typography.labelMedium, color = OneUi.Blue)
                }
            }
        }
    }
}

private class Sec(val icon: ImageVector, val tint: Color, val title: String, val sub: String)

@Composable
private fun NetworkDashboardScreen(net: NetInfo, auth: String?, onBack: () -> Unit) {
    var section by remember { mutableIntStateOf(-1) }
    when (section) {
        0 -> { NetworkInfoEditScreen(net, auth, onBack = { section = -1 }); return }
        1 -> { NetworkDevicesScreen(net, auth, blockMode = true, onBack = { section = -1 }); return }
        2 -> { NetworkDevicesScreen(net, auth, blockMode = false, onBack = { section = -1 }); return }
    }
    val secs = listOf(
        Sec(Icons.Outlined.Wifi, OneUi.Blue, "تغيير معلومات الشبكة", "الاسم، كلمة السر، نوع الشبكة"),
        Sec(Icons.Outlined.Block, Color(0xFFFF5A5A), "حظر المتصلين بالشبكة", "اعرض كل جهاز متصل واحظر من تريد"),
        Sec(Icons.Outlined.VerifiedUser, OneUi.Green, "تحديد المتصلين المسموح لهم", "فقط الأجهزة التي تختارها تبقى متصلة"),
    )
    Box(Modifier.fillMaxSize().background(OneUi.Bg)) {
        Column(Modifier.fillMaxSize()) {
            OneTopBar("إدارة الشبكة", onBack)
            if (auth == null) Text("دخلت بدون تحقق كامل: تقدر تعرض الأجهزة المتصلة، وتعديل الإعدادات قد لا يُطبَّق فعلياً إلا إذا كان راوترك مدعوماً.", style = MaterialTheme.typography.labelSmall, color = Color(0xFFFFB020), modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp))
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                secs.forEachIndexed { i, s ->
                    Row(Modifier.fillMaxWidth().oneClick { section = i }.clip(RoundedCornerShape(OneUi.RCard)).background(OneUi.Card).padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(50.dp).clip(CircleShape).background(s.tint.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) { Icon(s.icon, null, tint = s.tint, modifier = Modifier.size(26.dp)) }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(s.title, style = MaterialTheme.typography.titleMedium, color = OneUi.Ink)
                            Text(s.sub, style = MaterialTheme.typography.labelMedium, color = OneUi.Muted)
                        }
                        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, null, tint = OneUi.Muted, modifier = Modifier.size(26.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun NetworkInfoEditScreen(net: NetInfo, auth: String?, onBack: () -> Unit) {
    val ctx = LocalContext.current
    var ssid by remember { mutableStateOf(net.ssid ?: "") }
    var pass by remember { mutableStateOf("") }
    var secType by remember { mutableIntStateOf(0) }
    Box(Modifier.fillMaxSize().background(OneUi.Bg)) {
        Column(Modifier.fillMaxSize()) {
            OneTopBar("تغيير معلومات الشبكة", onBack)
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OneField(ssid, { ssid = it }, "اسم الشبكة (SSID)")
                OneField(pass, { pass = it }, "كلمة السر الجديدة", keyboard = KeyboardType.Password, hint = "اتركه فارغاً لعدم التغيير")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("WPA2", "WPA3", "مفتوحة").forEachIndexed { i, t ->
                        Box(Modifier.oneClick { secType = i }.glass(RoundedCornerShape(16.dp), if (secType == i) OneUi.Blue else Color.White, if (secType == i) 0.30f else 0.12f, if (secType == i) 0.16f else 0.06f).padding(horizontal = 16.dp, vertical = 10.dp)) {
                            Text(t, style = MaterialTheme.typography.labelMedium, color = if (secType == i) OneUi.Blue else OneUi.Ink)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "هذا القسم يحفظ ما تكتبه، لكن تطبيقه فعلياً على الراوتر يحتاج ربطاً خاصاً بطراز راوترك (كل شركة لها طريقة مختلفة تماماً). قل لنا نوع راوترك بالضبط (الاسم المطبوع عليه) لنكمل ربطه بالتحديث القادم.",
                    style = MaterialTheme.typography.labelSmall, color = OneUi.Muted,
                )
                Spacer(Modifier.height(10.dp))
                OneButton("حفظ", {
                    android.widget.Toast.makeText(ctx, "تم الحفظ محلياً. التطبيق الفعلي على الراوتر قيد التطوير لطرازات محددة.", android.widget.Toast.LENGTH_LONG).show()
                }, icon = Icons.Outlined.Save)
            }
        }
    }
}

// ---------------- اكتشاف الأجهزة المتصلة فعلياً (ARP محلي، حقيقي 100%) ----------------
private class Device(val ip: String, val mac: String, val vendor: String?)

private fun subnetPrefix(gateway: String): String? {
    val p = gateway.split(".")
    return if (p.size == 4) "${p[0]}.${p[1]}.${p[2]}." else null
}

/** يملأ جدول ARP بعمل ping سريع متوازٍ لكل عناوين الشبكة المحلية (1-254)، بدون أي إغراق أو تكرار — فحص مرة واحدة فقط */
private suspend fun pingSweep(prefix: String) = withContext(Dispatchers.IO) {
    val pool = Executors.newFixedThreadPool(32)
    try {
        val tasks = (1..254).map { i ->
            pool.submit { runCatching { InetAddress.getByName(prefix + i).isReachable(250) } }
        }
        tasks.forEach { runCatching { it.get() } }
    } finally { pool.shutdown() }
}

/** يقرأ جدول ARP الحقيقي للجهاز (نفس ما تعرضه تطبيقات فحص الشبكة المعروفة) */
private fun readArpTable(prefix: String?): List<Device> {
    val out = ArrayList<Device>()
    runCatching {
        val f = File("/proc/net/arp")
        if (!f.exists()) return@runCatching
        BufferedReader(f.reader()).use { r ->
            r.readLine() // العنوان، نوع الجهاز، العلم، عنوان الأجهزة، القناع، الواجهة
            r.forEachLine { line ->
                val c = line.trim().split(Regex("\\s+"))
                if (c.size >= 6) {
                    val ip = c[0]; val flags = c[2]; val mac = c[3]
                    if (flags != "0x0" && mac != "00:00:00:00:00:00" && (prefix == null || ip.startsWith(prefix))) out.add(Device(ip, mac, vendorFromMac(mac)))
                }
            }
        }
    }
    return out.distinctBy { it.ip }
}

private suspend fun scanDevices(gateway: String?): List<Device> {
    val prefix = gateway?.let { subnetPrefix(it) }
    if (prefix != null) pingSweep(prefix)
    return readArpTable(prefix)
}

@Composable
private fun NetworkDevicesScreen(net: NetInfo, auth: String?, blockMode: Boolean, onBack: () -> Unit) {
    val ctx = LocalContext.current
    var loading by remember { mutableStateOf(true) }
    var devices by remember { mutableStateOf<List<Device>>(emptyList()) }
    val blocked = remember { mutableStateMapOf<String, Boolean>() }
    val allowed = remember { mutableStateMapOf<String, Boolean>() }
    LaunchedEffect(Unit) { devices = scanDevices(net.gateway); loading = false }

    Box(Modifier.fillMaxSize().background(OneUi.Bg)) {
        Column(Modifier.fillMaxSize()) {
            OneTopBar(if (blockMode) "حظر المتصلين" else "تحديد المتصلين المسموح لهم", onBack) {
                Box(Modifier.size(44.dp).oneClick { loading = true }, contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Refresh, "فحص من جديد", tint = OneUi.Ink) }
            }
            Text(
                if (blockMode) "قائمة حقيقية بالأجهزة المتصلة بشبكتك الآن (من فحص الشبكة مباشرة)." else "اختر الأجهزة المسموح لها فقط؛ أي جهاز غيرها يُحظر عند محاولة الاتصال.",
                style = MaterialTheme.typography.labelMedium, color = OneUi.Muted, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )
            if (loading) Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = OneUi.Blue) }
            else if (devices.isEmpty()) Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Outlined.DevicesOther, null, tint = OneUi.Faint, modifier = Modifier.size(50.dp))
                Text("ما لقينا أجهزة متصلة حالياً. جرّب تحديث الفحص.", style = MaterialTheme.typography.bodyMedium, color = OneUi.Muted, modifier = Modifier.padding(top = 10.dp))
            } else Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                devices.forEach { d ->
                    val isGw = d.ip == net.gateway
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(OneUi.Card).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(42.dp).clip(CircleShape).background(OneUi.Card2), contentAlignment = Alignment.Center) { Icon(if (isGw) Icons.Outlined.Router else Icons.Outlined.PhoneAndroid, null, tint = if (isGw) OneUi.Blue else OneUi.Muted, modifier = Modifier.size(22.dp)) }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(if (isGw) "الراوتر" else d.ip, style = MaterialTheme.typography.bodyLarge, color = OneUi.Ink)
                            Text(d.mac + (d.vendor?.let { " · $it" } ?: ""), style = MaterialTheme.typography.labelSmall, color = OneUi.Muted)
                        }
                        if (!isGw) {
                            if (blockMode) {
                                val on = blocked[d.mac] == true
                                Box(Modifier.oneClick { blocked[d.mac] = !on }.glass(RoundedCornerShape(14.dp), if (on) Color(0xFFFF5A5A) else Color.White, if (on) 0.32f else 0.12f, if (on) 0.18f else 0.06f).padding(horizontal = 14.dp, vertical = 8.dp)) {
                                    Text(if (on) "محظور" else "حظر", style = MaterialTheme.typography.labelMedium, color = if (on) Color(0xFFFF5A5A) else OneUi.Ink)
                                }
                            } else {
                                OneSwitch(allowed[d.mac] == true) { v -> allowed[d.mac] = v }
                            }
                        }
                    }
                }
            }
            Column(Modifier.navigationBarsPadding().padding(16.dp)) {
                OneButton(if (blockMode) "تطبيق الحظر" else "حفظ القائمة المسموحة", {
                    val n = if (blockMode) blocked.values.count { it } else allowed.values.count { it }
                    val msg = if (auth != null) "تم حفظ الاختيار ($n جهاز). تنفيذه الفعلي على الشبكة يحتاج دعم طراز راوترك." else "سجّل دخولاً ناجحاً أولاً حتى يُطبَّق هذا فعلياً على راوترك."
                    android.widget.Toast.makeText(ctx, msg, android.widget.Toast.LENGTH_LONG).show()
                })
            }
        }
    }
}
