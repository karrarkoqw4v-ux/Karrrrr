"""الدفعة الثالثة: الوزاريات، الملازم، قارئ التكبير، وقصة حركة الدخول."""
from lib import Canvas
from tokens import *
from screens import header, primary_button, outline_button, field, chip, bottom_nav, progress, R, L
from screens2 import lock_pill


def seg(c, y, labels, active, x0=L, x1=R):
    c.rr(x0, y, x1, y + 42, 14, fill="#EAECF5")
    n = len(labels)
    w = (x1 - x0 - 8) / n
    for i, t in enumerate(labels):
        xr = x1 - 4 - i * w
        if i == active:
            c.rr(xr - w, y + 4, xr, y + 38, 11, fill=SURFACE)
        c.text(xr - w / 2, y + 21, t, 13, "b" if i == active else "m", PRIMARY if i == active else MUTED, "c")


def ministry_home():
    c = Canvas(BG)
    header(c, "الوزاريات")
    seg(c, 86, ["الملازم", "الأسئلة الوزارية"], 1)
    c.text(R, 150, "أسئلة الثالث المتوسط من 2011 إلى 2026 مع الأجوبة", 12, "r", MUTED, "r")
    items = [("lang", "الإنكليزي", "teal"), ("book", "العربي", "rose"), ("flask", "الكيمياء", "green"), ("bolt", "الفيزياء", "violet"),
             ("leaf", "الأحياء", "orange"), ("globe", "الاجتماعيات", "amber"), ("mosque", "الإسلامية", "cyan"), ("sigma", "الرياضيات", "blue")]
    cw = (R - L - 12) / 2
    for i, (ic, t, tone) in enumerate(items):
        row, col = divmod(i, 2)
        x1 = R - col * (cw + 12)
        y0 = 170 + row * 96
        c.card(x1 - cw, y0, x1, y0 + 84, 18, shadow=False)
        c.icon_tile(ic, x1 - 36, y0 + 42, 50, tone, r=15)
        c.text(x1 - 72, y0 + 32, t, 15, "b", INK, "r")
        c.text(x1 - 72, y0 + 56, "2011 - 2026", 12, "r", MUTED, "r")
    return c


def ministry_subject():
    c = Canvas(BG)
    header(c, "الكيمياء")
    c.text(R, 96, "السنة", 12, "m", MUTED, "r")
    x = R
    for i, y in enumerate([2026, 2025, 2024, 2023, 2022, 2021]):
        x = chip(c, x, 108, str(y), i == 2, h=36, size=13, pad=24)
    seg(c, 160, ["الدور الثالث", "الدور الثاني", "الدور الأول"], 2)
    qs = ["احسب عدد مولات 11.7 غم من كلوريد الصوديوم NaCl علماً أن الكتلة المولية 58.5", "ما المقصود بالرابطة التساهمية؟ وأعط مثالاً", "علّل: يزداد نصف القطر الذري بالنزول في المجموعة الواحدة", "وازن المعادلة الآتية: H2 + O2 = H2O"]
    y = 222
    for i, q in enumerate(qs):
        c.card(L, y, R, y + 84, 16, shadow=False)
        c.rr(R - 46, y + 14, R - 12, y + 48, 10, fill=PRIMARY_SOFT)
        c.text(R - 29, y + 31, str(i + 1), 15, "b", PRIMARY, "c")
        c.para(R - 60, y + 24, q, 13, "r", INK, maxw=R - 60 - L - 40, lh=22)
        c.icon("chev-l", L + 22, y + 42, 18, FAINT)
        y += 96
    return c


def ministry_qa():
    c = Canvas(BG)
    header(c, "الكيمياء 2024")
    c.text(R, 100, "الدور الأول - سؤال 1 من 8", 12, "m", MUTED, "r")
    c.card(L, 120, R, 262, 18)
    c.text(R - 16, 144, "السؤال", 12, "b", PRIMARY, "r")
    c.para(R - 16, 176, "احسب عدد مولات 11.7 غم من كلوريد الصوديوم NaCl علماً أن الكتلة المولية للمركب تساوي 58.5 غم/مول", 15, "b", INK, maxw=R - L - 32, lh=26)
    c.rr(L, 280, R, 460, 20, fill=SUCCESS_SOFT)
    c.text(R - 16, 304, "الجواب", 12, "b", "#166534", "r")
    c.para(R - 16, 336, "عدد المولات = الكتلة ÷ الكتلة المولية", 14, "m", "#14532D", maxw=R - L - 32, lh=24)
    c.text(180, 392, "11.7 ÷ 58.5 = 0.2 مول", 17, "x", "#166534", "c", rtl=False)
    c.text(R - 16, 430, "الجواب النهائي: 0.2 مول", 14, "b", "#166534", "r")
    outline_button(c, H - 84, "السابق", "chev-r", x0=W / 2 + 6, x1=R, h=52)
    primary_button(c, H - 84, "التالي", "chev-l", x0=L, x1=W / 2 - 6, h=52, end=True)
    return c


