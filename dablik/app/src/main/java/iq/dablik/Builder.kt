package iq.dablik

import android.content.Context
import android.net.Uri
import com.android.apksig.ApkSigner
import com.android.apksig.ApkVerifier
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.KeyStore
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.cert.X509Certificate
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/** مصدر محتوى التطبيق */
sealed interface Source {
    class Html(val code: String) : Source
    class HtmlFile(val uri: Uri) : Source
    class Zip(val uri: Uri) : Source
}

class BuildRequest(val name: String, val pkg: String, val version: String, val iconPng: ByteArray?, val source: Source)

/**
 * يبني APK قابل للتثبيت من قالب جاهز (shell.apk): يعدّل الاسم والحزمة والإصدار في AndroidManifest الثنائي،
 * يستبدل الأيقونة، يضع ملفات الموقع في assets/www، ثم يوقّع الحزمة (v1 + v2) ويتحقق منها.
 */
object ApkBuilder {
    private const val PH_PKG = "wbl.placeholder.appid"
    private const val PH_LABEL = "WBLLABELPLACEHOLDER"
    private const val PH_VER = "WBLVERNAMEPLACEHOLDER"
    private const val PH_CODE = 77777777

    fun versionCode(v: String): Int {
        val p = v.split(".").mapNotNull { it.trim().toIntOrNull() }
        return ((p.getOrElse(0) { 1 }) * 10000 + p.getOrElse(1) { 0 } * 100 + p.getOrElse(2) { 0 }).coerceIn(1, 2_000_000_000)
    }

    fun validPackage(p: String) = Regex("^[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)+$").matches(p)

    suspend fun buildSuspend(ctx: Context, req: BuildRequest, log: suspend (String) -> Unit): File {
        val outDir = File(ctx.filesDir, "builds").apply { mkdirs() }
        val work = File(ctx.cacheDir, "work").apply { deleteRecursively(); mkdirs() }

        log("[1/7] تهيئة مجلد العمل ... تم")
        val shell = File(work, "shell.apk")
        try { ctx.assets.open("shell.apk").use { i -> shell.outputStream().use { o -> i.copyTo(o) } } }
        catch (e: Exception) { throw IllegalStateException("قالب التطبيق (shell.apk) غير موجود داخل دبليك") }
        log("[2/7] استخراج قالب التطبيق: ${shell.length() / 1024} KB")

        val signerCfg = loadSigner(ctx)
        log("[3/7] تحميل مكتبة التوقيع apksig وشهادة التوقيع ... تم")

        val www = collectSite(ctx, req.source)
        log("[4/7] تجهيز ملفات الموقع: ${www.size} ملف، ${www.values.sumOf { it.size } / 1024} KB")

        val placeholderHash = sha(ctx.assets.open("icon_placeholder.png").readBytes())
        val unsigned = File(work, "unsigned.apk")
        var iconDone = false
        ZipFile(shell).use { zf ->
            ZipOutputStream(unsigned.outputStream().buffered()).use { zo ->
                for (e in zf.entries()) {
                    if (e.isDirectory || e.name.startsWith("META-INF/")) continue
                    val bytes = zf.getInputStream(e).readBytes()
                    when {
                        e.name == "AndroidManifest.xml" -> {
                            val patched = patchManifest(bytes, mapOf(PH_PKG to req.pkg, PH_LABEL to req.name, PH_VER to req.version), versionCode(req.version))
                            put(zo, e.name, patched, null)
                        }
                        req.iconPng != null && e.name.startsWith("res/") && e.name.endsWith(".png") && sha(bytes) == placeholderHash -> {
                            put(zo, e.name, req.iconPng, null); iconDone = true
                        }
                        else -> put(zo, e.name, bytes, e)
                    }
                }
                for ((path, data) in www) put(zo, "assets/www/$path", data, null)
            }
        }
        log("[5/7] تعديل AndroidManifest: الاسم «${req.name}» · الحزمة ${req.pkg} · الإصدار ${req.version} (${versionCode(req.version)})")
        log(if (req.iconPng == null) "[6/7] لم تُحدد أيقونة: تُستخدم الأيقونة الافتراضية" else if (iconDone) "[6/7] حقن الأيقونة المخصصة ... تم" else "[6/7] تحذير: لم أجد موضع الأيقونة في القالب، بقيت الافتراضية")

        val safe = req.name.replace(Regex("[^A-Za-z0-9\\u0600-\\u06FF_-]"), "_").ifBlank { "app" }
        val out = File(outDir, "${safe}_${req.version}.apk")
        ApkSigner.Builder(listOf(signerCfg)).setInputApk(unsigned).setOutputApk(out).setMinSdkVersion(21)
            .setV1SigningEnabled(true).setV2SigningEnabled(true).build().sign()
        log("[7/7] التجميع والتوقيع (v1 + v2) ... تم")

        val res = ApkVerifier.Builder(out).build().verify()
        if (!res.isVerified) throw IllegalStateException("فشل التحقق من التوقيع: " + res.errors.joinToString { it.toString() })
        log("التحقق من الحزمة: ناجح (v1=${res.isVerifiedUsingV1Scheme}, v2=${res.isVerifiedUsingV2Scheme})")
        log("الحجم النهائي: ${out.length() / 1024} KB")
        work.deleteRecursively()
        return out
    }

