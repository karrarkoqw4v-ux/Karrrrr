package iq.dablik

class Perm(val id: String, val title: String, val danger: Boolean)
class PermGroup(val title: String, val items: List<Perm>)

/** كتالوج صلاحيات أندرويد التي يمكن تفعيلها للتطبيق الناتج */
object Perms {
    private const val CATALOG = """
# الشبكة والاتصال
INTERNET|الوصول إلى الإنترنت
ACCESS_NETWORK_STATE|معرفة حالة الشبكة
ACCESS_WIFI_STATE|معرفة حالة الواي فاي
CHANGE_WIFI_STATE|تغيير حالة الواي فاي
CHANGE_NETWORK_STATE|تغيير حالة الشبكة
CHANGE_WIFI_MULTICAST_STATE|البث المتعدد عبر الواي فاي
NEARBY_WIFI_DEVICES|أجهزة الواي فاي القريبة|d
# الموقع
ACCESS_FINE_LOCATION|الموقع الدقيق (GPS)|d
ACCESS_COARSE_LOCATION|الموقع التقريبي|d
ACCESS_BACKGROUND_LOCATION|الموقع في الخلفية|d
ACCESS_MEDIA_LOCATION|مواقع التقاط الصور والفيديو|d
ACCESS_LOCATION_EXTRA_COMMANDS|أوامر موقع إضافية
# الكاميرا والصوت
CAMERA|الكاميرا|d
RECORD_AUDIO|تسجيل الصوت (الميكروفون)|d
MODIFY_AUDIO_SETTINGS|تعديل إعدادات الصوت
FLASHLIGHT|مصباح الفلاش
# التخزين والوسائط
READ_EXTERNAL_STORAGE|قراءة التخزين|d
WRITE_EXTERNAL_STORAGE|الكتابة في التخزين|d
READ_MEDIA_IMAGES|قراءة الصور|d
READ_MEDIA_VIDEO|قراءة الفيديو|d
READ_MEDIA_AUDIO|قراءة الصوتيات|d
READ_MEDIA_VISUAL_USER_SELECTED|الوسائط التي يختارها المستخدم|d
MANAGE_EXTERNAL_STORAGE|إدارة كل الملفات
MANAGE_MEDIA|إدارة الوسائط
# الإشعارات والتشغيل
POST_NOTIFICATIONS|إظهار الإشعارات|d
VIBRATE|الاهتزاز
WAKE_LOCK|إبقاء الجهاز مستيقظاً
USE_FULL_SCREEN_INTENT|إشعارات بملء الشاشة
SCHEDULE_EXACT_ALARM|جدولة منبهات دقيقة
USE_EXACT_ALARM|منبهات دقيقة (تطبيقات المنبه)
RECEIVE_BOOT_COMPLETED|التشغيل عند إقلاع الجهاز
ACCESS_NOTIFICATION_POLICY|الوصول إلى وضع عدم الإزعاج
REQUEST_IGNORE_BATTERY_OPTIMIZATIONS|تجاهل توفير البطارية
FOREGROUND_SERVICE|خدمة في المقدمة
FOREGROUND_SERVICE_DATA_SYNC|خدمة مزامنة بيانات
FOREGROUND_SERVICE_LOCATION|خدمة الموقع
FOREGROUND_SERVICE_CAMERA|خدمة الكاميرا
FOREGROUND_SERVICE_MICROPHONE|خدمة الميكروفون
FOREGROUND_SERVICE_MEDIA_PLAYBACK|خدمة تشغيل الوسائط
FOREGROUND_SERVICE_CONNECTED_DEVICE|خدمة الأجهزة المتصلة
FOREGROUND_SERVICE_HEALTH|خدمة الصحة
FOREGROUND_SERVICE_SPECIAL_USE|خدمة لاستخدام خاص
# الهاتف وجهات الاتصال
READ_CONTACTS|قراءة جهات الاتصال|d
WRITE_CONTACTS|تعديل جهات الاتصال|d
GET_ACCOUNTS|قراءة الحسابات|d
READ_PHONE_STATE|حالة الهاتف|d
READ_PHONE_NUMBERS|أرقام الهاتف|d
CALL_PHONE|إجراء مكالمات|d
ANSWER_PHONE_CALLS|الرد على المكالمات|d
READ_CALL_LOG|قراءة سجل المكالمات|d
WRITE_CALL_LOG|تعديل سجل المكالمات|d
ADD_VOICEMAIL|إضافة بريد صوتي|d
USE_SIP|استخدام SIP|d
MANAGE_OWN_CALLS|إدارة مكالمات التطبيق
# الرسائل
SEND_SMS|إرسال رسائل SMS|d
RECEIVE_SMS|استقبال رسائل SMS|d
READ_SMS|قراءة رسائل SMS|d
RECEIVE_MMS|استقبال MMS|d
RECEIVE_WAP_PUSH|استقبال WAP Push|d
# التقويم
READ_CALENDAR|قراءة التقويم|d
WRITE_CALENDAR|تعديل التقويم|d
# الحساسات واللياقة
BODY_SENSORS|حساسات الجسم|d
BODY_SENSORS_BACKGROUND|حساسات الجسم في الخلفية|d
ACTIVITY_RECOGNITION|التعرف على النشاط البدني|d
HIGH_SAMPLING_RATE_SENSORS|حساسات بمعدل عالٍ
UWB_RANGING|تحديد المسافة بـ UWB|d
# البلوتوث والاتصال القريب
BLUETOOTH|البلوتوث
BLUETOOTH_ADMIN|إدارة البلوتوث
BLUETOOTH_CONNECT|الاتصال بأجهزة البلوتوث|d
BLUETOOTH_SCAN|البحث عن أجهزة البلوتوث|d
BLUETOOTH_ADVERTISE|الظهور لأجهزة البلوتوث|d
NFC|NFC
NFC_TRANSACTION_EVENT|أحداث معاملات NFC
TRANSMIT_IR|مرسل الأشعة تحت الحمراء
# النظام والنوافذ
SYSTEM_ALERT_WINDOW|الظهور فوق التطبيقات
WRITE_SETTINGS|تعديل إعدادات النظام
REQUEST_INSTALL_PACKAGES|تثبيت التطبيقات
REQUEST_DELETE_PACKAGES|حذف التطبيقات
QUERY_ALL_PACKAGES|رؤية كل التطبيقات المثبتة
PACKAGE_USAGE_STATS|إحصائيات استخدام التطبيقات
DISABLE_KEYGUARD|تعطيل قفل الشاشة
EXPAND_STATUS_BAR|التحكم بشريط الحالة
GET_TASKS|معرفة المهام الجارية
KILL_BACKGROUND_PROCESSES|إنهاء عمليات الخلفية
REORDER_TASKS|ترتيب المهام
SET_WALLPAPER|تغيير الخلفية
SET_WALLPAPER_HINTS|تلميحات الخلفية
BROADCAST_STICKY|بث ثابت
CHANGE_CONFIGURATION|تغيير الإعدادات
READ_SYNC_SETTINGS|قراءة إعدادات المزامنة
WRITE_SYNC_SETTINGS|تعديل إعدادات المزامنة
READ_SYNC_STATS|قراءة إحصائيات المزامنة
SET_TIME_ZONE|تغيير المنطقة الزمنية
com.android.alarm.permission.SET_ALARM|ضبط المنبه
com.android.launcher.permission.INSTALL_SHORTCUT|إنشاء اختصارات
com.android.launcher.permission.UNINSTALL_SHORTCUT|حذف الاختصارات
# خدمات جوجل
com.google.android.gms.permission.AD_ID|معرّف الإعلانات
com.google.android.c2dm.permission.RECEIVE|إشعارات FCM
com.android.vending.BILLING|الدفع داخل التطبيق
com.android.vending.CHECK_LICENSE|فحص الترخيص
"""

    val groups: List<PermGroup> by lazy {
        val out = ArrayList<PermGroup>(); var title = ""; var cur = ArrayList<Perm>()
        fun flush() { if (title.isNotEmpty()) out.add(PermGroup(title, cur.toList())); cur = ArrayList() }
        for (line in CATALOG.lines().map { it.trim() }.filter { it.isNotEmpty() }) {
            if (line.startsWith("#")) { flush(); title = line.removePrefix("#").trim() }
            else {
                val p = line.split("|")
                val id = if (p[0].contains('.')) p[0] else "android.permission.${p[0]}"
                cur.add(Perm(id, p[1], p.getOrNull(2) == "d"))
            }
        }
        flush(); out
    }

    val total get() = groups.sumOf { it.items.size }
    val defaults = listOf("android.permission.INTERNET", "android.permission.ACCESS_NETWORK_STATE")
}
