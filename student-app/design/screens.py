"""شاشات التطبيق (واجهة فاتحة، RTL، أيقونات خطية)."""
from lib import Canvas
from tokens import *

R = W - PAD   # الحافة اليمنى للمحتوى (RTL)
L = PAD       # الحافة اليسرى


# ------------------------------------------------------------------ مكونات مشتركة
def header(c, title, left_icon=None, back=True):
    if back:
        c.circle(R - 20, 52, 20, fill=SURFACE, line=LINE)
        c.icon("arrow-r", R - 20, 52, 22, INK)
    c.text(180, 52, title, 18, "b", INK, "c")
    if left_icon:
        c.circle(L + 20, 52, 20, fill=SURFACE, line=LINE)
        c.icon(left_icon, L + 20, 52, 22, INK)


def primary_button(c, y, label, icon=None, x0=L, x1=R, h=BUTTON_H, fill=PRIMARY, color="#FFFFFF", end=False):
    """end=True: الأيقونة بجهة النهاية (يسار بالـ RTL) مثل أسهم المتابعة."""
    c.rr(x0, y, x1, y + h, RADIUS_BUTTON, fill=fill)
    cx, cy = (x0 + x1) / 2, y + h / 2
    if icon:
        wd = c.width(label, 16, "b")
        total = wd + 10 + 22
        right = cx + total / 2
        if end:
            c.text(right, cy, label, 16, "b", color, "r")
            c.icon(icon, right - wd - 10 - 11, cy, 22, color, bg=fill)
        else:
            c.icon(icon, right - 11, cy, 22, color, bg=fill)
            c.text(right - 32, cy, label, 16, "b", color, "r")
    else:
        c.text(cx, cy, label, 16, "b", color, "c")


def outline_button(c, y, label, icon, x0=L, x1=R, h=BUTTON_H):
    c.rr(x0, y, x1, y + h, RADIUS_BUTTON, fill=SURFACE, line=LINE, lw=1.2)
    cx, cy = (x0 + x1) / 2, y + h / 2
    wd = c.width(label, 15, "m")
    right = cx + (wd + 10 + 22) / 2
    c.icon(icon, right - 11, cy, 22, INK)
    c.text(right - 32, cy, label, 15, "m", INK, "r")


def field(c, y, label, placeholder, icon, left_icon=None, value=None, focus=False):
    c.text(R, y, label, 12, "m", MUTED, "r")
    y0 = y + 14
    c.rr(L, y0, R, y0 + FIELD_H, RADIUS_FIELD, fill=SURFACE, line=PRIMARY if focus else LINE, lw=1.8 if focus else 1.2)
    c.icon(icon, R - 24, y0 + FIELD_H / 2, 22, PRIMARY if focus else FAINT)
    c.text(R - 46, y0 + FIELD_H / 2, value or placeholder, 14, "r", INK if value else FAINT, "r")
    if left_icon:
        c.icon(left_icon, L + 24, y0 + FIELD_H / 2, 22, FAINT)
    return y0 + FIELD_H


def chip(c, xr, y, label, active=False, h=34, size=13, pad=28):
    wd = c.width(label, size, "m") + pad
    c.rr(xr - wd, y, xr, y + h, h / 2, fill=PRIMARY if active else SURFACE, line=None if active else LINE)
    c.text(xr - wd / 2, y + h / 2, label, size, "m", "#FFFFFF" if active else MUTED, "c")
    return xr - wd - 8


def bottom_nav(c, active=0):
    y0 = H - NAV_H
    c.shadow(0, y0, W, H + 20, r=0, blur=8, dy=-2, alpha=22)
    c.rr(0, y0, W, H + 20, 22, fill=SURFACE, corners=(True, True, False, False))
    items = [("home", "الرئيسية"), ("grid", "الأدوات"), ("chat", "المساعد"), ("bars", "تقدمي"), ("user", "حسابي")]
    slot = W / 5
    for i, (ic, lb) in enumerate(items):          # RTL: الأول من اليمين
        cx = W - slot * (i + 0.5)
        col = PRIMARY if i == active else FAINT
        if i == active:
            c.rr(cx - 28, y0 + 8, cx + 28, y0 + 36, 14, fill=PRIMARY_SOFT)
        c.icon(ic, cx, y0 + 22, 22, col, bg=PRIMARY_SOFT if i == active else SURFACE)
        c.text(cx, y0 + 50, lb, 11, "b" if i == active else "m", col, "c")


def progress(c, x0, x1, y, frac, color=PRIMARY, h=8, track=LINE):
    c.rr(x0, y, x1, y + h, h / 2, fill=track)
    c.rr(x1 - (x1 - x0) * frac, y, x1, y + h, h / 2, fill=color)    # يبدأ من اليمين