    private fun put(zo: ZipOutputStream, name: String, data: ByteArray, src: ZipEntry?) {
        val e = ZipEntry(name)
        if (src != null && src.method == ZipEntry.STORED) {
            e.method = ZipEntry.STORED; e.size = data.size.toLong(); e.compressedSize = data.size.toLong()
            e.crc = java.util.zip.CRC32().also { it.update(data) }.value
        }
        zo.putNextEntry(e); zo.write(data); zo.closeEntry()
    }

    private fun sha(b: ByteArray) = MessageDigest.getInstance("SHA-256").digest(b).joinToString("") { "%02x".format(it) }

    private fun loadSigner(ctx: Context): ApkSigner.SignerConfig {
        val ks = KeyStore.getInstance("PKCS12")
        ctx.assets.open("dablik.p12").use { ks.load(it, "dablik123".toCharArray()) }
        val key = ks.getKey("dablik", "dablik123".toCharArray()) as PrivateKey
        val cert = ks.getCertificate("dablik") as X509Certificate
        return ApkSigner.SignerConfig.Builder("dablik", key, listOf(cert)).build()
    }

    /** يجمع ملفات الموقع: المسار داخل www → المحتوى */
    private fun collectSite(ctx: Context, s: Source): Map<String, ByteArray> {
        when (s) {
            is Source.Html -> return mapOf("index.html" to s.code.toByteArray())
            is Source.HtmlFile -> return mapOf("index.html" to (ctx.contentResolver.openInputStream(s.uri)?.use { it.readBytes() } ?: throw IllegalStateException("تعذر قراءة ملف HTML")))
            is Source.Zip -> {
                val all = LinkedHashMap<String, ByteArray>()
                ctx.contentResolver.openInputStream(s.uri)?.use { ins ->
                    ZipInputStream(ins).use { zi ->
                        var e = zi.nextEntry
                        while (e != null) {
                            val n = e.name.replace('\\', '/')
                            if (!e.isDirectory && !n.contains("..") && !n.startsWith("__MACOSX/") && !n.substringAfterLast('/').startsWith("._")) {
                                val bo = ByteArrayOutputStream(); zi.copyTo(bo); all[n.trimStart('/')] = bo.toByteArray()
                            }
                            e = zi.nextEntry
                        }
                    }
                } ?: throw IllegalStateException("تعذر فتح ملف ZIP")
                val idx = all.keys.filter { it.substringAfterLast('/').equals("index.html", true) }.minByOrNull { it.count { c -> c == '/' } }
                    ?: throw IllegalStateException("لا يوجد index.html داخل الـ ZIP")
                val prefix = idx.substringBeforeLast('/', "").let { if (it.isEmpty()) "" else "$it/" }
                val out = LinkedHashMap<String, ByteArray>()
                for ((k, v) in all) if (k.startsWith(prefix)) out[k.removePrefix(prefix)] = v
                if (!out.containsKey("index.html")) out["index.html"] = all[idx]!!
                return out
            }
        }
    }

