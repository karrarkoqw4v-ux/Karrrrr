"""البروفايل والإعدادات والنوافذ السفلية (16 شاشة)."""
from kit import K, mix
from tokens import *
from feed import feed, post

R = W - PAD
L = PAD


def stats(c, y, items):
    for i, (n, t) in enumerate(items):
        cx = 212 - i * 74
        c.text(cx, y, n, 18, "x", INK, "c", rtl=False)
        c.text(cx, y + 22, t, 11.5, "m", MUTED, "c")


def profile_grid(c, y0, rows=3):
    cell = (W - 4) / 3
    for i in range(rows * 3):
        r_, cc = divmod(i, 3)
        xr = W - cc * (cell + 2)
        yy = y0 + r_ * (cell + 2)
        c.photo(xr - cell, yy, xr, yy + cell, i)
        if i == 1:
            c.icon("reel", xr - 16, yy + 16, 16, "#FFFFFF", bg="#000000")


def my_profile():
    c = K(BG)
    c.text(R, 50, "sara.ali", 19, "x", INK, "r", rtl=False)
    c.icon("plus", L + 14, 50, 26, INK); c.icon("sliders", L + 54, 50, 24, INK)
    c.avatar(R - 40, 122, 40, "س", 0, ring="new")
    stats(c, 106 if False else 108, [("128", "منشور"), ("2.4K", "متابِع"), ("310", "يتابع")])
    c.text(R, 190, "سارة علي", 15, "b", INK, "r")
    c.text(R, 212, "مصورة هاوية - بغداد", 13, "r", INK, "r")
    c.text(R, 232, "أشارك لحظات من مدينتي", 13, "r", MUTED, "r")
    bw = (R - L - 16) / 2
    c.rr(L, 252, L + bw, 288, 12, fill=BG2); c.text(L + bw / 2, 270, "تعديل الملف", 13.5, "b", INK, "c")
    c.rr(L + bw + 8, 252, R - 0 - 0 if False else L + 2 * bw + 8, 288, 12, fill=BG2); c.text(L + bw + 8 + bw / 2, 270, "مشاركة الملف", 13.5, "b", INK, "c")
    for i, nm in enumerate(["رحلات", "عائلة", "طعام", "جديد"]):
        cx = R - 30 - i * 74
        if nm == "جديد":
            c.circle(cx, 336, 27, fill=BG2); c.icon("plus", cx, 336, 24, MUTED, bg=BG2)
        else:
            c.photo(cx - 27, 309, cx + 27, 363, i, r=27)
        c.text(cx, 378, nm, 11.5, "m", INK, "c")
    c.hline(0, W, 398, LINE)
    for i, ic in enumerate(["grid", "reel", "tag"]):
        cx = R - 44 - i * 98 if False else 300 - i * 120
        c.icon(ic, cx, 420, 24, INK if i == 0 else FAINT)
    c.rr(300 - 18, 436, 300 + 18, 439, 1.5, fill=INK)
    profile_grid(c, 446, 2)
    c.tabbar(4)
    return c


def edit_profile():
    c = K(BG)
    c.icon("close", R - 10, 50, 25, INK)
    c.text(180, 50, "تعديل الملف", 17, "b", INK, "c")
    c.icon("check", L + 12, 50, 26, PRIMARY_DARK, sw=2.4)
    c.avatar(180, 140, 46, "س", 0)
    c.text(180, 208, "تغيير الصورة", 14, "b", PRIMARY_DARK, "c")
    y = 232
    for lab, val in [("الاسم", "سارة علي"), ("اسم المستخدم", "sara.ali"), ("النبذة", "مصورة هاوية - بغداد"), ("الرابط", "instagram.com/..." if False else "quiz.app/sara")]:
        c.text(R, y, lab, 12, "m", MUTED, "r")
        c.text(R, y + 24, val, 15, "r", INK, "r", rtl=lab not in ("اسم المستخدم", "الرابط"))
        c.hline(L, R, y + 42, LINE)
        y += 66
    y = c.row(y + 4, "حساب خاص", icon="lock", toggle=False, h=54)
    y = c.row(y, "تبديل إلى حساب احترافي", icon="sparkle", h=54)
    return c


