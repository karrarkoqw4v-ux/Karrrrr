package iq.student.data

import android.content.Context
import org.json.JSONArray

/** ملزمة مدرس. تُقرأ من assets/handouts.json (الملازم الحقيقية تُضاف هناك لاحقاً) */
data class Handout(
    val id: String, val title: String, val teacher: String, val subject: String,
    val rating: Int, val pages: Int, val year: String, val grade: String, val pdfUrl: String, val sample: Boolean,
)

object Handouts {
    fun load(ctx: Context): List<Handout> = runCatching {
        val text = ctx.assets.open("handouts.json").bufferedReader(Charsets.UTF_8).use { it.readText() }
        val arr = JSONArray(text)
        List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            Handout(
                o.getString("id"), o.getString("title"), o.optString("teacher"), o.optString("subject"),
                o.optInt("rating"), o.optInt("pages"), o.optString("year"), o.optString("grade"),
                o.optString("pdfUrl"), o.optBoolean("sample"),
            )
        }
    }.getOrDefault(emptyList())

    fun find(ctx: Context, id: String) = load(ctx).firstOrNull { it.id == id }
}