    // ---------- تعديل AndroidManifest الثنائي (AXML) ----------
    private fun u32(b: ByteArray, o: Int) = ByteBuffer.wrap(b, o, 4).order(ByteOrder.LITTLE_ENDIAN).int
    private fun u16(b: ByteArray, o: Int) = ByteBuffer.wrap(b, o, 2).order(ByteOrder.LITTLE_ENDIAN).short.toInt() and 0xFFFF

    fun patchManifest(src: ByteArray, repl: Map<String, String>, versionCode: Int): ByteArray {
        require(u16(src, 0) == 0x0003) { "AndroidManifest غير صالح" }
        val poolStart = 8
        require(u16(src, poolStart) == 0x0001) { "لم أجد جدول النصوص" }
        val poolSize = u32(src, poolStart + 4)
        val count = u32(src, poolStart + 8)
        val styleCount = u32(src, poolStart + 12)
        val flags = u32(src, poolStart + 16)
        val stringsStart = u32(src, poolStart + 20)
        require(styleCount == 0) { "الأنماط غير مدعومة" }
        val utf8 = flags and 0x100 != 0
        val strings = ArrayList<String>(count)
        for (i in 0 until count) {
            var p = poolStart + stringsStart + u32(src, poolStart + 28 + 4 * i)
            if (utf8) {
                var l = src[p].toInt() and 0xFF; p += 1; if (l and 0x80 != 0) { l = ((l and 0x7F) shl 8) or (src[p].toInt() and 0xFF); p += 1 }
                var bl = src[p].toInt() and 0xFF; p += 1; if (bl and 0x80 != 0) { bl = ((bl and 0x7F) shl 8) or (src[p].toInt() and 0xFF); p += 1 }
                strings.add(String(src, p, bl, Charsets.UTF_8))
            } else {
                var l = u16(src, p); p += 2
                if (l and 0x8000 != 0) { l = ((l and 0x7FFF) shl 16) or u16(src, p); p += 2 }
                strings.add(String(src, p, l * 2, Charsets.UTF_16LE))
            }
        }
        val patched = strings.map { s -> var r = s; for ((k, v) in repl) r = r.replace(k, v); r }
        // إعادة بناء جدول النصوص بترميز UTF-16
        val data = ByteArrayOutputStream(); val offs = IntArray(count)
        patched.forEachIndexed { i, s ->
            offs[i] = data.size()
            val bytes = s.toByteArray(Charsets.UTF_16LE)
            val len = s.length
            if (len >= 0x8000) { data.write(((len shr 16) or 0x80) and 0xFF); data.write((len shr 24) and 0xFF); data.write(len and 0xFF); data.write((len shr 8) and 0xFF) }
            else { data.write(len and 0xFF); data.write((len shr 8) and 0xFF) }
            data.write(bytes); data.write(0); data.write(0)
        }
        while (data.size() % 4 != 0) data.write(0)
        val newStringsStart = 28 + 4 * count
        val newPoolSize = newStringsStart + data.size()
        val head = ByteBuffer.allocate(newStringsStart).order(ByteOrder.LITTLE_ENDIAN)
        head.putShort(1).putShort(28).putInt(newPoolSize).putInt(count).putInt(0).putInt(flags and 0x100.inv()).putInt(newStringsStart).putInt(0)
        for (o in offs) head.putInt(o)
        val rest = src.copyOfRange(poolStart + poolSize, src.size)
        // رقم الإصدار (Int) داخل بقية الملف
        val pat = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(PH_CODE).array()
        val nv = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(versionCode).array()
        var i = 0
        while (i <= rest.size - 4) { if (rest[i] == pat[0] && rest[i + 1] == pat[1] && rest[i + 2] == pat[2] && rest[i + 3] == pat[3]) { System.arraycopy(nv, 0, rest, i, 4); break }; i++ }
        val out = ByteArrayOutputStream()
        val total = 8 + newPoolSize + rest.size
        out.write(src, 0, 4); out.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(total).array())
        out.write(head.array()); out.write(data.toByteArray()); out.write(rest)
        return out.toByteArray()
    }
}
