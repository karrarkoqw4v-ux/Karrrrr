package iq.student.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import iq.student.data.Prefs
import iq.student.ui.components.*
import iq.student.ui.theme.AppColors
import iq.student.ui.theme.dimens
import kotlin.math.roundToInt

/** شعار متحرك: يطلع بنطّة نابضة ثم يطفو بهدوء */
@Composable
private fun LogoPop() {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val scale by animateFloatAsState(if (shown) 1f else 0f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow), label = "logo")
    val float = rememberFloat(5f, 2400)
    Box(
        Modifier.size(64.dp).graphicsLayer { scaleX = scale; scaleY = scale; translationY = float * density; rotationZ = (1f - scale) * -30f }
            .clip(RoundedCornerShape(20.dp)).background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(AppColors.Primary, Color(0xFF5B7EF0)))),
        contentAlignment = Alignment.Center,
    ) { Icon(Icons.Outlined.School, null, tint = Color.White, modifier = Modifier.size(34.dp)) }
}

/** اهتزاز أفقي عند الخطأ */
@Composable
private fun rememberShake(trigger: Int): Animatable<Float, *> {
    val shake = remember { Animatable(0f) }
    LaunchedEffect(trigger) {
        if (trigger > 0) for (v in listOf(-16f, 14f, -10f, 8f, -4f, 0f)) shake.animateTo(v, tween(55))
    }
    return shake
}

@Composable
fun LoginScreen(onDone: (needsGrade: Boolean) -> Unit, onCreateAccount: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var show by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var errorTick by remember { mutableIntStateOf(0) }
    val shake = rememberShake(errorTick)

    Box(Modifier.fillMaxSize().background(AppColors.Surface)) {
        AnimatedBackdrop()
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .statusBarsPadding().navigationBarsPadding().imePadding()
                .padding(horizontal = dimens.pad).padding(top = 36.dp, bottom = 20.dp),
        ) {
            LogoPop()
            Spacer(Modifier.height(22.dp))
            Entrance(1) { Text("هلا بيك من جديد", style = MaterialTheme.typography.displaySmall, color = AppColors.Ink) }
            Spacer(Modifier.height(6.dp))
            Entrance(2) { Text("سجّل دخولك وكمّل دراستك من وين وقفت", style = MaterialTheme.typography.bodyMedium, color = AppColors.Muted) }
            Spacer(Modifier.height(26.dp))
            Column(Modifier.offset { IntOffset(shake.value.dp.roundToPx(), 0) }) {
                Entrance(3) { AppField("البريد الإلكتروني", email, { email = it; error = null }, Icons.Outlined.MailOutline, placeholder = "name@email.com") }
                Spacer(Modifier.height(dimens.gap))
                Entrance(4) {
                    AppField("كلمة المرور", password, { password = it; error = null }, Icons.Outlined.Lock, placeholder = "••••••••",
                        trailing = if (show) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, onTrailing = { show = !show }, password = true, showText = show)
                }
                androidx.compose.animation.AnimatedVisibility(error != null) {
                    Column { Spacer(Modifier.height(12.dp)); ErrorBox(error.orEmpty()) }
                }
            }
            Spacer(Modifier.height(24.dp))
            Entrance(5) {
                AppButton("تسجيل الدخول", {
                    when (val r = Prefs.signIn(email, password)) {
                        is Prefs.AuthResult.Ok -> onDone(Prefs.gradeId == null)
                        is Prefs.AuthResult.Error -> { error = r.message; errorTick++ }
                    }
                })
            }
            Spacer(Modifier.height(32.dp))
            Entrance(6, Modifier.align(Alignment.CenterHorizontally)) {
                Text(
                    buildAnnotatedString {
                        append("ماعندك حساب؟ ")
                        withStyle(SpanStyle(color = AppColors.Primary, fontWeight = FontWeight.Bold)) { append("أنشئ حساب") }
                    },
                    style = MaterialTheme.typography.bodyMedium, color = AppColors.Muted,
                    modifier = Modifier.bounceClick(onClick = onCreateAccount).padding(12.dp),
                )
            }
        }
    }
}

/** قوة كلمة المرور 0..4 */
private fun strength(p: String): Int {
    var s = 0
    if (p.length >= 6) s++
    if (p.length >= 9) s++
    if (p.any { it.isDigit() } && p.any { it.isLetter() }) s++
    if (p.any { !it.isLetterOrDigit() } || (p.any { it.isUpperCase() } && p.any { it.isLowerCase() })) s++
    return s
}

