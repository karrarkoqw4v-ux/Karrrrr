"""الدردشة والمكالمات (14 شاشة)."""
from kit import K, mix
from tokens import *

R = W - PAD
L = PAD


def bubble(c, y, text, mine, ticks=None, w=None, quote=None):
    wd = w or min(250, c.width(text, 14.5, "r") + 30)
    h = 40 + (30 if quote else 0)
    if mine:
        x1, x0 = R, R - wd
        c.grad(x0, y, x1, y + h, PRIMARY, PRIMARY_DARK, r=20, horizontal=True)
        col = "#FFFFFF"
    else:
        x0, x1 = L, L + wd
        c.rr(x0, y, x1, y + h, 20, fill=BG2)
        col = INK
    ty = y + h / 2 + (13 if quote else 0)
    if quote:
        c.rr(x1 - wd + 10 if mine else x0 + 10, y + 8, (x1 - 10) if mine else x1 - 10, y + 32, 8, fill=mix(PRIMARY, "#FFFFFF", 0.35) if mine else "#E1E7EB")
        c.text((x1 - 18) if mine else (x1 - 18), y + 20, quote, 11.5, "r", "#FFFFFF" if mine else MUTED, "r")
    c.text(x1 - 15, ty, text, 14.5, "r", col, "r")
    if ticks:
        c.icon("check2", x0 - 12, y + h - 6, 13, PRIMARY if ticks == "read" else FAINT, sw=2.2)
    return y + h + 8


def header_chat(c, name="سارة علي", idx=0, status="متصل الآن", online=True):
    c.icon("arrow-r", R - 10, 52, 24, INK)
    c.avatar(R - 52, 52, 19, name, idx, online=online)
    c.text(R - 82, 44, name, 15, "b", INK, "r")
    c.text(R - 82, 62, status, 11.5, "m", SUCCESS if online else MUTED, "r")
    c.icon("video", L + 62, 52, 25, INK)
    c.icon("call", L + 22, 52, 24, INK)
    c.hline(0, W, 84, LINE)


def input_bar(c, y=704, text=None, recording=False):
    c.rr(L, y, R, y + 48, 24, fill=BG2)
    c.grad(R - 40, y + 6, R - 4, y + 42, PRIMARY, PRIMARY_DARK, r=18, horizontal=True)
    c.icon("camera", R - 22, y + 24, 20, "#FFFFFF", bg=PRIMARY)
    if text:
        c.text(R - 52, y + 24, text, 14, "r", INK, "r")
        c.grad(L + 6, y + 6, L + 42, y + 42, PRIMARY, PRIMARY_DARK, r=18, horizontal=True)
        c.icon("send", L + 24, y + 24, 19, "#FFFFFF", bg=PRIMARY)
    else:
        c.text(R - 52, y + 24, "رسالة...", 14, "r", FAINT, "r")
        c.icon("mic", L + 22, y + 24, 22, INK, bg=BG2)
        c.icon("image", L + 56, y + 24, 22, INK, bg=BG2)
        c.icon("sticker", L + 90, y + 24, 22, INK, bg=BG2)


def inbox():
    c = K(BG)
    c.text(R, 52, "الرسائل", 24, "x", INK, "r")
    c.icon("pencil", L + 12, 52, 25, INK)
    c.rr(L, 80, R, 122, 21, fill=BG2)
    c.icon("search", R - 22, 101, 20, FAINT, bg=BG2)
    c.text(R - 44, 101, "بحث", 13.5, "r", FAINT, "r")
    for i, nm in enumerate(["سارة", "علي", "نور", "حسن", "مريم"]):
        cx = R - 28 - i * 66
        c.avatar(cx, 168, 26, nm, i, online=i < 3, ring="new" if i == 1 else None)
        c.text(cx, 210, nm, 11, "m", INK, "c")
    c.rr(R - 60, 240, R, 240, 0, fill=BG)
    c.text(R, 246, "الرسائل", 14, "b", INK, "r")
    c.text(L, 246, "الطلبات (3)", 13, "b", PRIMARY_DARK, "l")
    chats = [("سارة علي", 0, "شكراً حبيبتي، نلتقي باجر", "الآن", 2, True), ("المجموعة: شباب بغداد", 4, "علي: الاجتماع الساعة 8", "5 د", 5, False),
             ("نور محمد", 2, "أرسلت صورة", "12 د", 0, True), ("حسن كريم", 3, "تمام ", "1 س", 0, False), ("مريم أحمد", 5, "رسالة صوتية 0:24", "أمس", 0, False), ("كرار الموسوي", 6, "شلونك؟", "الاثنين", 0, False)]
    y = 266
    for nm, idx, last, t, unread, online in chats:
        c.avatar(R - 28, y + 32, 28, nm if nm[0] != "ا" else nm, idx, online=online)
        c.text(R - 68, y + 22, nm, 14.5, "b" if unread else "m", INK, "r")
        c.text(R - 68, y + 44, last, 12.5, "m" if unread else "r", INK if unread else MUTED, "r")
        c.text(L, y + 22, t, 11.5, "r", PRIMARY_DARK if unread else FAINT, "l")
        if unread:
            c.circle(L + 10, y + 46, 10, fill=PRIMARY); c.text(L + 10, y + 46, str(unread), 11, "b", "#FFFFFF", "c", rtl=False)
        y += 70
    c.tabbar(1)
    return c