# ------------------------------------------------------------------ 1. تسجيل الدخول
def login():
    c = Canvas(SURFACE)
    c.rr(R - 56, 56, R, 112, 18, fill=PRIMARY_SOFT)
    c.icon("cap", R - 28, 84, 30, PRIMARY, bg=PRIMARY_SOFT)
    c.text(R, 148, "هلا بيك من جديد", 26, "x", INK, "r")
    c.text(R, 182, "سجّل دخولك وكمّل دراستك من وين وقفت", 14, "r", MUTED, "r")
    y = field(c, 218, "البريد الإلكتروني أو رقم الهاتف", "name@email.com", "mail", focus=True, value="student@email.com")
    y = field(c, y + 14, "كلمة المرور", "••••••••", "lock", left_icon="eye", value="••••••••")
    c.text(L, y + 22, "نسيت كلمة المرور؟", 13, "m", PRIMARY, "l")
    primary_button(c, y + 52, "تسجيل الدخول")
    yy = y + 52 + BUTTON_H + 26
    c.hline(L, 150, yy); c.hline(210, R, yy)
    c.text(180, yy, "أو", 13, "m", FAINT, "c")
    outline_button(c, yy + 26, "المتابعة باستخدام Google", "google")
    outline_button(c, yy + 26 + BUTTON_H + 12, "الدخول برقم الهاتف", "phone")
    c.runs(R - (R - L) / 2 + 82, H - 44, [("ماعندك حساب؟ ", MUTED, "r"), ("أنشئ حساب", PRIMARY, "b")], 14)
    return c


# ------------------------------------------------------------------ 2. اختيار المرحلة
def stage():
    c = Canvas(SURFACE)
    header(c, "إنشاء حساب")
    for i in range(3):                                   # مؤشر الخطوات
        c.rr(R - 28 - i * 36, 98, R - i * 36, 102, 2, fill=PRIMARY if i < 2 else LINE)
    c.text(R, 134, "شنو مرحلتك الدراسية؟", 24, "x", INK, "r")
    c.text(R, 164, "نرتّب لك المواد والأدوات حسب منهج وزارة التربية", 13, "r", MUTED, "r")
    stages = [("pencil", "ابتدائي", "من الأول إلى السادس الابتدائي", "orange", False),
              ("book", "متوسط", "من الأول إلى الثالث المتوسط", "teal", False),
              ("flask", "إعدادي", "من الرابع إلى السادس الإعدادي", "blue", True),
              ("cap", "جامعي", "كل الكليات والاختصاصات", "violet", False)]
    y = 196
    for ic, t, sub, tone, sel in stages:
        c.card(L, y, R, y + 80, 18, fill=PRIMARY_SOFT if sel else SURFACE, line=PRIMARY if sel else LINE, shadow=not sel)
        if sel:
            c.rr(L, y, R, y + 80, 18, line=PRIMARY, lw=2)
        c.icon_tile(ic, R - 44, y + 40, 52, tone)
        c.text(R - 86, y + 28, t, 17, "b", INK, "r")
        c.text(R - 86, y + 54, sub, 12, "r", MUTED, "r")
        if sel:
            c.circle(L + 30, y + 40, 12, fill=PRIMARY)
            c.icon("check", L + 30, y + 40, 16, "#FFFFFF", sw=2.4)
        else:
            c.circle(L + 30, y + 40, 12, line=LINE, lw=1.6)
        y += 92
    c.text(R, y + 14, "الفرع", 12, "m", MUTED, "r")
    x = R
    for i, t in enumerate(["علمي", "أدبي", "مهني"]):
        x = chip(c, x, y + 28, t, i == 0, h=38, size=14)
    primary_button(c, H - 92, "متابعة", "arrow-l", end=True)
    return c


