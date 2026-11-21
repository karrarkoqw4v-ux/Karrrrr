package iq.dablik

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipInputStream

/** مجلد ملفات الموقع الذي يعمل عليه المستخدم قبل البناء */
object Project {
    fun dir(ctx: Context): File = File(ctx.filesDir, "project/www").apply { mkdirs() }

    fun files(ctx: Context): List<File> {
        val d = dir(ctx)
        return d.walkTopDown().filter { it.isFile }.sortedBy { rel(d, it).lowercase() }.toList()
    }

    fun rel(base: File, f: File): String = f.canonicalPath.removePrefix(base.canonicalPath + File.separator).replace(File.separatorChar, '/')

    fun clear(ctx: Context) { dir(ctx).deleteRecursively(); dir(ctx) }

    /** يمنع الخروج من مجلد المشروع */
    fun safe(ctx: Context, relPath: String): File? {
        val d = dir(ctx)
        val f = File(d, relPath.trim().trimStart('/')).canonicalFile
        return if (f.path.startsWith(d.canonicalPath + File.separator)) f else null
    }

    fun displayName(ctx: Context, uri: Uri): String =
        runCatching { ctx.contentResolver.query(uri, null, null, null, null)?.use { c -> if (c.moveToFirst()) c.getString(c.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME)) else null } }.getOrNull() ?: "ملف"

    /** يفك ZIP داخل مجلد المشروع (يستبدل الموجود). يعيد عدد الملفات. يتحمل أسماء عربية وملفات من تطبيقات المحادثة */
    fun importZip(ctx: Context, uri: Uri): Int {
        val tmp = File(ctx.cacheDir, "import.zip")
        ctx.contentResolver.openInputStream(uri)?.use { i -> tmp.outputStream().use { o -> i.copyTo(o) } } ?: throw IllegalStateException("تعذر فتح الملف")
        var last: Exception? = null
        try {
            for (cs in listOf(Charsets.UTF_8, java.nio.charset.Charset.forName("windows-1256"), Charsets.ISO_8859_1)) {
                try { return extractZip(ctx, tmp, cs) }
                catch (e: IllegalArgumentException) { last = e }
                catch (e: java.util.zip.ZipException) { if (e.message?.contains("END header", true) == true) throw IllegalStateException("الملف ليس ZIP صالحاً"); last = e }
            }
        } finally { tmp.delete() }
        throw IllegalStateException("تعذر قراءة الـ ZIP: " + (last?.message ?: ""))
    }

    private fun extractZip(ctx: Context, file: File, cs: java.nio.charset.Charset): Int {
        val all = LinkedHashMap<String, ByteArray>()
        java.util.zip.ZipFile(file, cs).use { zf ->
            for (e in zf.entries()) {
                val n = e.name.replace('\\', '/')
                if (e.isDirectory || n.contains("..") || n.startsWith("__MACOSX/") || n.substringAfterLast('/').startsWith("._")) continue
                all[n.trimStart('/')] = zf.getInputStream(e).readBytes()
            }
        }
        val idx = all.keys.filter { it.substringAfterLast('/').equals("index.html", true) }.minByOrNull { it.count { c -> c == '/' } }
        val prefix = idx?.substringBeforeLast('/', "")?.let { if (it.isEmpty()) "" else "$it/" } ?: ""
        clear(ctx)
        var n = 0
        for ((k, v) in all) {
            if (!k.startsWith(prefix)) continue
            val f = safe(ctx, k.removePrefix(prefix)) ?: continue
            f.parentFile?.mkdirs(); f.writeBytes(v); n++
        }
        return n
    }

    /** يضيف ملفات (HTML/CSS/JS/صور...) إلى المشروع. أول ملف HTML بلا index يصير index.html */
    fun importFiles(ctx: Context, uris: List<Uri>): Int {
        var n = 0
        val hasIndex = File(dir(ctx), "index.html").exists()
        var firstHtmlTaken = hasIndex
        for (u in uris) {
            var name = displayName(ctx, u).replace('/', '_')
            if (!firstHtmlTaken && (name.endsWith(".html", true) || name.endsWith(".htm", true))) { name = "index.html"; firstHtmlTaken = true }
            val f = safe(ctx, name) ?: continue
            ctx.contentResolver.openInputStream(u)?.use { i -> f.outputStream().use { o -> i.copyTo(o) } }
            n++
        }
        return n
    }

    fun writeHtml(ctx: Context, code: String) { File(dir(ctx), "index.html").writeText(code) }

    fun summary(ctx: Context): String {
        val fs = files(ctx)
        return if (fs.isEmpty()) "لا توجد ملفات" else "${fs.size} ملف · ${fs.sumOf { it.length() } / 1024} KB"
    }

    val textExt = setOf("html", "htm", "css", "js", "json", "txt", "xml", "svg", "md", "csv", "map", "webmanifest", "ts", "yml", "yaml")
    fun isText(f: File) = f.extension.lowercase() in textExt && f.length() < 800_000
    fun isImage(f: File) = f.extension.lowercase() in setOf("png", "jpg", "jpeg", "webp", "gif", "bmp", "ico")
}
