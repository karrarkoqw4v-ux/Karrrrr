package iq.hala.data

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.util.Base64
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

data class User(val id: Int, var name: String, var username: String, val idx: Int, var bio: String = "", val followers: Int = 0, val following: Int = 0)
data class Comment(val userId: Int, val text: String, val time: String)
class Post(val id: Long, val userId: Int, val kind: Int, val imagePath: String?, val caption: String, likes: Int, val time: String, val comments: MutableList<Comment> = mutableListOf()) {
    var likes by mutableStateOf(likes)
    var liked by mutableStateOf(false)
    var saved by mutableStateOf(false)
    val commentCount = mutableStateOf(comments.size)
}
class Msg(val id: Long, val mine: Boolean, val text: String, val imagePath: String? = null, val time: String = "الآن")
class Chat(val id: Int, val userId: Int, val title: String, val group: Boolean = false, val msgs: MutableList<Msg> = mutableStateListOf()) {
    var unread by mutableStateOf(0)
    var typing by mutableStateOf(false)
}

/**
 * مخزن البيانات المحلي (على الجهاز فقط). لا يوجد سيرفر: المنشورات والرسائل تُحفظ بهذا الجهاز،
 * وردود الأصدقاء تجريبية (بوت) لعرض شكل المحادثة. استبدل هذا الكلاس بسيرفر حقيقي (Firebase/Supabase) لاحقاً.
 */
object Repo {
    private lateinit var sp: SharedPreferences
    private lateinit var media: File

    val users = listOf(
        User(1, "سارة علي", "sara.ali", 0, "مصورة هاوية - بغداد", 2400, 310),
        User(2, "علي حسين", "ali.h", 1, "مهندس برمجيات", 1200, 280),
        User(3, "نور محمد", "noor.m", 2, "أحب الطبيعة والسفر", 890, 402),
        User(4, "حسن كريم", "hasan.k", 3, "طالب طب - البصرة", 640, 220),
        User(5, "مريم أحمد", "maryam.a", 4, "فنانة تشكيلية", 3100, 190),
        User(6, "كرار الموسوي", "karrar.m", 5, "مهندس - أحب السفر والتصوير", 1100, 402),
        User(7, "زينب علي", "zainab.a", 6, "طباخة هاوية", 760, 150),
        User(8, "أحمد", "ahmed", 7, "", 300, 90),
    )
    val me = User(0, "أنا", "me", 4)

    val posts = mutableStateListOf<Post>()
    val chats = mutableStateListOf<Chat>()
    var loggedIn by mutableStateOf(false)
    var dark by mutableStateOf(false)

    fun user(id: Int) = if (id == 0) me else users.firstOrNull { it.id == id } ?: users[0]

    fun init(ctx: Context) {
        sp = ctx.applicationContext.getSharedPreferences("hala", Context.MODE_PRIVATE)
        media = File(ctx.filesDir, "media").apply { mkdirs() }
        dark = sp.getBoolean("dark", false)
        sp.getString("cur", null)?.let { loadMe(it) }
        seed()
        loadUserPosts()
    }

    // ---------- بيانات تجريبية ----------
    private fun seed() {
        if (posts.isNotEmpty()) return
        val caps = listOf("غروب الشمس على دجلة", "صباح الخير من الجبال", "الربيع بالحديقة", "ليل بغداد", "رحلة الأهوار", "قهوة الصباح", "دفء الصحراء", "نسمة بحرية")
        val ago = listOf("قبل ساعتين", "قبل 5 ساعات", "أمس", "قبل يومين", "قبل 3 أيام", "الأسبوع الماضي", "الأسبوع الماضي", "قبل شهر")
        for (i in 0 until 8) {
            val p = Post(100L + i, users[i % users.size].id, i, null, caps[i], 400 + i * 173, ago[i])
            p.comments += Comment(users[(i + 1) % 8].id, "جميلة جداً!", "5 د")
            p.comments += Comment(users[(i + 2) % 8].id, "تصوير رائع", "12 د")
            p.commentCount.value = p.comments.size
            posts.add(p)
        }
        val first = listOf("هلا بيك! شلونك اليوم؟", "اجتماع الليلة الساعة 8", "شكراً على الصورة، حلوة!", "تمام", "رسالة صوتية", "شلونك؟")
        for ((i, u) in users.take(6).withIndex()) {
            val c = Chat(u.id, u.id, u.name)
            c.msgs.add(Msg(1, false, first[i]))
            c.unread = if (i < 2) 2 - i else 0
            chats.add(c)
        }
        chats.add(1, Chat(99, 2, "شباب بغداد", true).also { it.msgs.add(Msg(1, false, "علي: الاجتماع الساعة 8")); it.unread = 5 })
        // رسائل محفوظة
        loadMessages()
    }

    // ---------- الحسابات (محلية) ----------
    private fun hash(pass: String, salt: ByteArray): String {
        val spec = PBEKeySpec(pass.toCharArray(), salt, 60_000, 256)
        return Base64.encodeToString(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded, Base64.NO_WRAP)
    }

    sealed interface Result { data object Ok : Result; data class Fail(val msg: String) : Result }

