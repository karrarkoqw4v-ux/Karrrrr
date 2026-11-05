package iq.student.offline

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * طريقة اللفظ بحروف عربية: قاموس CMU (125 ألف كلمة) ثم تحويل الأصوات إلى حروف وحركات عربية.
 * تقريبية: الأفضل بالكلمات الشائعة، وتخطئ أحياناً بالأسماء النادرة.
 */
object Pronounce {
    private var dict: HashMap<String, List<String>>? = null

    private suspend fun load(ctx: Context): HashMap<String, List<String>> {
        dict?.let { return it }
        return withContext(Dispatchers.IO) {
            val map = HashMap<String, List<String>>(140_000)
            ctx.assets.open("cmudict.txt").bufferedReader().useLines { lines ->
                for (line in lines) {
                    val p = line.split(' ')
                    if (p.size >= 2) map[p[0]] = p.subList(1, p.size)
                }
            }
            dict = map
            map
        }
    }

    private val consonants = mapOf(
        "B" to "ب", "CH" to "تش", "D" to "د", "DH" to "ذ", "F" to "ف", "G" to "گ", "HH" to "ه", "JH" to "ج",
        "K" to "ك", "L" to "ل", "M" to "م", "N" to "ن", "NG" to "نگ", "P" to "پ", "R" to "ر", "S" to "س",
        "SH" to "ش", "T" to "ت", "TH" to "ث", "V" to "ڤ", "W" to "و", "Y" to "ي", "Z" to "ز", "ZH" to "ژ",
    )
    private const val FATHA = "َ"
    private const val KASRA = "ِ"
    private const val DAMMA = "ُ"

    /** صوت العلة -> (حركة, حرف مد إضافي) */
    private val vowels = mapOf(
        "AA" to (FATHA to "ا"), "AE" to (FATHA to "ا"), "AH" to (FATHA to ""), "AO" to (DAMMA to "و"),
        "AW" to (FATHA to "او"), "AY" to (FATHA to "اي"), "EH" to (KASRA to ""), "ER" to (KASRA to "ر"),
        "EY" to (FATHA to "ي"), "IH" to (KASRA to ""), "IY" to (KASRA to "ي"), "OW" to (DAMMA to "و"),
        "OY" to (DAMMA to "وي"), "UH" to (DAMMA to ""), "UW" to (DAMMA to "و"),
    )

    private fun convert(ph: List<String>): String {
        val sb = StringBuilder()
        for (p in ph) {
            val v = vowels[p]
            if (v != null) {
                if (sb.isEmpty() || sb.last() == ' ') sb.append("ا")
                sb.append(v.first).append(v.second)
            } else sb.append(consonants[p] ?: "")
        }
        return sb.toString()
    }

    /** حروف إنكليزية مفردة للكلمات غير الموجودة بالقاموس */
    private val letters = mapOf(
        'a' to "ا", 'b' to "ب", 'c' to "ك", 'd' to "د", 'e' to "ي", 'f' to "ف", 'g' to "گ", 'h' to "ه", 'i' to "ي",
        'j' to "ج", 'k' to "ك", 'l' to "ل", 'm' to "م", 'n' to "ن", 'o' to "و", 'p' to "پ", 'q' to "ك", 'r' to "ر",
        's' to "س", 't' to "ت", 'u' to "و", 'v' to "ڤ", 'w' to "و", 'x' to "كس", 'y' to "ي", 'z' to "ز",
    )

    suspend fun word(ctx: Context, w: String): String {
        val key = w.lowercase().filter { it.isLetter() || it == '\'' }
        if (key.isEmpty()) return ""
        load(ctx)[key]?.let { return convert(it) }
        return key.map { letters[it] ?: "" }.joinToString("")
    }

    suspend fun sentence(ctx: Context, text: String): String =
        text.split(Regex("\\s+")).filter { it.isNotBlank() }.joinToString(" ") { word(ctx, it) }.trim()
}
