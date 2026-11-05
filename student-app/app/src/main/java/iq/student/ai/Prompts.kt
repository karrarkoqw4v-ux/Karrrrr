package iq.student.ai

import iq.student.data.AiTask
import iq.student.data.Grade
import iq.student.data.Subject

object Prompts {
    const val DEFS = "###DEFINITIONS"
    const val ANSWERS = "###ANSWERS"

    fun system(grade: Grade?, subject: Subject?): String = """
You are a patient tutor inside an Android app for Iraqi students.
Student grade: ${grade?.label ?: "intermediate/preparatory"}. Subject: ${subject?.title ?: "general"}.
Follow the Iraqi Ministry of Education curriculum, terminology and methods for that grade.
Write in simple, clear Modern Standard Arabic (easy wording an Iraqi student understands). Keep math expressions and English words in Latin letters.
Be accurate. Read images carefully. If part of the image is blurry or unreadable, say exactly which part could not be read instead of guessing. Never invent content that is not in the image.
Output plain text only: no markdown symbols such as **, #, or backticks.
""".trimIndent()

    fun user(task: AiTask): String = when (task) {
        AiTask.TranslateText -> """
Read ALL the English text in the image (OCR) exactly, then translate it completely into Arabic.
Format:
النص الأصلي:
<the English text as written>
الترجمة:
<the full Arabic translation>
""".trimIndent()

        AiTask.TranslatePronounce -> """
Read the English text in the image (or the English text given below). Return ONLY a JSON object, no markdown, no extra words:
{"english": "<the English text exactly>", "arabic": "<full Arabic translation>", "pronunciation": "<how the whole English text is pronounced, written in Arabic letters with tashkeel where helpful>", "words": [{"en": "<word>", "say": "<its pronunciation in Arabic letters>", "ar": "<Arabic meaning>"}]}
List up to 40 words in reading order.
""".trimIndent()

        AiTask.TranslateTyped -> "Translate the text exactly. Output only the translation."

        AiTask.MathSimple -> """
Read the math problem in the image exactly. Then answer in this format:
المسألة:
<the problem exactly as written>
الحل:
<numbered short steps, the simplest possible method for this grade, one equation per line>
الجواب النهائي:
<the final answer with units if any>
""".trimIndent()

        AiTask.MathExplain -> """
Read the math problem in the image exactly. Then explain it in detail like a teacher, in this format:
المسألة:
<the problem exactly as written, read aloud friendly>
شنو المطلوب؟
<what is asked>
القوانين والأفكار المستخدمة:
<rules or ideas needed and why>
الحل خطوة خطوة:
<each step with a clear explanation of why we do it>
الجواب النهائي:
<final answer>
نصيحة:
<one tip to avoid common mistakes>
""".trimIndent()

        AiTask.SciencePage -> """
The image is a textbook page. Extract ONLY what the student must study from it, using the page's own wording:
$DEFS
<each definition on its own line as "term: definition">
$ANSWERS
<for every question printed on the page, write the question number or short question and its correct answer on its own line>
If the page has no definitions or no questions, write "لا يوجد" under that heading. Do not add anything else.
""".trimIndent()

        AiTask.ScienceSolve -> """
Read the problem in the image exactly. Solve it in this format:
المسألة:
<problem>
المعطيات والمطلوب:
<givens and what is asked, with units>
القانون:
<law or formula used>
الحل:
<substitution and calculation, one step per line, units shown>
الجواب النهائي:
<answer with units>
""".trimIndent()

        AiTask.BioExplain -> """
The image is a textbook page. Explain the lesson simply for the student: first a short summary, then the key ideas in numbered points, then how to memorize them. Use only content from the page.
""".trimIndent()

        AiTask.BioDiagram -> """
The image contains a scientific diagram. List every labeled or numbered part with its correct name and its function, then explain what the diagram shows in 2-3 sentences.
""".trimIndent()

        AiTask.PageAnswers -> """
The image is a textbook or worksheet page. Find every question on the page and answer each one correctly according to the Iraqi curriculum. Format each as:
السؤال: <question>
الجواب: <answer>
""".trimIndent()

        AiTask.Ask -> "Answer the student's question clearly and briefly, then give an example if useful."
    }
}