    fun signUp(contact: String, name: String, username: String, pass: String): Result {
        val u = username.trim().lowercase()
        val c = contact.trim().lowercase()
        if (c.length < 5) return Result.Fail("اكتب رقم هاتف أو بريد صحيح")
        if (name.isBlank()) return Result.Fail("اكتب اسمك")
        if (u.length < 3 || !u.all { it.isLetterOrDigit() || it == '.' || it == '_' }) return Result.Fail("اسم المستخدم 3 أحرف على الأقل (حروف وأرقام ونقطة)")
        if (pass.length < 6) return Result.Fail("كلمة المرور 6 أحرف على الأقل")
        if (sp.contains("acc_u_$u") || sp.contains("acc_c_$c")) return Result.Fail("الحساب موجود، سجّل دخول")
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val acc = JSONObject().put("name", name.trim()).put("username", u).put("contact", c)
            .put("salt", Base64.encodeToString(salt, Base64.NO_WRAP)).put("hash", hash(pass, salt)).put("bio", "")
        sp.edit().putString("acc_u_$u", acc.toString()).putString("acc_c_$c", u).putString("cur", u).apply()
        loadMe(u)
        return Result.Ok
    }

    fun signIn(id: String, pass: String): Result {
        val key = id.trim().lowercase()
        val u = if (sp.contains("acc_u_$key")) key else sp.getString("acc_c_$key", null) ?: return Result.Fail("ما لقينا حساب بهذا الاسم")
        val acc = JSONObject(sp.getString("acc_u_$u", null) ?: return Result.Fail("الحساب غير موجود"))
        val salt = Base64.decode(acc.getString("salt"), Base64.NO_WRAP)
        if (hash(pass, salt) != acc.getString("hash")) return Result.Fail("كلمة المرور غلط")
        sp.edit().putString("cur", u).apply()
        loadMe(u)
        return Result.Ok
    }

    fun usernameFree(u: String) = !sp.contains("acc_u_${u.trim().lowercase()}")

    private fun loadMe(u: String) {
        val acc = JSONObject(sp.getString("acc_u_$u", null) ?: return)
        me.name = acc.optString("name"); me.username = acc.optString("username"); me.bio = acc.optString("bio")
        loggedIn = true
    }

    fun updateMe(name: String, username: String, bio: String) {
        val u = sp.getString("cur", null) ?: return
        val acc = JSONObject(sp.getString("acc_u_$u", null) ?: return)
        acc.put("name", name.trim()).put("bio", bio.trim())
        sp.edit().putString("acc_u_$u", acc.toString()).apply()
        me.name = name.trim(); me.bio = bio.trim()
    }

    fun signOut() { sp.edit().remove("cur").apply(); loggedIn = false }

    fun updateDark(v: Boolean) { dark = v; sp.edit().putBoolean("dark", v).apply() }
    fun flag(key: String, def: Boolean) = sp.getBoolean("f_$key", def)
    fun setFlag(key: String, v: Boolean) { sp.edit().putBoolean("f_$key", v).apply() }

    // ---------- الوسائط ----------
    suspend fun importImage(ctx: Context, uri: Uri): String = withContext(Dispatchers.IO) {
        val f = File(media, "img_${System.currentTimeMillis()}.jpg")
        ctx.contentResolver.openInputStream(uri)?.use { i -> f.outputStream().use { i.copyTo(it) } } ?: error("تعذر قراءة الصورة")
        f.absolutePath
    }

    // ---------- المنشورات ----------
    fun addPost(imagePath: String, caption: String) {
        val p = Post(System.currentTimeMillis(), 0, 0, imagePath, caption, 0, "الآن")
        posts.add(0, p)
        val arr = JSONArray(sp.getString("my_posts", "[]"))
        arr.put(JSONObject().put("id", p.id).put("img", imagePath).put("cap", caption))
        sp.edit().putString("my_posts", arr.toString()).apply()
    }

    private fun loadUserPosts() {
        val arr = JSONArray(sp.getString("my_posts", "[]"))
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            if (File(o.getString("img")).exists()) posts.add(0, Post(o.getLong("id"), 0, 0, o.getString("img"), o.getString("cap"), 0, "قبل قليل"))
        }
    }

    fun addComment(p: Post, text: String) {
        p.comments.add(Comment(0, text, "الآن")); p.commentCount.value = p.comments.size
    }

    // ---------- الرسائل ----------
    fun send(chat: Chat, text: String, imagePath: String? = null) {
        val m = Msg(System.currentTimeMillis(), true, text, imagePath)
        chat.msgs.add(m)
        saveMessages()
    }

    suspend fun botReply(chat: Chat) {
        val replies = listOf("هلا والله!", "تمام، شكراً", "ههههه حلو", "باجر نشوفك ان شاء الله", "أكيد! متى؟", "حبيبي والله", "شلونك أنت؟", "أرسل لي التفاصيل")
        chat.typing = true
        kotlinx.coroutines.delay(1400)
        chat.typing = false
        chat.msgs.add(Msg(System.currentTimeMillis(), false, replies[(chat.msgs.size * 7 + chat.id) % replies.size]))
        saveMessages()
    }

    private fun saveMessages() {
        val o = JSONObject()
        for (c in chats) {
            val arr = JSONArray()
            for (m in c.msgs.filter { it.id > 1000 }) arr.put(JSONObject().put("id", m.id).put("mine", m.mine).put("t", m.text).put("img", m.imagePath ?: ""))
            o.put(c.id.toString(), arr)
        }
        sp.edit().putString("msgs", o.toString()).apply()
    }

    private fun loadMessages() {
        val o = JSONObject(sp.getString("msgs", "{}"))
        for (c in chats) {
            val arr = o.optJSONArray(c.id.toString()) ?: continue
            for (i in 0 until arr.length()) {
                val m = arr.getJSONObject(i)
                c.msgs.add(Msg(m.getLong("id"), m.getBoolean("mine"), m.getString("t"), m.getString("img").ifEmpty { null }))
            }
        }
    }
}
