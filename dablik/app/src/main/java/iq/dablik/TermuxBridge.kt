package iq.dablik

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

/**
 * ربط حقيقي مع تطبيق Termux الفعلي (يجب أن يكون مثبّتاً عندك) لتشغيل بايثون وأي لغة ثانية
 * نصّبها بـ pip/apt داخل صندوق Termux الخاص به. لا علاقة له بصلاحيات الجهاز — فقط ما يسمح به Termux
 * لنفسه، ونفس ما لو كتبت الأمر بيدك داخل تطبيق Termux.
 */
object TermuxBridge {
    const val PERM = "com.termux.permission.RUN_COMMAND"
    private const val PKG = "com.termux"
    private const val SERVICE = "com.termux.app.RunCommandService"
    const val PY = "/data/data/com.termux/files/usr/bin/python"
    const val PIP = "/data/data/com.termux/files/usr/bin/pip"
    const val PKGMGR = "/data/data/com.termux/files/usr/bin/pkg"

    fun installed(ctx: Context): Boolean = runCatching { ctx.packageManager.getPackageInfo(PKG, 0); true }.getOrDefault(false)
    fun granted(ctx: Context): Boolean = ctx.checkSelfPermission(PERM) == PackageManager.PERMISSION_GRANTED

    /** يفتح جلسة Termux حقيقية وينفّذ الأمر فيها أمام عينك (مو مخفي) */
    fun run(ctx: Context, path: String, args: List<String>) {
        val i = Intent().apply {
            setClassName(PKG, SERVICE)
            action = "com.termux.RUN_COMMAND"
            putExtra("com.termux.RUN_COMMAND_PATH", path)
            putExtra("com.termux.RUN_COMMAND_ARGUMENTS", args.toTypedArray())
            putExtra("com.termux.RUN_COMMAND_BACKGROUND", false)
            putExtra("com.termux.RUN_COMMAND_SESSION_ACTION", "0") // يفتح الجلسة ويعرضها فوراً
        }
        ctx.startForegroundService(i)
    }

    fun runPython(ctx: Context, code: String) = run(ctx, PY, listOf("-c", code))
    fun installPackages(ctx: Context, pkgs: String) = run(ctx, PIP, listOf("install") + pkgs.trim().split(Regex("\\s+")).filter { it.isNotBlank() })
    fun installSystemTool(ctx: Context, name: String) = run(ctx, PKGMGR, listOf("install", "-y", name))
}
