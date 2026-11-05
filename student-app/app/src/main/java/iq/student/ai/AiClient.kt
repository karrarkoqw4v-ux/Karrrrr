package iq.student.ai

import android.graphics.Bitmap
import android.util.Base64
import iq.student.data.Prefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

class AiException(message: String) : Exception(message)

/**
 * عميل Claude Messages API (طلب HTTP مباشر لأن Android لا يحتاج SDK ثقيل).
 * المفتاح يدخله الطالب بالإعدادات ويُحفظ على جهازه. للنسخة النهائية يُنصح بسيرفر وسيط يخبّئ المفتاح.
 */
object AiClient {
    private const val URL_MESSAGES = "https://api.anthropic.com/v1/messages"

    private fun jpegBase64(bmp: Bitmap): String {
        val out = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.JPEG, 88, out)
        return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }

    /** يرجع النص الناتج أو يرمي AiException برسالة عربية مفهومة */
    suspend fun ask(system: String, prompt: String, image: Bitmap? = null): String = withContext(Dispatchers.IO) {
        val key = Prefs.apiKey
        if (key.isBlank()) throw AiException("NO_KEY")

        val content = JSONArray()
        if (image != null) {
            content.put(JSONObject().put("type", "image").put("source", JSONObject()
                .put("type", "base64").put("media_type", "image/jpeg").put("data", jpegBase64(image))))
        }
        content.put(JSONObject().put("type", "text").put("text", prompt))

        val body = JSONObject()
            .put("model", Prefs.model)
            .put("max_tokens", 16000)
            .put("system", system)
            .put("messages", JSONArray().put(JSONObject().put("role", "user").put("content", content)))

        val conn = (URL(URL_MESSAGES).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 20_000
            readTimeout = 240_000
            doOutput = true
            setRequestProperty("content-type", "application/json")
            setRequestProperty("x-api-key", key)
            setRequestProperty("anthropic-version", "2023-06-01")
        }
        try {
            conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (code !in 200..299) throw AiException(errorMessage(code, text))
            val root = JSONObject(text)
            if (root.optString("stop_reason") == "refusal") throw AiException("الخدمة رفضت هذا الطلب، جرّب صورة أو سؤال ثاني")
            val blocks = root.getJSONArray("content")
            val sb = StringBuilder()
            for (i in 0 until blocks.length()) {
                val b = blocks.getJSONObject(i)
                if (b.optString("type") == "text") sb.append(b.optString("text"))
            }
            sb.toString().trim().ifEmpty { throw AiException("ما رجع جواب، جرّب مرة ثانية") }
        } catch (e: AiException) {
            throw e
        } catch (e: java.io.IOException) {
            throw AiException("ما كدرت أتصل بالإنترنت، تأكد من الشبكة")
        } finally {
            conn.disconnect()
        }
    }

    private fun errorMessage(code: Int, body: String): String {
        val api = runCatching { JSONObject(body).getJSONObject("error").optString("message") }.getOrDefault("")
        return when (code) {
            401, 403 -> "مفتاح الخدمة غير صحيح، راجع الإعدادات"
            404 -> "اسم الموديل غير صحيح، راجع الإعدادات ($api)"
            429 -> "كثرت الطلبات، انتظر شوية وجرّب"
            529, 503 -> "الخدمة مشغولة، جرّب بعد قليل"
            else -> "صار خطأ ($code) $api"
        }
    }
}
