"""شاشات الدفعة الثانية: الصفوف، المواد، الإنكليزي، الرياضيات، العلوم، الوزاريات، المكتبة، الإعدادات."""
from lib import Canvas
from tokens import *
from screens import (header, primary_button, outline_button, field, chip, bottom_nav, progress, R, L)

WARN_TXT = "#92400E"


def dim(c, alpha=0.5):
    from PIL import Image, ImageDraw
    c.img = Image.blend(c.img, Image.new("RGB", c.img.size, "#101828"), alpha)
    c.d = ImageDraw.Draw(c.img)
    return c


def lock_pill(c, x0, y, label="قيد التطوير", w=86):
    c.rr(x0, y, x0 + w, y + 22, 11, fill=WARN_SOFT)
    c.icon("lock", x0 + 14, y + 11, 13, WARN, sw=2, bg=WARN_SOFT)
    c.text(x0 + w - 10, y + 11, label, 10.5, "b", WARN_TXT, "r")


# ------------------------------------------------------------------ 15. إنشاء حساب
def signup():
    c = Canvas(SURFACE)
    header(c, "إنشاء حساب")
    c.text(R, 106, "أهلاً بيك بينا", 26, "x", INK, "r")
    c.text(R, 138, "سوّي حسابك وابدأ دراستك بدقيقة", 14, "r", MUTED, "r")
    y = field(c, 164, "الاسم الكامل", "اسمك الثلاثي", "user", value="اسم الطالب")
    y = field(c, y + 10, "البريد الإلكتروني", "name@email.com", "mail")
    y = field(c, y + 10, "كلمة المرور", "6 أحرف على الأقل", "lock", left_icon="eye")
    y = field(c, y + 10, "تأكيد كلمة المرور", "أعد كتابتها", "lock")
    c.rr(R - 22, y + 20, R, y + 42, 7, fill=PRIMARY)
    c.icon("check", R - 11, y + 31, 14, "#FFFFFF", sw=2.6, bg=PRIMARY)
    c.runs(R - 32, y + 31, [("أوافق على ", MUTED, "r"), ("شروط الاستخدام", PRIMARY, "b"), (" وسياسة الخصوصية", MUTED, "r")], 12.5)
    primary_button(c, y + 62, "إنشاء الحساب", "arrow-l", end=True)
    c.runs(R - (R - L) / 2 + 76, H - 40, [("عندك حساب؟ ", MUTED, "r"), ("تسجيل الدخول", PRIMARY, "b")], 14)
    return c


# ------------------------------------------------------------------ 16. اختيار الصف
def grades():
    c = Canvas(SURFACE)
    header(c, "اختر صفك")
    c.text(R, 104, "المنهج يتخصص حسب صفك", 13, "r", MUTED, "r")
    # ابتدائي مقفل
    c.rr(L, 124, R, 192, 18, fill="#F2F4F7", line=LINE)
    c.icon_tile("pencil", R - 40, 158, 46, "orange", r=14)
    c.text(R - 76, 146, "المرحلة الابتدائية", 15, "b", FAINT, "r")
    c.text(R - 76, 170, "من الأول إلى السادس", 12, "r", FAINT, "r")
    lock_pill(c, L + 14, 147)
    # متوسط
    c.text(R, 218, "المرحلة المتوسطة", 15, "b", INK, "r")
    gw = (R - L - 16) / 3
    for i, (t, sel) in enumerate([("الأول", False), ("الثاني", False), ("الثالث", True)]):
        x1 = R - i * (gw + 8)
        c.rr(x1 - gw, 236, x1, 304, 16, fill=PRIMARY_SOFT if sel else SURFACE, line=PRIMARY if sel else LINE, lw=2 if sel else 1.2)
        c.text(x1 - gw / 2, 262, t, 15, "b", PRIMARY if sel else INK, "c")
        c.text(x1 - gw / 2, 284, "متوسط", 12, "m", PRIMARY if sel else MUTED, "c")
    # إعدادي
    c.text(R, 336, "المرحلة الإعدادية", 15, "b", INK, "r")
    for i, (t, st) in enumerate([("الرابع", "n"), ("الخامس", "n"), ("السادس", "lock")]):
        x1 = R - i * (gw + 8)
        if st == "lock":
            c.rr(x1 - gw, 354, x1, 422, 16, fill="#F2F4F7", line=LINE)
            c.text(x1 - gw / 2, 376, t, 15, "b", FAINT, "c")
            c.icon("lock", x1 - gw / 2, 400, 16, FAINT, bg="#F2F4F7")
        else:
            c.rr(x1 - gw, 354, x1, 422, 16, fill=SURFACE, line=LINE, lw=1.2)
            c.text(x1 - gw / 2, 380, t, 15, "b", INK, "c")
            c.text(x1 - gw / 2, 402, "إعدادي", 12, "m", MUTED, "c")
    c.rr(L, 446, R, 506, 14, fill=WARN_SOFT)
    c.icon("info", R - 26, 476, 20, WARN, bg=WARN_SOFT)
    c.text(R - 50, 466, "الابتدائي والسادس الإعدادي", 13, "b", WARN_TXT, "r")
    c.text(R - 50, 488, "قيد التطوير وراح ينفتحون قريباً", 12, "r", WARN_TXT, "r")
    c.text(R, 540, "الفرع", 12, "m", MUTED, "r")
    x = R
    for i, t in enumerate(["علمي", "أدبي"]):
        x = chip(c, x, 554, t, i == 0, h=38, size=14)
    primary_button(c, H - 92, "متابعة", "arrow-l", end=True)
    return c


