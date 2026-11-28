package iq.quiz.data

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.IconCompat
import iq.quiz.MainActivity
import iq.quiz.R

/** إشعارات بأسلوب واتساب: اسم التطبيق، صورة المرسل واسمه، نص الرسالة أو معلومات المكالمة. */
object Notifier {
    private const val CH_MSG = "messages"
    private const val CH_POST = "posts"
    private const val CH_CALL = "calls"
    private val colors = intArrayOf(0xFF7C3AED.toInt(), 0xFFEC4899.toInt(), 0xFFF59E0B.toInt(), 0xFF10B981.toInt(), 0xFF3B82F6.toInt(), 0xFFEF4444.toInt())

    private fun ensure(ctx: Context) {
        if (Build.VERSION.SDK_INT < 26) return
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CH_MSG, "الرسائل", NotificationManager.IMPORTANCE_HIGH))
        nm.createNotificationChannel(NotificationChannel(CH_POST, "منشورات الأصدقاء", NotificationManager.IMPORTANCE_DEFAULT))
        nm.createNotificationChannel(NotificationChannel(CH_CALL, "المكالمات", NotificationManager.IMPORTANCE_HIGH))
    }

    private fun avatar(name: String, idx: Int): Bitmap {
        val s = 160
        val b = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888)
        val c = Canvas(b)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.color = colors[idx.mod(colors.size)]
        c.drawCircle(s / 2f, s / 2f, s / 2f, p)
        p.color = Color.WHITE
        p.textSize = 78f
        p.typeface = Typeface.DEFAULT_BOLD
        p.textAlign = Paint.Align.CENTER
        c.drawText(name.trim().take(1), s / 2f, s / 2f + 27f, p)
        return b
    }

    private fun open(ctx: Context): PendingIntent =
        PendingIntent.getActivity(ctx, 0, Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

    private fun post(ctx: Context, id: Int, n: NotificationCompat.Builder) {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        ensure(ctx)
        try { NotificationManagerCompat.from(ctx).notify(id, n.build()) } catch (_: SecurityException) {}
    }

    fun message(ctx: Context, name: String, idx: Int, text: String, id: Int = 1) {
        ensure(ctx)
        val av = avatar(name, idx)
        val person = Person.Builder().setName(name).setIcon(IconCompat.createWithBitmap(av)).build()
        val style = NotificationCompat.MessagingStyle(Person.Builder().setName("أنا").build()).addMessage(text, System.currentTimeMillis(), person)
        post(ctx, 1000 + id, NotificationCompat.Builder(ctx, CH_MSG).setSmallIcon(R.drawable.ic_notif).setLargeIcon(av).setStyle(style)
            .setContentTitle(name).setContentText(text).setSubText("كويز تواصلكم").setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setPriority(NotificationCompat.PRIORITY_HIGH).setAutoCancel(true).setContentIntent(open(ctx)))
    }

    fun friendPost(ctx: Context, name: String, idx: Int, caption: String) {
        ensure(ctx)
        val av = avatar(name, idx)
        post(ctx, 2000 + idx, NotificationCompat.Builder(ctx, CH_POST).setSmallIcon(R.drawable.ic_notif).setLargeIcon(av)
            .setContentTitle(name).setContentText("نشر منشوراً جديداً: $caption").setSubText("كويز تواصلكم")
            .setStyle(NotificationCompat.BigTextStyle().bigText("نشر منشوراً جديداً: $caption"))
            .setAutoCancel(true).setContentIntent(open(ctx)))
    }

    fun call(ctx: Context, name: String, idx: Int, video: Boolean, missed: Boolean = true) {
        ensure(ctx)
        val av = avatar(name, idx)
        val t = (if (missed) "مكالمة " else "اتصال ") + (if (video) "فيديو " else "صوتية ") + (if (missed) "فائتة" else "وارد")
        post(ctx, 3000 + idx, NotificationCompat.Builder(ctx, CH_CALL).setSmallIcon(R.drawable.ic_notif).setLargeIcon(av)
            .setContentTitle(name).setContentText(t).setSubText("كويز تواصلكم").setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(NotificationCompat.PRIORITY_HIGH).setAutoCancel(true).setContentIntent(open(ctx)))
    }
}
