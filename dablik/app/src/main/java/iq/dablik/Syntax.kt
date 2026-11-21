package iq.dablik

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

/** تلوين الأكواد لعدة لغات برمجة بألوان قريبة من VS Code / بايثون */
object Syntax {
    private val cKw = ("abstract|as|async|await|break|case|catch|class|const|continue|data|default|defer|delete|do|else|enum|export|extends|extern|false|final|finally|fn|for|fun|func|function|goto|if|impl|implements|import|in|inline|instanceof|interface|internal|is|lambda|let|lazy|loop|match|mod|mut|namespace|new|null|nil|object|of|open|operator|override|package|private|protected|pub|public|readonly|return|sealed|self|static|struct|super|switch|this|throw|throws|trait|true|try|type|typealias|typeof|undefined|unsafe|use|using|val|var|virtual|void|volatile|when|where|while|with|yield|int|long|float|double|bool|boolean|char|string|byte|short|unsigned|signed|auto|echo|print|elseif|endif|foreach|global|require|include|isset|unset|array|list|dict|String|Int|Long|Boolean|Double|Float|List|Map|Set|Unit|Any")
    private val pyKw = "and|as|assert|async|await|break|class|continue|def|del|elif|else|except|finally|for|from|global|if|import|in|is|lambda|nonlocal|not|or|pass|raise|return|try|while|with|yield|None|True|False|self|cls|print|range|len|str|int|float|list|dict|set|tuple"
    private val rbKw = "alias|and|begin|break|case|class|def|defined|do|else|elsif|end|ensure|false|for|if|in|module|next|nil|not|or|redo|rescue|retry|return|self|super|then|true|undef|unless|until|when|while|yield|puts|require|attr_accessor"
    private val shKw = "if|then|else|elif|fi|for|while|until|do|done|case|esac|in|function|return|exit|echo|export|local|source|cd|ls|rm|cp|mv|mkdir|cat|grep|sed|awk|sudo|apt|pip|npm|git"
    private val sqlKw = "(?i:select|from|where|insert|into|values|update|set|delete|create|table|drop|alter|add|index|join|left|right|inner|outer|on|group|by|order|having|limit|offset|union|distinct|as|and|or|not|null|is|in|like|between|primary|key|foreign|references|default|unique|count|sum|avg|min|max|case|when|then|else|end|database|view)"

    private val cTypeComment = "//[^\\n]*|/\\*[\\s\\S]*?\\*/"
    private val str = "\"(?:\\\\.|[^\"\\\\\\n])*\"|'(?:\\\\.|[^'\\\\\\n])*'|`(?:\\\\.|[^`\\\\])*`"
    private val num = "\\b0x[0-9a-fA-F]+\\b|\\b\\d+(?:\\.\\d+)?(?:[eE][+-]?\\d+)?\\b"

    private val cComment = Color(0xFF6A9955); private val cString = Color(0xFFCE9178); private val cNum = Color(0xFFB5CEA8)
    private val cKeyword = Color(0xFFC586C0); private val cFunc = Color(0xFFDCDCAA); private val cType = Color(0xFF4EC9B0)
    private val cTag = Color(0xFF569CD6); private val cAttr = Color(0xFF9CDCFE); private val cVar = Color(0xFF9CDCFE)

    private class Rule(val re: String, val color: Color)
    private class Lang(val rules: List<Rule>) { val regex = Regex(rules.joinToString("|") { "(${it.re})" }, RegexOption.MULTILINE) }

    private fun lang(vararg rules: Rule) = Lang(rules.toList())
    private val cache = HashMap<String, Lang>()