# ------------------------------------------------------------------ 17. الرئيسية (المواد)
def home_subjects():
    c = Canvas(BG)
    c.text(R, 56, "هلا بيك", 20, "b", INK, "r")
    c.text(R, 80, "اختر المادة الي تريد تدرسها", 13, "r", MUTED, "r")
    c.rr(L, 40, L + 118, 74, 17, fill=PRIMARY_SOFT)
    c.text(L + 59, 57, "الثالث المتوسط", 12.5, "b", PRIMARY, "c")
    items = [("lang", "اللغة الإنكليزية", "3 أقسام", "teal"), ("book", "اللغة العربية", "قسمين", "rose"),
             ("flask", "الكيمياء", "3 أقسام", "green"), ("bolt", "الفيزياء", "3 أقسام", "violet"),
             ("leaf", "الأحياء", "3 أقسام", "orange"), ("globe", "الاجتماعيات", "قسمين", "amber"),
             ("mosque", "الإسلامية", "قسمين", "cyan"), ("sigma", "الرياضيات", "3 أقسام", "blue"),
             ("quiz", "الوزاريات", "2011 - 2026", "rose"), ("book", "مكتبة الكتب", "كتب المنهج PDF", "blue")]
    cw = (R - L - 12) / 2
    for i, (ic, t, sub, tone) in enumerate(items):
        row, col = divmod(i, 2)
        x1 = R - col * (cw + 12)
        y0 = 100 + row * 98
        c.card(x1 - cw, y0, x1, y0 + 86, 18, shadow=False)
        c.icon_tile(ic, x1 - 34, y0 + 43, 46, tone, r=14)
        c.text(x1 - 68, y0 + 34, t, 14.5, "b", INK, "r")
        c.text(x1 - 68, y0 + 58, sub, 11.5, "r", MUTED, "r")
    bottom_nav(c, 0)
    return c


# ------------------------------------------------------------------ 18. أقسام الإنكليزي
def english():
    c = Canvas(BG)
    header(c, "اللغة الإنكليزية")
    c.text(R, 106, "شنو تريد تسوي؟", 18, "b", INK, "r")
    secs = [("scan", "ترجمة النصوص", "صوّر الصفحة وحدد النص ونترجمه كله للعربي", "teal"),
            ("speaker", "ترجمة مع النطق", "ترجمة + طريقة اللفظ بحروف عربية + قراءة صوتية", "violet"),
            ("pencil", "كتابة السؤال وترجمته", "اكتب السؤال أو الإنشاء ونترجمه ونقرأه لك", "orange")]
    y = 128
    for ic, t, sub, tone in secs:
        c.card(L, y, R, y + 136, 20)
        c.icon_tile(ic, R - 46, y + 44, 56, tone, r=18)
        c.text(R - 90, y + 34, t, 16, "b", INK, "r")
        c.para(R - 90, y + 58, sub, 12, "r", MUTED, maxw=R - 90 - L - 16)
        c.rr(L + 16, y + 92, R - 16, y + 124, 12, fill=TOOL[tone][1])
        c.text(180, y + 108, "ابدأ", 13.5, "b", TOOL[tone][0], "c")
        c.icon("arrow-l", L + 40, y + 108, 16, TOOL[tone][0], bg=TOOL[tone][1])
        y += 152
    return c


# ------------------------------------------------------------------ 19. الماسح
def scanner():
    c = Canvas("#E9ECF3")
    c.rr(30, 150, 330, 470, 12, fill="#FFFFFF")
    lines = ["Photosynthesis is the process by", "which green plants make food using", "sunlight, water and carbon dioxide.", "", "Chlorophyll absorbs light energy."]
    for i, t in enumerate(lines):
        c.text(46, 196 + i * 30, t, 15, "m", "#2B364F", "l", rtl=False)
    c.circle(R - 20, 56, 20, fill=SURFACE); c.icon("close", R - 20, 56, 20, INK)
    c.circle(L + 20, 56, 20, fill=SURFACE); c.icon("bolt", L + 20, 56, 20, INK)
    c.rr(112, 38, 248, 74, 18, fill=SURFACE)
    c.text(180, 56, "ترجمة النصوص", 14, "b", INK, "c")
    c.rr(64, 100, 296, 134, 17, fill=SURFACE)
    c.text(180, 117, "صوّر الصفحة كلها أو الجزء المطلوب", 12.5, "m", MUTED, "c")
    ax, ay, bx, by, ln = 40, 170, 320, 450, 28
    for (x, y, sx, sy) in [(ax, ay, 1, 1), (bx, ay, -1, 1), (ax, by, 1, -1), (bx, by, -1, -1)]:
        c.d.line([x * S, (y + sy * ln) * S, x * S, y * S, (x + sx * ln) * S, y * S], fill=PRIMARY, width=int(3.5 * S), joint="curve")
    c.shadow(0, 540, W, H + 20, r=0, blur=10, dy=-3, alpha=26)
    c.rr(0, 540, W, H + 20, 28, fill=SURFACE, corners=(True, True, False, False))
    c.circle(180, 650, 40, line=PRIMARY, lw=3); c.circle(180, 650, 31, fill=PRIMARY)
    c.icon("camera", 180, 650, 26, "#FFFFFF", bg=PRIMARY)
    c.circle(R - 22, 650, 26, fill=BG); c.icon("image", R - 22, 650, 24, INK, bg=BG)
    c.circle(L + 22, 650, 26, fill=BG); c.icon("pencil", L + 22, 650, 24, INK, bg=BG)
    c.text(R - 22, 700, "المعرض", 11, "m", MUTED, "c"); c.text(L + 22, 700, "كتابة", 11, "m", MUTED, "c")
    return c