# ------------------------------------------------------------------ 3. الرئيسية
def home():
    c = Canvas(BG)
    c.circle(R - 22, 56, 22, fill=PRIMARY_SOFT)
    c.icon("user", R - 22, 56, 24, PRIMARY, bg=PRIMARY_SOFT)
    c.text(R - 56, 47, "هلا بيك", 18, "b", INK, "r")
    c.text(R - 56, 69, "السادس الإعدادي - العلمي", 12, "r", MUTED, "r")
    c.circle(L + 20, 56, 20, fill=SURFACE, line=LINE)
    c.icon("bell", L + 20, 56, 22, INK)
    c.circle(L + 30, 44, 4.5, fill=DANGER, line=BG, lw=1.5)
    # بحث
    c.rr(L, 98, R, 148, 16, fill=SURFACE, line=LINE)
    c.icon("search", R - 26, 123, 22, FAINT)
    c.text(R - 50, 123, "دوّر عن درس أو سؤال أو أداة", 14, "r", FAINT, "r")
    # بطاقة البطل
    c.rr(L, 164, R, 284, 22, fill=PRIMARY)
    c.deco_circles(L, 164, R, 284, 22, [(L + 20, 278, 66), (L + 20, 278, 40)], "#5B7EF0")
    c.text(R - 20, 196, "حلّ أي مسألة بالصورة", 20, "x", "#FFFFFF", "r")
    c.text(R - 20, 224, "صوّرها ونشرح لك الحل خطوة بخطوة", 12.5, "r", "#DCE5FF", "r")
    c.rr(R - 140, 242, R - 20, 274, 16, fill="#FFFFFF")
    c.icon("camera", R - 38, 258, 18, PRIMARY)
    c.text(R - 54, 258, "صوّر الآن", 14, "b", PRIMARY, "r")
    # الأدوات
    c.text(R, 312, "أدوات الدراسة", 17, "b", INK, "r")
    c.text(L, 312, "عرض الكل", 13, "m", PRIMARY, "l")
    tools = [("globe", "الترجمة", "teal"), ("chat", "المساعد", "violet"), ("doc", "التلخيص", "orange"), ("scan", "قراءة صور", "cyan"),
             ("quiz", "وزاري", "rose"), ("cards", "بطاقات", "amber"), ("timer", "المؤقت", "green"), ("calc", "المعدل", "blue")]
    cw = (W - 2 * PAD) / 4
    for i, (ic, t, tone) in enumerate(tools):
        row, col = divmod(i, 4)
        cx = R - cw * (col + 0.5)
        cy = 372 + row * 92
        c.icon_tile(ic, cx, cy, 56, tone, r=18, icon_size=28)
        c.text(cx, cy + 42, t, 12, "m", INK, "c")
    # يومك
    c.card(L, 552, R, 642)
    c.icon_tile("flame", R - 38, 596, 48, "orange", r=15)
    c.text(R - 74, 580, "7 أيام متتالية", 15, "b", INK, "r")
    c.text(R - 74, 604, "هدف اليوم: 45 من 60 دقيقة", 12, "r", MUTED, "r")
    progress(c, L + 16, R - 74, 622, 0.75, PRIMARY, 7)
    c.text(L + 16, 580, "75%", 14, "b", PRIMARY, "l")
    bottom_nav(c, 0)
    return c


# ------------------------------------------------------------------ 4. الكاميرا
def camera():
    c = Canvas("#E9ECF3")
    # ورقة الدفتر
    c.rr(36, 150, 324, 430, 14, fill="#FFFFFF")
    for y in range(184, 420, 30):
        c.hline(36, 324, y, "#DDE3F0")
    c.text(180, 262, "2x + 6 = 14", 36, "m", "#1F2A44", "c", rtl=False)
    c.text(180, 320, "x = ?", 30, "m", "#667085", "c", rtl=False)
    # إطار المسح
    ax, ay, bx, by, ln = 54, 218, 306, 372, 26
    for (x, y, sx, sy) in [(ax, ay, 1, 1), (bx, ay, -1, 1), (ax, by, 1, -1), (bx, by, -1, -1)]:
        c.d.line([x * S, (y + sy * ln) * S, x * S, y * S, (x + sx * ln) * S, y * S], fill=PRIMARY, width=int(3.5 * S), joint="curve")
    # أعلى
    c.circle(R - 20, 56, 20, fill=SURFACE)
    c.icon("close", R - 20, 56, 20, INK)
    c.circle(L + 20, 56, 20, fill=SURFACE)
    c.icon("bolt", L + 20, 56, 20, INK)
    c.rr(112, 38, 248, 74, 18, fill=SURFACE)
    c.text(180, 56, "حلّال المسائل", 14, "b", INK, "c")
    c.rr(70, 98, 290, 132, 17, fill=SURFACE)
    c.text(180, 115, "ضع المسألة داخل الإطار", 13, "m", MUTED, "c")
    # اللوحة السفلية
    c.shadow(0, 500, W, H + 20, r=0, blur=10, dy=-3, alpha=26)
    c.rr(0, 500, W, H + 20, 28, fill=SURFACE, corners=(True, True, False, False))
    c.rr(W / 2 - 20, 510, W / 2 + 20, 514, 2, fill=LINE)
    x = R
    for i, t in enumerate(["رياضيات", "فيزياء", "كيمياء", "أحياء"]):
        x = chip(c, x, 532, t, i == 0)
    c.circle(180, 650, 40, line=PRIMARY, lw=3)
    c.circle(180, 650, 31, fill=PRIMARY)
    c.icon("camera", 180, 650, 26, "#FFFFFF", bg=PRIMARY)
    c.circle(R - 22, 650, 26, fill=BG)
    c.icon("image", R - 22, 650, 24, INK, bg=BG)
    c.circle(L + 22, 650, 26, fill=BG)
    c.icon("pencil", L + 22, 650, 24, INK, bg=BG)
    c.text(R - 22, 700, "المعرض", 11, "m", MUTED, "c")
    c.text(L + 22, 700, "كتابة", 11, "m", MUTED, "c")
    return c


