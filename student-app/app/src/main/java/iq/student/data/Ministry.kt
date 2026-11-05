package iq.student.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

data class MQuestion(val q: String, val a: String)

/**
 * الأسئلة الوزارية: مادة / سنة (2011 - 2026) / دور (1-3).
 * كل ملف JSON اسمه مثل  chemistry_2024_1.json  ومحتواه: [{"q":"السؤال","a":"الجواب"}, ...]
 * يُقرأ من assets/ministry/ أو من الرابط الاختياري بالإعدادات (Prefs.ministryBase) عند توفر الإنترنت.
 */
object Ministry {
    val years: List<Int> = (2026 downTo 2011).toList()
    val rounds = listOf(1 to "الدور الأول", 2 to "الدور الثاني", 3 to "الدور الثالث")
    private val cache = HashMap<String, List<MQuestion>>()

    private fun name(subject: String, year: Int, round: Int) = "${subject}_${year}_$round.json"

    private fun parse(text: String): List<MQuestion> {
        val arr = JSONArray(text)
        return List(arr.length()) { val o = arr.getJSONObject(it); MQuestion(o.optString("q"), o.optString("a")) }
    }

    suspend fun load(ctx: Context, subject: String, year: Int, round: Int): List<MQuestion> {
        val key = name(subject, year, round)
        cache[key]?.let { return it }
        val list = withContext(Dispatchers.IO) {
            runCatching { parse(ctx.assets.open("ministry/$key").bufferedReader(Charsets.UTF_8).use { it.readText() }) }.getOrNull()
                ?: Prefs.ministryBase.takeIf { it.startsWith("http") }?.let { base ->
                    runCatching {
                        val c = URL("$base/$key").openConnection() as HttpURLConnection
                        c.connectTimeout = 15_000; c.readTimeout = 30_000
                        try {
                            if (c.responseCode in 200..299) parse(c.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }) else null
                        } finally { c.disconnect() }
                    }.getOrNull()
                }
                ?: emptyList()
        }
        if (list.isNotEmpty()) cache[key] = list
        return list
    }
}
