package iq.student.offline

/**
 * استخراج تقريبي بدون ذكاء اصطناعي: التعاريف والأسئلة الظاهرة بالصفحة بعد قراءتها (OCR).
 * الأجوبة غير مولّدة؛ الصفحة تعرض فقط ما هو مكتوب بها.
 */
object PageExtract {
    private val defMarkers = listOf("يعرّف", "يعرف", "تعرّف", "تعرف", "هو ", "هي ", "يُسمى", "يسمى", "تسمى", "يقصد", "ويقصد", " is ", " are ", " refers ", "defined as")
    private val questionStarts = Regex("""^\s*(س\s*\d*|\d+\s*[\)\.\-]|[أ-ي]\s*[\)\-]|q\s*\d+|\d+\s*:)""", RegexOption.IGNORE_CASE)

    fun clean(raw: String): List<String> =
        raw.lines().map { it.trim().replace(Regex("\\s+"), " ") }.filter { it.length > 3 }

    fun definitions(lines: List<String>): List<String> = lines.filter { l ->
        (l.contains(":") && l.length in 12..220) || defMarkers.any { l.contains(it) }
    }

    fun questions(lines: List<String>): List<String> = lines.filter { l ->
        l.endsWith("؟") || l.endsWith("?") || questionStarts.containsMatchIn(l)
    }
}
