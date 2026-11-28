"""رموز تصميم تطبيق «هلا» (تطبيق محادثة اجتماعي). نفس القيم بكود Kotlin داخل ui/theme."""
W, H = 360, 780
S = 6

# الألوان (داكن مثل التطبيق)
BG = "#000000"
SURFACE = "#000000"
BG2 = "#1C1C1E"
INK = "#F5F5F7"
MUTED = "#A1A1AA"
FAINT = "#6B6B73"
LINE = "#2C2C2E"
# نفس قيم Theme.kt بالضبط: أزرق إنستقرام الرسمي
PRIMARY = "#0095F6"
PRIMARY_DARK = "#3797EF"
PRIMARY_SOFT = "#0F2A4A"
# تدرج الهوية الرسمي لإنستقرام (شعار 2016): أزرق - بنفسجي - وردي - برتقالي - كهرماني
IG_GRADIENT = ["#405DE6", "#5851DB", "#833AB4", "#C13584", "#E1306C", "#FD1D1D", "#F56040", "#F77737", "#FCAF45"]
BRAND1 = "#405DE6"
BRAND2 = "#C13584"
BRAND3 = "#FCAF45"
ACCENT = BRAND3
LIKE = "#ED4956"
BLUE = PRIMARY
SUCCESS = "#34C759"
SUCCESS_SOFT = "#12301B"
WARN = "#F59E0B"
WARN_SOFT = "#3A2A08"
DANGER = "#FF4D4F"

# مطلوبة من lib.py
TOOL = {
    "blue": ("#3B82F6", "#E6F0FF"), "teal": ("#0FB5A0", "#E3F7F4"), "violet": ("#7C5CFF", "#EEEAFE"),
    "orange": ("#FF7A59", "#FFEEE8"), "rose": ("#FF4D6A", "#FFE8EC"), "green": ("#16A34A", "#E3F6EA"),
    "amber": ("#F5A524", "#FFF4D6"), "cyan": ("#0891B2", "#DDF4FA"),
}
PAD = 16
RADIUS_CARD = 16
RADIUS_FIELD = 14
RADIUS_BUTTON = 14
BUTTON_H = 56
FIELD_H = 56
NAV_H = 74

AVATARS = ["#7C3AED", "#FF7A59", "#2F8CFF", "#F5A524", "#EC4899", "#34C759", "#12A594", "#B04FD9"]
