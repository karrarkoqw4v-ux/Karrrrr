package iq.dablik

import org.mozilla.javascript.Context
import org.mozilla.javascript.Function
import org.mozilla.javascript.ScriptableObject
import org.mozilla.javascript.Undefined
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicBoolean

/** ينفّذ أوامر شل حقيقية داخل مجلد مشروعك فقط (صندوق التطبيق، بدون أي صلاحية جهاز إضافية) */
object ShellRunner {
    fun run(cmd: String, workDir: File): Pair<Int, String> {
        return try {
            val p = ProcessBuilder("sh", "-c", cmd).directory(workDir).redirectErrorStream(true).start()
            val out = BufferedReader(InputStreamReader(p.inputStream)).use { it.readText() }
            val code = p.waitFor()
            code to out
        } catch (e: Exception) {
            -1 to (e.message ?: "تعذر تنفيذ الأمر")
        }
    }
}

/**
 * محرّك جافاسكربت نقي (Rhino) معزول داخل صندوق التطبيق: يوفّر فقط log() وsleep() وhttp.get/post،
 * بلا وصول لنظام الملفات خارج مجلد المشروع ولا لأي تطبيق آخر أو إعداد نظام.
 */
object CodeRunner {
    /** ينفّذ مرة واحدة وتنتهي؛ يستدعي onLog لكل سطر */
    fun runOnce(code: String, onLog: (String) -> Unit): String? {
        val ctx = Context.enter()
        return try {
            ctx.optimizationLevel = -1
            val scope: ScriptableObject = ctx.initStandardObjects()
            bind(ctx, scope, onLog, null)
            ctx.evaluateString(scope, code, "script", 1, null)
            null
        } catch (e: Exception) {
            e.message ?: e.toString()
        } finally { Context.exit() }
    }

    /** ينفّذ بحلقة مستمرة (لبوت يستقصي رسائله مثلاً) حتى تُطلب إيقافه */
    fun runLoop(code: String, stop: AtomicBoolean, onLog: (String) -> Unit): String? {
        val ctx = Context.enter()
        return try {
            ctx.optimizationLevel = -1
            val scope: ScriptableObject = ctx.initStandardObjects()
            bind(ctx, scope, onLog, stop)
            ctx.evaluateString(scope, code, "script", 1, null)
            null
        } catch (e: Exception) {
            e.message ?: e.toString()
        } finally { Context.exit() }
    }

    private fun bind(ctx: Context, scope: ScriptableObject, onLog: (String) -> Unit, stop: AtomicBoolean?) {
        ScriptableObject.putProperty(scope, "log", object : org.mozilla.javascript.BaseFunction() {
            override fun call(cx: Context?, scope: org.mozilla.javascript.Scriptable?, thisObj: org.mozilla.javascript.Scriptable?, args: Array<out Any?>?): Any {
                onLog(args?.joinToString(" ") { Context.toString(it) } ?: "")
                return Undefined.instance
            }
        })
        ScriptableObject.putProperty(scope, "sleep", object : org.mozilla.javascript.BaseFunction() {
            override fun call(cx: Context?, scope: org.mozilla.javascript.Scriptable?, thisObj: org.mozilla.javascript.Scriptable?, args: Array<out Any?>?): Any {
                val ms = (args?.getOrNull(0) as? Number)?.toLong() ?: 0L
                Thread.sleep(ms.coerceIn(0, 60_000))
                return Undefined.instance
            }
        })
        ScriptableObject.putProperty(scope, "stopped", object : org.mozilla.javascript.BaseFunction() {
            override fun call(cx: Context?, scope: org.mozilla.javascript.Scriptable?, thisObj: org.mozilla.javascript.Scriptable?, args: Array<out Any?>?): Any = stop?.get() ?: false
        })
        val http = ctx.newObject(scope)
        ScriptableObject.putProperty(http, "get", object : org.mozilla.javascript.BaseFunction() {
            override fun call(cx: Context?, scope: org.mozilla.javascript.Scriptable?, thisObj: org.mozilla.javascript.Scriptable?, args: Array<out Any?>?): Any {
                val url = Context.toString(args?.getOrNull(0))
                return httpCall(url, "GET", null)
            }
        })
        ScriptableObject.putProperty(http, "post", object : org.mozilla.javascript.BaseFunction() {
            override fun call(cx: Context?, scope: org.mozilla.javascript.Scriptable?, thisObj: org.mozilla.javascript.Scriptable?, args: Array<out Any?>?): Any {
                val url = Context.toString(args?.getOrNull(0))
                val body = args?.getOrNull(1)?.let { Context.toString(it) }
                return httpCall(url, "POST", body)
            }
        })
        ScriptableObject.putProperty(scope, "http", http)
    }

    private fun httpCall(url: String, method: String, body: String?): String = try {
        val c = URL(url).openConnection() as HttpURLConnection
        c.requestMethod = method
        c.connectTimeout = 15000; c.readTimeout = 25000
        if (body != null) {
            c.doOutput = true
            c.setRequestProperty("Content-Type", "application/json")
            c.outputStream.use { it.write(body.toByteArray()) }
        }
        val stream = if (c.responseCode in 200..299) c.inputStream else c.errorStream
        BufferedReader(InputStreamReader(stream)).use { it.readText() }
    } catch (e: Exception) {
        "{\"error\":\"${e.message?.replace("\"", "'")}\"}"
    }
}