def chat():
    c = K(BG)
    header_chat(c)
    c.rr(150, 98, 210, 120, 11, fill=BG2); c.text(180, 109, "اليوم", 11, "m", MUTED, "c")
    y = 134
    y = bubble(c, y, "هلا سارة، شلونك؟", True, "read")
    y = bubble(c, y, "هلا بيك! تمام والحمد لله", False)
    y = bubble(c, y, "اشتقتلك والله، متى نلتقي؟", False)
    y = bubble(c, y, "باجر بعد الدوام ان شاء الله", True, "read")
    y = bubble(c, y, "ماشي، نشوف بعضنا بالمقهى", False)
    y = bubble(c, y, "تمام، اتفقنا", True, "read")
    c.text(R, y + 4, "تمت المشاهدة 10:42", 11, "r", FAINT, "r")
    c.rr(L, y + 28, L + 70, y + 62, 17, fill=BG2)
    for i in range(3):
        c.circle(L + 20 + i * 15, y + 45, 3.6, fill=FAINT if i != 1 else MUTED)
    input_bar(c)
    return c


def chat_media():
    c = K(BG)
    header_chat(c, "نور محمد", 2, "نشطة قبل 5 د", online=False)
    y = 104
    c.photo(L, y, L + 220, y + 190, 3, r=16)
    c.text(L + 4, y + 204, "10:31", 10.5, "r", FAINT, "l")
    c.circle(L + 200, y + 180, 14, fill=SURFACE); c.circle(L + 200, y + 180, 13, fill="#FFFFFF", line=LINE); c.icon("heart-fill", L + 200, y + 180, 14, LIKE)
    y += 222
    y = bubble(c, y, "شكراً على الصورة، حلوة!", True, "read", quote="صورة")
    y = bubble(c, y + 4, "التقطتها اليوم بالصباح", False)
    # قائمة ضغط مطول
    c.rr(L + 6, 450, R - 70, 504, 27, fill=SURFACE, line=LINE); c.shadow(L + 6, 450, R - 70, 504, r=27, blur=8, dy=3, alpha=36)
    c.rr(L + 6, 450, R - 70, 504, 27, fill=SURFACE, line=LINE)
    for i, ic in enumerate(["heart-fill", "smile", "sparkle", "flag", "plus"]):
        col = LIKE if ic == "heart-fill" else (ACCENT if ic in ("smile", "sparkle") else MUTED)
        c.icon(ic, L + 36 + i * 46, 477, 24, col)
    c.rr(L + 6, 516, R - 70, 700, 18, fill=SURFACE, line=LINE)
    for i, (ic, t, col) in enumerate([("chat", "رد", INK), ("copy", "نسخ", INK), ("share", "إعادة توجيه", INK), ("bookmark", "تمييز", INK), ("trash", "إلغاء الإرسال", DANGER)]):
        yy = 540 + i * 34
        c.icon(ic, R - 90, yy, 19, col)
        c.text(R - 114, yy, t, 13.5, "m", col, "r")
    return c


