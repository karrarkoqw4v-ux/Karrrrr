package iq.student.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector
import iq.student.ui.theme.Tone

enum class Stage { Primary, Middle, Prep }

/** enabled=false تعني "قيد التطوير" */
data class Grade(val id: String, val label: String, val stage: Stage, val enabled: Boolean)

enum class SubjectKind { English, Math, Science, Biology, General }

data class Subject(val id: String, val title: String, val kind: SubjectKind, val icon: ImageVector, val tone: Tone)

enum class AiTask {
    TranslateText, TranslatePronounce, TranslateTyped,
    MathSimple, MathExplain,
    SciencePage, ScienceSolve,
    BioExplain, BioDiagram,
    PageAnswers, Ask,
}

/** قسم داخل المادة. typed=true يعني إدخال بالكتابة بدل الكاميرا */
data class Section(val task: AiTask, val title: String, val sub: String, val icon: ImageVector, val tone: Tone, val typed: Boolean = false)

object Catalog {
    /** الابتدائي (1-6) والسادس الإعدادي قيد التطوير حالياً */
    val grades = listOf(
        Grade("m1", "الأول المتوسط", Stage.Middle, true),
        Grade("m2", "الثاني المتوسط", Stage.Middle, true),
        Grade("m3", "الثالث المتوسط", Stage.Middle, true),
        Grade("p4", "الرابع الإعدادي", Stage.Prep, true),
        Grade("p5", "الخامس الإعدادي", Stage.Prep, true),
        Grade("p6", "السادس الإعدادي", Stage.Prep, false),
    )
    val enabledGrades get() = grades.filter { it.enabled }
    fun grade(id: String?) = grades.firstOrNull { it.id == id }

    private val arabic = Subject("arabic", "العربي", SubjectKind.General, Icons.Outlined.MenuBook, Tone.Rose)
    private val english = Subject("english", "الإنكليزي", SubjectKind.English, Icons.Outlined.Translate, Tone.Teal)
    private val math = Subject("math", "الرياضيات", SubjectKind.Math, Icons.Outlined.Calculate, Tone.Blue)
    private val physics = Subject("physics", "الفيزياء", SubjectKind.Science, Icons.Outlined.Bolt, Tone.Violet)
    private val chemistry = Subject("chemistry", "الكيمياء", SubjectKind.Science, Icons.Outlined.Science, Tone.Green)
    private val science = Subject("science", "العلوم", SubjectKind.Science, Icons.Outlined.Science, Tone.Green)
    private val biology = Subject("biology", "الأحياء", SubjectKind.Biology, Icons.Outlined.WorkspacePremium, Tone.Orange)
    private val social = Subject("social", "الاجتماعيات", SubjectKind.General, Icons.Outlined.Public, Tone.Amber)
    private val history = Subject("history", "التاريخ", SubjectKind.General, Icons.Outlined.Public, Tone.Amber)
    private val geography = Subject("geography", "الجغرافية", SubjectKind.General, Icons.Outlined.Public, Tone.Teal)
    private val islamic = Subject("islamic", "الإسلامية", SubjectKind.General, Icons.Outlined.School, Tone.Cyan)
    private val computer = Subject("computer", "الحاسوب", SubjectKind.General, Icons.Outlined.Computer, Tone.Violet)

    fun subjectsFor(gradeId: String?, branch: String?): List<Subject> = when (gradeId) {
        "m1", "m2" -> listOf(arabic, english, math, science, social, islamic, computer)
        "m3" -> listOf(arabic, english, math, physics, chemistry, biology, social, islamic, computer)
        "p4", "p5" ->
            if (branch == "lit") listOf(arabic, english, math, history, geography, islamic, computer)
            else listOf(arabic, english, math, physics, chemistry, biology, islamic, computer)
        else -> emptyList()
    }

    fun subject(gradeId: String?, branch: String?, id: String?) = subjectsFor(gradeId, branch).firstOrNull { it.id == id }

    fun sectionsFor(kind: SubjectKind): List<Section> = when (kind) {
        SubjectKind.English -> listOf(
            Section(AiTask.TranslateText, "ترجمة النصوص", "صوّر الصفحة وحدد النص ونترجمه كله للعربي", Icons.Outlined.DocumentScanner, Tone.Teal),
            Section(AiTask.TranslatePronounce, "ترجمة مع النطق", "ترجمة + طريقة اللفظ بحروف عربية + قراءة صوتية", Icons.Outlined.VolumeUp, Tone.Violet),
            Section(AiTask.TranslateTyped, "كتابة السؤال وترجمته", "اكتب السؤال أو الإنشاء ونترجمه ونقرأه لك", Icons.Outlined.Edit, Tone.Orange, typed = true),
        )
        SubjectKind.Math -> listOf(
            Section(AiTask.MathSimple, "حل مسائل بالصورة", "امسح المسألة ونعطيك السؤال بأبسط حل مع الجواب النهائي", Icons.Outlined.PhotoCamera, Tone.Blue),
            Section(AiTask.MathExplain, "حل وشرح المسألة", "نقرا لك المسألة بالصوت ونشرحها بشرح مفصل", Icons.Outlined.Lightbulb, Tone.Violet),
        )
        SubjectKind.Science -> listOf(
            Section(AiTask.SciencePage, "مسح الصفحة", "نمسح الصفحة كاملة ونطلع لك التعاريف والأجوبة المطلوبة فقط", Icons.Outlined.DocumentScanner, Tone.Green),
            Section(AiTask.ScienceSolve, "حل المسائل", "صوّر المسألة ونحلها بالقوانين والخطوات", Icons.Outlined.Science, Tone.Teal),
        )
        SubjectKind.Biology -> listOf(
            Section(AiTask.SciencePage, "مسح الصفحة", "تعاريف وأجوبة الصفحة", Icons.Outlined.DocumentScanner, Tone.Green),
            Section(AiTask.PageAnswers, "حل أسئلة الفصل", "صوّر السؤال والجواب فوراً", Icons.Outlined.Quiz, Tone.Blue),
            Section(AiTask.BioExplain, "شرح الدرس", "شرح مبسط للدرس", Icons.Outlined.MenuBook, Tone.Violet),
            Section(AiTask.BioDiagram, "الرسومات والتسميات", "اقرأ الرسم وسمّي الأجزاء", Icons.Outlined.Image, Tone.Orange),
        )
        SubjectKind.General -> listOf(
            Section(AiTask.PageAnswers, "مسح صفحة وجواب الأسئلة", "صوّر الصفحة ونجاوب الأسئلة الي بيها حسب المنهج", Icons.Outlined.DocumentScanner, Tone.Blue),
            Section(AiTask.Ask, "اسأل المساعد", "اكتب سؤالك ونشرح لك", Icons.Outlined.ChatBubbleOutline, Tone.Violet, typed = true),
        )
    }
}
