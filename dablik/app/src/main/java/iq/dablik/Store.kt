package iq.dablik

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.runtime.mutableStateListOf
import androidx.core.content.FileProvider
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class Built(val name: String, val pkg: String, val version: String, val path: String, val icon: String, val time: Long)

/** سجل التطبيقات التي أنشأها المستخدم (محفوظ على الجهاز) */
object Store {
    val items = mutableStateListOf<Built>()
    private lateinit var ctx: Context
    private val sp get() = ctx.getSharedPreferences("dablik", Context.MODE_PRIVATE)

    fun init(c: Context) {
        ctx = c.applicationContext
        val arr = JSONArray(sp.getString("recent", "[]"))
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            if (File(o.getString("path")).exists()) items.add(Built(o.getString("name"), o.getString("pkg"), o.getString("ver"), o.getString("path"), o.optString("icon"), o.getLong("t")))
        }
    }

    private fun save() {
        val a = JSONArray(); items.forEach { a.put(JSONObject().put("name", it.name).put("pkg", it.pkg).put("ver", it.version).put("path", it.path).put("icon", it.icon).put("t", it.time)) }
        sp.edit().putString("recent", a.toString()).apply()
    }

    fun add(b: Built) { items.removeAll { it.path == b.path }; items.add(0, b); save() }
    fun remove(b: Built) { items.remove(b); File(b.path).delete(); save() }

    fun install(c: Context, f: File) {
        if (!c.packageManager.canRequestPackageInstalls()) {
            c.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + c.packageName)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            return
        }
        val uri = FileProvider.getUriForFile(c, c.packageName + ".files", f)
        c.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun share(c: Context, f: File) {
        val uri = FileProvider.getUriForFile(c, c.packageName + ".files", f)
        c.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("application/vnd.android.package-archive").putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION), "مشاركة APK").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
