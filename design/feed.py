"""الفيد والستوري والاستكشاف والمنشورات (11 شاشة)."""
from kit import K, mix
from tokens import *

R = W - PAD
L = PAD

NAMES = ["سارة", "علي", "نور", "حسن", "مريم", "كرار", "زينب", "أحمد"]


def topbar(c, y=44):
    c.logo(R - 16, y, 30)
    c.text(R - 40, y, "كويز تواصلكم", 19, "b", INK, "r")
    c.icon("heart", 28, y, 25, INK)
    c.icon("send", 66, y, 25, INK)
    c.circle(78, y - 9, 4.5, fill=LIKE)


def stories(c, y, n=7):
    labels = ["قصتك"] + NAMES
    for i in range(n):
        cx = R - 30 - i * 66
        if i == 0:
            c.avatar(cx, y, 26, "أ", 4)
            c.circle(cx + 19, y + 19, 10, fill=SURFACE); c.circle(cx + 19, y + 19, 8, fill=PRIMARY)
            c.icon("plus", cx + 19, y + 19, 12, "#FFFFFF", sw=2.6, bg=PRIMARY)
        else:
            c.avatar(cx, y, 26, NAMES[i - 1], i - 1, ring="new" if i < 5 else "seen")
        c.text(cx, y + 42, labels[i], 11, "m", INK if i < 5 else MUTED, "c")


def post(c, y, idx, name, kind, likes, caption, ago, img_h=250, liked=False, saved=False, comments="86", follow=True):
    c.avatar(R - 20, y + 26, 20, name, idx, ring="new" if idx % 2 == 0 else None)
    c.text(R - 52, y + 18, name, 14.5, "b", INK, "r")
    c.text(R - 52, y + 38, "بغداد - الكرخ", 11, "r", MUTED, "r")
    if follow:
        c.rr(L + 30, y + 12, L + 100, y + 40, 14, fill=PRIMARY_SOFT)
        c.text(L + 65, y + 26, "متابعة", 11.5, "b", PRIMARY_DARK, "c")
    c.icon("dots", L + 10, y + 26, 22, INK)
    iy = y + 54
    c.photo(0, iy, W, iy + img_h, kind)
    ay = iy + img_h + 24
    # أزرار بكلمات وأعداد
    c.icon("heart-fill" if liked else "heart", R - 14, ay, 27, LIKE if liked else INK, sw=1.9)
    c.text(R - 34, ay, likes, 12, "b", LIKE if liked else INK, "r", rtl=False)
    c.icon("chat", R - 100, ay, 26, INK, sw=1.9)
    c.text(R - 120, ay, comments, 12, "b", INK, "r", rtl=False)
    c.icon("send", R - 176, ay, 26, INK, sw=1.9)
    c.text(R - 196, ay, "شارك", 11, "b", INK, "r")
    c.icon("bookmark", L + 14, ay, 26, PRIMARY if saved else INK, sw=1.9)
    # سطر المعجبين: أفاتارات متداخلة + "أعجب بها فلان و N آخرون"
    for k in range(3):
        c.avatar(R - 11 - k * 13, ay + 34, 10, NAMES[(idx + k + 1) % 8], (idx + k + 1) % 8)
    n_other = max(0, int(likes.replace(",", "")) - 1)
    c.runs(R - 52, ay + 34, [("أعجب بها ", MUTED, "r"), (NAMES[(idx + 1) % 8], INK, "b"), (f" و {n_other:,} آخرون", MUTED, "r")], 12.5)
    c.runs(R, ay + 58, [(name + "  ", INK, "b"), (caption, INK, "r")], 13.5)
    c.text(R, ay + 82, "عرض كل التعليقات", 12, "m", MUTED, "r")
    c.text(R, ay + 102, ago, 11, "r", FAINT, "r")
    return ay + 118


