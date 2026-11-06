package iq.quiz.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import iq.quiz.data.Notifier
import iq.quiz.data.Repo
import iq.quiz.ui.components.*
import iq.quiz.ui.theme.*

private class SItem(val key: String, val title: String, val sub: String? = null, val def: Boolean = true)
private class SCat(val title: String, val icon: ImageVector, val tint: Color, val items: List<SItem>)

private val cats = listOf(
    SCat("الإشعارات", Icons.Outlined.NotificationsNone, Color(0xFFEF4444), listOf(
        SItem("n_likes", "الإعجابات"), SItem("n_comments", "التعليقات"), SItem("n_follow", "متابعون جدد"),
        SItem("n_msgs", "الرسائل", "إشعار بأسلوب واتساب"), SItem("n_calls", "المكالمات"), SItem("n_posts", "منشورات الأصدقاء"),
        SItem("n_stories", "قصص الأصدقاء"), SItem("n_remind", "تذكيرات", null, false),
    )),
    SCat("الخصوصية", Icons.Outlined.Lock, Color(0xFF7C5CFF), listOf(
        SItem("private", "حساب خاص", "فقط من توافق عليه يرى منشوراتك", false), SItem("active", "حالة النشاط", "اعرض متى كنت متصلاً"),
        SItem("read", "إشعار القراءة", "علامة «تمت المشاهدة» في الرسائل"), SItem("tags", "السماح بالإشارة إليك"), SItem("mentions", "السماح بالمنشن"),
        SItem("story_reply", "السماح بالرد على قصتك"), SItem("map", "مشاركة موقعي على الخريطة", null, false),
    )),
    SCat("الأصدقاء المقرّبون", Icons.Outlined.Star, Color(0xFF10B981), listOf(
        SItem("close_only", "مشاركة قصصي مع المقرّبين فقط", null, false), SItem("close_ring", "إظهار الحلقة الخضراء للمقرّبين"),
    )),
    SCat("التعليقات والرسائل", Icons.Outlined.ChatBubbleOutline, Color(0xFF3B82F6), listOf(
        SItem("hidden_words", "إخفاء التعليقات المسيئة"), SItem("msg_requests", "طلبات الرسائل من غير المتابَعين"),
        SItem("typing", "إظهار «يكتب الآن»"), SItem("limit_comments", "تقييد التعليقات الجديدة", null, false),
    )),
    SCat("المحتوى والعرض", Icons.Outlined.Tune, Color(0xFFF59E0B), listOf(
        SItem("sugg", "اقتراحات في الرئيسية", "بطاقات «اقتراحات قد تعجبك»"), SItem("autoplay", "تشغيل الفيديو تلقائياً"),
        SItem("sound", "تشغيل الصوت في الريلز", null, true), SItem("hq", "جودة عالية للصور"), SItem("saver", "توفير البيانات", null, false),
    )),
    SCat("حسابك", Icons.Outlined.Person, Color(0xFF0891B2), listOf(
        SItem("save_login", "حفظ معلومات الدخول"), SItem("save_orig", "حفظ نسخة من الصور الملتقطة", null, false),
        SItem("two_fa", "تنبيه عند الدخول من جهاز جديد"),
    )),
)

