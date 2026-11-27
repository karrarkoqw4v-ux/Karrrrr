package iq.quiz.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

/**
 * طبقة حسابات ومراسلات حقيقية عبر Firebase (مشروع gvzkkf الحقيقي، حزمة iq.quiz.tawasulkum).
 * خلافاً لـ Repo القديم (محلي بالكامل على الجهاز)، كل شي هنا يُحفظ على سيرفر حقيقي
 * فيوصل أي جهاز ثاني مسجّل بنفس الحساب أو بحساب ثاني يراسله.
 */
object Cloud {
    private val auth get() = FirebaseAuth.getInstance()
    private val db get() = FirebaseFirestore.getInstance()

    data class Profile(val uid: String, val name: String, val username: String, val bio: String = "")
    data class CloudMsg(val id: String, val senderId: String, val text: String, val time: Long)

    sealed interface Result { data object Ok : Result; data class Fail(val msg: String) : Result }

    val uid: String? get() = auth.currentUser?.uid

    private fun usernameKey(u: String) = u.trim().lowercase()

    suspend fun usernameTaken(username: String): Boolean =
        db.collection("usernames").document(usernameKey(username)).get().await().exists()

    suspend fun signUp(email: String, name: String, username: String, pass: String): Result {
        val u = usernameKey(username)
        if (usernameTaken(u)) return Result.Fail("اسم المستخدم محجوز، جرّب اسم ثاني")
        return try {
            val cred = auth.createUserWithEmailAndPassword(email, pass).await()
            val authedUid = cred.user?.uid ?: return Result.Fail("تعذر إنشاء الحساب")
            val profile = hashMapOf("name" to name.trim(), "username" to u, "bio" to "", "email" to email)
            db.collection("users").document(authedUid).set(profile).await()
            db.collection("usernames").document(u).set(hashMapOf("uid" to authedUid)).await()
            Result.Ok
        } catch (e: Exception) {
            Result.Fail(friendlyError(e))
        }
    }

    suspend fun signIn(idOrEmail: String, pass: String): Result {
        val input = idOrEmail.trim()
        val email = if (input.contains("@")) {
            input
        } else {
            val doc = db.collection("usernames").document(usernameKey(input)).get().await()
            val targetUid = doc.getString("uid") ?: return Result.Fail("ما لقينا حساب بهذا الاسم")
            val prof = db.collection("users").document(targetUid).get().await()
            prof.getString("email") ?: return Result.Fail("ما لقينا حساب بهذا الاسم")
        }
        return try {
            auth.signInWithEmailAndPassword(email, pass).await()
            Result.Ok
        } catch (e: Exception) {
            Result.Fail(friendlyError(e))
        }
    }

    private fun friendlyError(e: Exception): String = when {
        e.message?.contains("badly formatted", true) == true -> "اكتب بريد إلكتروني صحيح"
        e.message?.contains("already in use", true) == true -> "الحساب موجود، سجّل دخول"
        e.message?.contains("password is invalid", true) == true || e.message?.contains("no user record", true) == true -> "البريد أو كلمة المرور غلط"
        e.message?.contains("weak password", true) == true -> "كلمة المرور ضعيفة، اكتب 6 أحرف فأكثر"
        else -> e.localizedMessage ?: "صار خطأ، جرّب مرة ثانية"
    }

    fun signOut() = auth.signOut()

    suspend fun myProfile(): Profile? {
        val id = uid ?: return null
        val doc = db.collection("users").document(id).get().await()
        return Profile(id, doc.getString("name") ?: "", doc.getString("username") ?: "", doc.getString("bio") ?: "")
    }

    suspend fun updateProfile(name: String, bio: String) {
        val id = uid ?: return
        db.collection("users").document(id).update(mapOf("name" to name.trim(), "bio" to bio.trim())).await()
    }

    suspend fun profile(uid: String): Profile? {
        val doc = db.collection("users").document(uid).get().await()
        if (!doc.exists()) return null
        return Profile(uid, doc.getString("name") ?: "", doc.getString("username") ?: "", doc.getString("bio") ?: "")
    }

    /** يدوّر على حساب حقيقي باسم المستخدم، لبدء محادثة معه */
    suspend fun findByUsername(username: String): Profile? {
        val u = usernameKey(username)
        val doc = db.collection("usernames").document(u).get().await()
        val id = doc.getString("uid") ?: return null
        val prof = db.collection("users").document(id).get().await()
        return Profile(id, prof.getString("name") ?: "", prof.getString("username") ?: u, prof.getString("bio") ?: "")
    }

    // ---------- مراسلات حقيقية بين حسابين ----------
    private fun chatId(a: String, b: String) = listOf(a, b).sorted().joinToString("_")

    fun sendMessage(peerUid: String, text: String) {
        val me = uid ?: return
        val id = chatId(me, peerUid)
        val chatRef = db.collection("chats").document(id)
        chatRef.collection("messages").add(hashMapOf("senderId" to me, "text" to text, "time" to System.currentTimeMillis()))
        chatRef.set(
            hashMapOf("participants" to listOf(me, peerUid), "lastMessage" to text, "lastTime" to System.currentTimeMillis()),
            SetOptions.merge(),
        )
    }

    fun listenMessages(peerUid: String, onUpdate: (List<CloudMsg>) -> Unit): ListenerRegistration? {
        val me = uid ?: return null
        val id = chatId(me, peerUid)
        return db.collection("chats").document(id).collection("messages")
            .orderBy("time", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, _ ->
                if (snap == null) return@addSnapshotListener
                onUpdate(snap.documents.map { d -> CloudMsg(d.id, d.getString("senderId") ?: "", d.getString("text") ?: "", d.getLong("time") ?: 0L) })
            }
    }

    data class ChatSummary(val peerUid: String, val lastMessage: String, val lastTime: Long)

    fun listenMyChats(onUpdate: (List<ChatSummary>) -> Unit): ListenerRegistration? {
        val me = uid ?: return null
        return db.collection("chats").whereArrayContains("participants", me)
            .addSnapshotListener { snap, _ ->
                if (snap == null) return@addSnapshotListener
                val list = snap.documents.mapNotNull { d ->
                    val parts = d.get("participants") as? List<*> ?: return@mapNotNull null
                    val peer = parts.firstOrNull { it != me } as? String ?: return@mapNotNull null
                    ChatSummary(peer, d.getString("lastMessage") ?: "", d.getLong("lastTime") ?: 0L)
                }.sortedByDescending { it.lastTime }
                onUpdate(list)
            }
    }
}
