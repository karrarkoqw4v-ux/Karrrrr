package iq.suls

import android.os.Environment
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * ينفّذ أمر شل حقيقي بصلاحية التطبيق نفسه (نفس صلاحية أي تطبيق عادي على الجهاز، بدون روت).
 * أوامر تحتاج صلاحية نظام/روت (مثل settings put global أو am force-stop لتطبيق ثاني)
 * سترجع فعلياً "Permission denied" من النظام نفسه — هذا تصرف أندرويد الحقيقي، مو قيد افرضه.
 */
object ShellRunner {
    data class Result(val exitCode: Int, val output: String)

    fun run(cmd: String): Result {
        return try {
            val p = ProcessBuilder("sh", "-c", cmd)
                .directory(Environment.getExternalStorageDirectory())
                .redirectErrorStream(true)
                .start()
            val out = BufferedReader(InputStreamReader(p.inputStream)).use { it.readText() }
            val code = p.waitFor()
            Result(code, out.ifBlank { if (code == 0) "(تم التنفيذ بدون أي إخراج)" else "(فشل بدون رسالة خطأ)" })
        } catch (e: Exception) {
            Result(-1, "تعذر تنفيذ الأمر: ${e.message}")
        }
    }
}