# ------------------------------------------------------------------ 20. تحديد النص (قص)
def crop():
    c = Canvas(BG)
    header(c, "حدد النص")
    c.rr(L, 96, R, 520, 16, fill="#FFFFFF", line=LINE)
    for i, t in enumerate(["Photosynthesis is the process by", "which green plants make food using", "sunlight, water and carbon dioxide.", "", "Chlorophyll absorbs light energy."]):
        c.text(36, 150 + i * 34, t, 15, "m", "#2B364F", "l", rtl=False)
    # منطقة التحديد
    c.rr(30, 126, 330, 306, 4, fill=None, line=PRIMARY, lw=2)
    for (x, y) in [(30, 126), (330, 126), (30, 306), (330, 306), (180, 126), (180, 306), (30, 216), (330, 216)]:
        c.circle(x, y, 7, fill=SURFACE, line=PRIMARY, lw=2.2)
    c.rr(L, 540, R, 592, 16, fill=PRIMARY_SOFT)
    c.icon("info", R - 26, 566, 20, PRIMARY, bg=PRIMARY_SOFT)
    c.text(R - 50, 566, "اسحب النقاط حتى يغطي الإطار النص المطلوب", 12.5, "m", PRIMARY, "r")
    outline_button(c, H - 92, "إعادة التصوير", "camera", x0=W / 2 + 6, x1=R, h=54)
    primary_button(c, H - 92, "التالي", "arrow-l", x0=L, x1=W / 2 - 6, h=54, end=True)
    return c


# ------------------------------------------------------------------ 21. نتيجة ترجمة النص
def translate_result():
    c = Canvas(BG)
    header(c, "الترجمة", "bookmark")
    c.card(L, 90, R, 232)
    c.text(L + 16, 112, "English", 12, "m", FAINT, "l", rtl=False)
    c.text(L + 16, 150, "Photosynthesis is the process by which", 14.5, "m", INK, "l", rtl=False)
    c.text(L + 16, 176, "green plants make food using sunlight,", 14.5, "m", INK, "l", rtl=False)
    c.text(L + 16, 202, "water and carbon dioxide.", 14.5, "m", INK, "l", rtl=False)
    c.rr(L, 246, R, 440, 20, fill=PRIMARY_SOFT)
    c.text(R - 16, 270, "الترجمة العربية", 12, "b", PRIMARY, "r")
    for i, t in enumerate(["البناء الضوئي هو العملية التي", "تصنع بها النباتات الخضراء غذاءها", "باستخدام ضوء الشمس والماء", "وثاني أكسيد الكربون."]):
        c.text(R - 16, 308 + i * 28, t, 16, "b", INK, "r")
    for i, ic in enumerate(["speaker", "copy", "bookmark"]):
        c.circle(L + 28 + i * 46, 418, 0.1)
        c.circle(L + 30 + i * 46, 410, 18, fill=SURFACE)
        c.icon(ic, L + 30 + i * 46, 410, 18, PRIMARY)
    c.text(R, 468, "اسأل عن الترجمة", 15, "b", INK, "r")
    x = R
    for t in ["اشرح لي الفقرة", "كلمات صعبة", "لخّصها"]:
        x = chip(c, x, 486, t, False, h=36)
    primary_button(c, H - 88, "ترجمة نص ثاني", "camera")
    return c


# ------------------------------------------------------------------ 22. الترجمة مع النطق
def pronounce():
    c = Canvas(BG)
    header(c, "ترجمة مع النطق", "bookmark")
    c.card(L, 90, R, 250)
    c.text(180, 116, "English", 12, "m", FAINT, "c", rtl=False)
    c.text(180, 154, "The cell is the basic", 20, "b", INK, "c", rtl=False)
    c.text(180, 184, "unit of life", 20, "b", INK, "c", rtl=False)
    c.hline(L + 16, R - 16, 208)
    c.text(R - 16, 230, "الخلية هي الوحدة الأساسية للحياة", 14, "b", PRIMARY, "r")
    c.text(R, 276, "اللفظ بحروف عربية", 15, "b", INK, "r")
    words = [("The", "ذا"), ("cell", "سِل"), ("is", "إز"), ("the", "ذا"), ("basic", "بيسِك"), ("unit", "يونِت"), ("of", "أوف"), ("life", "لايف")]
    cw = (R - L - 18) / 4
    for i, (en, ar) in enumerate(words):
        row, col = divmod(i, 4)
        x1 = R - col * (cw + 6)
        y0 = 296 + row * 88
        c.card(x1 - cw, y0, x1, y0 + 78, 14, shadow=False)
        c.text(x1 - cw / 2, y0 + 22, en, 13, "m", MUTED, "c", rtl=False)
        c.text(x1 - cw / 2, y0 + 50, ar, 17, "b", PRIMARY, "c")
    c.rr(L, 480, R, 548, 18, fill=SURFACE, line=LINE)
    c.text(R - 16, 502, "اللفظ كاملاً", 12, "m", MUTED, "r")
    c.text(R - 16, 530, "ذا سِل إز ذا بيسِك يونِت أوف لايف", 15.5, "b", INK, "r")
    # أزرار الاستماع
    primary_button(c, 568, "اسمع بالإنكليزي", "speaker", x0=W / 2 + 6, x1=R, h=54)
    outline_button(c, 568, "اقرأ وياي", "mic", x0=L, x1=W / 2 - 6, h=54)
    c.circle(0, 0, 0.1)
    c.rr(L, 640, R, 684, 14, fill=SUCCESS_SOFT)
    c.text(180, 662, "اضغط على أي كلمة حتى تسمعها لحالها", 12.5, "m", "#166534", "c")
    return c


