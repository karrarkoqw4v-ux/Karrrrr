"""رموز التصميم (Design tokens) - نفس القيم مستخدمة بتطبيق Kotlin داخل ui/theme.
القياسات كلها بوحدة dp حتى تنطبق على أندرويد بالضبط."""

# أبعاد الشاشة المرجعية (هاتف متوسط) بوحدة dp
W, H = 360, 780
S = 6          # بكسل لكل dp أثناء الرسم (يُصغَّر بعدها إلى 3x = 1080x2340)

# الألوان (واجهة فاتحة)
BG = "#F6F7FB"
SURFACE = "#FFFFFF"
INK = "#101828"
MUTED = "#667085"
FAINT = "#98A2B3"
LINE = "#E4E7EC"
PRIMARY = "#2F5BEA"
PRIMARY_SOFT = "#E8EEFF"
SUCCESS = "#16A34A"
SUCCESS_SOFT = "#E3F6EA"
WARN = "#B7791F"
WARN_SOFT = "#FFF4D6"
DANGER = "#D92D4A"

# ألوان الأدوات (غامق, فاتح)
TOOL = {
    "blue":   ("#2F5BEA", "#E8EEFF"),
    "teal":   ("#0E9384", "#DFF5F1"),
    "violet": ("#7A5AF8", "#EEEAFE"),
    "orange": ("#E8590C", "#FFEEE0"),
    "rose":   ("#D92D6A", "#FDE7EF"),
    "green":  ("#16A34A", "#E3F6EA"),
    "amber":  ("#B7791F", "#FFF4D6"),
    "cyan":   ("#0891B2", "#DDF4FA"),
}

# المسافات والحواف
PAD = 20          # هامش الشاشة الجانبي
RADIUS_CARD = 16
RADIUS_FIELD = 14
RADIUS_BUTTON = 16
BUTTON_H = 56
FIELD_H = 52
NAV_H = 72

# أحجام الخط (sp)
TEXT = {"display": 26, "title": 18, "body": 14, "label": 12, "caption": 11}