    private fun build(ext: String): Lang = when (ext) {
        "html", "htm", "xml", "svg", "xhtml", "vue" -> lang(Rule("<!--[\\s\\S]*?-->", cComment), Rule(str, cString), Rule("</?[A-Za-z][\\w:-]*|/?>", cTag), Rule("\\b[\\w:@.-]+(?==)", cAttr), Rule("&\\w+;", cNum))
        "css", "scss", "less" -> lang(Rule("/\\*[\\s\\S]*?\\*/", cComment), Rule(str, cString), Rule("#[0-9a-fA-F]{3,8}\\b", cNum), Rule("[.#][A-Za-z_][\\w-]*", cType), Rule("@[a-z-]+", cKeyword), Rule("[a-z-]+(?=\\s*:)", cAttr), Rule("-?\\d+(?:\\.\\d+)?(?:px|em|rem|%|vh|vw|s|ms|deg)?", cNum))
        "json", "webmanifest" -> lang(Rule("\"(?:\\\\.|[^\"\\\\\\n])*\"(?=\\s*:)", cAttr), Rule("\"(?:\\\\.|[^\"\\\\\\n])*\"", cString), Rule("\\b(?:true|false|null)\\b", cKeyword), Rule(num, cNum))
        "py" -> lang(Rule("\"\"\"[\\s\\S]*?\"\"\"|'''[\\s\\S]*?'''", cString), Rule("#[^\\n]*", cComment), Rule(str, cString), Rule("@[A-Za-z_][\\w.]*", cFunc), Rule("\\b(?:$pyKw)\\b", cKeyword), Rule("\\b[A-Za-z_]\\w*(?=\\s*\\()", cFunc), Rule("\\b[A-Z][A-Za-z0-9_]*\\b", cType), Rule(num, cNum))
        "rb" -> lang(Rule("#[^\\n]*", cComment), Rule(str, cString), Rule(":[A-Za-z_]\\w*", cNum), Rule("@{1,2}[A-Za-z_]\\w*", cVar), Rule("\\b(?:$rbKw)\\b", cKeyword), Rule("\\b[A-Za-z_]\\w*(?=\\s*\\()", cFunc), Rule("\\b[A-Z][A-Za-z0-9_]*\\b", cType), Rule(num, cNum))
        "sh", "bash", "zsh", "bat", "ps1" -> lang(Rule("#[^\\n]*", cComment), Rule(str, cString), Rule("\\$\\{?[A-Za-z_]\\w*\\}?", cVar), Rule("\\b(?:$shKw)\\b", cKeyword), Rule(num, cNum))
        "sql" -> lang(Rule("--[^\\n]*|/\\*[\\s\\S]*?\\*/", cComment), Rule(str, cString), Rule("\\b$sqlKw\\b", cKeyword), Rule(num, cNum))
        "php" -> lang(Rule("//[^\\n]*|#[^\\n]*|/\\*[\\s\\S]*?\\*/", cComment), Rule(str, cString), Rule("\\$[A-Za-z_]\\w*", cVar), Rule("<\\?php|\\?>", cTag), Rule("\\b(?:$cKw)\\b", cKeyword), Rule("\\b[A-Za-z_]\\w*(?=\\s*\\()", cFunc), Rule("\\b[A-Z][A-Za-z0-9_]*\\b", cType), Rule(num, cNum))
        "yml", "yaml", "toml", "ini", "env", "properties", "cfg" -> lang(Rule("#[^\\n]*|;[^\\n]*", cComment), Rule(str, cString), Rule("^[ \\t-]*[\\w.-]+(?=\\s*[:=])", cAttr), Rule("\\b(?:true|false|null|yes|no)\\b", cKeyword), Rule(num, cNum))
        "md", "markdown" -> lang(Rule("^#{1,6}[^\\n]*", cTag), Rule("`[^`\\n]+`", cString), Rule("\\*\\*[^*\\n]+\\*\\*", cType), Rule("\\[[^\\]\\n]*\\]\\([^)\\n]*\\)", cAttr))
        else -> lang(Rule(cTypeComment, cComment), Rule(str, cString), Rule("\\b(?:$cKw)\\b", cKeyword), Rule("\\b[A-Za-z_]\\w*(?=\\s*\\()", cFunc), Rule("\\b[A-Z][A-Za-z0-9_]*\\b", cType), Rule(num, cNum))
    }

    fun name(ext: String): String = when (ext) {
        "html", "htm", "xhtml" -> "HTML"; "css" -> "CSS"; "scss" -> "SCSS"; "less" -> "Less"; "js", "mjs", "jsx" -> "JavaScript"; "ts", "tsx" -> "TypeScript"
        "json", "webmanifest" -> "JSON"; "py" -> "Python"; "kt", "kts" -> "Kotlin"; "java" -> "Java"; "php" -> "PHP"; "c", "h" -> "C"; "cpp", "hpp", "cc" -> "C++"
        "cs" -> "C#"; "go" -> "Go"; "rs" -> "Rust"; "rb" -> "Ruby"; "swift" -> "Swift"; "dart" -> "Dart"; "sh", "bash", "zsh" -> "Shell"; "sql" -> "SQL"
        "xml" -> "XML"; "svg" -> "SVG"; "md", "markdown" -> "Markdown"; "yml", "yaml" -> "YAML"; "toml" -> "TOML"; "vue" -> "Vue"; "lua" -> "Lua"; "r" -> "R"
        "bat" -> "Batch"; "ps1" -> "PowerShell"; "txt" -> "Text"; else -> ext.uppercase().ifEmpty { "Text" }
    }

    fun highlight(text: String, ext: String): AnnotatedString {
        if (text.length > 150_000) return AnnotatedString(text)
        val lg = cache.getOrPut(ext) { build(ext) }
        return buildAnnotatedString {
            append(text)
            for (m in lg.regex.findAll(text)) {
                var gi = -1
                for (i in lg.rules.indices) if (m.groups[i + 1] != null) { gi = i; break }
                if (gi >= 0) addStyle(SpanStyle(color = lg.rules[gi].color), m.range.first, m.range.last + 1)
            }
        }
    }
}

class SyntaxTransformation(private val ext: String) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText = TransformedText(Syntax.highlight(text.text, ext), OffsetMapping.Identity)
}