# ------------------------------------------------------------------ 23. كتابة السؤال وترجمته
def typed():
    c = Canvas(BG)
    header(c, "كتابة السؤال وترجمته")
    c.rr(L, 90, R, 130, 14, fill="#EAECF5")
    c.rr(L + 4, 94, 176, 126, 11, fill=SURFACE)
    c.text(100, 110, "عربي إلى إنكليزي", 12.5, "m", MUTED, "c")
    c.rr(184, 94, R - 4, 126, 11, fill=SURFACE)
    c.text(266, 110, "إنكليزي إلى عربي", 12.5, "b", PRIMARY, "c")
    c.card(L, 148, R, 300)
    c.text(L + 16, 182, "What is photosynthesis and why is it", 14.5, "m", INK, "l", rtl=False)
    c.text(L + 16, 208, "important for living organisms?", 14.5, "m", INK, "l", rtl=False)
    c.hline(L + 16, R - 16, 262)
    c.text(L + 16, 282, "74 / 1000", 11, "m", FAINT, "l", rtl=False)
    c.icon("close", R - 24, 282, 16, FAINT)
    c.icon("mic", R - 54, 282, 18, FAINT)
    primary_button(c, 316, "ترجمة", "lang")
    c.rr(L, 392, R, 560, 20, fill=PRIMARY_SOFT)
    c.text(R - 16, 416, "الترجمة", 12, "b", PRIMARY, "r")
    c.text(R - 16, 454, "ما هو البناء الضوئي ولماذا هو", 16, "b", INK, "r")
    c.text(R - 16, 484, "مهم للكائنات الحية؟", 16, "b", INK, "r")
    for i, ic in enumerate(["speaker", "copy", "bookmark"]):
        c.circle(L + 30 + i * 46, 530, 18, fill=SURFACE)
        c.icon(ic, L + 30 + i * 46, 530, 18, PRIMARY)
    c.rr(R - 150, 512, R - 16, 548, 18, fill=SURFACE)
    c.icon("speaker", R - 40, 530, 18, PRIMARY)
    c.text(R - 56, 530, "اقرأ بالإنكليزي", 12.5, "b", PRIMARY, "r")
    return c


# ------------------------------------------------------------------ 24. أقسام الرياضيات
def math_sections():
    c = Canvas(BG)
    header(c, "الرياضيات")
    secs = [("camera", "حل مسائل بالصورة", "امسح المسألة ونعطيك السؤال بأبسط حل مع الجواب النهائي", "blue"),
            ("bulb", "حل وشرح المسألة", "نقرا لك المسألة بالصوت ونشرحها بشرح مفصل", "violet")]
    y = 100
    for ic, t, sub, tone in secs:
        c.card(L, y, R, y + 178, 22)
        c.icon_tile(ic, R - 50, y + 54, 62, tone, r=20)
        c.text(R - 96, y + 44, t, 17, "b", INK, "r")
        c.para(R - 96, y + 70, sub, 12, "r", MUTED, maxw=R - 96 - L - 16)
        c.rr(L + 16, y + 122, R - 16, y + 160, 14, fill=TOOL[tone][0])
        c.text(180, y + 141, "امسح المسألة", 14, "b", "#FFFFFF", "c")
        c.icon("scan", R - 90, y + 141, 18, "#FFFFFF", bg=TOOL[tone][0])
        y += 196
    c.text(R, y + 14, "آخر مسائلك", 15, "b", INK, "r")
    c.card(L, y + 36, R, y + 92, 14, shadow=False)
    c.text(R - 16, y + 64, "2x + 6 = 14", 15, "b", INK, "r", rtl=False)
    c.text(L + 16, y + 64, "x = 4", 14, "b", SUCCESS, "l", rtl=False)
    bottom_nav(c, 1)
    return c


# ------------------------------------------------------------------ 25. حل المسألة بالصورة
def math_solve():
    c = Canvas(BG)
    header(c, "حل مسألة بالصورة", "bookmark")
    c.card(L, 90, R, 196)
    c.text(R - 16, 112, "المسألة بعد القراءة", 12, "m", MUTED, "r")
    c.rr(L + 16, 100, L + 96, 124, 12, fill=SUCCESS_SOFT)
    c.icon("check", L + 30, 112, 12, SUCCESS, sw=2.4, bg=SUCCESS_SOFT)
    c.text(L + 64, 112, "قُرئت", 11.5, "b", "#166534", "c")
    c.text(180, 160, "3x - 7 = 2x + 5", 28, "b", INK, "c", rtl=False)
    c.text(R, 224, "الحل بأبسط طريقة", 16, "b", INK, "r")
    steps = [("ننقل 2x للطرف الأيسر", "3x - 2x = 5 + 7"), ("نبسّط", "x = 12")]
    y = 244
    for i, (t, eq) in enumerate(steps):
        c.card(L, y, R, y + 76, 16, shadow=False)
        c.circle(R - 28, y + 38, 14, fill=PRIMARY_SOFT)
        c.text(R - 28, y + 38, str(i + 1), 14, "b", PRIMARY, "c")
        c.text(R - 56, y + 26, t, 14, "m", INK, "r")
        c.text(R - 56, y + 54, eq, 17, "b", PRIMARY, "r", rtl=False)
        y += 88
    c.rr(L, y + 4, R, y + 78, 18, fill=SUCCESS_SOFT)
    c.circle(R - 32, y + 41, 17, fill=SUCCESS)
    c.icon("check", R - 32, y + 41, 20, "#FFFFFF", sw=2.5, bg=SUCCESS)
    c.text(R - 62, y + 28, "الجواب النهائي", 12, "m", "#166534", "r")
    c.text(R - 62, y + 54, "x = 12", 22, "x", "#166534", "r", rtl=False)
    outline_button(c, H - 84, "شرح مفصل", "bulb", x0=W / 2 + 6, x1=R, h=52)
    primary_button(c, H - 84, "مسألة ثانية", "camera", x0=L, x1=W / 2 - 6, h=52)
    return c


