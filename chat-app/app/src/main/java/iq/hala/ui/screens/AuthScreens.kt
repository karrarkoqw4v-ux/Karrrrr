package iq.hala.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import iq.hala.data.Repo
import iq.hala.ui.components.*
import iq.hala.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(onDone: () -> Unit) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true; delay(1900); onDone() }
    val scale by animateFloatAsState(if (shown) 1f else 0.3f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow), label = "logo")
    val a by animateFloatAsState(if (shown) 1f else 0f, tween(700), label = "a")
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF12C9B1), Color(0xFF075E57)))), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            HalaLogo(96.dp, Modifier.graphicsLayer { scaleX = scale; scaleY = scale; rotationZ = (1f - scale) * -25f }, onGradient = true)
            Spacer(Modifier.height(20.dp))
            Text("هلا", style = MaterialTheme.typography.displaySmall.copy(fontSize = 40.sp), color = Color.White, modifier = Modifier.graphicsLayer { alpha = a })
            Text("دردش، شارك، وانبسط", style = MaterialTheme.typography.bodyLarge, color = Color(0xFFD6F5F0), modifier = Modifier.graphicsLayer { alpha = a })
        }
        Text("صُنع بحب للعراقيين", style = MaterialTheme.typography.labelMedium, color = Color(0xFFCFF3EE),
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 28.dp).graphicsLayer { alpha = a })
    }
}

@Composable
fun OnboardingScreen(onFinish: () -> Unit, onLogin: () -> Unit) {
    val p = pal
    val pager = rememberPagerState { 3 }
    val scope = rememberCoroutineScope()
    val titles = listOf("شارك لحظاتك" to "صور وقصص تنشرها وأصحابك يشوفونها بلحظتها", "دردش بدون حدود" to "رسائل وصور ومكالمات بمكان واحد", "اكتشف ناس جدد" to "تابع أصحابك واكتشف محتوى يشبهك")
    Column(Modifier.fillMaxSize().background(p.bg).statusBarsPadding().navigationBarsPadding().padding(20.dp)) {
        Text("تخطي", style = MaterialTheme.typography.bodyLarge, color = p.muted, modifier = Modifier.bounceClick { onFinish() }.padding(8.dp))
        HorizontalPager(pager, Modifier.weight(1f).fillMaxWidth()) { page ->
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(28.dp)).background(Brush.verticalGradient(listOf(Color(0xFFE9FBF8), Color(0xFFFFF3DC)))), contentAlignment = Alignment.Center) {
                    OnboardArt(page)
                }
                Spacer(Modifier.height(22.dp))
                Text(titles[page].first, style = MaterialTheme.typography.displaySmall, color = p.ink)
                Spacer(Modifier.height(8.dp))
                Text(titles[page].second, style = MaterialTheme.typography.bodyMedium, color = p.muted, textAlign = TextAlign.Center)
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            repeat(3) { i ->
                val w by animateDpAsState(if (pager.currentPage == i) 22.dp else 7.dp, label = "dot")
                Box(Modifier.padding(3.dp).height(7.dp).width(w).clip(RoundedCornerShape(4.dp)).background(if (pager.currentPage == i) Primary else p.line))
            }
        }
        Spacer(Modifier.height(18.dp))
        GradButton(if (pager.currentPage < 2) "التالي" else "ابدأ الآن", {
            if (pager.currentPage < 2) scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } else onFinish()
        })
        Spacer(Modifier.height(10.dp))
        Text("عندي حساب؟ تسجيل الدخول", style = MaterialTheme.typography.bodyLarge, color = PrimaryDark, modifier = Modifier.align(Alignment.CenterHorizontally).bounceClick { onLogin() }.padding(10.dp))
    }
}