# ------------------------------------------------------------------ 5. الحل خطوة بخطوة
def solution():
    c = Canvas(BG)
    header(c, "الحل خطوة بخطوة", "bookmark")
    c.card(L, 90, R, 178)
    c.text(R - 16, 112, "المسألة", 12, "m", MUTED, "r")
    c.rr(L + 16, 100, L + 80, 124, 12, fill=PRIMARY_SOFT)
    c.text(L + 48, 112, "معادلة", 12, "b", PRIMARY, "c")
    c.text(180, 150, "2x + 6 = 14", 30, "b", INK, "c", rtl=False)
    steps = [("نطرح 6 من الطرفين", "2x = 8"), ("نقسم الطرفين على 2", "x = 4"), ("نتأكد من الجواب", "2(4) + 6 = 14")]
    y = 198
    for i, (t, eq) in enumerate(steps):
        c.circle(R - 16, y + 18, 16, fill=PRIMARY_SOFT)
        c.text(R - 16, y + 18, str(i + 1), 15, "b", PRIMARY, "c")
        if i < 2:
            c.d.line([(R - 16) * S, (y + 36) * S, (R - 16) * S, (y + 92) * S], fill=LINE, width=int(1.6 * S))
        c.card(L, y, R - 44, y + 74, 16)
        c.text(R - 60, y + 24, t, 14, "m", INK, "r")
        c.text(R - 60, y + 52, eq, 18, "b", PRIMARY, "r", rtl=False)
        y += 92
    c.rr(L, y + 4, R, y + 68, 16, fill=SUCCESS_SOFT)
    c.circle(R - 30, y + 36, 16, fill=SUCCESS)
    c.icon("check", R - 30, y + 36, 18, "#FFFFFF", sw=2.4)
    c.text(R - 58, y + 26, "الجواب النهائي", 12, "m", "#166534", "r")
    c.text(R - 58, y + 50, "x = 4", 20, "x", "#166534", "r", rtl=False)
    by = H - 84
    outline_button(c, by, "اشرح أبسط", "bulb", x0=W / 2 + 6, x1=R, h=52)
    primary_button(c, by, "مسألة مشابهة", "plus", x0=L, x1=W / 2 - 6, h=52)
    return c


# ------------------------------------------------------------------ 6. المترجم
def translate():
    c = Canvas(BG)
    header(c, "المترجم الذكي", "bookmark")
    # اللغات
    c.rr(L, 88, R, 140, 18, fill=SURFACE, line=LINE)
    c.text(R - (R - L) / 4 + 20, 114, "English", 15, "b", PRIMARY, "c", rtl=False)
    c.text(L + (R - L) / 4 - 20, 114, "العربية", 15, "b", INK, "c")
    c.circle(180, 114, 18, fill=PRIMARY_SOFT)
    c.icon("swap", 180, 114, 20, PRIMARY, bg=PRIMARY_SOFT)
    # الإدخال
    c.card(L, 154, R, 300)
    c.text(L + 16, 176, "English", 12, "m", FAINT, "l", rtl=False)
    c.icon("close", R - 24, 176, 16, FAINT)
    c.text(L + 16, 220, "Photosynthesis is the process by", 15, "m", INK, "l", rtl=False)
    c.text(L + 16, 246, "which plants make their own food.", 15, "m", INK, "l", rtl=False)
    c.icon("mic", R - 26, 278, 20, FAINT)
    c.icon("speaker", R - 62, 278, 20, FAINT)
    c.icon("scan", R - 98, 278, 20, FAINT)
    c.text(L + 16, 278, "67 / 500", 11, "m", FAINT, "l", rtl=False)
    # الناتج
    c.rr(L, 314, R, 480, 18, fill=PRIMARY_SOFT)
    c.text(R - 16, 336, "العربية", 12, "m", PRIMARY, "r")
    c.text(R - 16, 376, "البناء الضوئي هو العملية التي", 16, "b", INK, "r")
    c.text(R - 16, 404, "تصنع بها النباتات غذاءها بنفسها.", 16, "b", INK, "r")
    for i, ic in enumerate(["copy", "speaker", "bookmark"]):
        c.circle(L + 28 + i * 46, 448, 18, fill=SURFACE)
        c.icon(ic, L + 28 + i * 46, 448, 18, PRIMARY)
    # كلمات مهمة
    c.text(R, 508, "كلمات مهمة", 16, "b", INK, "r")
    words = [("Photosynthesis", "البناء الضوئي"), ("Process", "عملية"), ("Plants", "نباتات")]
    y = 530
    for en, ar in words:
        c.card(L, y, R, y + 48, 14, shadow=False)
        c.text(R - 16, y + 24, en, 14, "b", INK, "r", rtl=False)
        c.text(L + 16, y + 24, ar, 14, "m", MUTED, "l")
        y += 58
    bottom_nav(c, 1)
    return c