# ------------------------------------------------------------------ 26. حل وشرح المسألة
def math_explain():
    c = Canvas(BG)
    header(c, "حل وشرح المسألة", "bookmark")
    c.rr(L, 88, R, 156, 18, fill=PRIMARY)
    c.circle(R - 34, 122, 22, fill="#FFFFFF")
    c.icon("pause", R - 34, 122, 22, PRIMARY, sw=2.4)
    c.text(R - 70, 108, "نقرا لك المسألة والشرح", 14, "b", "#FFFFFF", "r")
    progress(c, L + 18, R - 70, 134, 0.35, "#FFFFFF", 5, "#7E9BF2")
    c.text(L + 18, 108, "0:42", 12, "b", "#DCE5FF", "l", rtl=False)
    c.card(L, 172, R, 246)
    c.text(R - 16, 194, "المسألة", 12, "m", MUTED, "r")
    c.text(180, 224, "x² - 5x + 6 = 0", 22, "b", INK, "c", rtl=False)
    secs = [("1", "شنو المطلوب؟", "نجد قيم x التي تجعل المعادلة صحيحة."),
            ("2", "نحلل المقدار", "نبحث عن عددين حاصل ضربهما 6 ومجموعهما سالب 5، وهما سالب 2 وسالب 3"),
            ("3", "نكتب الحل", "نكتب المقدار بشكل عوامل ثم نساوي كل عامل بالصفر")]
    y = 262
    for n, t, body in secs:
        c.card(L, y, R, y + 112, 16, shadow=False)
        c.circle(R - 26, y + 26, 13, fill=PRIMARY_SOFT)
        c.text(R - 26, y + 26, n, 13, "b", PRIMARY, "c")
        c.text(R - 50, y + 26, t, 14.5, "b", INK, "r")
        words = body.split(" ")
        line, ly = "", y + 54
        for w in words:
            if c.width(line + " " + w, 12.5, "r") > R - L - 40:
                c.text(R - 16, ly, line, 12.5, "r", MUTED, "r"); ly += 22; line = w
            else:
                line = (line + " " + w).strip()
        c.text(R - 16, ly, line, 12.5, "r", MUTED, "r")
        if n == "3":
            c.rr(L + 16, y + 74, R - 16, y + 102, 10, fill=PRIMARY_SOFT)
            c.text(180, y + 88, "(x-2)(x-3)=0  →  x=2 , x=3", 14, "b", PRIMARY, "c", rtl=False)
        y += 124
    c.text(180, y + 12, "اضغط على أي فقرة حتى تنقرأ لحالها", 12, "m", FAINT, "c")
    return c


# ------------------------------------------------------------------ 27. أقسام الفيزياء/الكيمياء
def science_sections():
    c = Canvas(BG)
    header(c, "الكيمياء")
    secs = [("scan", "مسح الصفحة", "نمسح الصفحة كاملة ونطلع لك التعاريف والأجوبة المطلوبة فقط", "green"),
            ("flask", "حل المسائل", "صوّر المسألة ونحلها بالقوانين والخطوات", "teal")]
    y = 100
    for ic, t, sub, tone in secs:
        c.card(L, y, R, y + 168, 22)
        c.icon_tile(ic, R - 50, y + 52, 62, tone, r=20)
        c.text(R - 96, y + 42, t, 17, "b", INK, "r")
        c.para(R - 96, y + 66, sub, 12, "r", MUTED, maxw=R - 96 - L - 16)
        c.rr(L + 16, y + 114, R - 16, y + 152, 14, fill=TOOL[tone][0])
        c.text(180, y + 133, "ابدأ المسح", 14, "b", "#FFFFFF", "c")
        c.icon("scan", R - 90, y + 133, 18, "#FFFFFF", bg=TOOL[tone][0])
        y += 186
    c.text(R, y + 14, "الفصول", 15, "b", INK, "r")
    x = R
    for i, t in enumerate(["الفصل 1", "الفصل 2", "الفصل 3", "الفصل 4"]):
        x = chip(c, x, y + 32, t, i == 0, h=34, size=12.5)
    bottom_nav(c, 1)
    return c


# ------------------------------------------------------------------ 28. نتيجة مسح الصفحة
def science_result():
    c = Canvas(BG)
    header(c, "نتيجة مسح الصفحة", "bookmark")
    c.rr(L, 88, R, 130, 14, fill="#EAECF5")
    c.rr(L + 4, 92, 176, 126, 11, fill=SURFACE)
    c.text(100, 109, "الأجوبة", 13, "m", MUTED, "c")
    c.rr(184, 92, R - 4, 126, 11, fill=SURFACE)
    c.text(266, 109, "التعاريف", 13, "b", PRIMARY, "c")
    c.rr(L + 4, 92, 176, 126, 11, fill="#EAECF5")
    c.text(100, 109, "الأجوبة", 13, "m", MUTED, "c")
    defs = [("المول", "كمية المادة التي تحتوي على عدد أفوغادرو من الجسيمات"),
            ("العدد الذري", "عدد البروتونات في نواة الذرة"),
            ("الرابطة التساهمية", "رابطة تنشأ بمشاركة زوج أو أكثر من الإلكترونات بين ذرتين")]
    y = 146
    for t, body in defs:
        c.card(L, y, R, y + 120, 16, shadow=False)
        c.rr(R - 16 - c.width(t, 13, "b") - 24, y + 14, R - 16, y + 40, 13, fill=TOOL["green"][1])
        c.text(R - 28, y + 27, t, 13, "b", "#166534", "r")
        line, ly = "", y + 64
        for w in body.split(" "):
            if c.width(line + " " + w, 13, "r") > R - L - 36:
                c.text(R - 16, ly, line, 13, "r", INK, "r"); ly += 24; line = w
            else:
                line = (line + " " + w).strip()
        c.text(R - 16, ly, line, 13, "r", INK, "r")
        y += 132
    c.hline(L, R, y + 2, "#FFFFFF")
    outline_button(c, H - 84, "نسخ الكل", "copy", x0=W / 2 + 6, x1=R, h=52)
    primary_button(c, H - 84, "اقرأ بصوت", "speaker", x0=L, x1=W / 2 - 6, h=52)
    return c


