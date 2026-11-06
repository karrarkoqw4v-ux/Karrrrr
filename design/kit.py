"""أدوات الرسم لتطبيق «هلا»: أيقونات إضافية + مكونات (صور، أفاتار، شريط تبويب...)."""
import math
from PIL import Image, ImageDraw
from lib import Canvas as _C, font
from tokens import *


def hexrgb(h):
    return tuple(int(h[i:i + 2], 16) for i in (1, 3, 5))


def mix(a, b, t):
    ra, rb = hexrgb(a), hexrgb(b)
    return "#%02X%02X%02X" % tuple(int(ra[i] + (rb[i] - ra[i]) * t) for i in range(3))


EXTRA = {"heart", "heart-fill", "dots", "dots-v", "video", "call", "smile", "moon", "qr", "shield", "share", "flag",
         "music", "users", "reel", "sparkle", "pin", "check2", "sticker", "hash", "at", "tag"}


class K(_C):
    # كل النصوص أكبر بـ 10% وبحد أدنى 13 (للوضوح)
    @staticmethod
    def _fs(size):
        return max(13.0, size * 1.1)

    def text(self, x, y, s, size=14, w="r", color=INK, al="r", rtl=True):
        super().text(x, y, s, self._fs(size), w, color, al, rtl)

    def width(self, s, size=14, w="r", rtl=True):
        return super().width(s, self._fs(size), w, rtl)

    # ---------------- أيقونات ----------------
    def icon(self, name, cx, cy, size=24, color=INK, sw=1.75, bg=SURFACE):
        if name not in EXTRA:
            return super().icon(name, cx, cy, size, color, sw, bg)
        u = size / 24 * S
        d = self.d
        wpx = max(2, round(sw * size / 24 * S))

        def P(x, y):
            return (cx * S + (x - 12) * u, cy * S + (y - 12) * u)

        def ln(*pts, close=False):
            q = [P(*p) for p in pts]
            if close:
                q.append(q[0])
            d.line(q, fill=color, width=wpx, joint="curve")
            for p in (q if not close else q[:-1]):
                d.ellipse([p[0] - wpx / 2, p[1] - wpx / 2, p[0] + wpx / 2, p[1] + wpx / 2], fill=color)

        def box(x0, y0, x1, y1):
            a, b = P(x0, y0), P(x1, y1)
            return [a[0], a[1], b[0], b[1]]

        def rrect(x0, y0, x1, y1, r, fill=None):
            d.rounded_rectangle(box(x0, y0, x1, y1), radius=r * u, outline=color, width=wpx, fill=fill)

        def circ(x, y, r, fill=None):
            d.ellipse(box(x - r, y - r, x + r, y + r), outline=color, width=wpx, fill=fill)

        def arc(x0, y0, x1, y1, a, b):
            d.arc(box(x0, y0, x1, y1), a, b, fill=color, width=wpx)

        def dot(x, y, r=1.1):
            p = P(x, y)
            d.ellipse([p[0] - r * u, p[1] - r * u, p[0] + r * u, p[1] + r * u], fill=color)

        def heart_pts():
            pts = []
            for i in range(64):
                t = i / 64 * 2 * math.pi
                x = 16 * math.sin(t) ** 3
                y = 13 * math.cos(t) - 5 * math.cos(2 * t) - 2 * math.cos(3 * t) - math.cos(4 * t)
                pts.append((12 + x * 0.58, 11.2 - y * 0.55))
            return pts

        n = name
        if n in ("heart", "heart-fill"):
            pts = heart_pts()
            if n == "heart-fill":
                d.polygon([P(*p) for p in pts], fill=color)
            else:
                ln(*pts, close=True)
        elif n == "dots":
            dot(5, 12, 1.5); dot(12, 12, 1.5); dot(19, 12, 1.5)
        elif n == "dots-v":
            dot(12, 5, 1.5); dot(12, 12, 1.5); dot(12, 19, 1.5)
        elif n == "video":
            rrect(2.5, 6.5, 15, 17.5, 3); ln((15, 10.5), (21, 7.5), (21, 16.5), (15, 13.5), close=True)
        elif n == "call":
            ln((5.5, 3.5), (9, 3.5), (10.5, 8), (8.5, 10), (11, 14), (14, 15.5), (16, 13.5), (20.5, 15), (20.5, 18.5), (18, 21), (11, 18.5), (5.5, 13), (3, 6.5), close=True)
        elif n == "smile":
            circ(12, 12, 9); arc(7.5, 8, 16.5, 17, 20, 160); dot(9, 9.5, 1); dot(15, 9.5, 1)
        elif n == "moon":
            ln((20, 14.5), (17, 18.5), (12.5, 20), (8, 18.5), (5, 14.5), (4.5, 9.5), (7.5, 5), (11.5, 3.5), (10, 7), (10.5, 11.5), (14, 14.5), (17.5, 15), close=True)
        elif n == "qr":
            rrect(3, 3, 10, 10, 1.6); rrect(14, 3, 21, 10, 1.6); rrect(3, 14, 10, 21, 1.6)
            dot(15.5, 15.5); dot(19, 15.5); dot(15.5, 19); dot(19.5, 19.5); dot(12.5, 12.5, 0.9)
        elif n == "shield":
            ln((12, 3), (19, 6), (19, 12), (12, 21), (5, 12), (5, 6), close=True); ln((9, 12), (11.2, 14.2), (15, 10))
        elif n == "share":
            ln((12, 15), (12, 4)); ln((8, 7.5), (12, 4), (16, 7.5)); ln((6, 11), (6, 20), (18, 20), (18, 11))
        elif n == "flag":
            ln((5, 21), (5, 4)); ln((5, 4), (18, 4), (15, 8), (18, 12), (5, 12))
        elif n == "music":
            ln((9, 18), (9, 6), (19, 4), (19, 16)); circ(7, 18, 2.2); circ(17, 16, 2.2)
        elif n == "users":
            circ(9, 8, 3.5); arc(2.5, 13, 15.5, 26, 180, 360); circ(17, 9, 2.8); arc(14, 14, 22, 24, 200, 340)
        elif n == "reel":
            rrect(3, 3, 21, 21, 4.5); ln((3, 9), (21, 9)); ln((9, 3), (12, 9)); ln((15, 3), (18, 9)); ln((10, 12.5), (15, 15.5), (10, 18.5), close=True)
        elif n == "sparkle":
            ln((12, 3), (14, 10), (21, 12), (14, 14), (12, 21), (10, 14), (3, 12), (10, 10), close=True)
        elif n == "pin":
            ln((9, 3), (15, 3)); ln((10, 3), (10, 9), (7, 13), (17, 13), (14, 9), (14, 3)); ln((12, 13), (12, 21))
        elif n == "check2":
            ln((2.5, 12.5), (7, 17), (14.5, 8)); ln((10.5, 15), (12.5, 17), (21, 8))
        elif n == "sticker":
            rrect(4, 4, 20, 20, 5); ln((20, 13), (13, 20)); dot(9.5, 10, 1); dot(14.5, 10, 1); arc(8, 10, 16, 17, 20, 160)
        elif n == "hash":
            ln((9, 4), (7, 20)); ln((17, 4), (15, 20)); ln((4, 9), (20, 9)); ln((4, 15), (20, 15))
        elif n == "at":
            circ(12, 12, 4); arc(3, 3, 21, 21, 40, 330); ln((16, 8.5), (16, 14), (18.5, 15))
        elif n == "tag":
            ln((3, 12), (12, 3), (21, 3), (21, 12), (12, 21), close=True); dot(16.5, 7.5, 1.3)

    # ---------------- رسم عام ----------------
    def grad(self, x0, y0, x1, y1, c1, c2, r=0, horizontal=False, corners=None):
        w, h = int((x1 - x0) * S), int((y1 - y0) * S)
        tile = Image.new("RGB", (w, h))
        td = ImageDraw.Draw(tile)
        if horizontal:
            for i in range(w):
                td.line([i, 0, i, h], fill=mix(c1, c2, i / max(1, w - 1)))
        else:
            for j in range(h):
                td.line([0, j, w, j], fill=mix(c1, c2, j / max(1, h - 1)))
        mask = Image.new("L", (w, h), 0)
        ImageDraw.Draw(mask).rounded_rectangle([0, 0, w, h], radius=r * S, fill=255, corners=corners)
        self.img.paste(tile, (int(x0 * S), int(y0 * S)), mask)

    def shade(self, x0, y0, x1, y1, color, a0, a1):
        """تظليل شفاف متدرج عمودياً فوق الصورة (a0 أعلى, a1 أسفل: 0..1)"""
        w, h = int((x1 - x0) * S), int((y1 - y0) * S)
        mask = Image.new("L", (w, h))
        md = ImageDraw.Draw(mask)
        for j in range(h):
            md.line([0, j, w, j], fill=int(255 * (a0 + (a1 - a0) * j / max(1, h - 1))))
        self.img.paste(Image.new("RGB", (w, h), color), (int(x0 * S), int(y0 * S)), mask)

    def acircle(self, cx, cy, r, color, a):
        w = int(2 * r * S)
        mask = Image.new("L", (w, w), 0)
        ImageDraw.Draw(mask).ellipse([0, 0, w - 1, w - 1], fill=int(255 * a))
        self.img.paste(Image.new("RGB", (w, w), color), (int((cx - r) * S), int((cy - r) * S)), mask)

    def arr(self, x0, y0, x1, y1, rad, color, a):
        w, h = int((x1 - x0) * S), int((y1 - y0) * S)
        mask = Image.new("L", (w, h), 0)
        ImageDraw.Draw(mask).rounded_rectangle([0, 0, w - 1, h - 1], radius=rad * S, fill=int(255 * a))
        self.img.paste(Image.new("RGB", (w, h), color), (int(x0 * S), int(y0 * S)), mask)

    def photo(self, x0, y0, x1, y1, kind=0, r=0):
        kinds = [
            ("#FFB36B", "#FF6F91", "#2B2D5B", "#3D3B7A"),    # غروب
            ("#8EC5FF", "#E3F2FF", "#2F6F5B", "#3F8F76"),    # جبال
            ("#1E2A4A", "#5B6BA8", "#10162B", "#1B2440"),    # مدينة ليلاً
            ("#7DE0D0", "#CFF5EE", "#0FA3A3", "#0A7F86"),    # بحر
            ("#FFD79A", "#FFF1D6", "#D98A3D", "#B96E2B"),    # صحراء
            ("#FFB5C8", "#FFE4EC", "#5BBF8A", "#3E9D6C"),    # حديقة
        ]
        top, bot, h1, h2 = kinds[kind % len(kinds)]
        w, h = x1 - x0, y1 - y0
        self.grad(x0, y0, x1, y1, top, bot, r=r)
        d = self.d
        # شمس/قمر
        sx, sy = x0 + w * (0.72 if kind % 2 == 0 else 0.28), y0 + h * 0.3
        self.circle(sx, sy, min(w, h) * 0.11, fill=mix("#FFFFFF", top, 0.15) if kind == 2 else "#FFF3D1")
        # تلال
        pts1 = [(x0, y1), (x0, y0 + h * 0.72), (x0 + w * 0.3, y0 + h * 0.58), (x0 + w * 0.55, y0 + h * 0.74), (x1, y0 + h * 0.62), (x1, y1)]
        pts2 = [(x0, y1), (x0, y0 + h * 0.86), (x0 + w * 0.4, y0 + h * 0.74), (x0 + w * 0.8, y0 + h * 0.88), (x1, y0 + h * 0.8), (x1, y1)]
        if kind == 2:
            for i in range(9):
                bw = w * 0.09
                bx = x0 + w * 0.04 + i * w * 0.108
                bh = h * (0.18 + 0.22 * ((i * 37) % 7) / 7)
                d.rectangle([bx * S, (y1 - bh) * S, (bx + bw) * S, y1 * S], fill=h1)
                for k in range(3):
                    d.rectangle([(bx + bw * 0.25) * S, (y1 - bh + 6 + k * 9) * S, (bx + bw * 0.55) * S, (y1 - bh + 9 + k * 9) * S], fill="#FFD76A")
        else:
            d.polygon([(a * S, b * S) for a, b in pts1], fill=h1)
            d.polygon([(a * S, b * S) for a, b in pts2], fill=h2)
        if r:
            # حواف مستديرة: نعيد مسح الزوايا بلون الخلفية عند الحاجة (لا نحتاجها داخل بطاقات بيضاء)
            pass

    def avatar(self, cx, cy, r, name="", idx=0, ring=None, online=False, ring_w=2.6):
        col = AVATARS[idx % len(AVATARS)]
        if ring == "new":
            bb = [(cx - r - 4) * S, (cy - r - 4) * S, (cx + r + 4) * S, (cy + r + 4) * S]
            self.d.ellipse(bb, outline=BRAND2, width=int(ring_w * S))
            self.d.arc(bb, 90, 220, fill=BRAND1, width=int(ring_w * S))
            self.d.arc(bb, 300, 390, fill=BRAND3, width=int(ring_w * S))
            self.circle(cx, cy, r + 1.5, fill=SURFACE)
        elif ring == "seen":
            self.d.ellipse([(cx - r - 4) * S, (cy - r - 4) * S, (cx + r + 4) * S, (cy + r + 4) * S], outline=LINE, width=int(2 * S))
            self.circle(cx, cy, r + 1.5, fill=SURFACE)
        self.grad(cx - r, cy - r, cx + r, cy + r, mix(col, "#FFFFFF", 0.25), col, r=r)
        if name:
            self.text(cx, cy + 0.5, name[0], max(10, r * 0.95), "b", "#FFFFFF", "c")
        if online:
            self.circle(cx + r * 0.72, cy + r * 0.72, 5.2, fill=SURFACE)
            self.circle(cx + r * 0.72, cy + r * 0.72, 3.6, fill=SUCCESS)

    # ---------------- مكونات واجهة ----------------
    def logo(self, cx, cy, size, white=False):
        """شعار كويز تواصلكم: مربع بتدرج الهوية + فقاعة محادثة بيضاء + علامة استفهام"""
        if white:
            self.rr(cx - size / 2, cy - size / 2, cx + size / 2, cy + size / 2, size * 0.3, fill="#FFFFFF")
            bub, mark = BRAND1, "#FFFFFF"
        else:
            self.grad(cx - size / 2, cy - size / 2, cx + size / 2, cy + size / 2, BRAND1, BRAND2, r=size * 0.3)
            bub, mark = "#FFFFFF", BRAND1
        bw = size * 0.56
        self.rr(cx - bw / 2, cy - size * 0.3, cx + bw / 2, cy - size * 0.3 + bw * 0.78, bw * 0.3, fill=bub)
        by = cy - size * 0.3 + bw * 0.78
        self.d.polygon([((cx + bw * 0.08) * S, (by - 1) * S), ((cx + bw * 0.36) * S, (by + bw * 0.28) * S), ((cx - bw * 0.16) * S, (by - 1) * S)], fill=bub)
        self.text(cx, cy - size * 0.3 + bw * 0.39, "؟", size * 0.34, "b", mark, "c")

    def tabbar(self, active=0):
        y0 = H - NAV_H
        self.shadow(0, y0, W, H + 20, r=0, blur=6, dy=-1, alpha=18)
        self.rr(0, y0, W, H + 20, 0, fill=SURFACE)
        self.hline(0, W, y0, LINE)
        items = [("home", "الرئيسية"), ("search", "استكشاف"), ("plus", "نشر"), ("reel", "ريلز"), ("user", "حسابي")]
        slot = W / 5
        for i, (ic, lb) in enumerate(items):
            cx = W - slot * (i + 0.5)       # RTL: الأول من اليمين
            on = i == active
            col = PRIMARY_DARK if on else FAINT
            if ic == "plus":
                self.grad(cx - 22, y0 + 10, cx + 22, y0 + 42, PRIMARY, PRIMARY_DARK, r=12, horizontal=True)
                self.icon("plus", cx, y0 + 26, 22, "#FFFFFF", sw=2.4, bg=PRIMARY)
            elif ic == "user":
                self.avatar(cx, y0 + 26, 14, "س", 0, ring="new" if on else None)
            else:
                self.icon(ic, cx, y0 + 26, 27, col, sw=2.1 if on else 1.7)
            self.text(cx, y0 + 58, lb, 11, "b" if on else "m", col, "c")

    def back(self, title, right=None, left=None, size=17):
        """ترويسة: سهم رجوع يمين، عنوان وسط، أيقونات يسار"""
        self.icon("arrow-r", W - 28, 50, 24, INK)
        self.text(180, 50, title, size, "b", INK, "c")
        x = 28
        for ic in (left or []):
            self.icon(ic, x, 50, 24, INK)
            x += 38
        if right:
            self.text(28, 50, right, 14, "b", PRIMARY, "l")

    def btn(self, y, label, x0=PAD, x1=None, h=BUTTON_H, kind="fill", icon=None):
        x1 = x1 or (W - PAD)
        if kind == "fill":
            self.grad(x0, y, x1, y + h, PRIMARY, PRIMARY_DARK, r=RADIUS_BUTTON, horizontal=True)
            col = "#FFFFFF"
        elif kind == "soft":
            self.rr(x0, y, x1, y + h, RADIUS_BUTTON, fill=PRIMARY_SOFT)
            col = PRIMARY_DARK
        else:
            self.rr(x0, y, x1, y + h, RADIUS_BUTTON, fill=SURFACE, line=LINE, lw=1.4)
            col = INK
        cx = (x0 + x1) / 2
        if icon:
            wd = self.width(label, 15, "b")
            right = cx + (wd + 8 + 20) / 2
            self.icon(icon, right - 10, y + h / 2, 20, col, bg=PRIMARY)
            self.text(right - 28, y + h / 2, label, 15, "b", col, "r")
        else:
            self.text(cx, y + h / 2, label, 15, "b", col, "c")

    def input(self, y, placeholder, icon=None, value=None, focus=False, trailing=None, x0=PAD, x1=None, label=None, err=False):
        x1 = x1 or (W - PAD)
        if label:
            self.text(x1, y, label, 12, "m", MUTED, "r")
            y += 14
        line = DANGER if err else (PRIMARY if focus else LINE)
        self.rr(x0, y, x1, y + FIELD_H, RADIUS_FIELD, fill=BG2 if not focus else SURFACE, line=line, lw=1.8 if (focus or err) else 1.2)
        xr = x1 - 16
        if icon:
            self.icon(icon, x1 - 24, y + FIELD_H / 2, 21, PRIMARY if focus else FAINT, bg=BG2)
            xr = x1 - 46
        self.text(xr, y + FIELD_H / 2, value or placeholder, 14, "r", INK if value else FAINT, "r")
        if trailing:
            self.icon(trailing, x0 + 24, y + FIELD_H / 2, 21, FAINT, bg=BG2)
        return y + FIELD_H

    def chip(self, xr, y, label, active=False, h=34, size=13, pad=26):
        wd = self.width(label, size, "m") + pad
        if active:
            self.grad(xr - wd, y, xr, y + h, PRIMARY, PRIMARY_DARK, r=h / 2, horizontal=True)
        else:
            self.rr(xr - wd, y, xr, y + h, h / 2, fill=BG2)
        self.text(xr - wd / 2, y + h / 2, label, size, "m" if not active else "b", "#FFFFFF" if active else MUTED, "c")
        return xr - wd - 8

    def row(self, y, title, sub=None, icon=None, tone=None, toggle=None, chev=True, h=58, idx=None, name=None, value=None):
        """صف قائمة: أيقونة/أفاتار يمين، عنوان، وعنصر يسار"""
        xr = W - PAD
        if idx is not None:
            self.avatar(xr - 22, y + h / 2, 21, name or title, idx)
            xr -= 56
        elif icon:
            if tone:
                self.rr(xr - 40, y + h / 2 - 20, xr, y + h / 2 + 20, 13, fill=TOOL[tone][1])
                self.icon(icon, xr - 20, y + h / 2, 22, TOOL[tone][0], bg=TOOL[tone][1])
            else:
                self.icon(icon, xr - 14, y + h / 2, 24, INK)
            xr -= 54
        if sub:
            self.text(xr, y + h / 2 - 10, title, 14.5, "b", INK, "r")
            self.text(xr, y + h / 2 + 11, sub, 12, "r", MUTED, "r")
        else:
            self.text(xr, y + h / 2, title, 14.5, "m", INK, "r")
        if toggle is not None:
            self.rr(PAD, y + h / 2 - 13, PAD + 46, y + h / 2 + 13, 13, fill=PRIMARY if toggle else "#3A3A3C")
            cx = PAD + 33 if toggle else PAD + 13
            self.circle(cx, y + h / 2, 10, fill="#FFFFFF")
        elif value:
            self.text(PAD + 22, y + h / 2, value, 13, "r", MUTED, "l")
            if chev:
                self.icon("chev-l", PAD + 8, y + h / 2, 16, FAINT)
        elif chev:
            self.icon("chev-l", PAD + 8, y + h / 2, 18, FAINT)
        return y + h

    def sheet_handle(self, y0, h=None):
        self.shadow(0, y0, W, H + 30, r=0, blur=12, dy=-4, alpha=40)
        self.rr(0, y0, W, H + 40, 26, fill=SURFACE, corners=(True, True, False, False))
        self.rr(W / 2 - 20, y0 + 9, W / 2 + 20, y0 + 13, 2, fill=LINE)

    def dim(self, alpha=0.5):
        self.img = Image.blend(self.img, Image.new("RGB", self.img.size, "#0B141B"), alpha)
        self.d = ImageDraw.Draw(self.img)