@Composable
fun SignupScreen(onBack: () -> Unit, onDone: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var pass2 by remember { mutableStateOf("") }
    var show by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var errorTick by remember { mutableIntStateOf(0) }
    val shake = rememberShake(errorTick)
    val st = strength(pass)
    val stColor by animateColorAsState(listOf(AppColors.Line, AppColors.Danger, Color(0xFFF59E0B), Color(0xFF84CC16), AppColors.Success)[st], label = "stc")
    val stFrac by animateFloatAsState(st / 4f, tween(350), label = "stf")
    val filled = listOf(name, email, pass, pass2).count { it.isNotBlank() }
    val progress by animateFloatAsState(filled / 4f, tween(400), label = "prog")

    Box(Modifier.fillMaxSize().background(AppColors.Surface)) {
        AnimatedBackdrop()
        Column(Modifier.fillMaxSize().imePadding()) {
            ScreenHeader("إنشاء حساب", onBack)
            // شريط تقدم الحقول
            Box(Modifier.padding(horizontal = dimens.pad).fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(AppColors.Line)) {
                Box(Modifier.fillMaxHeight().fillMaxWidth(progress).clip(RoundedCornerShape(2.dp)).background(AppColors.Primary))
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = dimens.pad).padding(top = 14.dp)) {
                Entrance(0) { Text("أهلاً بيك بينا", style = MaterialTheme.typography.displaySmall, color = AppColors.Ink) }
                Spacer(Modifier.height(6.dp))
                Entrance(1) { Text("سوّي حسابك وابدأ دراستك بدقيقة", style = MaterialTheme.typography.bodyMedium, color = AppColors.Muted) }
                Spacer(Modifier.height(18.dp))
                Column(Modifier.offset { IntOffset(shake.value.dp.roundToPx(), 0) }) {
                    Entrance(2) { AppField("الاسم الكامل", name, { name = it; error = null }, Icons.Outlined.Person, placeholder = "اسمك") }
                    Spacer(Modifier.height(10.dp))
                    Entrance(3) { AppField("البريد الإلكتروني", email, { email = it; error = null }, Icons.Outlined.MailOutline, placeholder = "name@email.com") }
                    Spacer(Modifier.height(10.dp))
                    Entrance(4) {
                        AppField("كلمة المرور", pass, { pass = it; error = null }, Icons.Outlined.Lock, placeholder = "6 أحرف على الأقل",
                            trailing = if (show) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, onTrailing = { show = !show }, password = true, showText = show)
                    }
                    // قوة كلمة المرور
                    Spacer(Modifier.height(8.dp))
                    Box(Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)).background(AppColors.Line)) {
                        Box(Modifier.fillMaxHeight().fillMaxWidth(stFrac).clip(RoundedCornerShape(3.dp)).background(stColor))
                    }
                    Text(
                        when (st) { 0 -> if (pass.isEmpty()) "" else "ضعيفة جداً"; 1 -> "ضعيفة"; 2 -> "مقبولة"; 3 -> "قوية"; else -> "قوية جداً" },
                        style = MaterialTheme.typography.labelSmall, color = stColor,
                    )
                    Spacer(Modifier.height(6.dp))
                    Entrance(5) {
                        AppField("تأكيد كلمة المرور", pass2, { pass2 = it; error = null }, Icons.Outlined.Lock, placeholder = "أعد كتابتها", password = true, showText = show)
                    }
                    androidx.compose.animation.AnimatedVisibility(error != null) {
                        Column { Spacer(Modifier.height(12.dp)); ErrorBox(error.orEmpty()) }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
            Box(Modifier.padding(horizontal = dimens.pad).padding(bottom = 16.dp).navigationBarsPadding()) {
                AppButton("إنشاء الحساب", {
                    if (pass != pass2) { error = "كلمتا المرور مو متطابقتين"; errorTick++; return@AppButton }
                    when (val r = Prefs.signUp(name, email, pass)) {
                        is Prefs.AuthResult.Ok -> onDone()
                        is Prefs.AuthResult.Error -> { error = r.message; errorTick++ }
                    }
                }, icon = Icons.AutoMirrored.Outlined.ArrowForward)
            }
        }
    }
}
