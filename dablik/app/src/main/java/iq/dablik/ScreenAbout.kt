package iq.dablik

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
private fun InfoRow(icon: ImageVector, tint: Color, title: String, sub: String) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp), verticalAlignment = Alignment.Top) {
        Box(Modifier.size(46.dp).clip(CircleShape).background(tint.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) { Icon(icon, null, tint = tint, modifier = Modifier.size(25.dp)) }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = OneUi.Ink)
            Text(sub, style = MaterialTheme.typography.labelMedium, color = OneUi.Muted)
        }
    }
}

@Composable
private fun Section(title: String) {
    Text(title, style = MaterialTheme.typography.titleMedium, color = OneUi.Ink, modifier = Modifier.padding(start = 10.dp, top = 22.dp, bottom = 10.dp))
}

@Composable
fun AboutScreen(onBack: () -> Unit) {
    Box(Modifier.fillMaxSize().background(OneUi.Bg)) {
        Column(Modifier.fillMaxSize()) {
            OneTopBar("عن التطبيق", onBack)
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 60.dp)) {
                // الشعار والإصدار
                Column(Modifier.fillMaxWidth().padding(vertical = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(92.dp).clip(RoundedCornerShape(28.dp)).background(OneUi.Blue), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Inventory2, null, tint = Color.White, modifier = Modifier.size(50.dp))
                    }
                    Text("دبليك", style = MaterialTheme.typography.headlineSmall, color = OneUi.Ink, modifier = Modifier.padding(top = 12.dp))
                    Text("الإصدار 1.0.0", style = MaterialTheme.typography.labelMedium, color = OneUi.Muted)
                }
                Section("ماذا يفعل")
                OneCard {
                    InfoRow(Icons.Outlined.Android, OneUi.Blue, "يحوّل موقعك إلى تطبيق", "صفحة HTML أو ملف ZIP لموقع كامل يصيران تطبيق أندرويد قابل للتثبيت.")
                    InfoRow(Icons.Outlined.ImageSearch, Color(0xFFFF9F1C), "أيقونة بتصميمك", "اختر صورة من المعرض وعدّل القص والألوان والنص والرسم والتغويش.")
                    InfoRow(Icons.Outlined.Inventory2, OneUi.Green, "سجل تطبيقاتك", "كل تطبيق تبنيه يُحفظ على جهازك لتثبته أو تشاركه أو تحذفه.")
                }
                Section("كيف يعمل")
                OneCard {
                    InfoRow(Icons.Outlined.Layers, OneUi.Blue, "1. قالب جاهز", "يستخدم قالب WebView داخل التطبيق ويعرض موقعك من الملفات المحلية.")
                    InfoRow(Icons.Outlined.Edit, Color(0xFFAF52DE), "2. تعديل الملف التعريفي", "يغيّر الاسم والحزمة ورقم الإصدار داخل AndroidManifest.")
                    InfoRow(Icons.Outlined.FolderZip, OneUi.Green, "3. ملفات الموقع", "تُنسخ ملفاتك إلى مجلد assets/www داخل الحزمة.")
                    InfoRow(Icons.Outlined.VpnKey, Color(0xFFFF6BAA), "4. التوقيع والتحقق", "توقيع v1 + v2 ثم فحص الحزمة قبل عرض زر التثبيت.")
                }
                Section("معلومات مهمة")
                OneCard {
                    InfoRow(Icons.Outlined.WifiOff, OneUi.Blue, "يعمل بدون إنترنت", "بناء التطبيقات يتم بالكامل على جهازك.")
                    InfoRow(Icons.Outlined.Security, OneUi.Green, "توقيع مشترك", "كل التطبيقات تُوقَّع بشهادة دبليك نفسها، ومناسبة للتجربة والمشاركة وليس للنشر في المتاجر.")
                    InfoRow(Icons.Outlined.Language, Color(0xFFFF9F1C), "مواقع HTML فقط", "التطبيق الناتج يعرض موقع HTML/JS/CSS. لا يحوّل مشاريع Kotlin أو Java.")
                    InfoRow(Icons.Outlined.InstallMobile, OneUi.Muted, "إذن التثبيت", "أول مرة يطلب أندرويد السماح لدبليك بتثبيت التطبيقات.")
                }
                Text("صُنع بواجهة مستوحاة من One UI", style = MaterialTheme.typography.labelSmall, color = OneUi.Faint, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 24.dp))
            }
        }
    }
}
