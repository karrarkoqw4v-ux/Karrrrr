"""شاشات الدخول والتسجيل (12 شاشة)."""
from kit import K, mix
from tokens import *

R = W - PAD
L = PAD


def bullets(c, y, items, title=None):
    """قائمة نقاط توضيحية: أيقونة + عنوان + شرح"""
    if title:
        c.text(R, y, title, 15, "b", INK, "r")
        y += 26
    for ic, t, d in items:
        c.rr(R - 44, y, R, y + 44, 14, fill=PRIMARY_SOFT)
        c.icon(ic, R - 22, y + 22, 22, PRIMARY_DARK, bg=PRIMARY_SOFT)
        c.text(R - 58, y + 12, t, 14, "b", INK, "r")
        c.text(R - 58, y + 34, d, 12, "r", MUTED, "r")
        y += 62
    return y


def splash():
    c = K(PRIMARY)
    c.grad(0, 0, W, H, "#12C9B1", "#075E57")
    c.circle(300, 110, 120, fill=mix("#12C9B1", "#FFFFFF", 0.10))
    c.circle(40, 640, 150, fill=mix("#075E57", "#FFFFFF", 0.07))
    c.rr(132, 282, 228, 378, 30, fill="#FFFFFF")
    c.rr(150, 300, 210, 350, 16, fill=PRIMARY)
    c.d.polygon([(186 * S, 345 * S), (200 * S, 366 * S), (176 * S, 349 * S)], fill=PRIMARY)
    for x in (169, 180, 191):
        c.circle(x, 325, 3.4, fill="#FFFFFF")
    c.text(180, 424, "هلا", 40, "x", "#FFFFFF", "c")
    c.text(180, 462, "دردش، شارك، وانبسط", 15, "m", "#D6F5F0", "c")
    c.text(180, 730, "صُنع بحب للعراقيين", 12.5, "m", "#CFF3EE", "c")
    return c


def _dots(c, y, active, n=3):
    for i in range(n):
        x = 180 + (i - (n - 1) / 2) * 16
        if i == active:
            c.rr(x - 9, y - 3.5, x + 9, y + 3.5, 3.5, fill=PRIMARY)
        else:
            c.circle(x, y, 3.5, fill=LINE)


def welcome(step):
    c = K(BG)
    c.text(L, 52, "تخطي", 14, "b", MUTED, "l")
    # رسم توضيحي
    c.grad(L, 96, R, 456, "#E9FBF8", "#FFF3DC", r=28)
    if step == 0:
        c.photo(66, 150, 190, 330, 0, r=0); c.photo(150, 190, 290, 392, 3); c.photo(98, 262, 230, 420, 5)
        c.circle(262, 168, 20, fill=LIKE); c.icon("heart-fill", 262, 168, 22, "#FFFFFF")
        c.rr(80, 380, 168, 414, 17, fill="#FFFFFF"); c.text(124, 397, "لحظات حلوة", 12, "b", PRIMARY_DARK, "c")
    elif step == 1:
        msgs = [(True, "هلا بيك!", 270), (False, "شلونك اليوم؟", 140), (True, "تمام، وانت؟", 190), (False, "ننتظرك بالمقهى", 210)]
        y = 140
        for mine, t, w in msgs:
            x1, x0 = (R - 20, R - 20 - w) if mine else (L + 20 + w, L + 20)
            c.rr(x0, y, x1, y + 44, 20, fill=PRIMARY if mine else "#FFFFFF")
            c.text((x0 + x1) / 2, y + 22, t, 13.5, "m", "#FFFFFF" if mine else INK, "c")
            y += 58
        c.rr(R - 150, y + 6, R - 20, y + 46, 20, fill="#FFFFFF")
        c.icon("mic", R - 40, y + 26, 20, PRIMARY); c.rr(R - 124, y + 24, R - 62, y + 28, 2, fill=PRIMARY_SOFT)
        for i in range(10):
            c.rr(R - 128 + i * 6, y + 26 - (4 + (i * 7) % 9), R - 125 + i * 6, y + 26 + (4 + (i * 7) % 9), 1.5, fill=PRIMARY)
    else:
        pos = [(180, 270, 46), (96, 200, 30), (264, 200, 32), (88, 340, 28), (272, 344, 30), (180, 160, 24), (180, 390, 26)]
        for i, (x, y, r) in enumerate(pos):
            c.avatar(x, y, r, "نمرسعحكلم"[i], i, ring="new" if i < 3 else None)
    titles = [("شارك لحظاتك", "صور وقصص تنشرها وأصحابك يشوفونها بلحظتها"), ("دردش بدون حدود", "رسائل ومكالمات وصور وتسجيلات صوتية بمكان واحد"), ("اكتشف ناس جدد", "تابع أصحابك واكتشف محتوى يشبهك")]
    t, s = titles[step]
    c.text(180, 504, t, 26, "x", INK, "c")
    c.text(180, 546, s, 13.5, "r", MUTED, "c")
    _dots(c, 600, step)
    c.btn(646, "التالي" if step < 2 else "ابدأ الآن")
    c.text(180, 724, "عندي حساب؟ تسجيل الدخول", 13.5, "b", PRIMARY_DARK, "c")
    return c


