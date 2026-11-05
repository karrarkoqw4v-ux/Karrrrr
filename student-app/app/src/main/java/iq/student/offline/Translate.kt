package iq.student.offline

import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.mlkit.common.model.DownloadConditions

/**
 * ترجمة على الجهاز (ML Kit). أول مرة تنزّل نموذج اللغة (حوالي 30 ميغابايت لكل لغة)،
 * وبعدها تشتغل بدون إنترنت.
 */
object Translate {
    suspend fun run(text: String, toArabic: Boolean): String {
        val src = if (toArabic) TranslateLanguage.ENGLISH else TranslateLanguage.ARABIC
        val dst = if (toArabic) TranslateLanguage.ARABIC else TranslateLanguage.ENGLISH
        val translator = Translation.getClient(TranslatorOptions.Builder().setSourceLanguage(src).setTargetLanguage(dst).build())
        try {
            translator.downloadModelIfNeeded(DownloadConditions.Builder().build()).awaitResult()
            // نترجم سطر سطر حتى تبقى الفقرات مرتبة
            val out = mutableListOf<String>()
            for (line in text.lines()) {
                out += if (line.isBlank()) "" else translator.translate(line.trim()).awaitResult()
            }
            return out.joinToString("\n").trim()
        } finally {
            translator.close()
        }
    }
}
