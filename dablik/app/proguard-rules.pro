# ---- Rhino (محرّك جافاسكربت): يستخدم انعكاساً داخلياً، نحافظ عليه كاملاً ليضمن عمله بعد التعتيم ----
-keep class org.mozilla.javascript.** { *; }
-dontwarn org.mozilla.javascript.**

# ---- apksig (توقيع والتحقق من APK): مكتبة أدوات بناء تستخدم أصناف أمنية انعكاسية ----
-keep class com.android.apksig.** { *; }
-dontwarn com.android.apksig.**

# ---- org.json مدمجة بأندرويد نفسه، لا تحتاج قواعد خاصة ----
-dontwarn org.json.**

# أسطر تتبّع الأخطاء مفيدة حتى بعد التعتيم (بدون كشف الأسماء الحقيقية)
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