def login():
    c = K(BG)
    c.text(180, 40, "العربية ▾".replace("▾", "").strip(), 13, "m", MUTED, "c")
    c.logo(180, 128, 72)
    c.text(180, 196, "هلا بيك مجدداً", 26, "x", INK, "c")
    c.text(180, 226, "ادخل حتى تكمل دردشتك", 13.5, "r", MUTED, "c")
    y = c.input(262, "رقم الهاتف أو البريد أو اسم المستخدم", "user", value="sara.ali", focus=True)
    y = c.input(y + 12, "كلمة المرور", "lock", trailing="eye", value="••••••••")
    c.text(L, y + 24, "نسيت كلمة المرور؟", 13, "b", PRIMARY_DARK, "l")
    c.btn(y + 52, "دخول")
    yy = y + 52 + BUTTON_H + 22
    c.hline(L, 150, yy); c.hline(210, R, yy); c.text(180, yy, "أو", 12.5, "m", FAINT, "c")
    c.btn(yy + 20, "المتابعة بحساب Google", kind="line", icon="google")
    c.btn(yy + 20 + BUTTON_H + 10, "الدخول برقم الهاتف", kind="line", icon="phone")
    c.hline(0, W, 696, LINE)
    c.runs(R - 56, 734, [("ما عندك حساب؟ ", MUTED, "r"), ("سجّل الحين", PRIMARY_DARK, "b")], 14)
    return c


def login_error():
    c = K(BG)
    c.logo(180, 110, 60)
    c.text(180, 168, "هلا بيك مجدداً", 24, "x", INK, "c")
    c.rr(L, 202, R, 252, 14, fill="#FFECEC")
    c.icon("info", R - 26, 227, 20, DANGER, bg="#FFECEC")
    c.text(R - 50, 218, "كلمة المرور غلط", 13.5, "b", DANGER, "r")
    c.text(R - 50, 238, "جرّب مرة ثانية أو استعدها", 12, "r", DANGER, "r")
    y = c.input(272, "", "user", value="sara.ali", err=True)
    y = c.input(y + 12, "", "lock", value="•••••", trailing="eye", err=True)
    c.text(L, y + 24, "نسيت كلمة المرور؟", 13, "b", PRIMARY_DARK, "l")
    c.btn(y + 52, "دخول")
    c.text(180, y + 52 + BUTTON_H + 30, "بقيت لك 3 محاولات قبل القفل المؤقت", 12.5, "m", MUTED, "c")
    bullets(c, 520, [("lock", "استعادة كلمة المرور", "اضغط نسيت كلمة المرور"), ("user", "تأكد من اسم المستخدم", "بدون مسافات أو حروف كبيرة")], "جرّب هذا")
    return c


def forgot():
    c = K(BG)
    c.back("استعادة الحساب")
    c.rr(130, 108, 230, 208, 50, fill=PRIMARY_SOFT)
    c.icon("lock", 180, 158, 44, PRIMARY_DARK, bg=PRIMARY_SOFT)
    c.text(180, 250, "نسيت كلمة المرور؟", 22, "x", INK, "c")
    c.text(180, 284, "اكتب بريدك أو رقم هاتفك ونرسل لك رمز", 13.5, "r", MUTED, "c")
    c.text(180, 304, "الاستعادة", 13.5, "r", MUTED, "c")
    y = c.input(336, "البريد أو رقم الهاتف", "mail", focus=True, value="sara@email.com")
    c.btn(y + 22, "إرسال الرمز")
    c.text(180, y + 100, "رجوع إلى تسجيل الدخول", 14, "b", PRIMARY_DARK, "c")
    bullets(c, 520, [("mail", "عبر البريد الإلكتروني", "نرسل لك رابط الاستعادة"), ("phone", "عبر رسالة SMS", "نرسل لك رمز من 6 أرقام"), ("info", "تحتاج مساعدة؟", "تواصل مع فريق الدعم")], "طرق الاستعادة")
    return c