@Composable
private fun OnboardArt(page: Int) {
    val f = rememberFloat(6f, 2600)
    when (page) {
        0 -> Box(Modifier.size(260.dp)) {
            PhotoArt(0, Modifier.size(120.dp, 160.dp).align(Alignment.TopStart).graphicsLayer { translationY = f }.clip(RoundedCornerShape(16.dp)))
            PhotoArt(3, Modifier.size(130.dp, 170.dp).align(Alignment.CenterEnd).graphicsLayer { translationY = -f }.clip(RoundedCornerShape(16.dp)))
            PhotoArt(5, Modifier.size(120.dp, 130.dp).align(Alignment.BottomStart).graphicsLayer { translationY = f }.clip(RoundedCornerShape(16.dp)))
            Box(Modifier.align(Alignment.TopEnd).size(40.dp).clip(CircleShape).background(Like), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Favorite, null, tint = Color.White) }
        }
        1 -> Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf(true to "هلا بيك!", false to "شلونك اليوم؟", true to "تمام، وانت؟", false to "ننتظرك بالمقهى").forEachIndexed { i, (mine, t) ->
                Entrance(i) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (mine) Arrangement.Start else Arrangement.End) {
                        Box(Modifier.clip(RoundedCornerShape(20.dp)).background(if (mine) Primary else Color.White).padding(horizontal = 16.dp, vertical = 11.dp)) {
                            Text(t, color = if (mine) Color.White else Color(0xFF0E1A24), style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        }
        else -> Box(Modifier.size(260.dp)) {
            Avatar("ن", 0, 92.dp, Modifier.align(Alignment.Center).graphicsLayer { translationY = f }, Ring.New)
            Avatar("س", 1, 58.dp, Modifier.align(Alignment.TopStart).graphicsLayer { translationY = -f }, Ring.New)
            Avatar("ر", 2, 62.dp, Modifier.align(Alignment.TopEnd).graphicsLayer { translationY = f }, Ring.New)
            Avatar("ع", 4, 54.dp, Modifier.align(Alignment.BottomEnd).graphicsLayer { translationY = -f })
            Avatar("ك", 3, 50.dp, Modifier.align(Alignment.BottomStart).graphicsLayer { translationY = f })
        }
    }
}

@Composable
private fun rememberShake(trigger: Int): Animatable<Float, AnimationVector1D> {
    val s = remember { Animatable(0f) }
    LaunchedEffect(trigger) { if (trigger > 0) for (v in listOf(-16f, 14f, -10f, 8f, -4f, 0f)) s.animateTo(v, tween(55)) }
    return s
}

@Composable
fun LoginScreen(onDone: () -> Unit, onSignup: () -> Unit, onForgot: () -> Unit) {
    val p = pal
    var id by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var show by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var tick by remember { mutableIntStateOf(0) }
    val shake = rememberShake(tick)
    Column(Modifier.fillMaxSize().background(p.bg).verticalScroll(rememberScrollState()).statusBarsPadding().navigationBarsPadding().imePadding().padding(horizontal = 20.dp, vertical = 28.dp)) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            val f = rememberFloat(4f)
            HalaLogo(72.dp, Modifier.graphicsLayer { translationY = f * density })
            Spacer(Modifier.height(18.dp))
            Entrance(1) { Text("هلا بيك مجدداً", style = MaterialTheme.typography.displaySmall, color = p.ink) }
            Entrance(2) { Text("ادخل حتى تكمل دردشتك", style = MaterialTheme.typography.bodyMedium, color = p.muted) }
        }
        Spacer(Modifier.height(28.dp))
        Column(Modifier.offset { IntOffset(shake.value.dp.roundToPx(), 0) }) {
            Entrance(3) { HalaField(id, { id = it; error = null }, "رقم الهاتف أو البريد أو اسم المستخدم", Icons.Outlined.Person, error = error != null) }
            Spacer(Modifier.height(12.dp))
            Entrance(4) {
                HalaField(pass, { pass = it; error = null }, "كلمة المرور", Icons.Outlined.Lock, trailing = if (show) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                    onTrailing = { show = !show }, password = true, show = show, error = error != null)
            }
        }
        AnimatedVisibility(error != null) { Column { Spacer(Modifier.height(12.dp)); ErrorBanner(error.orEmpty()) } }
        Spacer(Modifier.height(10.dp))
        Text("نسيت كلمة المرور؟", style = MaterialTheme.typography.labelMedium, color = PrimaryDark, modifier = Modifier.bounceClick { onForgot() }.padding(6.dp))
        Spacer(Modifier.height(14.dp))
        Entrance(5) {
            GradButton("دخول", {
                when (val r = Repo.signIn(id, pass)) {
                    is Repo.Result.Ok -> onDone()
                    is Repo.Result.Fail -> { error = r.msg; tick++ }
                }
            })
        }
        Spacer(Modifier.height(30.dp))
        Text(
            buildAnnotatedString { append("ما عندك حساب؟ "); withStyle(SpanStyle(color = PrimaryDark, fontWeight = FontWeight.Bold)) { append("سجّل الحين") } },
            style = MaterialTheme.typography.bodyMedium, color = p.muted, modifier = Modifier.align(Alignment.CenterHorizontally).bounceClick { onSignup() }.padding(12.dp),
        )
    }
}