# ------------------------------------------------------------------ 7. المساعد الدراسي
def chat():
    c = Canvas(BG)
    header(c, "المساعد الدراسي", "sliders")
    c.rr(98, 78, 262, 100, 11, fill=SUCCESS_SOFT)
    c.circle(116, 89, 4, fill=SUCCESS)
    c.text(190, 89, "يشرح بلهجة بسيطة", 11.5, "m", "#166534", "c")
    # رسالة الطالب
    c.rr(120, 120, R, 164, 20, fill=PRIMARY, corners=(True, False, True, True))
    c.text(R - 16, 142, "اشرحلي قانون نيوتن الثاني", 14, "m", "#FFFFFF", "r")
    # رد المساعد
    c.card(L, 184, R - 24, 436, 20, shadow=False)
    c.rr(L, 184, R - 24, 436, 20, fill=SURFACE, line=LINE)
    lines = [("قانون نيوتن الثاني يقول:", "b", INK), ("كل ما تزيد القوة المسلطة على جسم،", "r", INK),
             ("تزيد سرعته بالتغيّر، وكل ما تكبر كتلته", "r", INK), ("يصير تحريكه أصعب.", "r", INK)]
    y = 214
    for t, w, col in lines:
        c.text(R - 40, y, t, 14, w, col, "r"); y += 26
    c.rr(L + 16, 322, R - 40, 372, 12, fill=PRIMARY_SOFT)
    c.text(180 - 12, 347, "F = m × a", 22, "b", PRIMARY, "c", rtl=False)
    c.text(R - 40, 398, "مثال: دفع سيارة أصعب من دفع دراجة.", 13, "r", MUTED, "r")
    c.circle(R - 8, 184 + 14, 14, fill=PRIMARY_SOFT)
    c.icon("bulb", R - 8, 198, 16, PRIMARY, bg=PRIMARY_SOFT)
    # اقتراحات
    x = R
    for t in ["اشرحه أبسط", "أعطني مثال", "اختبرني"]:
        x = chip(c, x, 456, t, False, h=36)
    # شريط الإدخال
    c.shadow(0, H - 84, W, H + 20, r=0, blur=8, dy=-2, alpha=18)
    c.rr(0, H - 84, W, H + 20, 0, fill=SURFACE)
    c.rr(L, H - 70, R, H - 22, 24, fill=BG, line=LINE)
    c.circle(R - 24, H - 46, 18, fill=PRIMARY)
    c.icon("send", R - 24, H - 46, 18, "#FFFFFF", bg=PRIMARY)
    c.text(R - 54, H - 46, "اكتب سؤالك هنا", 14, "r", FAINT, "r")
    c.icon("image", L + 26, H - 46, 22, FAINT, bg=BG)
    c.icon("mic", L + 62, H - 46, 22, FAINT, bg=BG)
    return c


# ------------------------------------------------------------------ 8. التلخيص وقراءة الصور
def summarize():
    c = Canvas(BG)
    header(c, "التلخيص وقراءة الصور")
    c.rr(L, 88, R, 132, 16, fill="#EAECF5")
    c.rr(L + 4, 92, (L + R) / 2 - 2, 128, 13, fill=SURFACE)
    c.text((L + (L + R) / 2) / 2 + 1, 110, "قراءة النص", 14, "m", MUTED, "c")
    c.text(((L + R) / 2 + R) / 2 - 1, 110, "تلخيص", 14, "b", PRIMARY, "c")
    c.rr((L + R) / 2 + 2, 92, R - 4, 128, 13, fill=SURFACE)
    c.text(((L + R) / 2 + R) / 2 - 1, 110, "تلخيص", 14, "b", PRIMARY, "c")
    c.rr(L + 4, 92, (L + R) / 2 - 2, 128, 13, fill="#EAECF5")
    c.text((L + (L + R) / 2) / 2 + 1, 110, "قراءة النص", 14, "m", MUTED, "c")
    # رفع
    c.rr(L, 150, R, 270, 20, fill=SURFACE)
    c.dashed_rr(L, 150, R, 270, PRIMARY, r=20)
    c.icon_tile("scan", 180, 192, 52, "blue", r=16)
    c.text(180, 232, "صوّر الملزمة أو ارفع ملف PDF", 14, "b", INK, "c")
    c.text(180, 254, "حتى 30 صفحة بالمرة", 12, "r", MUTED, "c")
    # الملف
    c.card(L, 286, R, 346, 16, shadow=False)
    c.icon_tile("doc", R - 32, 316, 40, "orange", r=12)
    c.text(R - 64, 306, "ملزمة الفيزياء - الفصل الثالث", 13, "m", INK, "r")
    c.text(R - 64, 328, "ملف PDF - 12 صفحة", 11.5, "r", MUTED, "r")
    c.icon("close", L + 24, 316, 18, FAINT)
    # النتيجة
    c.card(L, 362, R, 600)
    c.text(R - 16, 386, "الملخص", 16, "b", INK, "r")
    c.rr(L + 16, 374, L + 90, 398, 12, fill=SUCCESS_SOFT)
    c.text(L + 53, 386, "مكتمل", 12, "b", "#166534", "c")
    pts = ["الشغل = القوة × الإزاحة", "القدرة هي معدل انجاز الشغل", "وحدة الطاقة هي الجول", "الطاقة لا تفنى ولا تُستحدث"]
    y = 424
    for t in pts:
        c.circle(R - 22, y, 4, fill=PRIMARY)
        c.text(R - 38, y, t, 14, "r", INK, "r")
        y += 34
    c.hline(L + 16, R - 16, 566)
    for i, (ic, lb) in enumerate([("copy", "نسخ"), ("speaker", "استماع"), ("quiz", "أسئلة")]):
        cx = R - 52 - i * 82
        c.icon(ic, cx + 18, 584, 18, PRIMARY)
        c.text(cx + 6, 584, lb, 12.5, "m", PRIMARY, "r")
    bottom_nav(c, 1)
    return c


