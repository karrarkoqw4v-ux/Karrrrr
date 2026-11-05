package iq.student.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * تخزين محلي على الجهاز فقط (بدون سيرفر): الحساب، الصف، مفتاح الخدمة، مراجع الكتب.
 * كلمات المرور تُحفظ مشفّرة بـ PBKDF2 مع salt.
 */
object Prefs {
    private lateinit var sp: SharedPreferences

    fun init(ctx: Context) {
        sp = ctx.applicationContext.getSharedPreferences("iq_student", Context.MODE_PRIVATE)
    }

    var currentEmail: String?
        get() = sp.getString("cur", null)
        set(v) { sp.edit().putString("cur", v).apply() }
    var gradeId: String?
        get() = sp.getString("grade", null)
        set(v) { sp.edit().putString("grade", v).apply() }
    var branch: String
        get() = sp.getString("branch", "sci") ?: "sci"
        set(v) { sp.edit().putString("branch", v).apply() }
    var apiKey: String
        get() = sp.getString("api_key", "") ?: ""
        set(v) { sp.edit().putString("api_key", v.trim()).apply() }
    var model: String
        get() = sp.getString("model", DEFAULT_MODEL) ?: DEFAULT_MODEL
        set(v) { sp.edit().putString("model", v.trim().ifEmpty { DEFAULT_MODEL }).apply() }
    var ttsRate: Float
        get() = sp.getFloat("tts_rate", 0.9f)
        set(v) { sp.edit().putFloat("tts_rate", v).apply() }

    const val DEFAULT_MODEL = "claude-opus-5-5"

    val userName: String get() = currentEmail?.let { sp.getString("name_$it", "") } ?: ""

    // ---- الكتب: "file:<path>" أو "url:<https...>" ----
    fun bookRef(key: String): String? = sp.getString("book_$key", null)
    fun setBookRef(key: String, ref: String?) { sp.edit().apply { if (ref == null) remove("book_$key") else putString("book_$key", ref) }.apply() }

    // ---- الحساب المحلي ----
    private fun hash(pass: String, salt: ByteArray): String {
        val spec = PBEKeySpec(pass.toCharArray(), salt, 60_000, 256)
        val bytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    sealed interface AuthResult { data object Ok : AuthResult; data class Error(val message: String) : AuthResult }

    fun signUp(name: String, email: String, pass: String): AuthResult {
        val e = email.trim().lowercase()
        if (name.isBlank()) return AuthResult.Error("اكتب اسمك")
        if (!e.contains("@") || !e.contains(".")) return AuthResult.Error("البريد الإلكتروني غير صحيح")
        if (pass.length < 6) return AuthResult.Error("كلمة المرور لازم 6 أحرف على الأقل")
        if (sp.contains("h_$e")) return AuthResult.Error("هذا البريد مسجّل من قبل")
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        sp.edit().putString("h_$e", hash(pass, salt)).putString("s_$e", Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString("name_$e", name.trim()).putString("cur", e).apply()
        return AuthResult.Ok
    }

    fun signIn(email: String, pass: String): AuthResult {
        val e = email.trim().lowercase()
        val stored = sp.getString("h_$e", null) ?: return AuthResult.Error("ما لقينا حساب بهذا البريد")
        val salt = Base64.decode(sp.getString("s_$e", "") ?: "", Base64.NO_WRAP)
        if (hash(pass, salt) != stored) return AuthResult.Error("كلمة المرور غلط")
        sp.edit().putString("cur", e).apply()
        return AuthResult.Ok
    }

    fun signOut() { sp.edit().remove("cur").apply() }
}
