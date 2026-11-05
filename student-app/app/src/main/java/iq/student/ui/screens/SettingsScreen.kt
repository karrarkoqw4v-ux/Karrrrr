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

@Composable
fun SettingsScreen(onBack: () -> Unit, onChangeGrade: () -> Unit, onLogout: () -> Unit) {
    var key by remember { mutableStateOf(Prefs.apiKey) }
    var model by remember { mutableStateOf(Prefs.model) }
    var rate by remember { mutableFloatStateOf(Prefs.ttsRate) }
    var show by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(AppColors.Bg)) {
        ScreenHeader("الإعدادات", onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = dimens.pad).padding(top = 8.dp, bottom = 24.dp)) {
            Text("الذكاء الاصطناعي", style = MaterialTheme.typography.titleMedium, color = AppColors.Ink)
            Spacer(Modifier.height(10.dp))
            AppCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    AppField("مفتاح الخدمة (API key)", key, { key = it; saved = false }, Icons.Outlined.Lock, placeholder = "sk-ant-...",
                        trailing = Icons.Outlined.Visibility, onTrailing = { show = !show }, password = true, showText = show)
                    Spacer(Modifier.height(6.dp))
                    Text("المفتاح يُحفظ على جهازك فقط ويُرسل لخدمة Claude حتى يشتغل الماسح والترجمة والحل.",
                        style = MaterialTheme.typography.labelSmall, color = AppColors.Muted)
                    Spacer(Modifier.height(12.dp))
                    AppField("اسم الموديل", model, { model = it; saved = false }, Icons.Outlined.Memory, placeholder = Prefs.DEFAULT_MODEL)
                    Spacer(Modifier.height(14.dp))
                    AppButton(if (saved) "انحفظ" else "حفظ", {
                        Prefs.apiKey = key; Prefs.model = model; saved = true
                    }, icon = Icons.Outlined.Check)
                }
            }
            Spacer(Modifier.height(20.dp))
            Text("القراءة الصوتية", style = MaterialTheme.typography.titleMedium, color = AppColors.Ink)
            Spacer(Modifier.height(10.dp))
            AppCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("سرعة القراءة", style = MaterialTheme.typography.bodyMedium, color = AppColors.Ink)
                    Slider(value = rate, onValueChange = { rate = it; Prefs.ttsRate = it }, valueRange = 0.5f..1.5f,
                        colors = SliderDefaults.colors(thumbColor = AppColors.Primary, activeTrackColor = AppColors.Primary))
                    Row {
                        Text("بطيئة", style = MaterialTheme.typography.labelSmall, color = AppColors.Faint, modifier = Modifier.weight(1f))
                        Text("سريعة", style = MaterialTheme.typography.labelSmall, color = AppColors.Faint)
                    }
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
