package iq.student.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import iq.student.data.Prefs
import iq.student.ui.components.*
import iq.student.ui.theme.AppColors
import iq.student.ui.theme.dimens

@Composable
fun LoginScreen(onDone: (needsGrade: Boolean) -> Unit, onCreateAccount: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var show by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        Modifier.fillMaxSize().background(AppColors.Surface).verticalScroll(rememberScrollState())
            .statusBarsPadding().navigationBarsPadding().imePadding()
            .padding(horizontal = dimens.pad).padding(top = 28.dp, bottom = 20.dp),
    ) {
        Box(Modifier.size(56.dp).clip(RoundedCornerShape(18.dp)).background(AppColors.PrimarySoft), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.School, null, tint = AppColors.Primary, modifier = Modifier.size(30.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text("هلا بيك من جديد", style = MaterialTheme.typography.displaySmall, color = AppColors.Ink)
        Spacer(Modifier.height(6.dp))
        Text("سجّل دخولك وكمّل دراستك من وين وقفت", style = MaterialTheme.typography.bodyMedium, color = AppColors.Muted)
        Spacer(Modifier.height(24.dp))
        AppField("البريد الإلكتروني", email, { email = it; error = null }, Icons.Outlined.MailOutline, placeholder = "name@email.com")
        Spacer(Modifier.height(dimens.gap))
        AppField("كلمة المرور", password, { password = it; error = null }, Icons.Outlined.Lock, placeholder = "••••••••",
            trailing = Icons.Outlined.Visibility, onTrailing = { show = !show }, password = true, showText = show)
        error?.let { Spacer(Modifier.height(12.dp)); ErrorBox(it) }
        Spacer(Modifier.height(24.dp))
        AppButton("تسجيل الدخول", {
            when (val r = Prefs.signIn(email, password)) {
                is Prefs.AuthResult.Ok -> onDone(Prefs.gradeId == null)
                is Prefs.AuthResult.Error -> error = r.message
            }
        })
        Spacer(Modifier.height(32.dp))
        Text(
            buildAnnotatedString {
                append("ماعندك حساب؟ ")
                withStyle(SpanStyle(color = AppColors.Primary, fontWeight = FontWeight.Bold)) { append("أنشئ حساب") }
            },
            style = MaterialTheme.typography.bodyMedium, color = AppColors.Muted,
            modifier = Modifier.align(Alignment.CenterHorizontally).clickable(onClick = onCreateAccount).padding(12.dp),
        )
    }
}

@Composable
fun SignupScreen(onBack: () -> Unit, onDone: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var pass2 by remember { mutableStateOf("") }
    var show by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize().background(AppColors.Surface).imePadding()) {
        ScreenHeader("إنشاء حساب", onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = dimens.pad).padding(top = 12.dp)) {
            Text("أهلاً بيك بينا", style = MaterialTheme.typography.displaySmall, color = AppColors.Ink)
            Spacer(Modifier.height(6.dp))
            Text("سوّي حسابك وابدأ دراستك بدقيقة", style = MaterialTheme.typography.bodyMedium, color = AppColors.Muted)
            Spacer(Modifier.height(18.dp))
            AppField("الاسم الكامل", name, { name = it; error = null }, Icons.Outlined.Person, placeholder = "اسمك")
            Spacer(Modifier.height(10.dp))
            AppField("البريد الإلكتروني", email, { email = it; error = null }, Icons.Outlined.MailOutline, placeholder = "name@email.com")
            Spacer(Modifier.height(10.dp))
            AppField("كلمة المرور", pass, { pass = it; error = null }, Icons.Outlined.Lock, placeholder = "6 أحرف على الأقل",
                trailing = Icons.Outlined.Visibility, onTrailing = { show = !show }, password = true, showText = show)
            Spacer(Modifier.height(10.dp))
            AppField("تأكيد كلمة المرور", pass2, { pass2 = it; error = null }, Icons.Outlined.Lock, placeholder = "أعد كتابتها",
                password = true, showText = show)
            error?.let { Spacer(Modifier.height(12.dp)); ErrorBox(it) }
            Spacer(Modifier.height(16.dp))
        }
        Box(Modifier.padding(horizontal = dimens.pad).padding(bottom = 16.dp).navigationBarsPadding()) {
            AppButton("إنشاء الحساب", {
                if (pass != pass2) { error = "كلمتا المرور مو متطابقتين"; return@AppButton }
                when (val r = Prefs.signUp(name, email, pass)) {
                    is Prefs.AuthResult.Ok -> onDone()
                    is Prefs.AuthResult.Error -> error = r.message
                }
            }, icon = Icons.AutoMirrored.Outlined.ArrowForward)
        }
    }
}
