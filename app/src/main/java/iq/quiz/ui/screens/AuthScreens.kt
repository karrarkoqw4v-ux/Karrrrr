package iq.quiz.ui.screens

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import iq.quiz.data.Repo
import iq.quiz.ui.components.*
import iq.quiz.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(onDone: () -> Unit) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true; delay(1900); onDone() }
    val scale by animateFloatAsState(if (shown) 1f else 0.3f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow), label = "logo")
    val a by animateFloatAsState(if (shown) 1f else 0f, tween(700), label = "a")
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF3B1275), Color(0xFF0A0614)))), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            HalaLogo(96.dp, Modifier.graphicsLayer { scaleX = scale; scaleY = scale; rotationZ = (1f - scale) * -25f }, onGradient = true)
            Spacer(Modifier.height(20.dp))
            Text("كويز تواصلكم", style = MaterialTheme.typography.displaySmall.copy(fontSize = 34.sp), color = Color.White, modifier = Modifier.graphicsLayer { alpha = a })
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
                Box(Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(28.dp)).background(Brush.verticalGradient(listOf(Color(0xFF1E1236), Color(0xFF0B0A14)))), contentAlignment = Alignment.Center) {
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
    val p = pal
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
                        Box(Modifier.clip(RoundedCornerShape(20.dp)).background(if (mine) Primary else p.bg2).padding(horizontal = 16.dp, vertical = 11.dp)) {
                            Text(t, color = if (mine) Color.White else p.ink, style = MaterialTheme.typography.bodyLarge)
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
    val scope = rememberCoroutineScope()
    var id by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var show by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var tick by remember { mutableIntStateOf(0) }
    val shake = rememberShake(tick)
    Box(Modifier.fillMaxSize().background(p.bg)) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().navigationBarsPadding().imePadding().padding(horizontal = 32.dp, vertical = 28.dp)) {
            CrashCard()
            Spacer(Modifier.weight(0.6f))
            Entrance(1) {
                Text(
                    "كويز تواصلكم", style = MaterialTheme.typography.displaySmall.copy(brush = BrandBrush),
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }
            Spacer(Modifier.height(44.dp))
            Column(Modifier.offset { IntOffset(shake.value.dp.roundToPx(), 0) }) {
                Entrance(2) { HalaField(id, { id = it; error = null }, "البريد الإلكتروني أو اسم المستخدم", Icons.Outlined.Person, error = error != null) }
                Spacer(Modifier.height(10.dp))
                Entrance(3) {
                    HalaField(pass, { pass = it; error = null }, "كلمة المرور", Icons.Outlined.Lock, trailing = if (show) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        onTrailing = { show = !show }, password = true, show = show, error = error != null)
                }
                AnimatedVisibility(error != null) { Column { Spacer(Modifier.height(12.dp)); ErrorBanner(error.orEmpty()) } }
                Spacer(Modifier.height(10.dp))
                Text("نسيت كلمة المرور؟", style = MaterialTheme.typography.labelMedium, color = Primary, modifier = Modifier.align(Alignment.End).bounceClick { onForgot() }.padding(6.dp))
                Spacer(Modifier.height(14.dp))
                Entrance(4) {
                    GradButton("دخول", {
                        loading = true
                        scope.launch {
                            when (val r = Repo.signIn(id, pass)) {
                                is Repo.Result.Ok -> onDone()
                                is Repo.Result.Fail -> { error = r.msg; tick++ }
                            }
                            loading = false
                        }
                    }, enabled = !loading)
                }
            }
            Spacer(Modifier.height(22.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                HorizontalDivider(Modifier.weight(1f), color = p.line)
                Text("أو", style = MaterialTheme.typography.labelMedium, color = p.faint, modifier = Modifier.padding(horizontal = 14.dp))
                HorizontalDivider(Modifier.weight(1f), color = p.line)
            }
            Spacer(Modifier.height(22.dp))
            Text(
                "تسجيل الدخول برقم الهاتف", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = Primary, modifier = Modifier.align(Alignment.CenterHorizontally).bounceClick { }.padding(8.dp),
            )
            Spacer(Modifier.weight(1f))
            HorizontalDivider(color = p.line)
            Spacer(Modifier.height(18.dp))
            Text(
                buildAnnotatedString { append("ما عندك حساب؟ "); withStyle(SpanStyle(color = Primary, fontWeight = FontWeight.Bold)) { append("سجّل الحين") } },
                style = MaterialTheme.typography.bodyMedium, color = p.muted, modifier = Modifier.align(Alignment.CenterHorizontally).bounceClick { onSignup() }.padding(12.dp),
            )
        }
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

/** تسجيل بصفحة وحدة: اسم المستخدم، الاسم، البريد، كلمة المرور، والموافقة على الشروط */
@Composable
fun SignupScreen(onBack: () -> Unit, onDone: () -> Unit) {
    val p = pal
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var show by remember { mutableStateOf(false) }
    var agree by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var tick by remember { mutableIntStateOf(0) }
    val shake = rememberShake(tick)
    val free = username.length >= 3 && Repo.usernameFree(username)

    fun submit() {
        error = null
        val msg = when {
            name.isBlank() -> "اكتب اسمك"
            !free -> "اسم المستخدم 3 أحرف فأكثر (حروف وأرقام ونقطة)"
            !email.contains("@") || !email.contains(".") -> "اكتب بريد إلكتروني صحيح"
            pass.length < 6 -> "كلمة المرور 6 أحرف على الأقل"
            !agree -> "لازم توافق على الشروط والأحكام"
            else -> null
        }
        if (msg != null) { error = msg; tick++; return }
        loading = true
        scope.launch {
            when (val r = Repo.signUp(email, name, username, pass)) {
                is Repo.Result.Ok -> onDone()
                is Repo.Result.Fail -> { error = r.msg; tick++ }
            }
            loading = false
        }
    }

    Column(Modifier.fillMaxSize().background(p.bg).imePadding()) {
        TopBack("إنشاء حساب", onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp)) {
            CrashCard(Modifier.padding(bottom = 16.dp))
            Column(Modifier.offset { IntOffset(shake.value.dp.roundToPx(), 0) }) {
                Text("حساب جديد", style = MaterialTheme.typography.headlineSmall, color = p.ink)
                Text("اسم المستخدم، اسمك، بريدك، وكلمة مرور", style = MaterialTheme.typography.bodyMedium, color = p.muted)
                Spacer(Modifier.height(20.dp))
                HalaField(username, { username = it.lowercase().filter { c -> c.isLetterOrDigit() || c == '.' || c == '_' } }, "اسم المستخدم", Icons.Outlined.AlternateEmail,
                    trailing = if (free) Icons.Outlined.CheckCircle else null)
                if (username.length >= 3) Text(if (free) "متاح" else "اسم غير صالح، جرّب ثاني", style = MaterialTheme.typography.labelMedium, color = if (free) Success else Danger, modifier = Modifier.padding(top = 6.dp))
                Spacer(Modifier.height(12.dp))
                HalaField(name, { name = it }, "الاسم الكامل", Icons.Outlined.Person)
                Spacer(Modifier.height(12.dp))
                HalaField(email, { email = it }, "البريد الإلكتروني", Icons.Outlined.MailOutline)
                Spacer(Modifier.height(12.dp))
                HalaField(pass, { pass = it }, "كلمة المرور", Icons.Outlined.Lock, trailing = if (show) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, onTrailing = { show = !show }, password = true, show = show)
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth().bounceClick { agree = !agree }, verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = agree, onCheckedChange = { agree = it }, colors = CheckboxDefaults.colors(checkedColor = Primary))
                    Text("أوافق على الشروط والأحكام", style = MaterialTheme.typography.bodyMedium, color = p.ink)
                }
                AnimatedVisibility(error != null) { Column { Spacer(Modifier.height(14.dp)); ErrorBanner(error.orEmpty()) } }
            }
        }
        Box(Modifier.padding(horizontal = 20.dp).padding(bottom = 16.dp).navigationBarsPadding()) {
            GradButton("إنشاء الحساب", { submit() }, enabled = !loading)
        }
    }
}