# ------------------------------------------------------------------ 29. أقسام الأحياء
def biology_sections():
    c = Canvas(BG)
    header(c, "الأحياء")
    secs = [("scan", "مسح الصفحة", "تعاريف وأجوبة الصفحة", "green"), ("quiz", "حل أسئلة الفصل", "صوّر السؤال والجواب فوراً", "blue"),
            ("book", "شرح الدرس", "شرح مبسط للدرس", "violet"), ("image", "الرسومات والتسميات", "اقرأ الرسم وسمّي الأجزاء", "orange")]
    cw = (R - L - 12) / 2
    for i, (ic, t, sub, tone) in enumerate(secs):
        row, col = divmod(i, 2)
        x1 = R - col * (cw + 12)
        y0 = 100 + row * 196
        c.card(x1 - cw, y0, x1, y0 + 184, 20)
        c.icon_tile(ic, x1 - cw / 2, y0 + 50, 62, tone, r=20)
        c.text(x1 - cw / 2, y0 + 106, t, 14.5, "b", INK, "c")
        c.text(x1 - cw / 2, y0 + 130, sub, 11.5, "r", MUTED, "c")
        c.rr(x1 - cw + 14, y0 + 146, x1 - 14, y0 + 172, 13, fill=TOOL[tone][1])
        c.text(x1 - cw / 2, y0 + 159, "فتح", 12.5, "b", TOOL[tone][0], "c")
    bottom_nav(c, 1)
    return c


# ------------------------------------------------------------------ 30. الوزاريات (قائمة الملازم)
def ministry():
    c = Canvas(BG)
    header(c, "الوزاريات", "search")
    c.rr(L, 86, R, 124, 14, fill=PRIMARY_SOFT)
    c.icon("award", R - 24, 105, 18, PRIMARY, bg=PRIMARY_SOFT)
    c.text(R - 46, 105, "ملازم أفضل مدرسين العراق - الثالث المتوسط", 12.5, "b", PRIMARY, "r")
    x = R
    for i, t in enumerate(["كل المواد", "الرياضيات", "الإنكليزي", "الفيزياء"]):
        x = chip(c, x, 138, t, i == 0, h=34, size=12.5)
    items = [("sigma", "blue", "ملزمة الرياضيات", 5), ("lang", "teal", "ملزمة الإنكليزي", 4), ("flask", "green", "ملزمة الكيمياء", 5)]
    y = 188
    for ic, tone, t, st in items:
        c.card(L, y, R, y + 148, 18, shadow=False)
        c.rr(R - 96, y + 12, R - 12, y + 136, 12, fill=TOOL[tone][1])
        c.icon(ic, R - 54, y + 66, 34, TOOL[tone][0], bg=TOOL[tone][1])
        c.text(R - 54, y + 108, "غلاف الملزمة", 10.5, "m", TOOL[tone][0], "c")
        c.text(R - 108, y + 30, t, 14.5, "b", INK, "r")
        c.text(R - 108, y + 54, "اسم المدرس", 12.5, "m", MUTED, "r")
        c.stars(R - 108, y + 80, st)
        c.text(R - 108 - 5 * 15 - 8, y + 80, f"{st}.0", 12, "b", INK, "r", rtl=False)
        c.rr(L + 14, y + 100, R - 108, y + 134, 12, fill=PRIMARY)
        c.icon("eye", R - 108 - 40, y + 117, 17, "#FFFFFF", bg=PRIMARY)
        c.text(R - 108 - 58, y + 117, "مشاهدة", 13, "b", "#FFFFFF", "r")
        y += 160
    bottom_nav(c, 1)
    return c


# ------------------------------------------------------------------ 31. تفاصيل الملزمة
def handout_detail():
    c = Canvas(BG)
    header(c, "تفاصيل الملزمة", "bookmark")
    c.rr(L, 88, R, 300, 22, fill=TOOL["blue"][1])
    c.rr(120, 104, 240, 284, 12, fill=SURFACE, line=LINE)
    c.icon("sigma", 180, 170, 44, PRIMARY, bg=SURFACE)
    c.text(180, 226, "ملزمة الرياضيات", 13, "b", INK, "c")
    c.text(180, 250, "الثالث المتوسط", 11.5, "m", MUTED, "c")
    c.text(R, 330, "ملزمة الرياضيات الشاملة", 19, "x", INK, "r")
    c.stars(R, 358, 5, 16)
    c.text(R - 100, 358, "5.0 (تقييم نموذجي)", 12, "m", MUTED, "r")
    rows = [("user", "المدرس", "اسم المدرس"), ("book", "المادة", "الرياضيات"), ("doc", "عدد الصفحات", "120 صفحة"), ("calendar", "السنة", "2025 - 2026")]
    y = 384
    for ic, k, v in rows:
        c.card(L, y, R, y + 46, 14, shadow=False)
        c.icon(ic, R - 26, y + 23, 18, PRIMARY)
        c.text(R - 50, y + 23, k, 13, "m", MUTED, "r")
        c.text(L + 16, y + 23, v, 13.5, "b", INK, "l")
        y += 54
    c.text(R, y + 14, "الوصف", 14, "b", INK, "r")
    c.text(R, y + 40, "تفاصيل الملزمة وأقسامها تظهر هنا من ملف الملزمة.", 12.5, "r", MUTED, "r")
    primary_button(c, H - 84, "مشاهدة الملزمة", "eye")
    return c


# ------------------------------------------------------------------ 32. قارئ PDF
def pdf_viewer():
    c = Canvas("#EEF0F6")
    c.rr(0, 0, W, 96, 0, fill=SURFACE)
    c.circle(R - 20, 56, 20, fill=SURFACE, line=LINE); c.icon("arrow-r", R - 20, 56, 22, INK)
    c.text(180, 48, "ملزمة الرياضيات الشاملة", 15, "b", INK, "c")
    c.text(180, 70, "صفحة 12 من 120", 11.5, "m", MUTED, "c")
    c.circle(L + 20, 56, 20, fill=SURFACE, line=LINE); c.icon("search", L + 20, 56, 20, INK)
    c.rr(24, 112, 336, 560, 6, fill=SURFACE)
    c.text(R - 8, 140, "الوحدة الثالثة: المعادلات", 15, "b", INK, "r")
    for i in range(14):
        wv = [270, 240, 276, 200, 258, 230, 272, 180, 262, 244, 270, 210, 250, 150][i]
        c.rr(R - 8 - wv * 0.95 + 0, 168 + i * 25, R - 8, 176 + i * 25, 4, fill="#E3E7F0")
    c.rr(24, 576, 336, 760, 6, fill=SURFACE)
    c.shadow(40, 650, 320, 704, r=27, blur=8, dy=2, alpha=34)
    c.rr(40, 650, 320, 704, 27, fill=INK)
    c.icon("chev-r", 74, 677, 20, "#FFFFFF", bg=INK)
    c.text(180, 677, "12 / 120", 14, "b", "#FFFFFF", "c", rtl=False)
    c.icon("chev-l", 286, 677, 20, "#FFFFFF", bg=INK)
    return c