def otp():
    c = K(BG)
    c.back("التحقق")
    c.text(180, 126, "أدخل الرمز", 24, "x", INK, "c")
    c.text(180, 160, "أرسلنا رمز من 6 أرقام إلى", 13.5, "r", MUTED, "c")
    c.text(180, 182, "sara@email.com", 14, "b", INK, "c", rtl=False)
    bw = 44
    for i in range(6):
        x1 = R - 2 - i * (bw + 8)
        filled = i < 3
        c.rr(x1 - bw, 230, x1, 230 + 56, 12, fill=BG2, line=PRIMARY if i == 3 else LINE, lw=2 if i == 3 else 1.2)
        if filled:
            c.text(x1 - bw / 2, 258, str([4, 8, 1][i]), 22, "b", INK, "c", rtl=False)
    c.text(180, 330, "00:42", 15, "b", PRIMARY_DARK, "c", rtl=False)
    c.text(180, 358, "ما وصلك الرمز؟ إعادة الإرسال", 13, "m", MUTED, "c")
    c.btn(396, "تأكيد")
    # لوحة أرقام
    keys = ["1", "2", "3", "4", "5", "6", "7", "8", "9", "", "0", "x"]
    for i, k in enumerate(keys):
        r_, c_ = divmod(i, 3)
        cx = 70 + c_ * 110
        cy = 500 + r_ * 66
        if k == "x":
            c.icon("close", cx, cy, 24, INK)
        elif k:
            c.text(cx, cy, k, 24, "m", INK, "c", rtl=False)
    return c


def _stepper(c, step, total=4):
    c.back("إنشاء حساب")
    for i in range(total):
        xr = R - i * (R - L) / total
        c.rr(xr - (R - L) / total + 4, 86, xr, 90, 2, fill=PRIMARY if i <= step else LINE)


def signup1():
    c = K(BG)
    _stepper(c, 0)
    c.text(R, 140, "ما هو رقمك أو بريدك؟", 24, "x", INK, "r")
    c.text(R, 172, "نستخدمه لتأكيد حسابك وحمايته", 13.5, "r", MUTED, "r")
    c.rr(L, 204, R, 246, 14, fill=BG2)
    c.rr(L + 4, 208, 178, 242, 11, fill=BG2)
    c.rr(182, 208, R - 4, 242, 11, fill=SURFACE)
    c.text(R - 4 - 79, 225, "رقم الهاتف", 13.5, "b", PRIMARY_DARK, "c")
    c.text(100, 225, "البريد الإلكتروني", 13.5, "m", MUTED, "c")
    c.rr(L, 266, 98, 266 + FIELD_H, 14, fill=BG2, line=LINE)
    c.text(L + 40, 266 + FIELD_H / 2, "964+", 14, "b", INK, "c", rtl=False)
    c.rr(106, 266, R, 266 + FIELD_H, 14, fill=SURFACE, line=PRIMARY, lw=1.8)
    c.text(R - 16, 266 + FIELD_H / 2, "7701234567", 15, "m", INK, "r", rtl=False)
    c.text(R, 346, "قد نرسل لك رسالة SMS للتأكيد، وقد تطبّق رسوم الشبكة.", 11.5, "r", FAINT, "r")
    c.btn(380, "التالي")
    bullets(c, 470, [("shield", "حماية حسابك", "نتأكد إنك أنت صاحب الحساب"), ("lock", "استعادة كلمة المرور", "ترجع لحسابك إذا نسيت الرمز"), ("users", "إيجاد أصحابك", "نقترح عليك ناس تعرفهم")], "ليش نطلب رقمك؟")
    c.runs(R - 60, 748, [("عندك حساب؟ ", MUTED, "r"), ("سجّل دخول", PRIMARY_DARK, "b")], 14)
    return c