def feed():
    c = K(BG)
    topbar(c)
    c.hline(0, W, 70, LINE)
    stories(c, 118)
    c.hline(0, W, 176, LINE)
    post(c, 184, 0, "سارة علي", 0, "1,284", "غروب الشمس على دجلة", "قبل ساعتين", img_h=270, liked=True, comments="86")
    c.tabbar(0)
    return c


def suggestions_band(c, y):
    c.rr(0, y, W, y + 190, 0, fill=BG2)
    c.text(R, y + 24, "اقتراحات لك", 15, "b", INK, "r")
    c.text(L, y + 24, "عرض الكل", 12.5, "b", PRIMARY, "l")
    for i, (nm, idx) in enumerate([("مريم أحمد", 4), ("كرار الموسوي", 5), ("زينب علي", 6)]):
        x1 = R - i * 128
        c.rr(x1 - 118, y + 44, x1, y + 178, 16, fill=BG, line=LINE)
        c.avatar(x1 - 59, y + 86, 26, nm, idx, ring="new")
        c.text(x1 - 59, y + 126, nm.split()[0], 13.5, "b", INK, "c")
        c.text(x1 - 59, y + 144, "مقترح لك", 11.5, "r", MUTED, "c")
        c.rr(x1 - 104, y + 154, x1 - 14, y + 172, 8, fill=PRIMARY)
        c.text(x1 - 59, y + 163, "متابعة", 11.5, "b", "#FFFFFF", "c")


def feed2():
    c = K(BG)
    topbar(c)
    c.hline(0, W, 70, LINE)
    y = post(c, 78, 3, "حسن كريم", 1, "932", "صباح الخير من الجبال", "قبل 5 ساعات", img_h=190, comments="41")
    suggestions_band(c, y + 6)
    c.tabbar(0)
    return c


def likers_sheet():
    c = feed()
    c.dim(0.55)
    c.sheet_handle(190)
    c.text(180, 228, "الإعجابات", 16, "b", INK, "c")
    c.hline(0, W, 250, LINE)
    y = 258
    for nm, un, idx, on in [("علي حسين", "ali.h", 1, False), ("نور محمد", "noor.m", 2, True), ("حسن كريم", "hasan.k", 3, False), ("مريم أحمد", "maryam.a", 4, False), ("كرار الموسوي", "karrar.m", 5, True), ("زينب علي", "zainab.a", 6, False)]:
        c.avatar(R - 26, y + 30, 22, nm, idx, ring="new")
        c.text(R - 62, y + 22, nm, 14.5, "b", INK, "r")
        c.text(R - 62, y + 44, "@" + un, 12, "r", MUTED, "r", rtl=False)
        if on:
            c.rr(L, y + 14, L + 92, y + 46, 10, fill=BG2, line=LINE); c.text(L + 46, y + 30, "متابَع", 12.5, "b", INK, "c")
        else:
            c.rr(L, y + 14, L + 92, y + 46, 10, fill=PRIMARY); c.text(L + 46, y + 30, "متابعة", 12.5, "b", "#FFFFFF", "c")
        y += 70
    return c


def comments_sheet():
    c = feed()
    c.dim(0.55)
    c.sheet_handle(200)
    c.text(180, 236, "التعليقات", 16, "b", INK, "c")
    c.hline(0, W, 256, LINE)
    items = [("نور", 2, "جميلة جداً! أي مكان؟", "5 د", "48"), ("علي", 1, "تصوير رائع 🔥".replace("🔥", ""), "12 د", "21"), ("مريم", 4, "نفسي أزور المكان هذا", "40 د", "9"), ("حسن", 3, "الألوان خرافية", "1 س", "3")]
    y = 270
    for nm, idx, t, ago, lk in items:
        c.avatar(R - 20, y + 22, 20, nm, idx)
        c.runs(R - 52, y + 12, [(nm + "  ", INK, "b"), (ago, FAINT, "r")], 12.5)
        c.text(R - 52, y + 34, t, 13.5, "r", INK, "r")
        c.text(R - 52, y + 56, "رد", 12, "b", MUTED, "r")
        c.icon("heart", L + 12, y + 28, 18, FAINT)
        c.text(L + 12, y + 50, lk, 11, "m", MUTED, "c", rtl=False)
        y += 80
    c.hline(0, W, 598, LINE)
    c.avatar(R - 18, 650, 18, "أ", 4)
    c.rr(R - 52 - 250, 628, R - 52, 672, 22, fill=BG2)
    c.text(R - 68, 650, "أضف تعليقاً...", 13.5, "r", FAINT, "r")
    c.icon("send", L + 22, 650, 24, PRIMARY)
    return c