# ------------------------------------------------------------------ 9. الأسئلة الوزارية
def exams():
    c = Canvas(BG)
    header(c, "الأسئلة الوزارية", "sliders")
    x = R
    for i, t in enumerate(["الدور الأول", "الدور الثاني", "التمهيدي"]):
        x = chip(c, x, 86, t, i == 0)
    subjects = [("sigma", "الرياضيات", "blue", 14, 0.62), ("flask", "الفيزياء", "violet", 12, 0.45),
                ("flask", "الكيمياء", "teal", 11, 0.30), ("book", "الأحياء", "green", 10, 0.80),
                ("lang", "اللغة الإنكليزية", "orange", 9, 0.55), ("pencil", "اللغة العربية", "rose", 9, 0.20)]
    y = 138
    for ic, t, tone, n, fr in subjects:
        c.card(L, y, R, y + 74, 18, shadow=False)
        c.icon_tile(ic, R - 40, y + 37, 48, tone, r=15)
        c.text(R - 78, y + 25, t, 15, "b", INK, "r")
        c.text(R - 78, y + 47, f"{n} نماذج امتحانية", 12, "r", MUTED, "r")
        progress(c, L + 56, R - 78 - 0, y + 62, 0, h=4)
        c.hline(L + 56, R - 78, y + 62, LINE, 1)
        c.d.line([(R - 78) * S, (y + 62) * S, (R - 78 - (R - 78 - L - 56) * fr) * S, (y + 62) * S], fill=TOOL[tone][0], width=int(3 * S))
        c.icon("chev-l", L + 24, y + 37, 20, FAINT)
        y += 84
    bottom_nav(c, 1)
    return c


# ------------------------------------------------------------------ 10. اختبار تجريبي
def quiz():
    c = Canvas(BG)
    header(c, "اختبار الفيزياء", back=True)
    c.rr(L, 76, L + 82, 106, 15, fill=WARN_SOFT)
    c.icon("timer", L + 62, 91, 16, WARN, bg=WARN_SOFT)
    c.text(L + 28, 91, "12:40", 13, "b", WARN, "c", rtl=False)
    c.text(R, 130, "السؤال 4 من 20", 13, "m", MUTED, "r")
    progress(c, L, R, 148, 0.2, PRIMARY, 6)
    c.card(L, 170, R, 306)
    c.text(R - 16, 196, "اختيار من متعدد", 12, "m", PRIMARY, "r")
    c.text(R - 16, 232, "جسم كتلته 2 كغم تؤثر فيه قوة مقدارها", 15, "b", INK, "r")
    c.text(R - 16, 260, "10 نيوتن. ما مقدار تعجيله؟", 15, "b", INK, "r")
    opts = [("أ", "2 م/ث²", "n"), ("ب", "5 م/ث²", "ok"), ("ج", "10 م/ث²", "n"), ("د", "20 م/ث²", "n")]
    y = 324
    for k, t, st in opts:
        fill = SUCCESS_SOFT if st == "ok" else SURFACE
        line = SUCCESS if st == "ok" else LINE
        c.rr(L, y, R, y + 62, 16, fill=fill, line=line, lw=2 if st == "ok" else 1.2)
        c.circle(R - 30, y + 31, 15, fill=SUCCESS if st == "ok" else BG, line=None if st == "ok" else LINE)
        if st == "ok":
            c.icon("check", R - 30, y + 31, 16, "#FFFFFF", sw=2.5)
        else:
            c.text(R - 30, y + 31, k, 13, "b", MUTED, "c")
        c.text(R - 58, y + 31, t, 15, "m", INK, "r", rtl=True)
        y += 72
    primary_button(c, H - 84, "السؤال التالي", "arrow-l", end=True)
    return c