def voice_rec():
    c = K(BG)
    header_chat(c)
    y = 110
    y = bubble(c, y, "ابعثلي تفاصيل الموعد", False)
    # رسالة صوتية مرسلة
    c.grad(R - 230, y, R, y + 48, PRIMARY, PRIMARY_DARK, r=24, horizontal=True)
    c.circle(R - 206, y + 24, 16, fill="#FFFFFF"); c.icon("play", R - 205, y + 24, 16, PRIMARY_DARK, bg="#FFFFFF")
    for i in range(22):
        h_ = 4 + (i * 11) % 14
        c.rr(R - 178 + i * 7, y + 24 - h_ / 2, R - 175 + i * 7, y + 24 + h_ / 2, 1.5, fill="#FFFFFF")
    c.text(L + 64 + 4, y + 24, "0:24", 11, "r", "#FFFFFF", "l", rtl=False) if False else c.text(R - 236, y + 56, "0:24", 10.5, "r", FAINT, "r", rtl=False)
    # شريط التسجيل
    c.rr(L, 692, R, 748, 28, fill="#FFECEE")
    c.circle(R - 28, 720, 9, fill=DANGER)
    c.text(R - 50, 720, "0:07", 16, "b", DANGER, "r", rtl=False)
    for i in range(18):
        h_ = 4 + (i * 13) % 20
        c.rr(R - 110 - i * 7, 720 - h_ / 2, R - 107 - i * 7, 720 + h_ / 2, 1.5, fill=DANGER)
    c.text(L + 60, 720, "اسحب للإلغاء", 12, "m", MUTED, "l")
    c.icon("chev-r", L + 44, 720, 16, MUTED)
    c.circle(L + 20, 720, 20, fill=PRIMARY); c.icon("mic", L + 20, 720, 22, "#FFFFFF", bg=PRIMARY)
    c.icon("lock", L + 20, 664, 20, MUTED)
    c.rr(L + 8, 646, L + 32, 684, 12, line=LINE, lw=1.2)
    return c


def new_message():
    c = K(BG)
    c.icon("close", R - 10, 52, 25, INK)
    c.text(180, 52, "رسالة جديدة", 17, "b", INK, "c")
    c.text(L + 4, 52, "دردشة", 14.5, "b", PRIMARY_DARK, "l")
    c.hline(0, W, 84, LINE)
    c.text(R, 112, "إلى:", 14, "b", INK, "r")
    x = R - 38
    for nm, idx in [("سارة", 0), ("علي", 1)]:
        wd = c.width(nm, 13, "m") + 36
        c.rr(x - wd, 98, x, 126, 14, fill=PRIMARY_SOFT)
        c.text(x - 12, 112, nm, 13, "m", PRIMARY_DARK, "r"); c.icon("close", x - wd + 12, 112, 12, PRIMARY_DARK, bg=PRIMARY_SOFT)
        x -= wd + 8
    c.hline(0, W, 142, LINE)
    c.row(150, "إنشاء مجموعة", icon="users", tone="teal", h=56)
    c.text(R, 226, "مقترح", 13, "b", MUTED, "r")
    ppl = [("سارة علي", 0, True), ("علي حسين", 1, True), ("نور محمد", 2, False), ("حسن كريم", 3, False), ("مريم أحمد", 4, False), ("كرار الموسوي", 5, False), ("زينب", 6, False)]
    y = 242
    for nm, idx, sel in ppl:
        c.avatar(R - 24, y + 28, 24, nm, idx)
        c.text(R - 60, y + 22, nm, 14.5, "b", INK, "r"); c.text(R - 60, y + 42, "@" + nm.split()[0], 12, "r", MUTED, "r")
        if sel:
            c.circle(L + 14, y + 28, 12, fill=PRIMARY); c.icon("check", L + 14, y + 28, 15, "#FFFFFF", sw=2.5, bg=PRIMARY)
        else:
            c.circle(L + 14, y + 28, 12, line=LINE, lw=1.8)
        y += 62
    return c


def group_create():
    c = K(BG)
    c.back("مجموعة جديدة", right="إنشاء")
    c.avatar(R - 36, 130, 34, "ش", 4)
    c.circle(R - 8, 152, 13, fill=PRIMARY, line="#FFFFFF", lw=2); c.icon("camera", R - 8, 152, 14, "#FFFFFF", bg=PRIMARY)
    c.input(106, "اسم المجموعة", value="شباب بغداد", x0=L, x1=R - 86, focus=True)
    c.text(R, 214, "الأعضاء (4)", 14, "b", INK, "r")
    for i, nm in enumerate(["سارة", "علي", "نور", "حسن"]):
        cx = R - 30 - i * 70
        c.avatar(cx, 266, 26, nm, i)
        c.circle(cx - 20, 244, 10, fill="#0E1A24"); c.icon("close", cx - 20, 244, 11, "#FFFFFF", bg="#0E1A24")
        c.text(cx, 308, nm, 11.5, "m", INK, "c")
    c.text(R, 350, "إضافة أعضاء", 14, "b", INK, "r")
    y = 366
    for nm, idx in [("مريم أحمد", 4), ("كرار الموسوي", 5), ("زينب علي", 6)]:
        c.avatar(R - 22, y + 26, 22, nm, idx)
        c.text(R - 56, y + 26, nm, 14.5, "m", INK, "r")
        c.circle(L + 14, y + 26, 12, line=LINE, lw=1.8)
        y += 58
    c.hline(L, R, y + 8, LINE)
    c.row(y + 16, "رسائل مؤقتة", icon="clock" if False else "bell", toggle=False, h=54)
    return c