@Composable
fun ForgotScreen(onBack: () -> Unit) {
    val p = pal
    var contact by remember { mutableStateOf("") }
    var sent by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(p.bg).imePadding()) {
        TopBack("استعادة الحساب", onBack)
        Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(100.dp).clip(CircleShape).background(p.soft), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Lock, null, tint = PrimaryDark, modifier = Modifier.size(44.dp)) }
            Spacer(Modifier.height(18.dp))
            Text("نسيت كلمة المرور؟", style = MaterialTheme.typography.headlineSmall, color = p.ink)
            Spacer(Modifier.height(6.dp))
            Text("اكتب بريدك أو رقمك ونرسل لك رمز الاستعادة", style = MaterialTheme.typography.bodyMedium, color = p.muted, textAlign = TextAlign.Center)
            Spacer(Modifier.height(22.dp))
            HalaField(contact, { contact = it; sent = false }, "البريد أو رقم الهاتف", Icons.Outlined.MailOutline)
            Spacer(Modifier.height(16.dp))
            GradButton("إرسال الرمز", { sent = true }, enabled = contact.length > 4)
            AnimatedVisibility(sent) {
                Column {
                    Spacer(Modifier.height(14.dp))
                    ErrorBanner("النسخة الحالية تحفظ الحسابات على جهازك فقط، فاستعادة الحساب بالرمز تحتاج سيرفر. سجّل حساب جديد مؤقتاً.")
                }
            }
        }
    }
}

private fun strength(p: String): Int {
    var s = 0
    if (p.length >= 6) s++
    if (p.length >= 9) s++
    if (p.any { it.isDigit() } && p.any { it.isLetter() }) s++
    if (p.any { !it.isLetterOrDigit() } || (p.any { it.isUpperCase() } && p.any { it.isLowerCase() })) s++
    return s
}

