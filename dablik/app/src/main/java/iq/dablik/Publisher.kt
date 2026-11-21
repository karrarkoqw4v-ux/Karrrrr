package iq.dablik

import android.content.Context
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** نشر تحديث للجميع: يرفع ملفات المشروع (ZIP + update.json) إلى مستودع GitHub عام، والتطبيقات المثبتة تحدّث نفسها منه */
object Publisher {
    fun token(ctx: Context) = ctx.getSharedPreferences("dablik", Context.MODE_PRIVATE).getString("gh_token", "") ?: ""
    fun saveToken(ctx: Context, t: String) { ctx.getSharedPreferences("dablik", Context.MODE_PRIVATE).edit().putString("gh_token", t.trim()).apply() }

    fun validRepo(r: String) = Regex("^[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+$").matches(r.trim())
    fun rawBase(repo: String, branch: String, pkg: String) = "https://raw.githubusercontent.com/${repo.trim()}/${branch.trim().ifBlank { "main" }}/dablik/$pkg"

    private fun zipProject(ctx: Context): ByteArray {
        val base = Project.dir(ctx)
        val bo = ByteArrayOutputStream()
        ZipOutputStream(bo).use { zo ->
            for (f in Project.files(ctx)) {
                zo.putNextEntry(ZipEntry(Project.rel(base, f))); zo.write(f.readBytes()); zo.closeEntry()
            }
        }
        return bo.toByteArray()
    }

    private fun call(method: String, url: String, token: String, body: String? = null): Pair<Int, String> {
        val c = URL(url).openConnection() as HttpURLConnection
        c.requestMethod = method
        c.connectTimeout = 20000; c.readTimeout = 60000
        c.setRequestProperty("Authorization", "Bearer $token")
        c.setRequestProperty("Accept", "application/vnd.github+json")
        c.setRequestProperty("User-Agent", "Dablik")
        if (body != null) { c.doOutput = true; c.setRequestProperty("Content-Type", "application/json"); c.outputStream.use { it.write(body.toByteArray()) } }
        val code = c.responseCode
        val text = (if (code < 400) c.inputStream else c.errorStream)?.use { it.readBytes().toString(Charsets.UTF_8) } ?: ""
        return code to text
    }

    private fun put(repo: String, branch: String, path: String, bytes: ByteArray, token: String, msg: String) {
        val api = "https://api.github.com/repos/${repo.trim()}/contents/$path"
        val (gc, gt) = call("GET", "$api?ref=$branch", token)
        val sha = if (gc == 200) JSONObject(gt).optString("sha") else null
        val body = JSONObject().put("message", msg).put("content", Base64.encodeToString(bytes, Base64.NO_WRAP)).put("branch", branch)
        if (!sha.isNullOrEmpty()) body.put("sha", sha)
        val (pc, pt) = call("PUT", api, token, body.toString())
        if (pc != 200 && pc != 201) throw IllegalStateException("GitHub $pc: " + runCatching { JSONObject(pt).optString("message") }.getOrDefault(pt.take(120)))
    }

    /** يعيد رقم الإصدار الجديد */
    fun publish(ctx: Context, f: FormState, log: (String) -> Unit): Int {
        val repo = f.ghRepo.trim(); val branch = f.ghBranch.trim().ifBlank { "main" }; val tk = token(ctx)
        if (!validRepo(repo)) throw IllegalStateException("repository must look like owner/name")
        if (tk.isBlank()) throw IllegalStateException("GitHub token is empty")
        if (!ApkBuilder.validPackage(f.pkg.trim())) throw IllegalStateException("fill the package name first")
        val files = Project.files(ctx)
        if (files.isEmpty()) throw IllegalStateException("no project files to publish")
        val ver = f.remoteVer + 1
        val zip = zipProject(ctx)
        val base = rawBase(repo, branch, f.pkg.trim())
        log("[INFO] files=${files.size}  zip=${zip.size / 1024} KB  version=$ver")
        log("[PUSH] dablik/${f.pkg.trim()}/site.zip")
        put(repo, branch, "dablik/${f.pkg.trim()}/site.zip", zip, tk, "Dablik update v$ver")
        val ads = JSONArray(); if (f.adsOn) f.ads.forEach { ads.put(JSONObject().put("title", it.title).put("text", it.text).put("url", it.url).put("popup", it.popup)) }
        val j = JSONObject().put("version", ver).put("zip", "$base/site.zip").put("ads", ads)
        log("[PUSH] dablik/${f.pkg.trim()}/update.json")
        put(repo, branch, "dablik/${f.pkg.trim()}/update.json", j.toString(2).toByteArray(), tk, "Dablik update manifest v$ver")
        f.remoteVer = ver; f.updateUrl = "$base/update.json"; f.update = true
        log("[ OK ] published v$ver  ->  $base/update.json")
        return ver
    }
}