def other_profile():
    c = K(BG)
    c.back("kareem_a", left=["dots"])
    c.avatar(R - 40, 138, 40, "ك", 5, ring="new")
    stats(c, 124, [("86", "منشور"), ("1.1K", "متابِع"), ("402", "يتابع")])
    c.text(R, 206, "كرار الموسوي", 15, "b", INK, "r")
    c.text(R, 228, "مهندس - أحب السفر والتصوير", 13, "r", INK, "r")
    c.runs(R, 252, [("يتابعه ", MUTED, "r"), ("سارة", INK, "b"), (" و 5 آخرين", MUTED, "r")], 12)
    c.btn(274, "متابعة", x0=L + 100 + 8, x1=R, h=40) if False else None
    c.grad(R - 150, 274, R, 314, PRIMARY, PRIMARY_DARK, r=12, horizontal=True); c.text(R - 75, 294, "متابعة", 14, "b", "#FFFFFF", "c")
    c.rr(R - 300, 274, R - 160, 314, 12, fill=BG2); c.text(R - 230, 294, "مراسلة", 14, "b", INK, "c")
    c.rr(L, 274, L + 44, 314, 12, fill=BG2); c.icon("chev-d", L + 22, 294, 18, INK, bg=BG2)
    c.hline(0, W, 334, LINE)
    c.icon("grid", 300, 356, 24, INK); c.icon("reel", 180, 356, 24, FAINT); c.icon("tag", 60, 356, 24, FAINT)
    c.rr(282, 372, 318, 375, 1.5, fill=INK)
    profile_grid(c, 382, 3)
    c.tabbar(4)
    return c


def followers():
    c = K(BG)
    c.back("sara.ali")
    for i, (t, a) in enumerate([("2.4K متابِع", True), ("310 يتابع", False)]):
        xr = R - i * 170
        c.text(xr - 80, 92, t, 14, "b" if a else "m", INK if a else MUTED, "c")
    c.rr(R - 160, 110, R, 113, 1.5, fill=INK)
    c.rr(L, 124, R, 162, 19, fill=BG2); c.icon("search", R - 22, 143, 19, FAINT, bg=BG2); c.text(R - 44, 143, "بحث", 13.5, "r", FAINT, "r")
    y = 176
    for nm, idx, st in [("علي حسين", 1, "remove"), ("نور محمد", 2, "follow"), ("حسن كريم", 3, "remove"), ("مريم أحمد", 4, "remove"), ("كرار الموسوي", 5, "follow"), ("زينب علي", 6, "remove"), ("أحمد", 7, "remove")]:
        c.avatar(R - 24, y + 28, 24, nm, idx)
        c.text(R - 60, y + 20, nm, 14.5, "b", INK, "r"); c.text(R - 60, y + 40, "@" + nm.split()[0], 12, "r", MUTED, "r")
        if st == "follow":
            c.grad(L, y + 14, L + 84, y + 44, PRIMARY, PRIMARY_DARK, r=15, horizontal=True); c.text(L + 42, y + 29, "متابعة", 12.5, "b", "#FFFFFF", "c")
        else:
            c.rr(L, y + 14, L + 84, y + 44, 15, fill=BG2); c.text(L + 42, y + 29, "إزالة", 12.5, "b", INK, "c")
        y += 66
    return c


def saved():
    c = K(BG)
    c.back("المحفوظات", left=["plus"])
    c.text(R, 100, "كل المنشورات", 13, "b", MUTED, "r")
    cell = (W - 2 * PAD - 12) / 2
    names = [("كل المنشورات", 0), ("رحلات", 3), ("وصفات", 4), ("إلهام", 5)]
    for i, (nm, k) in enumerate(names):
        r_, cc = divmod(i, 2)
        xr = R - cc * (cell + 12)
        y0 = 116 + r_ * (cell + 56)
        c.photo(xr - cell, y0, xr, y0 + cell, k, r=18)
        c.text(xr - 4, y0 + cell + 22, nm, 14, "b", INK, "r")
    c.tabbar(4)
    return c


def settings():
    c = K(BG)
    c.back("الإعدادات")
    c.rr(L, 80, R, 122, 21, fill=BG2); c.icon("search", R - 22, 101, 19, FAINT, bg=BG2); c.text(R - 44, 101, "بحث", 13.5, "r", FAINT, "r")
    y = 134
    for ic, t, tone in [("user", "الحساب", "teal"), ("lock", "الخصوصية", "violet"), ("bell", "الإشعارات", "orange"), ("moon", "المظهر", "blue"), ("globe", "اللغة", "cyan"), ("shield", "الأمان", "green"), ("users", "الأصدقاء المقرّبون", "rose"), ("flag", "الحسابات المحظورة", "amber"), ("info", "المساعدة", "teal")]:
        y = c.row(y, t, icon=ic, tone=tone, h=56)
    c.text(R, y + 24, "تسجيل الخروج", 14.5, "b", DANGER, "r")
    return c