def group_chat():
    c = K(BG)
    c.icon("arrow-r", R - 10, 52, 24, INK)
    for i, (nm, idx) in enumerate([("س", 0), ("ع", 1), ("ن", 2)]):
        c.avatar(R - 40 - i * 14, 52, 14, nm, idx)
    c.text(R - 100, 44, "شباب بغداد", 15, "b", INK, "r")
    c.text(R - 100, 62, "سارة، علي، نور و 12 آخرين", 11.5, "r", MUTED, "r")
    c.icon("video", L + 62, 52, 25, INK); c.icon("call", L + 22, 52, 24, INK)
    c.hline(0, W, 84, LINE)
    y = 100
    msgs = [("علي", 1, "هلا شباب، اجتماع الليلة؟", False), ("سارة", 0, "إي الساعة 8 بالمقهى", False), (None, 4, "تمام، أنا أجي", True), ("نور", 2, "أجيب معي الحلويات", False), ("حسن", 3, "يا سلام!", False)]
    for nm, idx, t, mine in msgs:
        if mine:
            y = bubble(c, y, t, True, "read")
        else:
            c.avatar(L + 16, y + 20, 15, nm, idx)
            c.text(L + 40, y - 2, nm, 11, "b", MUTED, "l")
            wd = min(230, c.width(t, 14.5) + 30)
            c.rr(L + 38, y + 6, L + 38 + wd, y + 46, 20, fill=BG2)
            c.text(L + 38 + wd - 15, y + 26, t, 14.5, "r", INK, "r")
            y += 56
    c.rr(L + 38, y + 4, L + 38 + 196, y + 96, 18, fill=BG2)
    c.photo(L + 44, y + 10, L + 38 + 190, y + 90, 4, r=12)
    input_bar(c)
    return c


def chat_info():
    c = K(BG)
    c.back("التفاصيل")
    c.avatar(180, 150, 48, "س", 0, ring="new")
    c.text(180, 226, "سارة علي", 20, "x", INK, "c")
    c.text(180, 252, "@sara.ali", 13, "r", MUTED, "c")
    for i, (ic, t) in enumerate([("call", "صوت"), ("video", "فيديو"), ("user", "الملف"), ("search", "بحث")]):
        cx = 270 - i * 60 + 0
        cx = R - 38 - i * 78
        c.circle(cx, 316, 24, fill=PRIMARY_SOFT); c.icon(ic, cx, 316, 23, PRIMARY_DARK, bg=PRIMARY_SOFT)
        c.text(cx, 356, t, 11.5, "m", INK, "c")
    c.hline(L, R, 384, LINE)
    y = 388
    y = c.row(y, "الوسائط والروابط", icon="image", value="48", h=54)
    y = c.row(y, "كتم الإشعارات", icon="bell", toggle=False, h=54)
    y = c.row(y, "رسائل مؤقتة", icon="moon", value="إيقاف", h=54)
    y = c.row(y, "السمة", icon="sparkle", value="تركواز", h=54)
    c.hline(L, R, y + 4, LINE)
    y = c.row(y + 8, "حظر", icon="shield", h=54)
    y = c.row(y, "إبلاغ", icon="flag", h=54)
    c.text(R - 54, y - 54 + 27, "إبلاغ", 14.5, "m", DANGER, "r")
    return c