# ------------------------------------------------------------------ 11. مؤقت الدراسة
def timer():
    c = Canvas(BG)
    header(c, "مؤقت الدراسة", "sliders")
    c.rr(L + 20, 88, R - 20, 130, 21, fill="#EAECF5")
    c.rr(180 + 2, 92, R - 24, 126, 17, fill=SURFACE)
    c.text((180 + R - 24) / 2 + 1, 109, "تركيز", 14, "b", PRIMARY, "c")
    c.text((L + 24 + 178) / 2, 109, "استراحة", 14, "m", MUTED, "c")
    cx, cy, r = 180, 290, 112
    c.circle(cx, cy, r, line="#E3E8F4", lw=14)
    c.d.arc([(cx - r) * S, (cy - r) * S, (cx + r) * S, (cy + r) * S], -90, 100, fill=PRIMARY, width=int(14 * S))
    c.text(cx, cy - 10, "17:42", 46, "x", INK, "c", rtl=False)
    c.text(cx, cy + 34, "جلسة تركيز 2 من 4", 13, "m", MUTED, "c")
    c.circle(cx - 78, cy + 90, 0.1)
    c.circle(L + 46, 456, 28, fill=SURFACE, line=LINE)
    c.icon("reset", L + 46, 456, 24, INK)
    c.circle(180, 456, 38, fill=PRIMARY)
    c.icon("pause", 180, 456, 30, "#FFFFFF", sw=2.6, bg=PRIMARY)
    c.circle(R - 46, 456, 28, fill=SURFACE, line=LINE)
    c.icon("book", R - 46, 456, 24, INK)
    c.text(L + 46, 504, "إعادة", 11.5, "m", MUTED, "c")
    c.text(R - 46, 504, "المادة", 11.5, "m", MUTED, "c")
    c.card(L, 536, R, 640)
    c.text(R - 16, 560, "إحصائيات اليوم", 14, "b", INK, "r")
    for i, (v, lb) in enumerate([("3", "جلسات"), ("75", "دقيقة"), ("7", "أيام متتالية")]):
        x = R - 16 - i * 106 - 44
        c.text(x, 596, v, 22, "x", PRIMARY, "c", rtl=False)
        c.text(x, 620, lb, 11.5, "m", MUTED, "c")
    bottom_nav(c, 1)
    return c


# ------------------------------------------------------------------ 12. حاسبة المعدل
def gpa():
    c = Canvas(BG)
    header(c, "حاسبة المعدل")
    c.rr(L, 88, R, 214, 22, fill=PRIMARY)
    c.deco_circles(L, 88, R, 214, 22, [(L + 24, 206, 60), (L + 24, 206, 36)], "#5B7EF0")
    c.text(R - 20, 116, "معدلك الحالي", 13, "m", "#DCE5FF", "r")
    c.text(R - 20, 160, "87.4", 44, "x", "#FFFFFF", "r", rtl=False)
    c.text(R - 20 - c.width("87.4", 44, "x", rtl=False) - 10, 168, "%", 20, "b", "#DCE5FF", "r", rtl=False)
    c.rr(L + 18, 140, L + 94, 172, 16, fill="#FFFFFF")
    c.text(L + 56, 156, "جيد جداً", 13, "b", PRIMARY, "c")
    c.text(R - 20, 194, "مجموع الدرجات 699 من 800", 12, "r", "#DCE5FF", "r")
    c.text(R, 240, "درجات المواد", 16, "b", INK, "r")
    c.text(L, 240, "إضافة مادة", 13, "m", PRIMARY, "l")
    rows = [("الرياضيات", "92"), ("الفيزياء", "85"), ("الكيمياء", "88"), ("الأحياء", "90"),
            ("اللغة الإنكليزية", "81"), ("اللغة العربية", "84"), ("التربية الإسلامية", "95"), ("الحاسوب", "84")]
    y = 262
    for t, v in rows:
        c.card(L, y, R, y + 46, 14, shadow=False)
        c.text(R - 16, y + 23, t, 14, "m", INK, "r")
        c.rr(L + 12, y + 8, L + 76, y + 38, 10, fill=BG, line=LINE)
        c.text(L + 44, y + 23, v, 15, "b", PRIMARY, "c", rtl=False)
        y += 54
    bottom_nav(c, 1)
    return c


