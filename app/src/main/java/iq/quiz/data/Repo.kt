package iq.quiz.data

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
class Comment(val userId: Int, val text: String, val time: String) {
    var liked by mutableStateOf(false)
    var likes by mutableStateOf(0)
}
class Post(val id: Long, val userId: Int, val kind: Int, val imagePath: String?, caption: String, likes: Int, val time: String, val comments: MutableList<Comment> = mutableListOf(), val videoPath: String? = null) {
    var caption by mutableStateOf(caption)
    var likes by mutableStateOf(likes)
    var liked by mutableStateOf(false)
    var saved by mutableStateOf(false)
    var archived by mutableStateOf(false)
    var hideLikes by mutableStateOf(false)
    var commentsOff by mutableStateOf(false)
    var location by mutableStateOf("")
    val commentCount = mutableStateOf(comments.size)
}
class Story(val id: Long, val path: String, val time: Long)
class Msg(val id: Long, val mine: Boolean, val text: String, val imagePath: String? = null, val time: String = "الآن") {
    var reaction by mutableStateOf("")
}
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
    var dark by mutableStateOf(true)
    /** محتوى تجريبي (أشخاص ومنشورات ورسائل وهمية): مغلق افتراضياً */
    var demo by mutableStateOf(false)
    var foreground = false
    lateinit var appCtx: Context

    // ---------- المتابعة ----------
    val following = mutableStateListOf<Int>()
    fun isFollowing(id: Int) = id in following
    fun toggleFollow(id: Int) {
        if (!following.remove(id)) following.add(id)
        sp.edit().putString("following", following.joinToString(",")).apply()
    }
    fun followersOf(u: User) = u.followers + if (isFollowing(u.id)) 1 else 0
    /** المعجبون بمنشور (ثابتون لكل منشور) */
    fun likers(p: Post): List<User> = users.shuffled(kotlin.random.Random(p.id)).take(6)

    fun user(id: Int) = if (id == 0) me else users.firstOrNull { it.id == id } ?: users[0]

    fun init(ctx: Context) {
        appCtx = ctx.applicationContext
        sp = ctx.applicationContext.getSharedPreferences("hala", Context.MODE_PRIVATE)
        media = File(ctx.filesDir, "media").apply { mkdirs() }
        dark = sp.getBoolean("dark", true)
        demo = sp.getBoolean("demo", true)
        sp.getString("following", "")?.split(",")?.mapNotNull { it.toIntOrNull() }?.let { following.addAll(it) }
        sp.getString("cur", null)?.let { loadMe(it) }
        seed()
        loadUserPosts()
        if (System.currentTimeMillis() - sp.getLong("note_t", 0) < 24 * 3600_000L) note = sp.getString("note", "") ?: ""
    }

    // ---------- بيانات تجريبية ----------
    private fun seed() {
        chats.add(Chat(0, 0, "مذكرتي - رسائل لنفسك"))
        if (demo) seedDemo()
        loadMessages()
    }

    fun updateDemo(v: Boolean) {
        if (demo == v) return
        demo = v
        sp.edit().putBoolean("demo", v).apply()
        posts.removeAll { it.userId != 0 }
        chats.removeAll { it.id != 0 }
        if (v) { seedDemo(); loadMessages(skipSelf = true) }
    }

    private fun seedDemo() {
        if (posts.any { it.userId != 0 }) return
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
    var note by mutableStateOf("")
    fun saveNote(v: String) { note = v.take(60); sp.edit().putString("note", note).putLong("note_t", System.currentTimeMillis()).apply() }

    val myStories = mutableStateListOf<Story>()
    val hiddenIds = mutableStateListOf<Long>()
    val mutedUsers = mutableStateListOf<Int>()

    fun addPost(imagePath: String, caption: String, location: String = "", hideLikes: Boolean = false, commentsOff: Boolean = false) {
        val p = Post(System.currentTimeMillis(), 0, 0, imagePath, caption, 0, "الآن")
        p.location = location; p.hideLikes = hideLikes; p.commentsOff = commentsOff
        posts.add(0, p); persistMine()
    }

    fun deletePost(p: Post) { posts.remove(p); persistMine() }

    fun newMediaFile(ext: String) = File(media, "m_${System.currentTimeMillis()}.$ext")

    suspend fun importVideo(ctx: Context, uri: Uri): String = withContext(Dispatchers.IO) {
        val f = newMediaFile("mp4")
        ctx.contentResolver.openInputStream(uri)!!.use { i -> f.outputStream().use { o -> i.copyTo(o) } }
        f.absolutePath
    }

    fun addReel(videoPath: String, caption: String, location: String = "", hideLikes: Boolean = false, commentsOff: Boolean = false) {
        val p = Post(System.currentTimeMillis(), 0, 0, null, caption, 0, "الآن", videoPath = videoPath)
        p.location = location; p.hideLikes = hideLikes; p.commentsOff = commentsOff
        posts.add(0, p); persistMine()
    }

    /** يحفظ منشوراتي وريلزي كاملة (الوصف، الأرشفة، إخفاء الإعجابات...) */
    fun persistMine() {
        val ps = JSONArray(); val rs = JSONArray()
        posts.filter { it.userId == 0 }.reversed().forEach { p ->
            val o = JSONObject().put("id", p.id).put("cap", p.caption).put("arch", p.archived).put("hide", p.hideLikes).put("coff", p.commentsOff).put("loc", p.location)
            if (p.videoPath != null) rs.put(o.put("vid", p.videoPath)) else ps.put(o.put("img", p.imagePath ?: ""))
        }
        sp.edit().putString("my_posts", ps.toString()).putString("my_reels", rs.toString()).apply()
    }

    private fun readPost(o: JSONObject, video: Boolean): Post? {
        val path = if (video) o.optString("vid") else o.optString("img")
        if (!File(path).exists()) return null
        val p = Post(o.getLong("id"), 0, 0, if (video) null else path, o.optString("cap"), 0, "قبل قليل", videoPath = if (video) path else null)
        p.archived = o.optBoolean("arch"); p.hideLikes = o.optBoolean("hide"); p.commentsOff = o.optBoolean("coff"); p.location = o.optString("loc")
        return p
    }

    private fun loadUserPosts() {
        val rs = JSONArray(sp.getString("my_reels", "[]"))
        for (i in 0 until rs.length()) readPost(rs.getJSONObject(i), true)?.let { posts.add(0, it) }
        val arr = JSONArray(sp.getString("my_posts", "[]"))
        for (i in 0 until arr.length()) readPost(arr.getJSONObject(i), false)?.let { posts.add(0, it) }
        // القصص: تنتهي بعد 24 ساعة
        val st = JSONArray(sp.getString("my_stories", "[]")); val now = System.currentTimeMillis()
        for (i in 0 until st.length()) {
            val o = st.getJSONObject(i)
            if (now - o.getLong("t") < 24 * 3600_000L && File(o.getString("p")).exists()) myStories.add(Story(o.getLong("t"), o.getString("p"), o.getLong("t")))
        }
        sp.getString("hidden", "")?.split(",")?.mapNotNull { it.toLongOrNull() }?.let { hiddenIds.addAll(it) }
    }

    private fun saveStories() {
        val a = JSONArray(); myStories.forEach { a.put(JSONObject().put("p", it.path).put("t", it.time)) }
        sp.edit().putString("my_stories", a.toString()).apply()
    }
    fun addStory(path: String) { myStories.add(Story(System.currentTimeMillis(), path, System.currentTimeMillis())); saveStories() }
    fun deleteStory(s: Story) { myStories.remove(s); saveStories() }
    fun hidePost(p: Post) { hiddenIds.add(p.id); sp.edit().putString("hidden", hiddenIds.joinToString(",")).apply() }

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
        if (!demo || chat.id == 0) return
        val replies = listOf("هلا والله!", "تمام، شكراً", "ههههه حلو", "باجر نشوفك ان شاء الله", "أكيد! متى؟", "حبيبي والله", "شلونك أنت؟", "أرسل لي التفاصيل")
        chat.typing = true
        kotlinx.coroutines.delay(1400)
        chat.typing = false
        val reply = replies[(chat.msgs.size * 7 + chat.id) % replies.size]
        chat.msgs.add(Msg(System.currentTimeMillis(), false, reply))
        chat.unread += 0
        saveMessages()
        if (!foreground && flag("n_msgs", true)) Notifier.message(appCtx, chat.title, user(chat.userId).idx, reply, chat.id)
    }

    fun deleteMsg(chat: Chat, m: Msg) { chat.msgs.remove(m); saveMessages() }

    /** فلاتر الصور: تُدمج في الملف عند النشر. 0 أصلي */
    val filterNames = listOf("أصلي", "أبيض وأسود", "دافئ", "بارد", "قديم", "ساطع")
    fun filterMatrix(kind: Int): FloatArray? = when (kind) {
        1 -> floatArrayOf(.33f, .59f, .11f, 0f, 0f, .33f, .59f, .11f, 0f, 0f, .33f, .59f, .11f, 0f, 0f, 0f, 0f, 0f, 1f, 0f)
        2 -> floatArrayOf(1.12f, 0f, 0f, 0f, 8f, 0f, 1.02f, 0f, 0f, 0f, 0f, 0f, .86f, 0f, -6f, 0f, 0f, 0f, 1f, 0f)
        3 -> floatArrayOf(.9f, 0f, 0f, 0f, -4f, 0f, 1f, 0f, 0f, 0f, 0f, 0f, 1.15f, 0f, 10f, 0f, 0f, 0f, 1f, 0f)
        4 -> floatArrayOf(.39f, .77f, .19f, 0f, 0f, .35f, .69f, .17f, 0f, 0f, .27f, .53f, .13f, 0f, 0f, 0f, 0f, 0f, 1f, 0f)
        5 -> floatArrayOf(1.2f, 0f, 0f, 0f, 14f, 0f, 1.2f, 0f, 0f, 14f, 0f, 0f, 1.2f, 0f, 14f, 0f, 0f, 0f, 1f, 0f)
        else -> null
    }
    suspend fun bakeFilter(path: String, kind: Int): String = withContext(Dispatchers.IO) {
        val m = filterMatrix(kind) ?: return@withContext path
        val src = android.graphics.BitmapFactory.decodeFile(path) ?: return@withContext path
        val out = android.graphics.Bitmap.createBitmap(src.width, src.height, android.graphics.Bitmap.Config.ARGB_8888)
        val paint = android.graphics.Paint().apply { colorFilter = android.graphics.ColorMatrixColorFilter(android.graphics.ColorMatrix(m)) }
        android.graphics.Canvas(out).drawBitmap(src, 0f, 0f, paint)
        val f = newMediaFile("jpg")
        f.outputStream().use { out.compress(android.graphics.Bitmap.CompressFormat.JPEG, 92, it) }
        f.absolutePath
    }

    /** الوسوم الأكثر استخداماً في المنشورات */
    fun trendingTags(): List<Pair<String, Int>> =
        posts.filter { !it.archived }.flatMap { Regex("#[\\p{L}\\p{N}_]+").findAll(it.caption).map { m -> m.value }.toList() }
            .groupingBy { it }.eachCount().entries.sortedByDescending { it.value }.take(10).map { it.key to it.value }

    private fun saveMessages() {
        val o = JSONObject()
        for (c in chats) {
            val arr = JSONArray()
            for (m in c.msgs.filter { it.id > 1000 }) arr.put(JSONObject().put("id", m.id).put("mine", m.mine).put("t", m.text).put("img", m.imagePath ?: ""))
            o.put(c.id.toString(), arr)
        }
        sp.edit().putString("msgs", o.toString()).apply()
    }

    private fun loadMessages(skipSelf: Boolean = false) {
        val o = JSONObject(sp.getString("msgs", "{}"))
        for (c in chats) {
            if (skipSelf && c.id == 0) continue
            val arr = o.optJSONArray(c.id.toString()) ?: continue
            for (i in 0 until arr.length()) {
                val m = arr.getJSONObject(i)
                c.msgs.add(Msg(m.getLong("id"), m.getBoolean("mine"), m.getString("t"), m.getString("img").ifEmpty { null }))
            }
        }
    }
}