def story_view():
    c = K("#000000")
    c.photo(0, 0, W, H, 0)
    c.shade(0, 0, W, 150, "#000000", 0.55, 0.0)
    c.shade(0, H - 150, W, H, "#000000", 0.0, 0.6)
    for i in range(4):
        xr = R - i * 84
        c.rr(xr - 78, 20, xr, 24, 2, fill="#FFFFFF") if i < 2 else c.arr(xr - 78, 20, xr, 24, 2, "#FFFFFF", 0.4)
    c.avatar(R - 18, 62, 18, "س", 0, ring="new")
    c.text(R - 46, 54, "سارة علي", 14, "b", "#FFFFFF", "r")
    c.text(R - 46, 74, "قبل ساعتين", 11.5, "r", "#E8F5F3", "r")
    c.icon("dots", L + 10, 62, 22, "#FFFFFF", bg="#000000")
    c.icon("close", L + 46, 62, 22, "#FFFFFF", bg="#000000")
    c.rr(L, 700, R - 100, 746, 23, fill="#FFFFFF22", line="#FFFFFF", lw=1.4) if False else c.rr(L, 700, R - 90, 746, 23, line="#FFFFFF", lw=1.4)
    c.text(R - 106, 723, "أرسل رسالة...", 13.5, "r", "#FFFFFF", "r")
    c.icon("heart", R - 56, 723, 26, "#FFFFFF", bg="#000000")
    c.icon("send", R - 16, 723, 26, "#FFFFFF", bg="#000000")
    return c


def story_create():
    c = K("#111A24")
    c.photo(0, 0, W, H, 2)
    c.shade(0, 0, W, 130, "#000000", 0.55, 0.0)
    c.shade(0, H - 220, W, H, "#000000", 0.0, 0.75)
    c.icon("close", R - 12, 56, 26, "#FFFFFF", bg="#000000")
    c.icon("bolt", L + 12, 56, 24, "#FFFFFF", bg="#000000")
    c.icon("sliders", L + 52, 56, 24, "#FFFFFF", bg="#000000")
    c.circle(180, 640, 38, line="#FFFFFF", lw=3.4); c.circle(180, 640, 29, fill="#FFFFFF")
    c.acircle(R - 20, 640, 24, "#FFFFFF", 0.25)
    c.icon("image", R - 20, 640, 24, "#FFFFFF", bg="#000000")
    c.acircle(L + 24, 640, 24, "#FFFFFF", 0.25)
    c.icon("sparkle", L + 24, 640, 22, "#FFFFFF", bg="#000000")
    x = 180
    for i, t in enumerate(["نص", "صورة", "فيديو", "بث"]):
        xx = 180 + (1 - i) * 76 + (1) * 0
        c.text(xx + 38 * 0, 710, t, 14, "b" if i == 1 else "m", "#FFFFFF" if i == 1 else "#B8C4CC", "c")
    c.circle(180, 730, 2.6, fill=ACCENT)
    return c