# ------------------------------------------------------------------ 13. البطاقات
def flashcards():
    c = Canvas(BG)
    header(c, "بطاقات الحفظ", "plus")
    c.rr(R - 150, 80, R, 112, 16, fill=SURFACE, line=LINE)
    c.text(R - 14, 96, "الكيمياء - العناصر", 12.5, "m", INK, "r")
    c.icon("chev-d", R - 136 + 6, 96, 16, FAINT)
    c.text(L, 96, "12 / 30", 13, "b", MUTED, "l", rtl=False)
    progress(c, L, R, 124, 0.4, PRIMARY, 6)
    # بطاقة خلفية
    c.rr(L + 16, 168, R - 16, 500, 26, fill="#DCE3F7")
    c.card(L, 150, R, 500, 26)
    c.rr(R - 112, 170, R - 16, 198, 14, fill=PRIMARY_SOFT)
    c.text(R - 64, 184, "السؤال", 12, "b", PRIMARY, "c")
    c.icon("speaker", L + 28, 184, 22, FAINT)
    c.text(180, 300, "ما الرمز الكيميائي", 20, "b", INK, "c")
    c.text(180, 334, "للصوديوم؟", 20, "b", INK, "c")
    c.hline(L + 60, R - 60, 392, LINE)
    wd = c.width("اضغط لقلب البطاقة", 12.5, "m")
    c.icon("reset", 180 + wd / 2 + 14, 430, 18, FAINT)
    c.text(180, 430, "اضغط لقلب البطاقة", 12.5, "m", FAINT, "c")
    c.rr(L, 528, W / 2 - 6, 592, 18, fill=SURFACE, line=DANGER, lw=1.6)
    c.icon("close", W / 2 - 62, 560, 20, DANGER)
    c.text(W / 2 - 96, 560, "أعيدها", 15, "b", DANGER, "c")
    c.rr(W / 2 + 6, 528, R, 592, 18, fill=SUCCESS)
    c.icon("check", R - 54, 560, 20, "#FFFFFF", sw=2.4, bg=SUCCESS)
    c.text(R - 94, 560, "أعرفها", 15, "b", "#FFFFFF", "c")
    c.text(180, 626, "التكرار المتباعد يعرض لك البطاقات الصعبة أكثر", 12, "r", MUTED, "c")
    bottom_nav(c, 1)
    return c


# ------------------------------------------------------------------ 14. تقدمي
def progress_screen():
    c = Canvas(BG)
    header(c, "تقدمي", "calendar", back=False)
    c.card(L, 88, R, 258)
    c.text(R - 16, 112, "ساعات الدراسة هذا الأسبوع", 14, "b", INK, "r")
    c.text(L + 16, 112, "14.5 ساعة", 14, "b", PRIMARY, "l")
    days = ["س", "ح", "ن", "ث", "ر", "خ", "ج"]
    vals = [0.5, 0.8, 0.4, 0.9, 0.65, 0.3, 0.0]
    bw = 26
    for i, (dname, v) in enumerate(zip(days, vals)):
        cx = R - 34 - i * 41.5
        c.rr(cx - bw / 2, 130, cx + bw / 2, 214, 9, fill="#EEF1F8")
        if v > 0:
            c.rr(cx - bw / 2, 214 - 84 * v, cx + bw / 2, 214, 9, fill=PRIMARY if i == 3 else "#9DB4F5")
        c.text(cx, 236, dname, 12, "m", MUTED, "c")
    # بطاقتين
    c.card(L, 274, W / 2 - 6, 358)
    c.icon("flame", W / 2 - 6 - 28, 300, 22, TOOL["orange"][0])
    c.text(W / 2 - 22, 328, "7 أيام", 20, "x", INK, "r")
    c.text(W / 2 - 22, 346, "سلسلة متتالية", 11.5, "m", MUTED, "r")
    c.card(W / 2 + 6, 274, R, 358)
    c.icon("award", R - 28, 300, 22, TOOL["violet"][0])
    c.text(R - 16, 328, "12 شارة", 20, "x", INK, "r")
    c.text(R - 16, 346, "إنجازات", 11.5, "m", MUTED, "r")
    # المواد
    c.text(R, 386, "مستوى المواد", 16, "b", INK, "r")
    subs = [("الرياضيات", 0.88, "green"), ("الأحياء", 0.74, "blue"), ("الكيمياء", 0.55, "amber"), ("الفيزياء", 0.38, "rose")]
    y = 414
    for t, v, tone in subs:
        c.card(L, y, R, y + 58, 14, shadow=False)
        c.text(R - 16, y + 20, t, 14, "m", INK, "r")
        c.text(L + 16, y + 20, f"{int(v * 100)}%", 13, "b", TOOL[tone][0], "l", rtl=False)
        progress(c, L + 16, R - 16, y + 38, v, TOOL[tone][0], 7)
        y += 66
    c.text(R, y + 18, "تحتاج مراجعة: الفيزياء", 12.5, "m", TOOL["rose"][0], "r")
    bottom_nav(c, 3)
    return c


SCREENS = [
    ("01_login", "تسجيل الدخول", login),
    ("02_stage", "اختيار المرحلة", stage),
    ("03_home", "الرئيسية", home),
    ("04_camera", "حلال المسائل", camera),
    ("05_solution", "الحل خطوة بخطوة", solution),
    ("06_translate", "المترجم", translate),
    ("07_chat", "المساعد الدراسي", chat),
    ("08_summarize", "التلخيص وقراءة الصور", summarize),
    ("09_exams", "الأسئلة الوزارية", exams),
    ("10_quiz", "الاختبار التجريبي", quiz),
    ("11_timer", "مؤقت الدراسة", timer),
    ("12_gpa", "حاسبة المعدل", gpa),
    ("13_flashcards", "بطاقات الحفظ", flashcards),
    ("14_progress", "تقدمي", progress_screen),
]
