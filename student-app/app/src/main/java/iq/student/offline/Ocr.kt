package iq.student.offline

import android.content.Context
import android.graphics.Bitmap
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.googlecode.tesseract.android.TessBaseAPI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** انتظار نتيجة Task من Google بدون مكتبة إضافية */
suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { c ->
    addOnSuccessListener { c.resume(it) }
    addOnFailureListener { c.resumeWithException(it) }
}

/**
 * قراءة النص من الصور بدون إنترنت وبدون ذكاء اصطناعي سحابي:
 * - الإنكليزي: ML Kit (على الجهاز)
 * - العربي (والمختلط): Tesseract بنموذج ara + eng
 */
object Ocr {
    private val latin by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    suspend fun english(bmp: Bitmap): String = latin.process(InputImage.fromBitmap(bmp, 0)).awaitResult().text.trim()

    suspend fun arabic(ctx: Context, bmp: Bitmap): String = withContext(Dispatchers.Default) {
        val dir = prepareTessData(ctx)
        val api = TessBaseAPI()
        try {
            check(api.init(dir.absolutePath, "ara+eng")) { "تعذر تشغيل قارئ العربي" }
            api.setImage(bmp)
            (api.getUTF8Text() ?: "").trim()
        } finally {
            api.recycle()
        }
    }

    /** ينسخ نماذج Tesseract من assets إلى مخزن التطبيق أول مرة */
    private fun prepareTessData(ctx: Context): File {
        val root = File(ctx.filesDir, "tess")
        val tess = File(root, "tessdata").apply { mkdirs() }
        for (name in listOf("ara.traineddata", "eng.traineddata")) {
            val out = File(tess, name)
            if (!out.exists() || out.length() < 1000) {
                ctx.assets.open("tessdata/$name").use { i -> out.outputStream().use { i.copyTo(it) } }
            }
        }
        return root
    }
}
