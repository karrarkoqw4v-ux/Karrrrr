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

/** مهام التطبيق كلها محلية على الجهاز (بدون ذكاء اصطناعي) */
enum class StudyTask {
    TranslateText, TranslatePronounce, TranslateTyped,
    MathSimple, MathExplain, MathTyped,
    SciencePage, ScienceSolve,
    PageAnswers, ReadPage,
}

/** قسم داخل المادة. typed=true يعني إدخال بالكتابة بدل الكاميرا */
data class Section(val task: StudyTask, val title: String, val sub: String, val icon: ImageVector, val tone: Tone, val typed: Boolean = false)

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

    private val arabic = Subject("arabic", "اللغة العربية", SubjectKind.General, Icons.Outlined.MenuBook, Tone.Rose)
    private val english = Subject("english", "اللغة الإنكليزية", SubjectKind.English, Icons.Outlined.Translate, Tone.Teal)
    private val math = Subject("math", "الرياضيات", SubjectKind.Math, Icons.Outlined.Calculate, Tone.Blue)
    private val physics = Subject("physics", "الفيزياء", SubjectKind.Science, Icons.Outlined.Bolt, Tone.Violet)
    private val chemistry = Subject("chemistry", "الكيمياء", SubjectKind.Science, Icons.Outlined.Science, Tone.Green)
    private val biology = Subject("biology", "الأحياء", SubjectKind.Biology, Icons.Outlined.Eco, Tone.Orange)
    private val social = Subject("social", "الاجتماعيات", SubjectKind.General, Icons.Outlined.Public, Tone.Amber)
    private val history = Subject("history", "التاريخ", SubjectKind.General, Icons.Outlined.Public, Tone.Amber)
    private val geography = Subject("geography", "الجغرافية", SubjectKind.General, Icons.Outlined.Public, Tone.Teal)
    private val islamic = Subject("islamic", "التربية الإسلامية", SubjectKind.General, Icons.Outlined.Mosque, Tone.Cyan)

    /** الأول والثاني والثالث متوسط: نفس المواد. الحاسوب محذوف. */
    fun subjectsFor(gradeId: String?, branch: String?): List<Subject> = when (gradeId) {
        "m1", "m2", "m3" -> listOf(english, arabic, chemistry, physics, biology, social, islamic, math)
        "p4", "p5" ->
            if (branch == "lit") listOf(arabic, english, math, history, geography, islamic)
            else listOf(arabic, english, math, physics, chemistry, biology, islamic)
        else -> emptyList()
    }

    fun subject(gradeId: String?, branch: String?, id: String?) = subjectsFor(gradeId, branch).firstOrNull { it.id == id }

    fun sectionsFor(kind: SubjectKind): List<Section> = when (kind) {
        SubjectKind.English -> listOf(
            Section(StudyTask.TranslateText, "ترجمة النصوص", "صوّر الصفحة وحدد النص ونترجمه كله للعربي", Icons.Outlined.DocumentScanner, Tone.Teal),
            Section(StudyTask.TranslatePronounce, "ترجمة مع النطق", "ترجمة + طريقة اللفظ بحروف عربية + قراءة صوتية", Icons.Outlined.RecordVoiceOver, Tone.Violet),
            Section(StudyTask.TranslateTyped, "كتابة السؤال وترجمته", "اكتب السؤال أو الإنشاء ونترجمه ونقرأه لك", Icons.Outlined.Edit, Tone.Orange, typed = true),
        )
        SubjectKind.Math -> listOf(
            Section(StudyTask.MathSimple, "حل مسائل بالصورة", "امسح المسألة ونعطيك أبسط حل مع الجواب النهائي", Icons.Outlined.PhotoCamera, Tone.Blue),
            Section(StudyTask.MathExplain, "حل وشرح المسألة", "نقرا لك المسألة بالصوت ونشرح خطواتها", Icons.Outlined.Lightbulb, Tone.Violet),
            Section(StudyTask.MathTyped, "اكتب المسألة", "اكتب المعادلة بنفسك واحصل على الحل بالخطوات", Icons.Outlined.Keyboard, Tone.Teal, typed = true),
        )
        SubjectKind.Science -> listOf(
            Section(StudyTask.SciencePage, "مسح الصفحة", "نمسح الصفحة ونطلع لك التعاريف والأسئلة الي بيها", Icons.Outlined.DocumentScanner, Tone.Green),
            Section(StudyTask.ScienceSolve, "حل المسائل", "صوّر المعادلة ونحلها بالخطوات", Icons.Outlined.Functions, Tone.Teal),
            Section(StudyTask.ReadPage, "قراءة الصفحة صوتياً", "نقرا لك الصفحة بصوت واضح", Icons.Outlined.VolumeUp, Tone.Violet),
        )
        SubjectKind.Biology -> listOf(
            Section(StudyTask.SciencePage, "مسح الصفحة", "التعاريف والأسئلة الي بالصفحة", Icons.Outlined.DocumentScanner, Tone.Green),
            Section(StudyTask.PageAnswers, "أسئلة الصفحة", "نستخرج لك كل الأسئلة الي بالصفحة", Icons.Outlined.Quiz, Tone.Blue),
            Section(StudyTask.ReadPage, "قراءة الدرس صوتياً", "نقرا لك الدرس بصوت واضح", Icons.Outlined.VolumeUp, Tone.Violet),
        )
        SubjectKind.General -> listOf(
            Section(StudyTask.ReadPage, "قراءة الصفحة صوتياً", "صوّر الصفحة ونقرأها لك بصوت واضح", Icons.Outlined.VolumeUp, Tone.Violet),
            Section(StudyTask.PageAnswers, "أسئلة الصفحة", "نستخرج لك الأسئلة والتعاريف الي بالصفحة", Icons.Outlined.Quiz, Tone.Blue),
        )
    }
}