def privacy():
    c = K(BG)
    c.back("الخصوصية")
    y = 90
    for t, s, v in [("حساب خاص", "فقط من توافق عليه يشوف منشوراتك", False), ("حالة النشاط", "اعرض متى كنت متصل", True), ("إشعار القراءة", "اعرض علامة المشاهدة بالرسائل", True), ("السماح بالإشارة", "من يقدر يشير لحسابك", None)]:
        if v is None:
            y = c.row(y, t, sub=s, value="الكل", h=64)
        else:
            y = c.row(y, t, sub=s, toggle=v, h=64)
        c.hline(L, R, y, LINE)
    c.text(R, y + 28, "التفاعلات", 13, "b", MUTED, "r")
    y = c.row(y + 38, "الرسائل", sub="من يقدر يراسلك", value="الأصدقاء", h=64)
    y = c.row(y, "التعليقات", sub="فلترة الكلمات المسيئة", value="تلقائي", h=64)
    y = c.row(y, "المكالمات", sub="من يقدر يتصل بك", value="المتابَعون", h=64)
    return c


def notif_settings():
    c = K(BG)
    c.back("الإشعارات")
    c.text(R, 100, "إيقاف مؤقت للكل", 14, "b", INK, "r")
    c.rr(L, 88, L + 46, 114, 13, fill="#D5DCE2"); c.circle(L + 13, 101, 10, fill="#FFFFFF")
    c.hline(L, R, 128, LINE)
    y = 128
    for t, v in [("الإعجابات", True), ("التعليقات", True), ("المتابعون الجدد", True), ("الرسائل", True), ("المكالمات", True), ("القصص", False), ("الأخبار والعروض", False)]:
        y = c.row(y, t, toggle=v, h=56)
    return c


def appearance():
    c = K(BG)
    c.back("المظهر")
    c.rr(L, 88, R, 130, 14, fill=BG2)
    for i, (t, a) in enumerate([("تلقائي", False), ("داكن", False), ("فاتح", True)]):
        x1 = R - 4 - i * ((R - L - 8) / 3)
        w3 = (R - L - 8) / 3
        if a:
            c.rr(x1 - w3, 92, x1, 126, 11, fill=SURFACE)
        c.text(x1 - w3 / 2, 109, t, 13.5, "b" if a else "m", PRIMARY_DARK if a else MUTED, "c")
    # معاينة فاتح وداكن
    for k, (x0, dark, lab) in enumerate([(R - 160, False, "فاتح"), (L, True, "داكن")]):
        bgc = "#10181F" if dark else "#FFFFFF"
        fg = "#FFFFFF" if dark else INK
        c.rr(x0, 150, x0 + 160, 470, 22, fill=bgc, line=PRIMARY if not dark else LINE, lw=2.4 if not dark else 1.2)
        c.avatar(x0 + 130, 184, 14, "س", 0)
        c.rr(x0 + 70, 172, x0 + 114, 178, 3, fill="#5D6B78" if dark else "#C9D2D9"); c.rr(x0 + 84, 190, x0 + 114, 195, 2.5, fill="#3A4650" if dark else "#E3E8EC")
        c.photo(x0 + 12, 214, x0 + 148, 330, 0 if not dark else 2, r=12)
        c.circle(x0 + 20, 352, 6, fill=LIKE); c.rr(x0 + 36, 348, x0 + 100, 356, 4, fill="#3A4650" if dark else "#E3E8EC")
        c.rr(x0 + 12, 380, x0 + 148, 396, 8, fill="#1B2630" if dark else BG2)
        c.rr(x0 + 12, 408, x0 + 90, 424, 8, fill=PRIMARY)
        c.text(x0 + 80, 494, lab, 13.5, "b", INK, "c")
    c.text(R, 540, "اللون الأساسي", 14, "b", INK, "r")
    for i, col in enumerate([PRIMARY, BLUE, "#7C5CFF", "#FF7A59", "#E5484D"]):
        cx = R - 22 - i * 56
        c.circle(cx, 586, 20, fill=col)
        if i == 0:
            c.circle(cx, 586, 25, line=col, lw=2); c.icon("check", cx, 586, 18, "#FFFFFF", sw=2.5, bg=col)
    y = c.row(640, "حجم الخط", value="متوسط", h=56)
    return c