def handouts_web():
    c = Canvas(SURFACE)
    header(c, "ملازم")
    c.rr(L, 92, R, 96, 2, fill=LINE)
    c.rr(R - (R - L) * 0.62, 92, R, 96, 2, fill=PRIMARY)
    c.rr(L, 108, R, 150, 14, fill=BG, line=LINE)
    c.icon("lock", R - 24, 129, 16, SUCCESS, bg=BG)
    c.text(R - 46, 129, "موقع الملازم", 13, "m", INK, "r")
    for i in range(4):
        y = 172 + i * 126
        c.rr(L, y, R, y + 112, 14, fill=BG, line=LINE)
        c.rr(R - 84, y + 10, R - 10, y + 102, 10, fill="#E3E7F0")
        c.rr(R - 200, y + 20, R - 98, y + 30, 5, fill="#D5DAE6")
        c.rr(R - 170, y + 42, R - 98, y + 50, 4, fill="#E3E7F0")
        c.rr(R - 200, y + 66, R - 130, y + 94, 12, fill=PRIMARY_SOFT)
        c.text(R - 165, y + 80, "PDF", 11, "b", PRIMARY, "c", rtl=False)
    c.rr(L, H - 70, R, H - 18, 18, fill=PRIMARY_SOFT)
    c.icon("info", R - 28, H - 44, 20, PRIMARY, bg=PRIMARY_SOFT)
    c.text(R - 54, H - 44, "أي رابط PDF ينفتح بقارئ التطبيق مباشرة", 12.5, "m", PRIMARY, "r")
    return c


def pdf_zoom():
    c = Canvas("#EEF0F6")
    c.rr(0, 0, W, 96, 0, fill=SURFACE)
    c.circle(R - 20, 56, 20, fill=SURFACE, line=LINE); c.icon("arrow-r", R - 20, 56, 22, INK)
    c.text(180, 48, "ملزمة الرياضيات", 15, "b", INK, "c")
    c.text(180, 70, "صفحة 12 من 120", 11.5, "m", MUTED, "c")
    # صفحة مكبّرة 2.5x (نصها أكبر وتتجاوز الحواف)
    c.rr(-90, 112, 450, 760, 6, fill=SURFACE)
    c.text(R + 60, 150, "الوحدة الثالثة: المعادلات", 26, "b", INK, "r")
    for i in range(9):
        wv = [470, 400, 450, 330, 440, 380, 460, 300, 420][i]
        c.rr(R + 70 - wv, 196 + i * 46, R + 70, 212 + i * 46, 8, fill="#DCE1EC")
    c.text(180, 640, "2x + 6 = 14", 40, "b", INK, "c", rtl=False)
    c.rr(130, 112, 230, 144, 16, fill="#101828E6") if False else None
    c.rr(L, 700, L + 74, 738, 19, fill=INK)
    c.text(L + 37, 719, "2.5x", 14, "b", "#FFFFFF", "c", rtl=False)
    # أصابع التكبير
    for (x, y) in [(120, 400), (240, 330)]:
        c.circle(x, y, 22, fill="#2F5BEA33", line=PRIMARY, lw=2)
    c.d.line([120 * S, 400 * S, 240 * S, 330 * S], fill=PRIMARY, width=int(2 * S))
    c.rr(80, 760, 280, 790, 14, fill=SURFACE) if False else None
    return c


