package iq.student.util

import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import iq.student.data.Prefs
import java.util.Locale

/** قراءة صوتية بمحرك Android (TextToSpeech). عربي وإنكليزي حسب الأصوات المثبتة بالجهاز. */
class Speaker(ctx: Context) {
    private var ready = false
    private val tts: TextToSpeech = TextToSpeech(ctx.applicationContext) { ready = it == TextToSpeech.SUCCESS }

    val arabic: Locale = Locale("ar")
    val english: Locale = Locale.US

    fun speak(text: String, locale: Locale, rate: Float = Prefs.ttsRate) {
        if (!ready || text.isBlank()) return
        tts.language = locale
        tts.setSpeechRate(rate)
        tts.stop()
        // المحرك يقبل طول محدود بالجملة الواحدة، نقسم النص إلى أجزاء
        splitForSpeech(text).forEachIndexed { i, part ->
            tts.speak(part, TextToSpeech.QUEUE_ADD, null, "u$i")
        }
    }

    fun stop() { tts.stop() }
    fun shutdown() { tts.stop(); tts.shutdown() }

    private fun splitForSpeech(text: String, max: Int = 1500): List<String> {
        val parts = mutableListOf<String>()
        val cur = StringBuilder()
        for (line in text.split('\n', '.', '؟', '?', '!')) {
            if (cur.length + line.length > max && cur.isNotEmpty()) { parts += cur.toString(); cur.clear() }
            cur.append(line.trim()).append(". ")
        }
        if (cur.isNotBlank()) parts += cur.toString()
        return parts
    }
}

@Composable
fun rememberSpeaker(): Speaker {
    val ctx = LocalContext.current
    val s = remember { Speaker(ctx) }
    DisposableEffect(Unit) { onDispose { s.shutdown() } }
    return s
}
