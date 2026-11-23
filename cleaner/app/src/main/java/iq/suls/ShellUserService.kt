package iq.suls

import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * هذا الكود ما يشتغل داخل تطبيق سلس نفسه — Shizuku يشغّله بعملية منفصلة
 * بهوية شل (أو روت لو المستخدم شغّل Shizuku بالروت)، فقط بعد ما المستخدم
 * يوافق صراحة على طلب الصلاحية من نافذة Shizuku نفسها.
 */
class ShellUserService : IShellService.Stub() {

    override fun destroy() {
        Log.i("Suls-ShellUserService", "destroy")
        System.exit(0)
    }

    override fun exec(cmd: String): String {
        return try {
            val p = ProcessBuilder("sh", "-c", cmd).redirectErrorStream(true).start()
            val out = BufferedReader(InputStreamReader(p.inputStream)).use { it.readText() }
            val code = p.waitFor()
            if (out.isBlank()) {
                if (code == 0) "(تم التنفيذ بصلاحية Shizuku بدون أي إخراج)" else "(فشل، رمز الخروج: $code)"
            } else out
        } catch (e: Exception) {
            "تعذر تنفيذ الأمر عبر Shizuku: ${e.message}"
        }
    }
}
