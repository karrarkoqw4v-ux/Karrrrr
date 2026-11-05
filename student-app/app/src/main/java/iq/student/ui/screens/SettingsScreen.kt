package iq.student.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import iq.student.data.Prefs
import iq.student.ui.components.*
import iq.student.ui.theme.AppColors
import iq.student.ui.theme.dimens
import iq.student.util.rememberSpeaker

@Composable
fun SettingsScreen(onBack: () -> Unit, onChangeGrade: () -> Unit, onLogout: () -> Unit) {
    val speaker = rememberSpeaker()
    var rate by remember { mutableFloatStateOf(Prefs.ttsRate) }
    var base by remember { mutableStateOf(Prefs.ministryBase) }
    var saved by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(AppColors.Bg)) {
        ScreenHeader("الإعدادات", onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = dimens.pad).padding(top = 8.dp, bottom = 24.dp)) {
            Text("القراءة الصوتية", style = MaterialTheme.typography.titleMedium, color = AppColors.Ink)
            Spacer(Modifier.height(10.dp))
            AppCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("سرعة القراءة", style = MaterialTheme.typography.bodyMedium, color = AppColors.Ink)
                    Slider(
                        value = rate, onValueChange = { rate = it; Prefs.ttsRate = it }, valueRange = 0.5f..1.5f,
                        colors = SliderDefaults.colors(thumbColor = AppColors.Primary, activeTrackColor = AppColors.Primary),
                    )
                    Row {
                        Text("بطيئة", style = MaterialTheme.typography.labelSmall, color = AppColors.Faint, modifier = Modifier.weight(1f))
                        Text("سريعة", style = MaterialTheme.typography.labelSmall, color = AppColors.Faint)
                    }
                    Spacer(Modifier.height(10.dp))
                    AppButton("جرّب الصوت", { speaker.speak("مرحباً بك في تطبيق طالب العراق", speaker.arabic) }, icon = Icons.Outlined.VolumeUp, outlined = true)
                }
            }
            Spacer(Modifier.height(20.dp))
            Text("بنك الأسئلة الوزارية (اختياري)", style = MaterialTheme.typography.titleMedium, color = AppColors.Ink)
            Spacer(Modifier.height(10.dp))
            AppCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    AppField("رابط مجلد ملفات الأسئلة", base, { base = it; saved = false }, Icons.Outlined.Public, placeholder = "https://...")
                    Spacer(Modifier.height(6.dp))
                    Text("ملفات بصيغة مادة_سنة_دور.json مثل chemistry_2024_1.json. تُجلب عند توفر الإنترنت.", style = MaterialTheme.typography.labelSmall, color = AppColors.Muted)
                    Spacer(Modifier.height(12.dp))
                    AppButton(if (saved) "انحفظ" else "حفظ", { Prefs.ministryBase = base; saved = true }, icon = Icons.Outlined.Check)
                }
            }
            Spacer(Modifier.height(20.dp))
            Text("الحساب", style = MaterialTheme.typography.titleMedium, color = AppColors.Ink)
            Spacer(Modifier.height(10.dp))
            AppButton("تغيير الصف", onChangeGrade, icon = Icons.Outlined.School, outlined = true)
            Spacer(Modifier.height(10.dp))
            AppButton("تسجيل الخروج", { Prefs.signOut(); onLogout() }, icon = Icons.Outlined.Logout, outlined = true)
        }
    }
}
