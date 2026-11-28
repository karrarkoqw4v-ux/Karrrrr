package iq.dablik

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/**
 * يشغّل كود المستخدم بحلقة مستمرة في الخلفية (مثل بوت تيليجرام يستقصي رسائله) كخدمة أمامية
 * حقيقية: إشعار دائم ظاهر طول وقت التشغيل (شرط أندرويد نفسه لمنع أي تشغيل خفي)، وزر إيقاف فيه.
 */
class ScriptService : Service() {
    companion object {
        const val CH = "dablik_script"
        const val ACTION_STOP = "iq.dablik.STOP_SCRIPT"
        var running = false
            private set
        var lastLog = ""
            private set
        private val stop = AtomicBoolean(false)
        private val listeners = ArrayList<(String) -> Unit>()
        fun addListener(l: (String) -> Unit) { listeners.add(l) }
        fun removeListener(l: (String) -> Unit) { listeners.remove(l) }
        private fun emit(line: String) { lastLog = line; listeners.forEach { it(line) } }
    }

    private var thread: Thread? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) { stopScript(); return START_NOT_STICKY }
        val code = intent?.getStringExtra("code") ?: return START_NOT_STICKY
        startForeground(1, notif("جارٍ تشغيل السكربت..."))
        running = true
        stop.set(false)
        thread = Thread {
            val err = CodeRunner.runLoop(code, stop) { line ->
                emit(line)
                updateNotif(line)
            }
            running = false
            if (err != null) emit("[خطأ] $err")
            stopForeground(true)
            stopSelf()
        }.also { it.start() }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        stop.set(true)
        running = false
        super.onDestroy()
    }

    private fun stopScript() {
        stop.set(true)
        running = false
        stopForeground(true)
        stopSelf()
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(NotificationChannel(CH, "سكربتات تعمل بالخلفية", NotificationManager.IMPORTANCE_LOW))
        }
    }

    private fun stopIntent(): PendingIntent {
        val i = Intent(this, ScriptService::class.java).setAction(ACTION_STOP)
        return PendingIntent.getService(this, 0, i, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun notif(text: String): Notification {
        ensureChannel()
        return NotificationCompat.Builder(this, CH)
            .setContentTitle("دبليك: سكربت يعمل بالخلفية")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_media_pause, "إيقاف", stopIntent())
            .build()
    }

    private fun updateNotif(text: String) {
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(1, notif(text.take(80)))
    }
}

object ScriptRunner {
    fun start(ctx: Context, code: String) {
        val i = Intent(ctx, ScriptService::class.java).putExtra("code", code)
        ctx.startForegroundService(i)
    }
    fun stop(ctx: Context) {
        ctx.startService(Intent(ctx, ScriptService::class.java).setAction(ScriptService.ACTION_STOP))
    }
}