def explore():
    c = K(BG)
    c.rr(L, 28, R, 72, 22, fill=BG2)
    c.icon("search", R - 24, 50, 21, FAINT, bg=BG2)
    c.text(R - 46, 50, "ابحث عن ناس، وسوم، أماكن", 13.5, "r", FAINT, "r")
    x = R
    for i, t in enumerate(["لك", "رحلات", "طعام", "فن", "رياضة", "موسيقى"]):
        x = c.chip(x, 86, t, i == 0, h=32, size=12.5, pad=22)
    cell = (W - 4) / 3
    layout = [(0, 0, 1, 2, 0), (1, 0, 1, 1, 1), (2, 0, 1, 1, 3), (1, 1, 1, 1, 4), (2, 1, 1, 1, 5), (0, 2, 1, 1, 2), (1, 2, 1, 2, 1), (2, 2, 1, 1, 0), (0, 3, 1, 1, 3), (2, 3, 1, 1, 4), (1, 4, 1, 1, 5), (0, 4, 1, 1, 0), (2, 4, 1, 1, 2)]
    for col, row, cw, rh, kind in layout:
        xr = W - col * (cell + 2)
        x0, x1 = xr - cell, xr
        y0 = 132 + row * (cell + 2)
        y1 = y0 + rh * cell + (rh - 1) * 2
        c.photo(x0, y0, x1, y1, kind)
        if rh == 2:
            c.icon("reel", x1 - 18, y0 + 18, 18, "#FFFFFF", bg="#000000")
    c.tabbar(1)
    return c


def search_results():
    c = K(BG)
    c.rr(L + 38, 28, R, 72, 22, fill=BG2)
    c.icon("search", R - 24, 50, 21, FAINT, bg=BG2)
    c.text(R - 46, 50, "سارة", 14, "m", INK, "r")
    c.icon("close", L + 20, 50, 20, FAINT)
    x = R
    for i, t in enumerate(["الكل", "حسابات", "وسوم", "أماكن"]):
        x = c.chip(x, 90, t, i == 1, h=32, size=12.5, pad=24)
    people = [("سارة علي", "@sara.ali", 0, False), ("سارة محمد", "@sara.m", 6, True), ("سارة الجبوري", "@sara_j", 2, False), ("سارة كريم", "@sarakarim", 3, True), ("سارة حسين", "@sarah.h", 1, False), ("سارة العبيدي", "@s.obeidi", 5, False)]
    y = 140
    for nm, un, idx, fol in people:
        c.avatar(R - 24, y + 28, 24, nm, idx, ring="new" if idx % 2 == 0 else None)
        c.text(R - 60, y + 20, nm, 14.5, "b", INK, "r")
        c.text(R - 60, y + 40, un, 12, "r", MUTED, "r", rtl=False) if False else c.text(R - 60, y + 40, un, 12, "r", MUTED, "r")
        if fol:
            c.rr(L, y + 14, L + 84, y + 44, 15, fill=BG2); c.text(L + 42, y + 29, "متابَع", 12.5, "b", INK, "c")
        else:
            c.grad(L, y + 14, L + 84, y + 44, PRIMARY, PRIMARY_DARK, r=15, horizontal=True); c.text(L + 42, y + 29, "متابعة", 12.5, "b", "#FFFFFF", "c")
        y += 66
    c.tabbar(1)
    return c


def post_detail():
    c = K(BG)
    c.back("المنشور", left=["dots"])
    post(c, 78, 4, "مريم أحمد", 5, "3,402", "أجمل صباح بالحديقة", "قبل 3 ساعات", img_h=330, liked=True, saved=True)
    c.avatar(R - 16, 660, 16, "ن", 2)
    c.runs(R - 42, 652, [("نور  ", INK, "b"), ("شو هالجمال", INK, "r")], 13)
    c.text(R - 42, 672, "قبل ساعة", 11, "r", FAINT, "r")
    c.rr(L, 704, R, 748, 22, fill=BG2)
    c.text(R - 18, 726, "أضف تعليقاً...", 13.5, "r", FAINT, "r")
    c.text(L + 24, 726, "نشر", 13.5, "b", PRIMARY_DARK, "c")
    return c


