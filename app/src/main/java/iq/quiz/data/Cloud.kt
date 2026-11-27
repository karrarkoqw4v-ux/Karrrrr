package iq.quiz.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.tasks.await

/**
 * طبقة حسابات ومراسلات حقيقية عبر Firebase (مشروع gvzkkf الحقيقي، حزمة iq.quiz.tawasulkum).
 * تسجيل الدخول: Firebase Authentication. البيانات والمراسلات: Realtime Database
 * (نفس قاعدة "gvzkkf-default-rtdb" اللي تظهر بقسم "Realtime Database" بالكونسول).
 * خلافاً لـ Repo القديم (محلي بالكامل على الجهاز)، كل شي هنا يُحفظ على سيرفر حقيقي
 * فيوصل أي جهاز ثاني مسجّل بنفس الحساب أو بحساب ثاني يراسله.
 */
object Cloud {
    private val auth get() = FirebaseAuth.getInstance()
    private val db get() = FirebaseDatabase.getInstance().reference

    data class Profile(val uid: String, val name: String, val username: String, val bio: String = "")
    data class CloudMsg(val id: String, val senderId: String, val text: String, val time: Long)
    data class ChatSummary(val peerUid: String, val lastMessage: String, val lastTime: Long)

    sealed interface Result { data object Ok : Result; data class Fail(val msg: String) : Result }

    /** مرجع إلغاء اشتراك مستمع Realtime Database (بديل ListenerRegistration مال Firestore) */
    class Registration(private val ref: DatabaseReference, private val listener: ValueEventListener) {
        fun remove() = ref.removeEventListener(listener)
    }

    val uid: String? get() = auth.currentUser?.uid

    private fun usernameKey(u: String) = u.trim().lowercase()

    suspend fun usernameTaken(username: String): Boolean =
        db.child("usernames").child(usernameKey(username)).get().await().exists()

    suspend fun signUp(email: String, name: String, username: String, pass: String): Result {
        val u = usernameKey(username)
        if (usernameTaken(u)) return Result.Fail("اسم المستخدم محجوز، جرّب اسم ثاني")
        return try {
            val cred = auth.createUserWithEmailAndPassword(email, pass).await()
            val authedUid = cred.user?.uid ?: return Result.Fail("تعذر إنشاء الحساب")
            db.child("users").child(authedUid).setValue(mapOf("name" to name.trim(), "username" to u, "bio" to "", "email" to email)).await()
            db.child("usernames").child(u).setValue(authedUid).await()
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
            val targetUid = db.child("usernames").child(usernameKey(input)).get().await().getValue(String::class.java)
                ?: return Result.Fail("ما لقينا حساب بهذا الاسم")
            db.child("users").child(targetUid).child("email").get().await().getValue(String::class.java)
                ?: return Result.Fail("ما لقينا حساب بهذا الاسم")
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

    suspend fun myProfile(): Profile? = uid?.let { profile(it) }

    suspend fun updateProfile(name: String, bio: String) {
        val id = uid ?: return
        db.child("users").child(id).updateChildren(mapOf("name" to name.trim(), "bio" to bio.trim())).await()
    }

    suspend fun profile(uid: String): Profile? {
        val snap = db.child("users").child(uid).get().await()
        if (!snap.exists()) return null
        return Profile(uid, snap.child("name").getValue(String::class.java) ?: "", snap.child("username").getValue(String::class.java) ?: "", snap.child("bio").getValue(String::class.java) ?: "")
    }

    /** يدوّر على حساب حقيقي باسم المستخدم، لبدء محادثة معه */
    suspend fun findByUsername(username: String): Profile? {
        val u = usernameKey(username)
        val id = db.child("usernames").child(u).get().await().getValue(String::class.java) ?: return null
        return profile(id)
    }

    // ---------- مراسلات حقيقية بين حسابين ----------
    private fun chatId(a: String, b: String) = listOf(a, b).sorted().joinToString("_")

    fun sendMessage(peerUid: String, text: String) {
        val me = uid ?: return
        val id = chatId(me, peerUid)
        val now = System.currentTimeMillis()
        db.child("chats").child(id).child("messages").push().setValue(mapOf("senderId" to me, "text" to text, "time" to now))
        db.child("chats").child(id).child("meta").updateChildren(
            mapOf("participants" to mapOf(me to true, peerUid to true), "lastMessage" to text, "lastTime" to now),
        )
        db.child("userChats").child(me).child(id).setValue(peerUid)
        db.child("userChats").child(peerUid).child(id).setValue(me)
    }

    fun listenMessages(peerUid: String, onUpdate: (List<CloudMsg>) -> Unit): Registration? {
        val me = uid ?: return null
        val id = chatId(me, peerUid)
        val ref = db.child("chats").child(id).child("messages")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.mapNotNull { d ->
                    val sender = d.child("senderId").getValue(String::class.java) ?: return@mapNotNull null
                    val text = d.child("text").getValue(String::class.java) ?: return@mapNotNull null
                    val time = d.child("time").getValue(Long::class.java) ?: 0L
                    CloudMsg(d.key ?: "", sender, text, time)
                }.sortedBy { it.time }
                onUpdate(list)
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        ref.addValueEventListener(listener)
        return Registration(ref, listener)
    }

    fun listenMyChats(onUpdate: (List<ChatSummary>) -> Unit): Registration? {
        val me = uid ?: return null
        val ref = db.child("userChats").child(me)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val chatIds = snapshot.children.mapNotNull { it.key to (it.getValue(String::class.java) ?: return@mapNotNull null) }
                if (chatIds.isEmpty()) { onUpdate(emptyList()); return }
                val results = mutableListOf<ChatSummary>()
                var remaining = chatIds.size
                chatIds.forEach { (cid, peer) ->
                    db.child("chats").child(cid!!).child("meta").get().addOnSuccessListener { meta ->
                        results.add(ChatSummary(peer, meta.child("lastMessage").getValue(String::class.java) ?: "", meta.child("lastTime").getValue(Long::class.java) ?: 0L))
                        remaining--
                        if (remaining == 0) onUpdate(results.sortedByDescending { it.lastTime })
                    }.addOnFailureListener {
                        remaining--
                        if (remaining == 0) onUpdate(results.sortedByDescending { it.lastTime })
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        ref.addValueEventListener(listener)
        return Registration(ref, listener)
    }
}