def requests():
    c = K(BG)
    c.back("طلبات الرسائل")
    c.text(180, 104, "هذي رسائل من ناس ما تتابعهم", 12.5, "r", MUTED, "c")
    y = 124
    for nm, idx, t in [("ياسر عادل", 7, "هلا، شفت صورك وحبيتها"), ("لمى", 1, "ممكن نتعرف؟"), ("بشار", 3, "هل ممكن أسألك سؤال؟")]:
        c.avatar(R - 28, y + 40, 28, nm, idx)
        c.text(R - 68, y + 26, nm, 14.5, "b", INK, "r"); c.text(R - 68, y + 48, t, 12.5, "r", MUTED, "r")
        c.grad(R - 68 - 100 + 100 - 100, y + 66, R - 68, y + 96, PRIMARY, PRIMARY_DARK, r=15, horizontal=True) if False else None
        c.grad(R - 168, y + 62, R - 68, y + 92, PRIMARY, PRIMARY_DARK, r=15, horizontal=True); c.text(R - 118, y + 77, "قبول", 12.5, "b", "#FFFFFF", "c")
        c.rr(R - 244, y + 62, R - 178, y + 92, 15, fill=BG2); c.text(R - 211, y + 77, "حذف", 12.5, "b", MUTED, "c")
        c.rr(R - 316 + 316 - 316, y + 62, R - 316 + 316 - 316, y + 62, 0) if False else None
        c.hline(L, R, y + 108, LINE)
        y += 114
    return c


def voice_call():
    c = K(PRIMARY)
    c.grad(0, 0, W, H, "#0FB5A0", "#06433F")
    c.circle(300, 140, 150, fill=mix("#0FB5A0", "#FFFFFF", 0.07)); c.circle(40, 600, 190, fill=mix("#0A6A63", "#FFFFFF", 0.05))
    c.icon("arrow-r", R - 10, 52, 24, "#FFFFFF", bg="#0FB5A0")
    c.icon("users", L + 14, 52, 24, "#FFFFFF", bg="#0FB5A0")
    for r_, a in [(110, 0.10), (86, 0.16)]:
        c.acircle(180, 270, r_, "#FFFFFF", a)
    c.avatar(180, 270, 64, "س", 0, ring="new")
    c.text(180, 380, "سارة علي", 24, "x", "#FFFFFF", "c")
    c.text(180, 412, "00:42", 16, "m", "#C9F2EC", "c", rtl=False)
    for i, (ic, t) in enumerate([("mic", "كتم"), ("speaker", "السماعة"), ("video", "فيديو")]):
        cx = R - 52 - i * 112
        c.acircle(cx, 600, 30, "#FFFFFF", 0.2); c.icon(ic, cx, 600, 26, "#FFFFFF", bg="#0FB5A0")
        c.text(cx, 646, t, 12, "m", "#E3F8F5", "c")
    c.circle(180, 700, 32, fill=DANGER); c.icon("call", 180, 700, 28, "#FFFFFF", bg=DANGER)
    return c


def video_call():
    c = K("#000000")
    c.photo(0, 0, W, H, 5)
    c.shade(0, 0, W, 150, "#000000", 0.5, 0.0)
    c.shade(0, H - 240, W, H, "#000000", 0.0, 0.7)
    c.icon("arrow-r", R - 10, 52, 24, "#FFFFFF", bg="#000000")
    c.text(180, 46, "سارة علي", 16, "b", "#FFFFFF", "c")
    c.text(180, 66, "02:15", 12.5, "m", "#E3F8F5", "c", rtl=False)
    c.rr(L, 100, L + 96, 236, 16, fill="#FFFFFF"); c.photo(L + 3, 103, L + 93, 233, 2, r=14)
    c.icon("camera", L + 80, 220, 14, "#FFFFFF", bg="#000000")
    for i, (ic, col, bgc) in enumerate([("mic", "#FFFFFF", None), ("camera", "#FFFFFF", None), ("sparkle", "#FFFFFF", None), ("call", "#FFFFFF", DANGER)]):
        cx = R - 36 - i * 78
        if bgc:
            c.circle(cx, 702, 30, fill=bgc)
        else:
            c.acircle(cx, 702, 28, "#FFFFFF", 0.25)
        c.icon(ic, cx, 702, 26, col, bg="#000000" if not bgc else bgc)
    return c