def login_frame(t):
    """t: 0 البداية، 1 أثناء الدخول، 2 مكتمل. يمثل الحركة بحالة كل عنصر."""
    c = Canvas(SURFACE)
    import math
    # خلفية: دوائر متحركة بمواقع مختلفة حسب الإطار
    ofs = [0.0, 0.5, 1.0][t]
    c.circle(60 + 90 * ofs, 70 + 20 * ofs, 190, fill="#EEF2FE")
    c.circle(330 - 80 * ofs, 220 + 20 * ofs, 140, fill="#E6F6F3")
    c.circle(190 + 80 * ofs, 760 - 40 * ofs, 170, fill="#F1EEFE")
    # الشعار: يكبر مع دوران
    scale = [0.35, 1.12, 1.0][t]
    s = 64 * scale
    cx, cy = R - 32, 84 + [18, -6, 0][t]
    c.rr(cx - s / 2, cy - s / 2, cx + s / 2, cy + s / 2, 20 * scale, fill=PRIMARY)
    c.icon("cap", cx, cy, 34 * scale, "#FFFFFF", bg=PRIMARY)
    def fade(a, col):
        # تلاشي بخلط اللون مع الأبيض
        k = max(0.0, min(1.0, a))
        r1, g1, b1 = [int(col[i:i + 2], 16) for i in (1, 3, 5)]
        return "#%02X%02X%02X" % (int(255 - (255 - r1) * k), int(255 - (255 - g1) * k), int(255 - (255 - b1) * k))
    a = [0, 0.7, 1][t]
    c.text(R, 150 + [10, 3, 0][t], "هلا بيك من جديد", 26, "x", fade(a, INK), "r")
    c.text(R, 184 + [10, 3, 0][t], "سجّل دخولك وكمّل دراستك من وين وقفت", 14, "r", fade(a, MUTED), "r")
    fa = [0, 0.45, 1][t]
    dy = [26, 10, 0][t]
    for i, (lab, ic, val) in enumerate([("البريد الإلكتروني", "mail", "student@email.com"), ("كلمة المرور", "lock", "••••••••")]):
        y = 232 + i * 82 + dy * (i + 1) / 2
        c.text(R, y, lab, 12, "m", fade(fa, MUTED), "r")
        c.rr(L, y + 14, R, y + 14 + FIELD_H, RADIUS_FIELD, fill=SURFACE, line=fade(fa, PRIMARY if i == 0 else LINE), lw=1.6)
        c.icon(ic, R - 24, y + 14 + FIELD_H / 2, 22, fade(fa, FAINT), bg=SURFACE)
        if t == 2:
            c.text(R - 46, y + 14 + FIELD_H / 2, val, 14, "r", INK, "r")
    bt = [0, 0.4, 1][t]
    by = 424 + [30, 12, 0][t]
    c.rr(L, by, R, by + BUTTON_H, RADIUS_BUTTON, fill=fade(bt, PRIMARY))
    c.text(180, by + BUTTON_H / 2, "تسجيل الدخول", 16, "b", "#FFFFFF" if bt > 0.3 else fade(bt, PRIMARY), "c")
    return c


def login_press():
    c = Canvas(BG)
    c.text(R, 56, "حركة الضغط", 22, "x", INK, "r")
    c.text(R, 86, "ينكمش العنصر بنطّة نابضة ويميل ميلاناً خفيفاً ثم يرجع", 12.5, "r", MUTED, "r")
    c.text(R, 140, "عادي", 13, "b", MUTED, "r")
    c.card(L, 156, R, 242, 18, shadow=True)
    c.icon_tile("flask", R - 38, 199, 52, "green", r=16)
    c.text(R - 76, 188, "الكيمياء", 16, "b", INK, "r"); c.text(R - 76, 212, "3 أقسام", 12, "r", MUTED, "r")
    c.text(R, 288, "أثناء الضغط (تصغير 7% + ميلان 6°)", 13, "b", MUTED, "r")
    m = 14
    c.card(L + m, 304 + 4, R - m, 390 - 4, 18, shadow=False)
    c.icon_tile("flask", R - 38 - m, 347, 48, "green", r=15)
    c.text(R - 76 - m + 6, 336, "الكيمياء", 15, "b", INK, "r"); c.text(R - 76 - m + 6, 360, "3 أقسام", 11.5, "r", MUTED, "r")
    c.text(R, 440, "قوة كلمة المرور", 13, "b", MUTED, "r")
    for i, (w, col, lab) in enumerate([(0.25, DANGER, "ضعيفة"), (0.6, "#F59E0B", "مقبولة"), (1.0, SUCCESS, "قوية جداً")]):
        y = 462 + i * 52
        c.rr(L, y, R, y + 6, 3, fill=LINE); c.rr(R - (R - L) * w, y, R, y + 6, 3, fill=col)
        c.text(R, y + 24, lab, 11.5, "m", col, "r")
    c.text(R, 640, "الدخول المتتابع", 13, "b", MUTED, "r")
    c.text(R, 664, "كل عنصر يظهر بعد الي قبله بـ 55 ملّي ثانية بتلاشي وصعود", 12, "r", MUTED, "r")
    return c


SCREENS3 = [
    ("38_ministry_home", "الوزاريات - المواد", ministry_home),
    ("39_ministry_subject", "السنة والدور والأسئلة", ministry_subject),
    ("40_ministry_qa", "السؤال والجواب", ministry_qa),
    ("41_handouts_web", "متصفح الملازم", handouts_web),
    ("42_pdf_zoom", "قارئ الملزمة بالتكبير", pdf_zoom),
    ("43_login_t0", "الدخول - بداية الحركة", lambda: login_frame(0)),
    ("44_login_t1", "الدخول - أثناء الحركة", lambda: login_frame(1)),
    ("45_login_t2", "الدخول - مكتمل", lambda: login_frame(2)),
    ("46_motion", "الحركات", login_press),
]