/** تسجيل بأربع خطوات: الاتصال، الاسم وكلمة المرور، اسم المستخدم وتاريخ الميلاد، إنهاء */
@Composable
fun SignupScreen(onBack: () -> Unit, onDone: () -> Unit) {
    val p = pal
    var step by remember { mutableIntStateOf(0) }
    var phone by remember { mutableStateOf(true) }
    var contact by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var show by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf("") }
    var day by remember { mutableStateOf("") }
    var month by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var tick by remember { mutableIntStateOf(0) }
    val shake = rememberShake(tick)
    val st = strength(pass)
    val stColor by animateColorAsState(listOf(p.line, Danger, Color(0xFFF59E0B), Color(0xFF84CC16), Success)[st], label = "stc")
    val stFrac by animateFloatAsState(st / 4f, tween(350), label = "stf")
    val free = username.length >= 3 && Repo.usernameFree(username)

    fun next() {
        error = null
        val msg = when (step) {
            0 -> if (contact.trim().length < 5) "اكتب رقم هاتف أو بريد صحيح" else null
            1 -> if (name.isBlank()) "اكتب اسمك" else if (pass.length < 6) "كلمة المرور 6 أحرف على الأقل" else null
            2 -> if (!free) "اسم المستخدم غير متاح أو قصير" else if (day.toIntOrNull() !in 1..31 || month.toIntOrNull() !in 1..12 || (year.toIntOrNull() ?: 0) !in 1930..2015) "تاريخ الميلاد غير صحيح" else null
            else -> null
        }
        if (msg != null) { error = msg; tick++; return }
        if (step < 3) { step++; return }
        when (val r = Repo.signUp(contact, name, username, pass)) {
            is Repo.Result.Ok -> onDone()
            is Repo.Result.Fail -> { error = r.msg; tick++; step = 0 }
        }
    }

    Column(Modifier.fillMaxSize().background(p.bg).imePadding()) {
        TopBack("إنشاء حساب", { if (step > 0) step-- else onBack() })
        Row(Modifier.padding(horizontal = 20.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(4) { i ->
                val c by animateColorAsState(if (i <= step) Primary else p.line, label = "seg")
                Box(Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(2.dp)).background(c))
            }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp)) {
            AnimatedContent(step, transitionSpec = { (slideInHorizontally { -it / 3 } + fadeIn()) togetherWith (slideOutHorizontally { it / 3 } + fadeOut()) }, label = "step") { s ->
                Column(Modifier.offset { IntOffset(shake.value.dp.roundToPx(), 0) }) {
                    when (s) {
                        0 -> {
                            Text("ما هو رقمك أو بريدك؟", style = MaterialTheme.typography.headlineSmall, color = p.ink)
                            Text("نستخدمه لتعريف حسابك", style = MaterialTheme.typography.bodyMedium, color = p.muted)
                            Spacer(Modifier.height(18.dp))
                            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(p.bg2).padding(4.dp)) {
                                listOf("رقم الهاتف" to true, "البريد الإلكتروني" to false).forEach { (t, v) ->
                                    Box(Modifier.weight(1f).height(38.dp).bounceClick { phone = v; contact = "" }.clip(RoundedCornerShape(11.dp)).background(if (phone == v) p.bg else Color.Transparent), contentAlignment = Alignment.Center) {
                                        Text(t, style = MaterialTheme.typography.labelMedium, color = if (phone == v) PrimaryDark else p.muted)
                                    }
                                }
                            }
                            Spacer(Modifier.height(14.dp))
                            HalaField(contact, { contact = it }, if (phone) "7701234567" else "name@email.com", if (phone) Icons.Outlined.PhoneAndroid else Icons.Outlined.MailOutline, error = error != null)
                        }
                        1 -> {
                            Text("اسمك وكلمة المرور", style = MaterialTheme.typography.headlineSmall, color = p.ink)
                            Text("اسمك يظهر لأصحابك بالدردشة", style = MaterialTheme.typography.bodyMedium, color = p.muted)
                            Spacer(Modifier.height(18.dp))
                            HalaField(name, { name = it }, "الاسم الكامل", Icons.Outlined.Person)
                            Spacer(Modifier.height(12.dp))
                            HalaField(pass, { pass = it }, "كلمة المرور", Icons.Outlined.Lock, trailing = if (show) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, onTrailing = { show = !show }, password = true, show = show)
                            Spacer(Modifier.height(10.dp))
                            Box(Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)).background(p.line)) { Box(Modifier.fillMaxHeight().fillMaxWidth(stFrac).clip(RoundedCornerShape(3.dp)).background(stColor)) }
                            Text(when (st) { 0 -> if (pass.isEmpty()) "" else "ضعيفة جداً"; 1 -> "ضعيفة"; 2 -> "مقبولة"; 3 -> "قوية"; else -> "قوية جداً" }, style = MaterialTheme.typography.labelSmall, color = stColor)
                        }
                        2 -> {
                            Text("اختر اسم مستخدم", style = MaterialTheme.typography.headlineSmall, color = p.ink)
                            Text("يعرفك به الناس", style = MaterialTheme.typography.bodyMedium, color = p.muted)
                            Spacer(Modifier.height(18.dp))
                            HalaField(username, { username = it.lowercase().filter { c -> c.isLetterOrDigit() || c == '.' || c == '_' } }, "اسم المستخدم", Icons.Outlined.AlternateEmail,
                                trailing = if (free) Icons.Outlined.CheckCircle else null)
                            if (username.length >= 3) Text(if (free) "متاح" else "غير متاح", style = MaterialTheme.typography.labelMedium, color = if (free) Success else Danger, modifier = Modifier.padding(top = 6.dp))
                            Spacer(Modifier.height(18.dp))
                            Text("تاريخ الميلاد", style = MaterialTheme.typography.titleMedium, color = p.ink)
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                HalaField(day, { day = it.take(2).filter(Char::isDigit) }, "اليوم", modifier = Modifier.weight(1f))
                                HalaField(month, { month = it.take(2).filter(Char::isDigit) }, "الشهر", modifier = Modifier.weight(1f))
                                HalaField(year, { year = it.take(4).filter(Char::isDigit) }, "السنة", modifier = Modifier.weight(1.3f))
                            }
                        }
                        else -> Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("جاهز!", style = MaterialTheme.typography.headlineSmall, color = p.ink)
                            Text("هذا حسابك الجديد", style = MaterialTheme.typography.bodyMedium, color = p.muted)
                            Spacer(Modifier.height(26.dp))
                            Avatar(name.ifBlank { "أ" }, 0, 110.dp, ring = Ring.New)
                            Spacer(Modifier.height(14.dp))
                            Text(name, style = MaterialTheme.typography.titleLarge, color = p.ink)
                            Text("@$username", style = MaterialTheme.typography.bodyMedium, color = p.muted)
                        }
                    }
                }
            }
            AnimatedVisibility(error != null) { Column { Spacer(Modifier.height(14.dp)); ErrorBanner(error.orEmpty()) } }
        }
        Box(Modifier.padding(horizontal = 20.dp).padding(bottom = 16.dp).navigationBarsPadding()) {
            GradButton(if (step < 3) "التالي" else "ابدأ", { next() })
        }
    }
}