def incoming():
    c = K(PRIMARY)
    c.grad(0, 0, W, H, "#16273A", "#0A111A")
    for r_, a in [(150, 0.05), (115, 0.08), (86, 0.12)]:
        c.acircle(180, 250, r_, "#FFFFFF", a)
    c.avatar(180, 250, 62, "ك", 5, ring="new")
    c.text(180, 360, "كرار الموسوي", 24, "x", "#FFFFFF", "c")
    c.text(180, 394, "مكالمة فيديو واردة...", 15, "m", "#9EC9C3", "c")
    c.circle(R - 54, 640, 34, fill=DANGER); c.icon("close", R - 54, 640, 28, "#FFFFFF", bg=DANGER, sw=2.2)
    c.circle(L + 54, 640, 34, fill=SUCCESS); c.icon("video", L + 54, 640, 28, "#FFFFFF", bg=SUCCESS)
    c.text(R - 54, 696, "رفض", 12.5, "m", "#C9D6DB", "c"); c.text(L + 54, 696, "قبول", 12.5, "m", "#C9D6DB", "c")
    c.rr(112, 724, 248, 758, 17, fill="#1E2F42"); c.text(180, 741, "رد برسالة", 12.5, "m", "#C9D6DB", "c")
    return c


def shared_media():
    c = K(BG)
    c.back("الوسائط والروابط")
    c.rr(L, 80, R, 122, 14, fill=BG2)
    for i, (t, a) in enumerate([("ملفات", False), ("روابط", False), ("وسائط", True)]):
        x1 = R - 4 - i * ((R - L - 8) / 3)
        w3 = (R - L - 8) / 3
        if a:
            c.rr(x1 - w3, 84, x1, 118, 11, fill=SURFACE)
        c.text(x1 - w3 / 2, 101, t, 13.5, "b" if a else "m", PRIMARY_DARK if a else MUTED, "c")
    c.text(R, 148, "هذا الشهر", 13, "b", MUTED, "r")
    cell = (W - 6) / 3
    for i in range(12):
        r_, cc = divmod(i, 3)
        xr = W - cc * (cell + 2)
        y0 = 166 + r_ * (cell + 2)
        c.photo(xr - cell, y0, xr, y0 + cell, i)
        if i in (2, 7):
            c.icon("video", xr - 16, y0 + 16, 16, "#FFFFFF", bg="#000000")
    return c


def sticker_panel():
    c = K(BG)
    header_chat(c)
    y = 108
    y = bubble(c, y, "ابعثلي ملصق حلو", False)
    y = bubble(c, y, "تمام لحظة", True, "read")
    c.sheet_handle(396)
    c.rr(R - 280, 420, R, 458, 19, fill=BG2)
    for i, t in enumerate(["ملصقات", "GIF", "إيموجي"]):
        xr = R - 4 - i * 94
        if i == 0:
            c.rr(xr - 90, 424, xr, 454, 15, fill=SURFACE)
        c.text(xr - 45, 439, t, 13, "b" if i == 0 else "m", PRIMARY_DARK if i == 0 else MUTED, "c", rtl=(i != 1))
    ic = ["heart-fill", "smile", "sparkle", "music", "bolt", "star" if False else "bell", "heart", "moon", "camera", "flag", "check", "users"]
    cols = [LIKE, ACCENT, PRIMARY, BLUE, ACCENT, "#B04FD9", LIKE, "#6C63FF", "#FF7A59", DANGER, SUCCESS, PRIMARY_DARK]
    cell = (W - 2 * PAD - 18) / 4
    for i in range(12):
        r_, cc = divmod(i, 4)
        xr = R - cc * (cell + 6)
        y0 = 478 + r_ * (cell + 6)
        c.rr(xr - cell, y0, xr, y0 + cell, 18, fill=mix(cols[i], "#FFFFFF", 0.86))
        c.icon(ic[i], xr - cell / 2, y0 + cell / 2, 34, cols[i], bg=mix(cols[i], "#FFFFFF", 0.86), sw=1.9)
    return c


SCREENS = [
    ("25_inbox", "صندوق الرسائل", inbox),
    ("26_chat", "المحادثة", chat),
    ("27_chat_media", "صورة وتفاعل", chat_media),
    ("28_voice_rec", "تسجيل صوتي", voice_rec),
    ("29_new_message", "رسالة جديدة", new_message),
    ("30_group_create", "إنشاء مجموعة", group_create),
    ("31_group_chat", "محادثة جماعية", group_chat),
    ("32_chat_info", "تفاصيل المحادثة", chat_info),
    ("33_requests", "طلبات الرسائل", requests),
    ("34_voice_call", "مكالمة صوتية", voice_call),
    ("35_video_call", "مكالمة فيديو", video_call),
    ("36_incoming", "مكالمة واردة", incoming),
    ("37_shared_media", "الوسائط المشتركة", shared_media),
    ("38_stickers", "الملصقات", sticker_panel),
]
