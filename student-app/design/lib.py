"""مكتبة الرسم: لوحة + أيقونات خطية (outline) مرسومة بالكود، بدون إيموجي."""
import os
from functools import lru_cache
from PIL import Image, ImageDraw, ImageFont, ImageFilter
from tokens import *

FONT_DIR = os.path.join(os.path.dirname(__file__), "fonts")
WEIGHT = {"r": "Regular", "m": "Medium", "b": "Bold", "x": "ExtraBold"}


@lru_cache(maxsize=None)
def font(w, px):
    return ImageFont.truetype(os.path.join(FONT_DIR, f"Tajawal-{WEIGHT[w]}.ttf"), int(px))


class Canvas:
    def __init__(self, bg=BG):
        self.img = Image.new("RGB", (W * S, H * S), bg)
        self.d = ImageDraw.Draw(self.img)

    # ---------- أشكال ----------
    def rr(self, x0, y0, x1, y1, r=12, fill=None, line=None, lw=1, corners=None):
        self.d.rounded_rectangle([x0 * S, y0 * S, x1 * S, y1 * S], radius=r * S, fill=fill,
                                 outline=line, width=int(lw * S) if line else 0, corners=corners)

    def circle(self, cx, cy, r, fill=None, line=None, lw=1):
        self.d.ellipse([(cx - r) * S, (cy - r) * S, (cx + r) * S, (cy + r) * S], fill=fill,
                       outline=line, width=int(lw * S) if line else 0)

    def hline(self, x0, x1, y, color=LINE, lw=1):
        self.d.line([x0 * S, y * S, x1 * S, y * S], fill=color, width=int(lw * S))

    def deco_circles(self, x0, y0, x1, y1, r, circles, color, lw=1.4):
        """دوائر زخرفية تُقصّ داخل البطاقة (لا تخرج منها)."""
        w, h = int((x1 - x0) * S), int((y1 - y0) * S)
        layer = Image.new("RGBA", (w, h), (0, 0, 0, 0))
        ld = ImageDraw.Draw(layer)
        for cx, cy, rad in circles:
            ld.ellipse([(cx - x0 - rad) * S, (cy - y0 - rad) * S, (cx - x0 + rad) * S, (cy - y0 + rad) * S],
                       outline=color, width=int(lw * S))
        mask = Image.new("L", (w, h), 0)
        ImageDraw.Draw(mask).rounded_rectangle([0, 0, w, h], radius=r * S, fill=255)
        alpha = layer.getchannel("A")
        from PIL import ImageChops
        layer.putalpha(ImageChops.multiply(alpha, mask))
        self.img.paste(layer, (int(x0 * S), int(y0 * S)), layer)

    def shadow(self, x0, y0, x1, y1, r=16, blur=10, dy=3, alpha=26):
        pad = blur * 3
        w, h = int((x1 - x0 + pad * 2) * S), int((y1 - y0 + pad * 2) * S)
        layer = Image.new("L", (w, h), 0)
        ImageDraw.Draw(layer).rounded_rectangle([pad * S, pad * S, (pad + x1 - x0) * S, (pad + y1 - y0) * S],
                                                radius=r * S, fill=alpha)
        layer = layer.filter(ImageFilter.GaussianBlur(blur * S / 2))
        black = Image.new("RGB", (w, h), "#1D2939")
        self.img.paste(black, (int((x0 - pad) * S), int((y0 - pad + dy) * S)), layer)

    def card(self, x0, y0, x1, y1, r=RADIUS_CARD, fill=SURFACE, line=LINE, shadow=True):
        if shadow:
            self.shadow(x0, y0, x1, y1, r)
        self.rr(x0, y0, x1, y1, r, fill=fill, line=line)

    def dashed_rr(self, x0, y0, x1, y1, color, dash=6, gap=5, lw=1.5, r=16):
        # حواف متقطعة مع زوايا مستديرة بسيطة
        d, w = self.d, int(lw * S)
        x = x0 + r
        while x < x1 - r:
            e = min(x + dash, x1 - r)
            d.line([x * S, y0 * S, e * S, y0 * S], fill=color, width=w)
            d.line([x * S, y1 * S, e * S, y1 * S], fill=color, width=w)
            x += dash + gap
        y = y0 + r
        while y < y1 - r:
            e = min(y + dash, y1 - r)
            d.line([x0 * S, y * S, x0 * S, e * S], fill=color, width=w)
            d.line([x1 * S, y * S, x1 * S, e * S], fill=color, width=w)
            y += dash + gap
        for (cx, cy, a) in [(x0 + r, y0 + r, 180), (x1 - r, y0 + r, 270), (x1 - r, y1 - r, 0), (x0 + r, y1 - r, 90)]:
            d.arc([(cx - r) * S, (cy - r) * S, (cx + r) * S, (cy + r) * S], a, a + 90, fill=color, width=w)

    # ---------- نصوص ----------
    def text(self, x, y, s, size=14, w="r", color=INK, al="r", rtl=True):
        f = font(w, size * S)
        anchor = {"r": "rm", "l": "lm", "c": "mm"}[al]
        kw = dict(direction="rtl", language="ar") if rtl else dict(direction="ltr", language="en")
        self.d.text((x * S, y * S), s, font=f, fill=color, anchor=anchor, **kw)

    def width(self, s, size=14, w="r", rtl=True):
        f = font(w, size * S)
        kw = dict(direction="rtl", language="ar") if rtl else dict(direction="ltr", language="en")
        return f.getlength(s, **kw) / S

    def para(self, xr, y, text, size=13, w="r", color=MUTED, maxw=200, lh=None):
        """فقرة عربية تلتف على الكلمات (من اليمين). تُرجع y بعد آخر سطر."""
        lh = lh or size * 1.7
        line = ""
        for word in text.split(" "):
            test = (line + " " + word).strip()
            if line and self.width(test, size, w) > maxw:
                self.text(xr, y, line, size, w, color, "r")
                y += lh
                line = word
            else:
                line = test
        if line:
            self.text(xr, y, line, size, w, color, "r")
            y += lh
        return y

    def runs(self, xr, y, parts, size=14):
        """نصوص متعددة الألوان تُرسم من اليمين لليسار."""
        for s, color, w in parts:
            self.text(xr, y, s, size, w, color, "r")
            xr -= self.width(s, size, w)

    # ---------- أيقونات خطية ----------
    def icon(self, name, cx, cy, size=24, color=INK, sw=1.75, bg=SURFACE):
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

        def dot(x, y, r=0.9):
            p = P(x, y)
            d.ellipse([p[0] - r * u, p[1] - r * u, p[0] + r * u, p[1] + r * u], fill=color)

        n = name
        if n == "camera":
            rrect(3, 7, 21, 20, 3.5); ln((8, 7), (9.5, 4.5), (14.5, 4.5), (16, 7)); circ(12, 13.5, 3.4)
        elif n == "globe":
            circ(12, 12, 9); d.ellipse(box(7.5, 3, 16.5, 21), outline=color, width=wpx); ln((3, 12), (21, 12))
        elif n == "chat":
            rrect(3, 4, 21, 17, 4.5); ln((8, 17), (8, 21), (12, 17))
        elif n == "doc":
            rrect(5, 3, 19, 21, 3); ln((9, 9), (15, 9)); ln((9, 13), (15, 13)); ln((9, 17), (13, 17))
        elif n == "quiz":
            rrect(5, 4.5, 19, 21, 3); rrect(9, 2.5, 15, 6.5, 1.8, fill=bg); ln((9, 13.5), (11.3, 15.8), (15.2, 11.3))
        elif n == "timer":
            circ(12, 13.5, 8); ln((12, 13.5), (12, 9.5)); ln((9.5, 2.5), (14.5, 2.5)); ln((12, 2.5), (12, 5.5))
        elif n == "calc":
            rrect(5, 3, 19, 21, 3); rrect(8, 6, 16, 10, 1.2)
            for x in (9, 12, 15):
                for y in (14, 18):
                    dot(x, y, 0.8)
        elif n == "cards":
            rrect(7, 3.5, 21, 15.5, 3); rrect(3, 8.5, 17, 20.5, 3, fill=bg)
        elif n == "book":
            ln((3, 5.5), (11, 7.5), (11, 20), (3, 18), close=True); ln((21, 5.5), (13, 7.5), (13, 20), (21, 18), close=True)
        elif n == "mic":
            rrect(9, 2.5, 15, 14, 3); arc(5.5, 6, 18.5, 19, 0, 180); ln((12, 19), (12, 22)); ln((9, 22), (15, 22))
        elif n == "bell":
            arc(6, 3.5, 18, 15.5, 180, 360); ln((6, 9.5), (6, 14.5), (4, 18), (20, 18), (18, 14.5), (18, 9.5)); arc(9.5, 17, 14.5, 22, 0, 180)
        elif n == "search":
            circ(10.5, 10.5, 6.5); ln((15.5, 15.5), (21, 21))
        elif n == "home":
            ln((3, 11), (12, 3), (21, 11)); ln((5.5, 9.5), (5.5, 20.5), (18.5, 20.5), (18.5, 9.5)); ln((10, 20.5), (10, 14.5), (14, 14.5), (14, 20.5))
        elif n == "user":
            circ(12, 8, 4); arc(4, 14, 20, 30, 180, 360)
        elif n == "lock":
            rrect(5, 10.5, 19, 21, 3); ln((8.5, 10.5), (8.5, 8)); arc(8.5, 3.5, 15.5, 12.5, 180, 360); ln((15.5, 8), (15.5, 10.5)); dot(12, 15.7, 1.1)
        elif n == "mail":
            rrect(3, 5, 21, 19, 3.5); ln((4.5, 7.5), (12, 13.5), (19.5, 7.5))
        elif n == "phone":
            rrect(7, 2.5, 17, 21.5, 3); ln((10.5, 18.5), (13.5, 18.5))
        elif n == "eye":
            d.ellipse(box(2, 6.5, 22, 17.5), outline=color, width=wpx); circ(12, 12, 2.8)
        elif n == "arrow-r":
            ln((4, 12), (20, 12)); ln((14, 6), (20, 12), (14, 18))
        elif n == "arrow-l":
            ln((4, 12), (20, 12)); ln((10, 6), (4, 12), (10, 18))
        elif n == "chev-l":
            ln((14.5, 6), (8.5, 12), (14.5, 18))
        elif n == "chev-r":
            ln((9.5, 6), (15.5, 12), (9.5, 18))
        elif n == "chev-d":
            ln((6, 9.5), (12, 15.5), (18, 9.5))
        elif n == "check":
            ln((5, 12.5), (10, 17.5), (19, 7))
        elif n == "plus":
            ln((12, 5), (12, 19)); ln((5, 12), (19, 12))
        elif n == "close":
            ln((6, 6), (18, 18)); ln((18, 6), (6, 18))
        elif n == "flame":
            ln((12, 2.5), (17, 9), (18.5, 14), (16.5, 19), (12, 21.5), (7.5, 19), (5.5, 14), (7.2, 10), (9.7, 12.7), (10.5, 7), close=True)
        elif n == "bars":
            ln((6, 20), (6, 13)); ln((12, 20), (12, 5)); ln((18, 20), (18, 10))
        elif n == "calendar":
            rrect(3.5, 5, 20.5, 20.5, 3.5); ln((3.5, 10), (20.5, 10)); ln((8, 3), (8, 7)); ln((16, 3), (16, 7))
        elif n == "bookmark":
            ln((6, 3.5), (18, 3.5), (18, 21), (12, 16.5), (6, 21), close=True)
        elif n == "sliders":
            ln((4, 7), (20, 7)); ln((4, 17), (20, 17)); circ(9, 7, 2.4, fill=bg); circ(15, 17, 2.4, fill=bg)
        elif n == "scan":
            for sx in (-1, 1):
                for sy in (-1, 1):
                    x, y = 12 + sx * 8.5, 12 + sy * 8.5
                    ln((x, y - sy * 5), (x, y), (x - sx * 5, y))
            ln((7.5, 12), (16.5, 12))
        elif n == "speaker":
            ln((4, 9.5), (8, 9.5), (13, 5), (13, 19), (8, 14.5), (4, 14.5), close=True); arc(10, 8, 19, 16, -55, 55); arc(8, 4.5, 21.5, 19.5, -45, 45)
        elif n == "sigma":
            ln((17.5, 4), (7, 4), (13, 12), (7, 20), (17.5, 20))
        elif n == "flask":
            ln((9, 3), (15, 3)); ln((10, 3), (10, 10), (4.5, 19), (19.5, 19), (14, 10), (14, 3)); ln((7.5, 14.5), (16.5, 14.5))
        elif n == "pencil":
            ln((4, 20), (5, 15), (16, 4), (20, 8), (9, 19), close=True); ln((13.5, 6.5), (17.5, 10.5))
        elif n == "cap":
            ln((12, 4), (22, 9), (12, 14), (2, 9), close=True); ln((6, 11.5), (6, 16), (12, 19), (18, 16), (18, 11.5)); ln((22, 9), (22, 14))
        elif n == "send":
            ln((3, 11), (21, 3), (14, 21), (11, 13), close=True); ln((11, 13), (21, 3))
        elif n == "image":
            rrect(3, 4, 21, 20, 3.5); circ(8.5, 9.5, 1.8); ln((4, 18), (10, 12.5), (14, 16), (17, 13), (20, 16))
        elif n == "bolt":
            ln((13, 2.5), (5, 13.5), (11.5, 13.5), (10.5, 21.5), (19, 10), (12.5, 10), close=True)
        elif n == "google":
            arc(4, 4, 20, 20, 35, 325); ln((12, 12), (19.5, 12))
        elif n == "bulb":
            arc(6, 4, 18, 16, 125, 415); ln((9.5, 15.5), (9.5, 18.5), (14.5, 18.5), (14.5, 15.5)); ln((10, 21.5), (14, 21.5))
        elif n == "swap":
            ln((4, 8), (20, 8)); ln((16, 4), (20, 8), (16, 12)); ln((20, 16), (4, 16)); ln((8, 12), (4, 16), (8, 20))
        elif n == "copy":
            rrect(8.5, 8.5, 20.5, 20.5, 3); ln((15.5, 8.5), (15.5, 5.5), (12.5, 3.5), (6, 3.5), (3.5, 6), (3.5, 14), (6, 15.5), (8.5, 15.5))
        elif n == "play":
            ln((7, 4.5), (19, 12), (7, 19.5), close=True)
        elif n == "pause":
            ln((8, 5), (8, 19)); ln((16, 5), (16, 19))
        elif n == "reset":
            arc(4.5, 4.5, 19.5, 19.5, 30, 300); ln((14.2, 1.3), (15.75, 5.5), (11.3, 6.3))
        elif n == "grid":
            for (a, b) in [(4, 4), (13.5, 4), (4, 13.5), (13.5, 13.5)]:
                rrect(a, b, a + 6.5, b + 6.5, 2)
        elif n == "award":
            circ(12, 9, 6); ln((8.5, 14), (7, 21), (12, 18.5), (17, 21), (15.5, 14))
        elif n == "trash":
            ln((4, 7), (20, 7)); ln((6.5, 7), (7.5, 20.5), (16.5, 20.5), (17.5, 7)); ln((9.5, 7), (9.5, 4), (14.5, 4), (14.5, 7))
        elif n == "info":
            circ(12, 12, 9); ln((12, 11), (12, 16.5)); dot(12, 7.6, 1.0)
        elif n == "lang":
            ln((4, 6), (14, 6)); ln((9, 3.5), (9, 6)); ln((6, 6), (7.5, 11), (12, 14.5)); ln((12.5, 6), (10, 11), (5, 14.5))
            ln((12.5, 20.5), (16.5, 10.5), (20.5, 20.5)); ln((14, 17), (19, 17))
        else:
            raise ValueError(n)

    def star(self, cx, cy, size, color, filled=True, line=None):
        import math
        pts = []
        for i in range(10):
            r = size / 2 if i % 2 == 0 else size / 4.2
            a = -math.pi / 2 + i * math.pi / 5
            pts.append(((cx + r * math.cos(a)) * S, (cy + r * math.sin(a)) * S))
        self.d.polygon(pts, fill=color if filled else None, outline=line or color)
        if not filled:
            self.d.line(pts + [pts[0]], fill=line or color, width=int(1.2 * S), joint="curve")

    def stars(self, xr, cy, rating, size=13, color="#F59E0B"):
        """نجوم التقييم من اليمين"""
        for i in range(5):
            cx = xr - size / 2 - i * (size + 2)
            self.star(cx, cy, size, color if i < round(rating) else "#E4E7EC")

    def icon_tile(self, name, cx, cy, size, tone, r=None, icon_size=None):
        dark, soft = TOOL[tone]
        r = size * 0.3 if r is None else r
        self.rr(cx - size / 2, cy - size / 2, cx + size / 2, cy + size / 2, r, fill=soft)
        self.icon(name, cx, cy, icon_size or size * 0.5, dark, bg=soft)

    def save(self, path):
        out = self.img.resize((W * 3, H * 3), Image.LANCZOS)
        out.save(path, optimize=True)
        return out
