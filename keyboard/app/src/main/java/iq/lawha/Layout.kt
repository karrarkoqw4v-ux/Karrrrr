package iq.lawha

enum class Page { LETTERS, DIACRITICS, NUMBERS }

sealed interface KeyDef {
    data class Ch(val char: String) : KeyDef
    object Shift : KeyDef
    object Delete : KeyDef
    object Space : KeyDef
    object Enter : KeyDef
    object Mode123 : KeyDef
    object ModeAbc : KeyDef
    object ModeSymDiac : KeyDef
    object Globe : KeyDef
    object Emoji : KeyDef
    object Mic : KeyDef
}

object Layout {
    // ترتيب الحروف العربي الحقيقي (مطابق لكيبورد آيفون الفعلي)
    val lettersRows: List<List<KeyDef>> = listOf(
        listOf("ض","ص","ق","ف","غ","ع","ه","خ","ح","ج").map { KeyDef.Ch(it) },
        listOf("ش","س","ي","ب","ل","ا","ت","ن","م","ك").map { KeyDef.Ch(it) },
        listOf(KeyDef.Shift) + listOf("ظ","ط","ذ","د","ز","ر","و","ة","ث").map { KeyDef.Ch(it) } + listOf(KeyDef.Delete),
    )

    // صفحة التشكيل والترقيم (تفتح بزر Shift بما إن العربي ما عنده حالة أحرف كبيرة/صغيرة)
    val diacriticsRows: List<List<KeyDef>> = listOf(
        listOf("ً","ٌ","ٍ","َ","ُ","ِ","ّ","ْ","ء","ـ").map { KeyDef.Ch(it) },
        listOf("،","؛",":","؟","!","\"","'","(",")","-").map { KeyDef.Ch(it) },
        listOf(KeyDef.Shift) + listOf(".",",","/","\\","*","+","=","%").map { KeyDef.Ch(it) } + listOf(KeyDef.Delete),
    )

    val numbersRows: List<List<KeyDef>> = listOf(
        listOf("1","2","3","4","5","6","7","8","9","0").map { KeyDef.Ch(it) },
        listOf("-","/",":",";","(",")","$","&","@","\"").map { KeyDef.Ch(it) },
        listOf(KeyDef.ModeSymDiac) + listOf(".",",","?","!","'").map { KeyDef.Ch(it) } + listOf(KeyDef.Delete),
    )

    fun rowsFor(page: Page): List<List<KeyDef>> = when (page) {
        Page.LETTERS -> lettersRows
        Page.DIACRITICS -> diacriticsRows
        Page.NUMBERS -> numbersRows
    }

    fun bottomRow(page: Page): List<KeyDef> = listOf(
        if (page == Page.NUMBERS) KeyDef.ModeAbc else KeyDef.Mode123,
        KeyDef.Space,
        KeyDef.Enter,
    )
}
