package iq.quiz.data

import android.content.Context
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

/** يسجّل أي كراش حقيقي بملف محلي حتى نقدر نشوف نص الخطأ بالضبط من داخل التطبيق نفسه. */
object CrashLog {
    private const val FILE = "last_crash.txt"
    @Volatile private var installed = false

    fun install(ctx: Context) {
        if (installed) return
        installed = true
        val appCtx = ctx.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val sw = StringWriter()
                throwable.printStackTrace(PrintWriter(sw))
                val text = "وقت الكراش: ${java.util.Date()}\nالخيط: ${thread.name}\n\n$sw"
                File(appCtx.filesDir, FILE).writeText(text)
            } catch (_: Throwable) { }
            previous?.uncaughtException(thread, throwable)
        }
    }

    fun read(ctx: Context): String? {
        val f = File(ctx.applicationContext.filesDir, FILE)
        return if (f.exists()) f.readText() else null
    }

    fun clear(ctx: Context) {
        File(ctx.applicationContext.filesDir, FILE).delete()
    }
}