# ------------------------------------------------------------------ 33. مكتبة الكتب
def library():
    c = Canvas(BG)
    header(c, "مكتبة الكتب")
    x = R
    for i, t in enumerate(["الأول", "الثاني", "الثالث", "الرابع", "الخامس"]):
        x = chip(c, x, 90, t, i == 2, h=34, size=12.5, pad=20)
    cw = (R - L - 24) / 3
    books = [("lang", "teal", "الإنكليزي", True), ("sigma", "blue", "الرياضيات", True), ("book", "rose", "العربي", True),
             ("bolt", "violet", "الفيزياء", False), ("flask", "green", "الكيمياء", False), ("award", "orange", "الأحياء", True),
             ("globe", "amber", "التاريخ", False), ("cap", "cyan", "الإسلامية", False), ("globe", "teal", "الجغرافية", False)]
    for i, (ic, tone, t, has) in enumerate(books):
        row, col = divmod(i, 3)
        x1 = R - col * (cw + 12)
        y0 = 144 + row * 168
        c.card(x1 - cw, y0, x1, y0 + 156, 16, shadow=False)
        c.rr(x1 - cw + 10, y0 + 10, x1 - 10, y0 + 100, 10, fill=TOOL[tone][1])
        c.icon(ic, x1 - cw / 2, y0 + 55, 30, TOOL[tone][0], bg=TOOL[tone][1])
        c.text(x1 - cw / 2, y0 + 116, t, 13, "b", INK, "c")
        if has:
            c.text(x1 - cw / 2, y0 + 138, "افتح", 11.5, "b", PRIMARY, "c")
        else:
            c.text(x1 - cw / 2, y0 + 138, "أضف الكتاب", 11.5, "b", WARN, "c")
    bottom_nav(c, 1)
    return c


# ------------------------------------------------------------------ 34. إضافة كتاب
def add_book():
    c = dim(library())
    c.shadow(0, 360, W, H + 20, r=0, blur=12, dy=-4, alpha=40)
    c.rr(0, 360, W, H + 30, 28, fill=SURFACE, corners=(True, True, False, False))
    c.rr(W / 2 - 20, 372, W / 2 + 20, 376, 2, fill=LINE)
    c.text(R, 410, "إضافة كتاب الرياضيات", 18, "b", INK, "r")
    c.text(R, 438, "الثالث المتوسط - يُحفظ بجهازك وتفتحه بعدين بدون نت", 12.5, "r", MUTED, "r")
    c.card(L, 462, R, 536, 16, shadow=False)
    c.icon_tile("doc", R - 40, 499, 46, "blue", r=14)
    c.text(R - 76, 488, "رفع ملف PDF من الجهاز", 14.5, "b", INK, "r")
    c.text(R - 76, 511, "اختر الكتاب من الملفات", 12, "r", MUTED, "r")
    c.icon("chev-l", L + 24, 499, 20, FAINT)
    c.card(L, 548, R, 622, 16, shadow=False)
    c.icon_tile("globe", R - 40, 585, 46, "teal", r=14)
    c.text(R - 76, 574, "لصق رابط الكتاب", 14.5, "b", INK, "r")
    c.text(R - 76, 597, "رابط مباشر لملف PDF", 12, "r", MUTED, "r")
    c.icon("chev-l", L + 24, 585, 20, FAINT)
    outline_button(c, 646, "إلغاء", "close", h=52)
    return c


# ------------------------------------------------------------------ 35. قيد التطوير (نافذة)
def dev_dialog():
    c = dim(grades())
    c.shadow(30, 250, 330, 500, r=28, blur=14, dy=8, alpha=50)
    c.rr(30, 250, 330, 500, 28, fill=SURFACE)
    c.rr(150, 278, 210, 338, 20, fill=WARN_SOFT)
    c.icon("lock", 180, 308, 30, WARN, bg=WARN_SOFT)
    c.text(180, 366, "قيد التطوير", 20, "x", INK, "c")
    c.text(180, 398, "هذي المرحلة ما انفتحت للحين،", 13.5, "r", MUTED, "c")
    c.text(180, 420, "راح تنفتح بالتحديثات الجاية.", 13.5, "r", MUTED, "c")
    primary_button(c, 442, "تمام", None, x0=50, x1=310, h=44)
    return c