def language():
    c = K(BG)
    c.back("اللغة")
    y = 94
    for t, s, sel in [("العربية", "العراق", True), ("English", "United States", False), ("الكردية", "إقليم كردستان", False), ("Türkçe", "Türkiye", False), ("الفارسية", "إيران", False)]:
        c.text(R, y + 22, t, 15, "b", INK, "r", rtl=True) if False else c.text(R, y + 22, t, 15, "b", INK, "r", rtl=(t not in ("English", "Türkçe")))
        c.text(R, y + 44, s, 12, "r", MUTED, "r", rtl=(t not in ("English", "Türkçe")))
        if sel:
            c.circle(L + 14, y + 32, 12, fill=PRIMARY); c.icon("check", L + 14, y + 32, 15, "#FFFFFF", sw=2.5, bg=PRIMARY)
        else:
            c.circle(L + 14, y + 32, 12, line=LINE, lw=1.8)
        c.hline(L, R, y + 64, LINE)
        y += 66
    return c


def blocked():
    c = K(BG)
    c.back("الحسابات المحظورة")
    c.text(180, 100, "المحظورون ما يقدرون يشوفون منشوراتك أو يراسلونك", 12, "r", MUTED, "c")
    y = 124
    for nm, idx in [("بشار", 3), ("ياسر عادل", 7), ("مجهول", 1)]:
        c.avatar(R - 24, y + 28, 24, nm, idx)
        c.text(R - 60, y + 20, nm, 14.5, "b", INK, "r"); c.text(R - 60, y + 40, "@" + str(idx) + "user", 12, "r", MUTED, "r")
        c.rr(L, y + 14, L + 90, y + 44, 15, fill=BG2); c.text(L + 45, y + 29, "إلغاء الحظر", 12, "b", INK, "c")
        y += 66
    return c


def close_friends():
    c = K(BG)
    c.back("الأصدقاء المقرّبون", right="تم")
    c.rr(L, 80, R, 122, 21, fill=BG2); c.icon("search", R - 22, 101, 19, FAINT, bg=BG2); c.text(R - 44, 101, "بحث", 13.5, "r", FAINT, "r")
    c.rr(L, 136, R, 196, 16, fill=PRIMARY_SOFT)
    c.circle(R - 28, 166, 18, fill=SUCCESS); c.icon("users", R - 28, 166, 20, "#FFFFFF", bg=SUCCESS)
    c.text(R - 58, 156, "قصصك لأصدقائك المقرّبين فقط", 13.5, "b", INK, "r"); c.text(R - 58, 178, "هم الوحيدون الي يشوفونها", 12, "r", MUTED, "r")
    y = 210
    for nm, idx, sel in [("سارة علي", 0, True), ("علي حسين", 1, True), ("نور محمد", 2, False), ("حسن كريم", 3, True), ("مريم أحمد", 4, False), ("كرار الموسوي", 5, False), ("زينب", 6, False)]:
        c.avatar(R - 22, y + 26, 22, nm, idx)
        c.text(R - 56, y + 26, nm, 14.5, "m", INK, "r")
        if sel:
            c.circle(L + 14, y + 26, 12, fill=SUCCESS); c.icon("check", L + 14, y + 26, 15, "#FFFFFF", sw=2.5, bg=SUCCESS)
        else:
            c.circle(L + 14, y + 26, 12, line=LINE, lw=1.8)
        y += 56
    return c


def qr():
    c = K(PRIMARY)
    c.grad(0, 0, W, H, "#3B1275", "#0A0614")
    c.icon("close", R - 10, 56, 26, "#FFFFFF", bg="#3B1275")
    c.rr(48, 150, 312, 470, 28, fill="#FFFFFF")
    c.avatar(180, 196, 30, "س", 0)
    n = 21
    cell = 172 / n
    x0, y0 = 94, 238
    import random
    rnd = random.Random(7)
    for i in range(n):
        for j in range(n):
            finder = (i < 7 and j < 7) or (i < 7 and j >= n - 7) or (i >= n - 7 and j < 7)
            if finder:
                continue
            if rnd.random() < 0.5:
                c.rr(x0 + j * cell, y0 + i * cell, x0 + (j + 1) * cell - 0.6, y0 + (i + 1) * cell - 0.6, 1.2, fill=INK)
    for (fi, fj) in [(0, 0), (0, n - 7), (n - 7, 0)]:
        xx, yy = x0 + fj * cell, y0 + fi * cell
        c.rr(xx, yy, xx + 7 * cell, yy + 7 * cell, 3, fill=INK)
        c.rr(xx + cell, yy + cell, xx + 6 * cell, yy + 6 * cell, 2, fill="#FFFFFF")
        c.rr(xx + 2 * cell, yy + 2 * cell, xx + 5 * cell, yy + 5 * cell, 2, fill=PRIMARY_DARK)
    c.text(180, 440, "@sara.ali", 15, "b", INK, "c", rtl=False)
    c.text(180, 510, "اجعل أصحابك يمسحون الرمز حتى يتابعونك", 13, "r", "#D6F5F0", "c")
    c.rr(80, 560, 180, 604, 22, fill="#FFFFFF"); c.icon("share", 150, 582, 20, PRIMARY_DARK); c.text(122, 582, "مشاركة", 13, "b", PRIMARY_DARK, "c")
    c.rr(190, 560, 280, 604, 22, fill=mix("#3B1275", "#FFFFFF", 0.2)); c.icon("camera", 258, 582, 20, "#FFFFFF", bg=mix("#3B1275", "#FFFFFF", 0.2)); c.text(222, 582, "مسح", 13, "b", "#FFFFFF", "c")
    return c