def notifications():
    c = K(BG)
    c.text(R, 50, "النشاط", 24, "x", INK, "r")
    c.text(R, 96, "اليوم", 14, "b", INK, "r")
    items = [("علي", 1, "أعجب بمنشورك", "5 د", "photo", 0), ("نور", 2, "بدأت بمتابعتك", "20 د", "follow", None), ("مريم", 4, "علّقت: جميلة جداً!", "1 س", "photo", 3), ("حسن", 3, "ذكرك بتعليق", "2 س", "photo", 1)]
    y = 112
    for nm, idx, t, ago, kind, ph in items:
        c.avatar(R - 24, y + 28, 24, nm, idx)
        c.runs(R - 60, y + 22, [(nm + " ", INK, "b"), (t, INK, "r")], 13.5)
        c.text(R - 60, y + 42, ago, 11.5, "r", FAINT, "r")
        if kind == "photo":
            c.photo(L, y + 8, L + 48, y + 56, ph, r=8)
        else:
            c.grad(L, y + 14, L + 88, y + 44, PRIMARY, PRIMARY_DARK, r=15, horizontal=True); c.text(L + 44, y + 29, "متابعة", 12.5, "b", "#FFFFFF", "c")
        y += 70
    c.text(R, y + 14, "هذا الأسبوع", 14, "b", INK, "r")
    items2 = [("زينب", 6, "أعجبت بتعليقك", "أمس"), ("أحمد", 7, "بدأ بمتابعتك", "الاثنين"), ("كرار", 5, "شارك منشورك", "الأحد")]
    y += 30
    for nm, idx, t, ago in items2:
        c.avatar(R - 24, y + 26, 24, nm, idx)
        c.runs(R - 60, y + 20, [(nm + " ", INK, "b"), (t, INK, "r")], 13.5)
        c.text(R - 60, y + 40, ago, 11.5, "r", FAINT, "r")
        y += 62
    c.tabbar(3)
    return c


def create_post():
    c = K(BG)
    c.icon("close", R - 12, 50, 26, INK)
    c.text(180, 50, "منشور جديد", 17, "b", INK, "c")
    c.text(L + 4, 50, "التالي", 14.5, "b", PRIMARY_DARK, "l")
    c.photo(0, 80, W, 380, 1)
    c.rr(L, 340, L + 34, 366, 13, fill="#00000066") if False else None
    c.circle(R - 20, 354, 16, fill="#0E1A2499" if False else "#3A4650"); c.icon("grid", R - 20, 354, 16, "#FFFFFF", bg="#3A4650")
    c.text(R, 410, "الحديثة", 15, "b", INK, "r")
    c.icon("chev-d", R - 66, 410, 16, INK)
    c.rr(L, 396, L + 100, 424, 14, fill=BG2); c.icon("camera", L + 78, 410, 17, INK, bg=BG2); c.text(L + 36, 410, "كاميرا", 12.5, "b", INK, "c")
    cell = (W - 6) / 4
    for i in range(16):
        r_, cc = divmod(i, 4)
        xr = W - cc * (cell + 2)
        y0 = 438 + r_ * (cell + 2)
        c.photo(xr - cell, y0, xr, y0 + cell, i)
        if i in (0, 5, 6):
            n = [1, 2, 3][[0, 5, 6].index(i)]
            c.circle(xr - 14, y0 + 14, 10, fill=PRIMARY, line="#FFFFFF", lw=1.6); c.text(xr - 14, y0 + 14, str(n), 12, "b", "#FFFFFF", "c", rtl=False)
        else:
            c.circle(xr - 14, y0 + 14, 10, line="#FFFFFF", lw=1.6)
    return c