# ------------------------------------------------------------------ 36. الإعدادات
def settings():
    c = Canvas(BG)
    header(c, "الإعدادات")
    c.text(R, 106, "الذكاء الاصطناعي", 14, "b", INK, "r")
    c.card(L, 124, R, 262)
    c.text(R - 16, 148, "مفتاح الخدمة (API key)", 12, "m", MUTED, "r")
    c.rr(L + 14, 162, R - 14, 208, 12, fill=BG, line=LINE)
    c.icon("lock", R - 36, 185, 18, FAINT, bg=BG)
    c.text(R - 56, 185, "sk-ant-••••••••••••", 13, "r", INK, "r", rtl=False)
    c.icon("eye", L + 36, 185, 18, FAINT, bg=BG)
    c.text(R - 34, 232, "المفتاح يُحفظ على جهازك فقط", 11.5, "m", SUCCESS, "r")
    c.icon("check", R - 24, 232, 13, SUCCESS, sw=2.6, bg=SURFACE)
    c.text(R, 292, "القراءة الصوتية", 14, "b", INK, "r")
    c.card(L, 310, R, 410)
    c.text(R - 16, 334, "سرعة القراءة", 13, "m", INK, "r")
    progress(c, L + 16, R - 16, 358, 0.55, PRIMARY, 6)
    c.circle(R - 16 - (R - L - 32) * 0.55, 361, 9, fill=SURFACE, line=PRIMARY, lw=2.4)
    c.text(R - 16, 388, "بطيئة", 11.5, "m", FAINT, "r"); c.text(L + 16, 388, "سريعة", 11.5, "m", FAINT, "l")
    c.text(R, 438, "الحساب", 14, "b", INK, "r")
    c.card(L, 456, R, 582)
    for i, (ic, t) in enumerate([("user", "الاسم والصف"), ("lock", "تغيير كلمة المرور"), ("close", "تسجيل الخروج")]):
        y = 478 + i * 40
        c.icon(ic, R - 28, y, 20, DANGER if i == 2 else INK)
        c.text(R - 52, y, t, 14, "m", DANGER if i == 2 else INK, "r")
        c.icon("chev-l", L + 28, y, 18, FAINT)
        if i < 2:
            c.hline(L + 16, R - 16, y + 20)
    bottom_nav(c, 4)
    return c


# ------------------------------------------------------------------ 37. لوحة المواصفات (الألوان، الخط، القياسات)
def specs():
    c = Canvas(SURFACE)
    c.text(R, 44, "مواصفات التصميم", 22, "x", INK, "r")
    c.text(R, 72, "نفس القيم داخل كود Kotlin (Theme / Dimens / Type)", 12, "r", MUTED, "r")
    c.text(R, 106, "الألوان", 14, "b", INK, "r")
    cols = [("Primary", PRIMARY), ("Soft", PRIMARY_SOFT), ("Ink", INK), ("Muted", MUTED), ("Line", LINE), ("Success", SUCCESS)]
    for i, (n, col) in enumerate(cols):
        x1 = R - i * 54
        c.rr(x1 - 46, 122, x1, 164, 12, fill=col, line=LINE)
        c.text(x1 - 23, 178, n, 10, "m", MUTED, "c", rtl=False)
    c.text(R, 214, "الخط: Tajawal", 14, "b", INK, "r")
    rows = [("26sp", "ExtraBold", "عنوان رئيسي", 26, "x"), ("18sp", "Bold", "عنوان الشاشة", 18, "b"), ("14sp", "Regular", "نص عادي للقراءة", 14, "r"),
            ("12sp", "Medium", "تسمية صغيرة", 12, "m"), ("11sp", "Medium", "شرح سفلي", 11, "m")]
    y = 244
    for sz, wt, ex, px, w in rows:
        c.text(R, y, ex, px, w, INK, "r")
        c.text(L, y, f"{sz} / {wt}", 11, "m", MUTED, "l", rtl=False)
        y += px + 13
    c.text(R, y + 10, "القياسات (dp)", 14, "b", INK, "r")
    items = [("هامش الشاشة", "20"), ("ارتفاع الزر", "56"), ("ارتفاع الحقل", "52"), ("زوايا البطاقة", "16"), ("شريط التنقل", "72"), ("أقل مساحة لمس", "44")]
    y += 36
    for i, (k, v) in enumerate(items):
        row, col = divmod(i, 2)
        x1 = R - col * 170
        yy = y + row * 44
        c.rr(x1 - 160, yy, x1, yy + 36, 10, fill=BG)
        c.text(x1 - 12, yy + 18, k, 12, "m", INK, "r")
        c.text(x1 - 148, yy + 18, v, 13, "b", PRIMARY, "l", rtl=False)
    y += 3 * 44 + 18
    c.text(R, y, "الأجهزة", 14, "b", INK, "r")
    devs = [("هاتف صغير", 320, 80), ("هاتف عادي", 360, 90), ("هاتف كبير", 412, 104), ("تابلت", 600, 150)]
    for i, (n, wdp, bw) in enumerate(devs):
        yy = y + 22 + i * 30
        c.rr(R - bw, yy, R, yy + 22, 6, fill=PRIMARY_SOFT, line=PRIMARY)
        c.text(R - bw - 10, yy + 11, f"{n} - {wdp}dp", 11.5, "m", INK, "r")
    c.text(R, H - 18, "التكبير 0.9 إلى 1.25 - عرض أقصى 600dp - الخط حتى 1.15", 11, "m", MUTED, "r")
    return c


SCREENS2 = [
    ("15_signup", "إنشاء حساب", signup),
    ("16_grades", "اختيار الصف (مع القفل)", grades),
    ("17_home_subjects", "الرئيسية - المواد", home_subjects),
    ("18_english", "أقسام الإنكليزي", english),
    ("19_scanner", "الماسح", scanner),
    ("20_crop", "تحديد النص", crop),
    ("21_translate_result", "نتيجة ترجمة النص", translate_result),
    ("22_pronounce", "الترجمة مع النطق", pronounce),
    ("23_typed", "كتابة السؤال وترجمته", typed),
    ("24_math_sections", "أقسام الرياضيات", math_sections),
    ("25_math_solve", "حل بالصورة", math_solve),
    ("26_math_explain", "حل وشرح", math_explain),
    ("27_science_sections", "أقسام الكيمياء", science_sections),
    ("28_science_result", "نتيجة مسح الصفحة", science_result),
    ("29_biology", "أقسام الأحياء", biology_sections),
    ("30_ministry", "الوزاريات", ministry),
    ("31_handout", "تفاصيل الملزمة", handout_detail),
    ("32_pdf", "قارئ الملزمة", pdf_viewer),
    ("33_library", "مكتبة الكتب", library),
    ("34_add_book", "إضافة كتاب", add_book),
    ("35_dev", "قيد التطوير", dev_dialog),
    ("36_settings", "الإعدادات", settings),
    ("37_specs", "المواصفات", specs),
]