def share_sheet():
    c = feed()
    c.dim(0.5)
    c.sheet_handle(330)
    c.rr(R - 280, 358, R, 396, 19, fill=BG2); c.icon("search", R - 22, 377, 18, FAINT, bg=BG2); c.text(R - 44, 377, "بحث", 13, "r", FAINT, "r")
    for i, (nm, idx) in enumerate([("سارة", 0), ("علي", 1), ("نور", 2), ("حسن", 3), ("مريم", 4), ("كرار", 5), ("زينب", 6), ("أحمد", 7)]):
        r_, cc = divmod(i, 4)
        cx = R - 36 - cc * 84
        cy = 448 + r_ * 92
        c.avatar(cx, cy, 28, nm, idx)
        c.text(cx, cy + 42, nm, 11.5, "m", INK, "c")
    c.hline(0, W, 612, LINE)
    for i, (ic, t) in enumerate([("copy", "نسخ الرابط"), ("share", "مشاركة إلى"), ("plus", "للقصة"), ("bookmark", "حفظ")]):
        cx = R - 36 - i * 84
        c.circle(cx, 656, 24, fill=BG2); c.icon(ic, cx, 656, 22, INK, bg=BG2)
        c.text(cx, 698, t, 11, "m", INK, "c")
    return c


def post_options():
    c = feed()
    c.dim(0.5)
    c.sheet_handle(380)
    y = 408
    for ic, t, col in [("bookmark", "حفظ", INK), ("copy", "نسخ الرابط", INK), ("users", "إضافة للأصدقاء المقرّبين", INK), ("bell", "كتم هذا الحساب", INK), ("eye", "غير مهتم", INK), ("flag", "إبلاغ", DANGER)]:
        c.icon(ic, R - 12, y + 22, 23, col)
        c.text(R - 40, y + 22, t, 14.5, "m" if col == INK else "b", col, "r")
        y += 52
    return c


def account_switch():
    c = my_profile()
    c.dim(0.5)
    c.sheet_handle(430)
    c.text(180, 468, "تبديل الحساب", 16, "b", INK, "c")
    y = 490
    for nm, un, idx, cur in [("سارة علي", "sara.ali", 0, True), ("متجر سارة", "sara.shop", 4, False)]:
        c.avatar(R - 24, y + 28, 24, nm, idx)
        c.text(R - 60, y + 20, nm, 14.5, "b", INK, "r"); c.text(R - 60, y + 40, "@" + un, 12, "r", MUTED, "r", rtl=False)
        if cur:
            c.circle(L + 14, y + 28, 12, fill=PRIMARY); c.icon("check", L + 14, y + 28, 15, "#FFFFFF", sw=2.5, bg=PRIMARY)
        else:
            c.circle(L + 14, y + 28, 12, line=LINE, lw=1.8)
        y += 62
    c.circle(R - 24, y + 28, 24, fill=BG2); c.icon("plus", R - 24, y + 28, 22, INK, bg=BG2)
    c.text(R - 60, y + 28, "إضافة حساب", 14.5, "b", PRIMARY_DARK, "r")
    return c


SCREENS = [
    ("39_my_profile", "ملفي الشخصي", my_profile),
    ("40_edit_profile", "تعديل الملف", edit_profile),
    ("41_other_profile", "ملف شخص آخر", other_profile),
    ("42_followers", "المتابعون", followers),
    ("43_saved", "المحفوظات", saved),
    ("44_settings", "الإعدادات", settings),
    ("45_privacy", "الخصوصية", privacy),
    ("46_notif_settings", "إعدادات الإشعارات", notif_settings),
    ("47_appearance", "المظهر", appearance),
    ("48_language", "اللغة", language),
    ("49_blocked", "المحظورون", blocked),
    ("50_close_friends", "الأصدقاء المقرّبون", close_friends),
    ("51_qr", "رمز QR", qr),
    ("52_share", "نافذة المشاركة", share_sheet),
    ("53_post_options", "خيارات المنشور", post_options),
    ("54_switch_account", "تبديل الحساب", account_switch),
]
