package iq.student.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import iq.student.ui.components.AppButton
import iq.student.ui.components.AppField
import iq.student.ui.theme.AppColors
import iq.student.ui.theme.dimens

@Composable
fun LoginScreen(onLogin: () -> Unit, onCreateAccount: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var show by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().background(AppColors.Surface)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding().navigationBarsPadding().imePadding()
            .padding(horizontal = dimens.pad).padding(top = 28.dp, bottom = 20.dp),
    ) {
        Box(
            Modifier.size(56.dp).clip(RoundedCornerShape(18.dp)).background(AppColors.PrimarySoft),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Outlined.School, null, tint = AppColors.Primary, modifier = Modifier.size(30.dp)) }
        Spacer(Modifier.height(20.dp))
        Text("هلا بيك من جديد", style = MaterialTheme.typography.displaySmall, color = AppColors.Ink)
        Spacer(Modifier.height(6.dp))
        Text("سجّل دخولك وكمّل دراستك من وين وقفت", style = MaterialTheme.typography.bodyMedium, color = AppColors.Muted)
        Spacer(Modifier.height(24.dp))

        AppField("البريد الإلكتروني أو رقم الهاتف", email, { email = it }, Icons.Outlined.MailOutline,
            placeholder = "name@email.com")
        Spacer(Modifier.height(dimens.gap))
        AppField("كلمة المرور", password, { password = it }, Icons.Outlined.Lock, placeholder = "••••••••",
            trailing = Icons.Outlined.Visibility, onTrailing = { show = !show }, password = true, showText = show)
        Spacer(Modifier.height(12.dp))
        Text("نسيت كلمة المرور؟", style = MaterialTheme.typography.labelMedium, color = AppColors.Primary,
            modifier = Modifier.clickable { })
        Spacer(Modifier.height(24.dp))

        AppButton("تسجيل الدخول", onLogin)
        Spacer(Modifier.height(22.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            HorizontalDivider(Modifier.weight(1f), color = AppColors.Line)
            Text("  أو  ", style = MaterialTheme.typography.labelMedium, color = AppColors.Faint)
            HorizontalDivider(Modifier.weight(1f), color = AppColors.Line)
        }
        Spacer(Modifier.height(20.dp))
        AppButton("المتابعة باستخدام Google", onLogin, icon = Icons.Outlined.Public, outlined = true)
        Spacer(Modifier.height(dimens.gap))
        AppButton("الدخول برقم الهاتف", onLogin, icon = Icons.Outlined.PhoneAndroid, outlined = true)

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
