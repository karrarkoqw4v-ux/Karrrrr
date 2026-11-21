package iq.dablik

import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * طبقة القياسات التلقائية: تضيف viewport ونصاً وأحجاماً تتكيف مع كل جهاز.
 * إذا وُجد ملف device-sizes.json في المشروع يُستخدم، وإلا تُطبَّق قيم افتراضية تلقائياً.
 */
object Responsive {
    const val FILE = "device-sizes.json"

    fun defaults(): JSONObject = JSONObject()
        .put("mode", "fluid")      // fluid: حجم متدرج مع عرض الشاشة، stepped: خطوات حسب العرض
        .put("baseWidth", 390)     // عرض التصميم المرجعي بالبكسل
        .put("baseFont", 16)       // حجم الخط الأساسي عند العرض المرجعي
        .put("minFont", 14)
        .put("maxFont", 22)
        .put("breakpoints", JSONArray()
            .put(JSONObject().put("minWidth", 0).put("fontSize", 14))
            .put(JSONObject().put("minWidth", 360).put("fontSize", 16))
            .put(JSONObject().put("minWidth", 600).put("fontSize", 18))
            .put(JSONObject().put("minWidth", 900).put("fontSize", 20)))

    fun templateText(): String = defaults().toString(2)

    /** يقرأ الملف المخصص إن وُجد. يعيد (القيم، هل هو مخصص) */
    fun read(dir: File): Pair<JSONObject, Boolean> {
        val f = File(dir, FILE)
        if (f.exists()) {
            runCatching {
                val j = JSONObject(f.readText())
                val d = defaults()
                for (k in d.keys()) if (!j.has(k)) j.put(k, d.get(k))
                return j to true
            }
        }
        return defaults() to false
    }

    fun isValid(text: String) = runCatching { JSONObject(text); true }.getOrDefault(false)

    private val viewportRe = Regex("<meta[^>]+name=[\"']viewport[\"']", RegexOption.IGNORE_CASE)

    /** يحقن الطبقة داخل صفحة HTML. يعيد null إذا كانت محقونة مسبقاً */
    fun inject(html: String, sizes: JSONObject): String? {
        if (html.contains("id=\"dablik-auto\"")) return null
        val meta = if (viewportRe.containsMatchIn(html)) "" else "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1, viewport-fit=cover\">\n"
        val block = meta + """<style id="dablik-auto">
html{-webkit-text-size-adjust:100%;text-size-adjust:100%}
body{overflow-x:hidden;word-wrap:break-word}
img,video,canvas,svg{max-width:100%;height:auto}
input,select,textarea,button{font-size:max(16px,1em);max-width:100%}
:root{--du:1px;--dw:100vw;--dh:100vh;--sat:env(safe-area-inset-top);--sab:env(safe-area-inset-bottom)}
</style>
<script id="dablik-auto-js">(function(){var S=${sizes};
function fit(){var d=document.documentElement,w=window.innerWidth,h=window.innerHeight,s=Math.min(w,h);
var fs;if(S.mode==='stepped'&&S.breakpoints){fs=S.baseFont;S.breakpoints.forEach(function(b){if(w>=(b.minWidth||0))fs=b.fontSize});}
else{fs=S.baseFont*(s/S.baseWidth);}
fs=Math.max(S.minFont,Math.min(S.maxFont,fs));
d.style.fontSize=fs+'px';d.style.setProperty('--du',(s/S.baseWidth)+'px');d.style.setProperty('--dw',w+'px');d.style.setProperty('--dh',h+'px');d.style.setProperty('--dfs',fs+'px');}
fit();window.addEventListener('resize',fit);window.addEventListener('orientationchange',fit);})();</script>
"""
        val head = Regex("<head[^>]*>", RegexOption.IGNORE_CASE).find(html)
        return if (head != null) html.substring(0, head.range.last + 1) + "\n" + block + html.substring(head.range.last + 1)
        else if (Regex("<html[^>]*>", RegexOption.IGNORE_CASE).containsMatchIn(html)) Regex("<html[^>]*>", RegexOption.IGNORE_CASE).replace(html) { it.value + "<head>\n$block</head>" }
        else "<!doctype html><html><head><meta charset=\"utf-8\">\n$block</head><body>\n$html\n</body></html>"
    }
}
