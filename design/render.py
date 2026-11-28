"""يولّد صور تصاميم تطبيق «هلا»: كل شاشة + لوحات (5 شاشات بكل لوحة) + لوحة شاملة.
التشغيل:  python3 render.py"""
import importlib
import os
import sys
from PIL import Image, ImageDraw, ImageFilter
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from lib import font

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "screens")
os.makedirs(OUT, exist_ok=True)
MODULES = ["auth", "feed", "chatscreens", "account"]
BG = "#101014"


def phone(img, scale):
    w, h = int(img.width * scale), int(img.height * scale)
    shot = img.resize((w, h), Image.LANCZOS)
    r = int(46 * scale)
    pad = int(48 * scale)
    out = Image.new("RGBA", (w + pad * 2, h + pad * 2), (0, 0, 0, 0))
    sh = Image.new("L", out.size, 0)
    ImageDraw.Draw(sh).rounded_rectangle([pad, pad + int(12 * scale), pad + w, pad + h + int(12 * scale)], r, fill=70)
    sh = sh.filter(ImageFilter.GaussianBlur(int(20 * scale)))
    out.paste(Image.new("RGBA", out.size, (60, 60, 66, 255)), (0, 0), sh)
    mask = Image.new("L", (w, h), 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, w, h], r, fill=255)
    bez = int(6 * scale)
    ImageDraw.Draw(out).rounded_rectangle([pad - bez, pad - bez, pad + w + bez, pad + h + bez], r + bez, fill=(60, 60, 66, 255))
    out.paste(shot, (pad, pad), mask)
    return out


def label(d, cx, y, text, size):
    d.text((cx, y), text, font=font("b", size), fill="#F5F5F7", anchor="mm", direction="rtl", language="ar")


def sheet(items, name, title, scale=0.5):
    phones = [phone(im, scale) for _, im in items]
    cw = phones[0].width
    gap = 28
    Wd = cw * len(phones) + gap * (len(phones) + 1)
    Hd = phones[0].height + 170
    canvas = Image.new("RGB", (Wd, Hd), BG)
    d = ImageDraw.Draw(canvas)
    label(d, Wd // 2, 56, title, 46)
    for i, ((t, _), p) in enumerate(zip(items, phones)):
        x = Wd - gap - cw - i * (cw + gap)          # من اليمين لليسار
        canvas.paste(p, (x, 100), p)
        label(d, x + cw // 2, 100 + p.height + 22, t, 30)
    canvas.save(os.path.join(OUT, name), optimize=True)


def main():
    rendered, n = [], 0
    for m in MODULES:
        try:
            mod = importlib.import_module(m)
        except ModuleNotFoundError:
            continue
        for fname, title, fn in mod.SCREENS:
            img = fn().save(os.path.join(OUT, fname + ".png"))
            rendered.append((title, img))
            n += 1
            print("ok", fname)
    per = 5
    for i in range(0, len(rendered), per):
        k = i // per + 1
        sheet(rendered[i:i + per], f"sheet_{k:02d}.png", f"تطبيق كويز تواصلكم - لوحة {k} (الشاشات {i + 1} إلى {min(i + per, len(rendered))})")
    # لوحة شاملة
    ph = [phone(im, 0.26) for _, im in rendered]
    cols, gap = 13, 18
    pw, phh = ph[0].size
    rows = (len(ph) + cols - 1) // cols
    Wd = cols * pw + (cols + 1) * gap
    Hd = rows * (phh + 56) + 130
    canvas = Image.new("RGB", (Wd, Hd), BG)
    d = ImageDraw.Draw(canvas)
    label(d, Wd // 2, 50, f"تطبيق كويز تواصلكم - كل الشاشات ({n} شاشة)", 44)
    for i, ((t, _), p) in enumerate(zip(rendered, ph)):
        r, ci = divmod(i, cols)
        x = Wd - gap - pw - ci * (pw + gap)
        y = 96 + r * (phh + 56)
        canvas.paste(p, (x, y), p)
        label(d, x + pw // 2, y + phh + 6, t, 20)
    canvas.save(os.path.join(OUT, "overview.png"), optimize=True)
    print("total", n)


if __name__ == "__main__":
    main()
