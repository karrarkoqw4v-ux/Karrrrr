package iq.suls

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.IBinder
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import rikka.shizuku.Shizuku

/**
 * جسر رفيع لمكتبة Shizuku الرسمية. سلس ما يعيد بناء أي بروتوكول ADB بنفسه —
 * كل الصلاحية تمر فعلياً عبر تطبيق Shizuku (أو Sui) ونافذة الموافقة الخاصة فيه.
 */
object ShizukuBridge {
    const val REQUEST_CODE = 9001
    private const val PKG = "iq.suls.cleaner"

    var binderAlive by mutableStateOf(false)
        private set
    var permissionGranted by mutableStateOf(false)
        private set
    var service: IShellService? = null
        private set

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        binderAlive = true
        refreshPermission()
    }
    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        binderAlive = false
        permissionGranted = false
        service = null
    }
    private val permissionResultListener = Shizuku.OnRequestPermissionResultListener { _, grantResult ->
        permissionGranted = grantResult == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    private val userServiceArgs = Shizuku.UserServiceArgs(
        ComponentName(PKG, ShellUserService::class.java.name)
    ).daemon(false).processNameSuffix("shell_service").debuggable(false).version(BuildConfig.VERSION_CODE)

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            service = if (binder.pingBinder()) IShellService.Stub.asInterface(binder) else null
        }
        override fun onServiceDisconnected(name: ComponentName) {
            service = null
        }
    }

    fun register() {
        try {
            binderAlive = Shizuku.pingBinder()
        } catch (e: Throwable) {
            binderAlive = false
        }
        Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
        Shizuku.addBinderDeadListener(binderDeadListener)
        Shizuku.addRequestPermissionResultListener(permissionResultListener)
        if (binderAlive) refreshPermission()
    }

    fun unregister() {
        Shizuku.removeBinderReceivedListener(binderReceivedListener)
        Shizuku.removeBinderDeadListener(binderDeadListener)
        Shizuku.removeRequestPermissionResultListener(permissionResultListener)
    }

    private fun refreshPermission() {
        permissionGranted = try {
            Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED
        } catch (e: Throwable) { false }
        if (permissionGranted) bindService()
    }

    fun requestPermission() {
        if (!binderAlive) return
        if (Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            permissionGranted = true
            bindService()
        } else {
            Shizuku.requestPermission(REQUEST_CODE)
        }
    }

    private fun bindService() {
        try {
            Shizuku.bindUserService(userServiceArgs, connection)
        } catch (e: Throwable) { /* نتجاهلها، البطاقة بالواجهة توضح الحالة */ }
    }

    fun openDeveloperSettings(ctx: Context) {
        val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
        intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
        try { ctx.startActivity(intent) } catch (e: Exception) { }
    }
}
