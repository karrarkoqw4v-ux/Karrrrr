package iq.suls

import android.os.Environment
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

data class ScannedFile(
    val file: File,
    val name: String,
    val sizeBytes: Long,
    val lastModified: Long
)

enum class TimeBucket(val label: String) {
    TODAY("اليوم"),
    THIS_WEEK("هذا الأسبوع"),
    THIS_MONTH("هذا الشهر"),
    OLDER("أقدم من شهر")
}

object FilesScanner {

    // المجلدات الشائعة اللي تتجمع فيها الملفات المحمّلة فعلياً
    private val SCAN_DIRS = listOf(
        "Download", "Downloads", "DCIM/Camera", "Pictures", "Movies",
        "WhatsApp/Media/WhatsApp Documents", "Telegram/Telegram Documents"
    )

    fun scanRecentFiles(): Map<TimeBucket, List<ScannedFile>> {
        val root = Environment.getExternalStorageDirectory()
        val now = System.currentTimeMillis()
        val dayMs = TimeUnit.DAYS.toMillis(1)
        val weekMs = TimeUnit.DAYS.toMillis(7)
        val monthMs = TimeUnit.DAYS.toMillis(30)

        val result = mutableMapOf<TimeBucket, MutableList<ScannedFile>>()
        TimeBucket.values().forEach { result[it] = mutableListOf() }

        for (dirName in SCAN_DIRS) {
            val dir = File(root, dirName)
            if (!dir.exists() || !dir.isDirectory) continue
            val files = dir.listFiles() ?: continue
            for (f in files) {
                if (!f.isFile) continue
                val age = now - f.lastModified()
                val bucket = when {
                    age <= dayMs -> TimeBucket.TODAY
                    age <= weekMs -> TimeBucket.THIS_WEEK
                    age <= monthMs -> TimeBucket.THIS_MONTH
                    else -> TimeBucket.OLDER
                }
                result[bucket]!!.add(ScannedFile(f, f.name, f.length(), f.lastModified()))
            }
        }
        result.forEach { (_, list) -> list.sortByDescending { it.lastModified } }
        return result
    }

    fun delete(files: List<ScannedFile>): Long {
        var freed = 0L
        for (sf in files) {
            val size = sf.file.length()
            if (sf.file.delete()) freed += size
        }
        return freed
    }

    // فحص ملفات غير مرغوبة: مؤقتة/سجلات + صور مكررة فعلياً (بالتجزئة الحقيقية للمحتوى)
    fun scanJunk(): JunkResult {
        val root = Environment.getExternalStorageDirectory()
        val tmpLogFiles = mutableListOf<ScannedFile>()
        val oldInstallers = mutableListOf<ScannedFile>()
        val duplicates = mutableListOf<ScannedFile>()
        val seenHashes = HashMap<String, File>()
        val now = System.currentTimeMillis()
        val weekMs = TimeUnit.DAYS.toMillis(7)

        fun walk(dir: File, depth: Int) {
            if (depth > 4) return
            val children = dir.listFiles() ?: return
            for (c in children) {
                if (c.isDirectory) {
                    if (c.name.startsWith(".")) continue
                    walk(c, depth + 1)
                } else {
                    val lower = c.name.lowercase()
                    when {
                        lower.endsWith(".tmp") || lower.endsWith(".log") || lower.endsWith(".bak") || lower.endsWith(".crdownload") -> {
                            tmpLogFiles.add(ScannedFile(c, c.name, c.length(), c.lastModified()))
                        }
                        lower.endsWith(".apk") && (now - c.lastModified()) > weekMs -> {
                            oldInstallers.add(ScannedFile(c, c.name, c.length(), c.lastModified()))
                        }
                        lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") -> {
                            if (c.length() > 0) {
                                val key = "${c.length()}_${quickHash(c)}"
                                val prev = seenHashes[key]
                                if (prev != null) {
                                    duplicates.add(ScannedFile(c, c.name, c.length(), c.lastModified()))
                                } else {
                                    seenHashes[key] = c
                                }
                            }
                        }
                    }
                }
            }
        }
        for (dirName in SCAN_DIRS) {
            val dir = File(root, dirName)
            if (dir.exists()) walk(dir, 0)
        }
        return JunkResult(tmpLogFiles, oldInstallers, duplicates)
    }

    // تجزئة سريعة (أول وآخر 64KB + الحجم) كافية لاكتشاف التكرار الفعلي بدون قراءة الملف كامل لكل صورة كبيرة
    private fun quickHash(f: File): String {
        return try {
            val md = MessageDigest.getInstance("SHA-256")
            f.inputStream().use { ins ->
                val buf = ByteArray(65536)
                val n = ins.read(buf)
                if (n > 0) md.update(buf, 0, n)
            }
            md.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            f.name
        }
    }
}

data class JunkResult(
    val tmpLogFiles: List<ScannedFile>,
    val oldInstallers: List<ScannedFile>,
    val duplicateImages: List<ScannedFile>
) {
    val totalSize: Long
        get() = (tmpLogFiles + oldInstallers + duplicateImages).sumOf { it.sizeBytes }
}
