package iq.student.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import iq.student.data.Prefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** مفتاح الكتاب: صف + مادة، مثل m3_math */
fun bookKey(gradeId: String, subjectId: String) = "${gradeId}_$subjectId"

class PdfDoc(file: File) {
    private val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    private val renderer = PdfRenderer(pfd)
    private val mutex = Mutex()
    val pageCount: Int get() = renderer.pageCount

    suspend fun render(index: Int, width: Int): Bitmap = mutex.withLock {
        withContext(Dispatchers.IO) {
            val page = renderer.openPage(index)
            try {
                val h = (width * page.height / page.width.toFloat()).toInt().coerceAtLeast(1)
                val bmp = Bitmap.createBitmap(width, h, Bitmap.Config.ARGB_8888)
                bmp.eraseColor(Color.WHITE)
                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                bmp
            } finally {
                page.close()
            }
        }
    }

    fun close() { runCatching { renderer.close() }; runCatching { pfd.close() } }
}

object Books {
    private fun dir(ctx: Context) = File(ctx.filesDir, "books").apply { mkdirs() }

    /** ينسخ PDF اختاره الطالب من جهازه إلى مخزن التطبيق ويحفظ المرجع */
    suspend fun importFile(ctx: Context, key: String, uri: Uri) = withContext(Dispatchers.IO) {
        val out = File(dir(ctx), "$key.pdf")
        ctx.contentResolver.openInputStream(uri)?.use { input -> out.outputStream().use { input.copyTo(it) } }
            ?: error("تعذر قراءة الملف")
        Prefs.setBookRef(key, "file:${out.absolutePath}")
    }

    fun saveUrl(key: String, url: String) = Prefs.setBookRef(key, "url:${url.trim()}")

    fun has(key: String) = Prefs.bookRef(key) != null

    /** يرجع الملف المحلي؛ إن كان رابطاً ينزّله مرة واحدة ويخزنه */
    suspend fun resolve(ctx: Context, ref: String): File = withContext(Dispatchers.IO) {
        when {
            ref.startsWith("file:") -> File(ref.removePrefix("file:")).also { if (!it.exists()) error("الملف انحذف، أضفه من جديد") }
            ref.startsWith("url:") -> {
                val url = ref.removePrefix("url:")
                val cache = File(dir(ctx), "dl_${url.hashCode()}.pdf")
                if (!cache.exists() || cache.length() < 1000) {
                    val conn = URL(url).openConnection() as HttpURLConnection
                    conn.connectTimeout = 20_000
                    conn.readTimeout = 60_000
                    conn.instanceFollowRedirects = true
                    try {
                        if (conn.responseCode !in 200..299) error("تعذر تنزيل الكتاب (${conn.responseCode})")
                        val tmp = File(cache.path + ".part")
                        conn.inputStream.use { i -> tmp.outputStream().use { i.copyTo(it) } }
                        tmp.renameTo(cache)
                    } finally { conn.disconnect() }
                }
                cache
            }
            else -> error("مرجع غير معروف")
        }
    }
}
