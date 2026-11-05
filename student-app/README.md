# طالب العراق (Android)

تطبيق أدوات للطالب العراقي (ابتدائي - متوسط - إعدادي - جامعي): حلّال مسائل بالصورة، مترجم، مساعد دراسي،
تلخيص وقراءة صور، أسئلة وزارية، بطاقات حفظ، مؤقت دراسة، حاسبة معدل.

## التقنية
- **Kotlin + Jetpack Compose + Material 3** (أندرويد أصلي، بدون HTML/WebView)
- واجهة **RTL** عربية، خط **Tajawal**، أيقونات **Outlined** خطية (بدون إيموجي)
- قياسات محددة بالـ dp/sp في `ui/theme/Dimens.kt` و`Type.kt`: مرجع 360dp، تكبير تدريجي 0.9 إلى 1.25،
  عرض أقصى 600dp على التابلت، وتحديد تكبير الخط عند 1.15 حتى لا تنكسر الواجهة.

## الحالة
| الشاشة | الحالة |
|---|---|
| تسجيل الدخول، اختيار المرحلة، الرئيسية + التنقل | منفّذة بـ Compose |
| بقية الأدوات (13 شاشة تصميم) | تصميم جاهز بـ `design/screens`، والتنفيذ شاشة مؤقتة |

## التصاميم
`design/screens/*.png` (14 شاشة + لوحات). تُعاد بـ `python3 design/render_mockups.py` (يحتاج Pillow مع libraqm).

## البناء
يُبنى تلقائياً عبر GitHub Actions (`.github/workflows/student-apk.yml`) ويُنشر `IraqiStudent.apk` بالـ Releases.
محلياً: `cd student-app && gradle assembleRelease` (يحتاج Android SDK).
