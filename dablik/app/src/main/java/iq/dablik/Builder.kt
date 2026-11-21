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

/** طلب بناء: مجلد الموقع + الصلاحيات + إعدادات الأدوات */
class BuildRequest(
    val name: String, val pkg: String, val version: String, val iconPng: ByteArray?,
    val siteDir: File, val perms: List<String>, val config: String,
)

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

        val www = collectSite(req.siteDir)
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
                            val patched = patchManifest(bytes, mapOf(PH_PKG to req.pkg, PH_LABEL to req.name, PH_VER to req.version), versionCode(req.version), req.perms)
                            put(zo, e.name, patched, null)
                        }
                        req.iconPng != null && e.name.startsWith("res/") && e.name.endsWith(".png") && sha(bytes) == placeholderHash -> {
                            put(zo, e.name, req.iconPng, null); iconDone = true
                        }
                        else -> put(zo, e.name, bytes, e)
                    }
                }
                for ((path, data) in www) put(zo, "assets/www/$path", data, null)
                put(zo, "assets/wbl_config.json", req.config.toByteArray(), null)
            }
        }
        log("[5/7] تعديل AndroidManifest: الاسم «${req.name}» · الحزمة ${req.pkg} · الإصدار ${req.version} (${versionCode(req.version)}) · ${req.perms.size} صلاحية")
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

    /** يجمع ملفات الموقع من المجلد: المسار النسبي → المحتوى */
    private fun collectSite(dir: File): Map<String, ByteArray> {
        val out = LinkedHashMap<String, ByteArray>()
        val base = dir.canonicalPath + File.separator
        dir.walkTopDown().filter { it.isFile }.forEach { f ->
            val rel = f.canonicalPath.removePrefix(base).replace(File.separatorChar, '/')
            if (!rel.startsWith(".")) out[rel] = f.readBytes()
        }
        if (!out.containsKey("index.html")) throw IllegalStateException("لا يوجد ملف index.html في ملفات التطبيق")
        return out
    }

    // ---------- تعديل AndroidManifest الثنائي (AXML) ----------
    private fun u32(b: ByteArray, o: Int) = ByteBuffer.wrap(b, o, 4).order(ByteOrder.LITTLE_ENDIAN).int
    private fun u16(b: ByteArray, o: Int) = ByteBuffer.wrap(b, o, 2).order(ByteOrder.LITTLE_ENDIAN).short.toInt() and 0xFFFF

    private class Chunk(val type: Int, val bytes: ByteArray)

    private fun putInt(b: ByteArray, o: Int, v: Int) { ByteBuffer.wrap(b, o, 4).order(ByteOrder.LITTLE_ENDIAN).putInt(v) }

    fun patchManifest(src: ByteArray, repl: Map<String, String>, versionCode: Int, perms: List<String> = emptyList()): ByteArray {
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
        val patched = ArrayList(strings.map { s -> var r = s; for ((k, v) in repl) r = r.replace(k, v); r })
        val rest = src.copyOfRange(poolStart + poolSize, src.size)
        // رقم الإصدار (Int) داخل بقية الملف
        val pat = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(PH_CODE).array()
        val nv = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(versionCode).array()
        var k = 0
        while (k <= rest.size - 4) { if (rest[k] == pat[0] && rest[k + 1] == pat[1] && rest[k + 2] == pat[2] && rest[k + 3] == pat[3]) { System.arraycopy(nv, 0, rest, k, 4); break }; k++ }

        // تقسيم بقية الملف إلى أجزاء لإضافة عناصر uses-permission
        val chunks = ArrayList<Chunk>()
        var q = 0
        while (q < rest.size) { val sz = u32(rest, q + 4); chunks.add(Chunk(u16(rest, q), rest.copyOfRange(q, q + sz))); q += sz }
        val permName = patched.indexOf("uses-permission")
        var insertAt = -1; var template: ByteArray? = null
        val existing = HashSet<Int>()
        if (permName >= 0) {
            for ((ci, c) in chunks.withIndex()) {
                if (c.type == 0x0102 && u32(c.bytes, 20) == permName && u16(c.bytes, 28) >= 1) {
                    existing.add(u32(c.bytes, 44))
                    if (template == null) {
                        template = c.bytes
                        for (j in ci + 1 until chunks.size) if (chunks[j].type == 0x0103 && u32(chunks[j].bytes, 20) == permName) { insertAt = j + 1; break }
                    }
                }
            }
        }
        val added = ArrayList<Chunk>()
        if (template != null && insertAt > 0) {
            val endTemplate = chunks[insertAt - 1].bytes
            for (p in perms.distinct()) {
                var idx = patched.indexOf(p)
                if (idx < 0) { patched.add(p); idx = patched.size - 1 }
                if (idx in existing) continue
                existing.add(idx)
                val st = template.copyOf(); putInt(st, 44, idx); putInt(st, 52, idx)
                added.add(Chunk(0x0102, st)); added.add(Chunk(0x0103, endTemplate.copyOf()))
            }
            chunks.addAll(insertAt, added)
        }
        val newRest = ByteArrayOutputStream(); for (c in chunks) newRest.write(c.bytes)

        // إعادة بناء جدول النصوص بترميز UTF-16
        val n = patched.size
        val data = ByteArrayOutputStream(); val offs = IntArray(n)
        patched.forEachIndexed { i, s ->
            offs[i] = data.size()
            val bytes = s.toByteArray(Charsets.UTF_16LE)
            val len = s.length
            if (len >= 0x8000) { data.write(((len shr 16) or 0x80) and 0xFF); data.write((len shr 24) and 0xFF); data.write(len and 0xFF); data.write((len shr 8) and 0xFF) }
            else { data.write(len and 0xFF); data.write((len shr 8) and 0xFF) }
            data.write(bytes); data.write(0); data.write(0)
        }
        while (data.size() % 4 != 0) data.write(0)
        val newStringsStart = 28 + 4 * n
        val newPoolSize = newStringsStart + data.size()
        val head = ByteBuffer.allocate(newStringsStart).order(ByteOrder.LITTLE_ENDIAN)
        head.putShort(1).putShort(28).putInt(newPoolSize).putInt(n).putInt(0).putInt(flags and 0x100.inv()).putInt(newStringsStart).putInt(0)
        for (o in offs) head.putInt(o)
        val out = ByteArrayOutputStream()
        val total = 8 + newPoolSize + newRest.size()
        out.write(src, 0, 4); out.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(total).array())
        out.write(head.array()); out.write(data.toByteArray()); out.write(newRest.toByteArray())
        return out.toByteArray()
    }
}