def post_editor():
    c = K(BG)
    c.back("منشور جديد", right="مشاركة")
    c.photo(R - 74, 84, R, 158, 1, r=10)
    c.text(R - 90, 108, "اكتب وصفاً...", 14, "r", FAINT, "r")
    c.text(R - 90, 134, "#بغداد #غروب", 13, "r", PRIMARY_DARK, "r")
    c.hline(L, R, 176, LINE)
    y = 176
    for ic, t, v in [("users", "الإشارة إلى أشخاص", ""), ("pin", "إضافة موقع", "بغداد"), ("music", "إضافة موسيقى", "")]:
        y = c.row(y, t, icon=ic, value=v or None, h=54)
    c.hline(L, R, y, LINE)
    c.text(R, y + 26, "المشاركة أيضاً", 13, "b", MUTED, "r")
    y = c.row(y + 36, "إضافة للقصة", icon="plus", toggle=True, h=52)
    y = c.row(y, "السماح بالتعليقات", icon="chat", toggle=True, h=52)
    y = c.row(y, "إخفاء عدد الإعجابات", icon="eye", toggle=False, h=52)
    c.text(R, y + 24, "الفلاتر", 13, "b", MUTED, "r")
    for i, nm in enumerate(["عادي", "دافئ", "بارد", "ذهبي", "أسود"]):
        xr = R - i * 64
        c.photo(xr - 56, y + 40, xr, y + 96, [1, 0, 3, 4, 2][i], r=10)
        c.text(xr - 28, y + 108, nm, 11, "b" if i == 0 else "m", PRIMARY_DARK if i == 0 else MUTED, "c")
    return c


def reels():
    c = K("#000000")
    c.photo(0, 0, W, H, 3)
    c.shade(0, H - 260, W, H, "#000000", 0.0, 0.72)
    c.text(R, 52, "ريلز", 22, "x", "#FFFFFF", "r")
    c.icon("camera", L + 12, 52, 26, "#FFFFFF", bg="#000000")
    ax = L + 24
    for i, (ic, lab) in enumerate([("heart", "12.4 ألف"), ("chat", "834"), ("send", "2.1 ألف"), ("dots-v", "")]):
        y = 400 + i * 66
        c.icon(ic, ax, y, 30, "#FFFFFF", bg="#000000")
        if lab:
            c.text(ax, y + 24, lab, 11, "b", "#FFFFFF", "c")
    c.avatar(R - 20, 660, 20, "ك", 5, ring="new")
    c.text(R - 52, 650, "كرار الموسوي", 14.5, "b", "#FFFFFF", "r")
    c.text(R - 52, 670, "قبل ساعة", 11, "r", "#E6EEF2", "r")
    c.rr(L, 644, L + 92, 676, 16, line="#FFFFFF", lw=1.4); c.text(L + 46, 660, "متابعة", 12.5, "b", "#FFFFFF", "c")
    c.text(R, 704, "رحلة الأهوار - أجمل ما شفت", 14, "m", "#FFFFFF", "r")
    c.icon("music", R - 8, 730, 16, "#FFFFFF", bg="#000000")
    c.text(R - 30, 730, "صوت أصلي - كرار", 12, "r", "#E6EEF2", "r")
    return c


SCREENS = [
    ("13_feed", "الرئيسية", feed),
    ("14_feed2", "اقتراحات المتابعة", feed2),
    ("15_comments", "التعليقات", comments_sheet),
    ("15b_likers", "قائمة المعجبين", likers_sheet),
    ("16_story_view", "عرض القصة", story_view),
    ("17_story_create", "إنشاء قصة", story_create),
    ("18_explore", "الاستكشاف", explore),
    ("19_search", "نتائج البحث", search_results),
    ("20_post_detail", "تفاصيل المنشور", post_detail),
    ("21_notifications", "النشاط", notifications),
    ("22_create_post", "منشور جديد", create_post),
    ("23_post_editor", "تحرير المنشور", post_editor),
    ("24_reels", "ريلز", reels),
]