@Composable
fun SettingsScreen(onBack: () -> Unit, onLogout: () -> Unit, onOpen: (String) -> Unit) {
    val p = pal
    var open by remember { mutableStateOf<SCat?>(null) }
    var q by remember { mutableStateOf("") }
    var lang by remember { mutableStateOf(false) }
    BackHandler(open != null) { open = null }
    val cat = open
    if (cat != null) {
        Column(Modifier.fillMaxSize().background(p.bg)) {
            TopBack(cat.title, { open = null })
            Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
                cat.items.forEach { it ->
                    var on by remember(it.key) { mutableStateOf(Repo.flag(it.key, it.def)) }
                    SettingRow(cat.icon, it.title, it.sub, cat.tint, toggle = on, onToggle = { v -> on = v; Repo.setFlag(it.key, v) })
                }
                Text("يُحفظ كل إعداد على جهازك. التأثير على الآخرين يحتاج سيرفر.", style = MaterialTheme.typography.labelMedium, color = p.faint, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(20.dp))
            }
        }
        return
    }
    Column(Modifier.fillMaxSize().background(p.bg)) {
        TopBack("الإعدادات والنشاط", onBack)
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp).height(46.dp).clip(RoundedCornerShape(23.dp)).background(p.bg2).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Search, null, tint = p.muted, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            BasicTextField(q, { q = it }, Modifier.weight(1f), singleLine = true, textStyle = MaterialTheme.typography.bodyLarge.copy(color = p.ink), cursorBrush = SolidColor(Primary),
                decorationBox = { inner -> if (q.isEmpty()) Text("بحث", style = MaterialTheme.typography.bodyLarge, color = p.muted); inner() })
        }
        Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
            fun hit(t: String) = q.isBlank() || t.contains(q)
            Text("نشاطك", style = MaterialTheme.typography.labelMedium, color = p.muted, modifier = Modifier.padding(16.dp, 12.dp, 16.dp, 4.dp))
            if (hit("الأرشيف")) SettingRow(Icons.Outlined.Archive, "الأرشيف", "منشورات أرشفتها", Color(0xFF6366F1)) { onOpen("archive") }
            if (hit("المحفوظات")) SettingRow(Icons.Outlined.BookmarkBorder, "المحفوظات", null, Color(0xFFEC4899)) { onOpen("saved") }
            if (hit("الإشعارات النشاط")) SettingRow(Icons.Outlined.FavoriteBorder, "النشاط", "إعجابات وتعليقات ومتابعون", Like) { onOpen("activity") }
            Text("إعدادات التطبيق", style = MaterialTheme.typography.labelMedium, color = p.muted, modifier = Modifier.padding(16.dp, 14.dp, 16.dp, 4.dp))
            cats.filter { hit(it.title) || it.items.any { i -> hit(i.title) } }.forEach { c ->
                SettingRow(c.icon, c.title, c.items.take(3).joinToString("، ") { it.title }, c.tint) { open = c }
            }
            if (hit("المظهر") || hit("الوضع الداكن")) SettingRow(Icons.Outlined.DarkMode, "الوضع الداكن", "تغيير ألوان التطبيق", Color(0xFF3B82F6), toggle = Repo.dark, onToggle = { Repo.updateDark(it) })
            if (hit("اللغة")) SettingRow(Icons.Outlined.Language, "اللغة", null, Color(0xFF0891B2), value = "العربية") { lang = true }
            if (q.isBlank()) {
                Text("جرّب الإشعارات", style = MaterialTheme.typography.labelMedium, color = p.muted, modifier = Modifier.padding(16.dp, 14.dp, 16.dp, 4.dp))
                SettingRow(Icons.Outlined.NotificationsNone, "إشعار رسالة", "صورة واسم المرسل والنص مثل واتساب", Primary) { Notifier.message(Repo.appCtx, "سارة أحمد", 1, "هلا! شلونك اليوم؟") }
                SettingRow(Icons.Outlined.Image, "إشعار منشور", "منشور جديد من صديق", Color(0xFFEC4899)) { Notifier.friendPost(Repo.appCtx, "علي حسن", 2, "غروب الشمس على دجلة") }
                SettingRow(Icons.Outlined.Call, "إشعار مكالمة", "مكالمة فيديو فائتة", Success) { Notifier.call(Repo.appCtx, "زينب كريم", 3, true) }
                Text("المحتوى", style = MaterialTheme.typography.labelMedium, color = p.muted, modifier = Modifier.padding(16.dp, 14.dp, 16.dp, 4.dp))
                SettingRow(Icons.Outlined.Science, "محتوى تجريبي", "أشخاص ومنشورات ورسائل وملاحظات وهمية لتجربة التطبيق", Color(0xFFF59E0B), toggle = Repo.demo, onToggle = { Repo.updateDemo(it) })
                Text("الحساب", style = MaterialTheme.typography.labelMedium, color = p.muted, modifier = Modifier.padding(16.dp, 14.dp, 16.dp, 4.dp))
                SettingRow(Icons.Outlined.Logout, "تسجيل الخروج", null, Danger) { Repo.signOut(); onLogout() }
                Text("كويز تواصلكم 0.3 - البيانات محفوظة على جهازك فقط", style = MaterialTheme.typography.labelSmall, color = p.faint, modifier = Modifier.fillMaxWidth().padding(20.dp), textAlign = TextAlign.Center)
            }
        }
    }
    if (lang) AlertDialog(
        onDismissRequest = { lang = false }, confirmButton = { TextButton({ lang = false }) { Text("تمام") } },
        title = { Text("اللغة") }, text = { Text("النسخة الحالية بالعربية فقط. لغات إضافية تأتي بالتحديثات.") },
    )
}