def signup2():
    c = K(BG)
    _stepper(c, 1)
    c.text(R, 140, "اسمك وكلمة المرور", 24, "x", INK, "r")
    c.text(R, 172, "اسمك يظهر لأصحابك بالدردشة", 13.5, "r", MUTED, "r")
    y = c.input(206, "الاسم الكامل", "user", value="سارة علي", focus=True)
    y = c.input(y + 12, "كلمة المرور", "lock", value="Hala#2026", trailing="eye")
    for i in range(4):
        c.rr(R - (i + 1) * 80 + 4, y + 14, R - i * 80, y + 19, 2.5, fill=SUCCESS if i < 3 else LINE)
    c.text(R, y + 36, "قوية  - استخدم 8 أحرف مع رموز وأرقام", 12, "m", SUCCESS, "r")
    yy = y + 66
    c.text(R, yy, "كلمة مرور قوية تحتوي على:", 13.5, "b", INK, "r")
    for i, (t, ok) in enumerate([("8 أحرف أو أكثر", True), ("حرف كبير وصغير", True), ("رقم واحد على الأقل", True), ("رمز مثل # أو @", False)]):
        yb = yy + 26 + i * 30
        c.circle(R - 11, yb + 8, 10, fill=SUCCESS if ok else LINE)
        c.icon("check", R - 11, yb + 8, 13, "#FFFFFF", sw=2.6, bg=SUCCESS if ok else LINE)
        c.text(R - 32, yb + 8, t, 13, "m", INK if ok else MUTED, "r")
    c.btn(yy + 160, "التالي")
    return c


def signup3():
    c = K(BG)
    _stepper(c, 2)
    c.text(R, 140, "اختر اسم مستخدم", 24, "x", INK, "r")
    c.text(R, 172, "يعرفك به الناس ويمكنك تغييره لاحقاً", 13.5, "r", MUTED, "r")
    y = c.input(206, "اسم المستخدم", "at", value="sara.ali", focus=True, trailing="check")
    c.icon("check", L + 24, 232, 21, SUCCESS)
    c.text(R, y + 22, "متاح - sara.ali", 12.5, "m", SUCCESS, "r")
    c.text(R, y + 60, "تاريخ الميلاد", 14, "b", INK, "r")
    c.rr(L, y + 80, R, y + 230, 18, fill=BG2)
    cols = [("اليوم", ["14", "15", "16", "17", "18"]), ("الشهر", ["أيار", "حزيران", "تموز", "آب", "أيلول"]), ("السنة", ["2000", "2001", "2002", "2003", "2004"])]
    for ci, (lab, vals) in enumerate(cols):
        cx = R - 55 - ci * 118
        for vi, v in enumerate(vals):
            sel = vi == 2
            c.text(cx, y + 100 + vi * 26, v, 17 if sel else 13.5, "b" if sel else "r", INK if sel else FAINT, "c", rtl=not v.isdigit())
    c.rr(L + 10, y + 150, R - 10, y + 180, 10, fill="#0FB5A01F") if False else c.hline(L + 14, R - 14, y + 140, LINE)
    c.hline(L + 14, R - 14, y + 166, LINE)
    c.text(R, y + 262, "اقتراحات متاحة:", 12.5, "m", MUTED, "r")
    x = R
    for t in ["sara_ali", "sara.2000", "sara_baghdad"]:
        x = c.chip(x, y + 276, t, False, h=32, size=12, pad=22)
    c.btn(y + 330, "التالي")
    return c


def signup4():
    c = K(BG)
    _stepper(c, 3)
    c.text(R, 140, "أضف صورتك", 24, "x", INK, "r")
    c.text(R, 172, "حتى يعرفك أصحابك بسهولة", 13.5, "r", MUTED, "r")
    c.avatar(180, 290, 66, "س", 0, ring="new")
    c.circle(232, 340, 18, fill=PRIMARY); c.icon("camera", 232, 340, 20, "#FFFFFF", bg=PRIMARY)
    c.text(180, 396, "سارة علي", 18, "b", INK, "c")
    c.text(180, 420, "@sara.ali", 13, "r", MUTED, "c", rtl=False)
    c.btn(466, "اختيار من المعرض", icon="image")
    c.btn(526, "التقاط صورة", kind="line", icon="camera")
    c.text(180, 622, "تخطي الآن", 14, "b", MUTED, "c")
    c.text(180, 700, "بالتسجيل أنت توافق على الشروط وسياسة الخصوصية", 11.5, "r", FAINT, "c")
    return c


SCREENS = [
    ("01_splash", "شاشة البداية", splash),
    ("02_welcome1", "ترحيب 1", lambda: welcome(0)),
    ("03_welcome2", "ترحيب 2", lambda: welcome(1)),
    ("04_welcome3", "ترحيب 3", lambda: welcome(2)),
    ("05_login", "تسجيل الدخول", login),
    ("06_login_error", "خطأ بالدخول", login_error),
    ("07_forgot", "استعادة الحساب", forgot),
    ("08_otp", "رمز التحقق", otp),
    ("09_signup1", "تسجيل 1: الهاتف", signup1),
    ("10_signup2", "تسجيل 2: الاسم", signup2),
    ("11_signup3", "تسجيل 3: المستخدم", signup3),
    ("12_signup4", "تسجيل 4: الصورة", signup4),
]
