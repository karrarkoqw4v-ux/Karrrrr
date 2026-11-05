"""يولّد صور PNG للتصاميم: كل شاشة لوحدها + لوحات تجمع كل 3 شاشات + لوحة شاملة.
التشغيل:  python3 render_mockups.py"""
import os
import sys
from PIL import Image, ImageDraw, ImageFilter
sys.path.insert(0, os.path.dirname(__file__))
from lib import font
from screens import SCREENS
from screens2 import SCREENS2

OUT = os.path.join(os.path.dirname(__file__), "screens")
os.makedirs(OUT, exist_ok=True)


def phone(img, scale):
    """يلف الشاشة بإطار هاتف خفيف بزوايا مستديرة وظل."""
    w, h = int(img.width * scale), int(img.height * scale)
    shot = img.resize((w, h), Image.LANCZOS)
    r = int(48 * scale)
    pad = int(60 * scale)
    out = Image.new("RGBA", (w + pad * 2, h + pad * 2), (0, 0, 0, 0))
    sh = Image.new("L", out.size, 0)
    ImageDraw.Draw(sh).rounded_rectangle([pad, pad + int(14 * scale), pad + w, pad + h + int(14 * scale)], r, fill=70)
    sh = sh.filter(ImageFilter.GaussianBlur(int(22 * scale)))
    out.paste(Image.new("RGBA", out.size, (29, 41, 57, 255)), (0, 0), sh)
    mask = Image.new("L", (w, h), 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, w, h], r, fill=255)
    bez = int(7 * scale)
    ImageDraw.Draw(out).rounded_rectangle([pad - bez, pad - bez, pad + w + bez, pad + h + bez], r + bez, fill=(16, 24, 40, 255))
    out.paste(shot, (pad, pad), mask)
    return out


def label(draw, cx, y, text, size):
    f = font("b", size)
    draw.text((cx, y), text, font=f, fill="#101828", anchor="mm", direction="rtl", language="ar")


def sheet(items, name, title, scale=0.62, bg="#E9EDF6"):
    phones = [phone(im, scale) for _, im in items]
    cw = phones[0].width
    gap = 40
    W = cw * len(phones) + gap * (len(phones) + 1)
    H = phones[0].height + 190
    canvas = Image.new("RGB", (W, H), bg)
    d = ImageDraw.Draw(canvas)
    label(d, W // 2, 60, title, 52)
    for i, (t, im) in enumerate(zip([t for t, _ in items], phones)):
        x = gap + i * (cw + gap)
        canvas.paste(im, (x, 110), im)
        label(d, x + cw // 2, 110 + im.height + 28, t, 34)
    canvas.save(os.path.join(OUT, name), optimize=True)


def main():
    rendered = []
    for fname, title, fn in SCREENS:
        img = fn().save(os.path.join(OUT, fname + ".png"))
        rendered.append((title, img))
        print("ok", fname)
    groups = [("لوحة 1: الدخول والبداية", rendered[0:3]), ("لوحة 2: حل المسائل والترجمة", rendered[3:6]),
              ("لوحة 3: المساعد والتلخيص والوزاري", rendered[6:9]), ("لوحة 4: الاختبار والمؤقت والمعدل", rendered[9:12]),
              ("لوحة 5: البطاقات والتقدم", rendered[12:14])]
    for i, (title, items) in enumerate(groups, 1):
        sheet(items, f"sheet_{i}.png", title)
    rendered2 = []
    for fname, title, fn in SCREENS2:
        img = fn().save(os.path.join(OUT, fname + ".png"))
        rendered2.append((title, img))
        print("ok", fname)
    titles = ["الحساب والصفوف", "المواد وأقسام الإنكليزي", "الماسح وتحديد النص والترجمة", "النطق وكتابة السؤال والرياضيات",
              "الرياضيات والعلوم", "الكيمياء والأحياء والوزاريات", "الملازم وقارئ PDF والمكتبة", "الإضافة والقفل والإعدادات", "المواصفات"]
    for i in range(0, len(rendered2), 3):
        n = i // 3
        sheet(rendered2[i:i + 3], f"sheet_{6 + n}.png", f"لوحة {6 + n}: {titles[n]}")
    rendered = rendered + rendered2
    # لوحة شاملة
    phones = [phone(im, 0.3) for _, im in rendered]
    cols, gap = 10, 24
    pw, ph = phones[0].size
    rows = (len(phones) + cols - 1) // cols
    W = cols * pw + (cols + 1) * gap
    H = rows * (ph + 70) + 150
    canvas = Image.new("RGB", (W, H), "#E9EDF6")
    d = ImageDraw.Draw(canvas)
    label(d, W // 2, 60, "تطبيق الطالب العراقي - كل الشاشات", 48)
    for i, ((t, _), p) in enumerate(zip(rendered, phones)):
        r, cidx = divmod(i, cols)
        x = gap + (cols - 1 - cidx) * (pw + gap)          # يمين لليسار
        y = 110 + r * (ph + 70)
        canvas.paste(p, (x, y), p)
        label(d, x + pw // 2, y + ph + 10, t, 24)
    canvas.save(os.path.join(OUT, "overview.png"), optimize=True)


if __name__ == "__main__":
    main()
