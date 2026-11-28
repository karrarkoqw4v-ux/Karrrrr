package iq.suls

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import java.util.concurrent.TimeUnit

data class InstalledAppInfo(
    val packageName: String,
    val label: String,
    val icon: Drawable?,
    val lastUsedMillis: Long,
    val isSystemApp: Boolean
)

object AppsScanner {

    /** يرجّع التطبيقات المثبتة (غير النظامية) مرتّبة من الأقل استخداماً للأكثر، بالاعتماد على UsageStatsManager الحقيقي */
    fun getLeastRecentlyUsedApps(ctx: Context): List<InstalledAppInfo> {
        val pm = ctx.packageManager
        val usm = ctx.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

        val now = System.currentTimeMillis()
        val start = now - TimeUnit.DAYS.toMillis(365 * 2)
        val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_YEARLY, start, now)
        val lastUsedMap = HashMap<String, Long>()
        stats?.forEach { stat ->
            val prev = lastUsedMap[stat.packageName] ?: 0L
            if (stat.lastTimeUsed > prev) lastUsedMap[stat.packageName] = stat.lastTimeUsed
        }

        val installed = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        val result = mutableListOf<InstalledAppInfo>()
        for (app in installed) {
            if (app.packageName == ctx.packageName) continue
            val isSystem = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            val lastUsed = lastUsedMap[app.packageName] ?: 0L
            result.add(
                InstalledAppInfo(
                    packageName = app.packageName,
                    label = pm.getApplicationLabel(app).toString(),
                    icon = try { pm.getApplicationIcon(app) } catch (e: Exception) { null },
                    lastUsedMillis = lastUsed,
                    isSystemApp = isSystem
                )
            )
        }
        // التطبيقات اللي ما ظهرت أبداً بالإحصائيات (lastUsed=0) تعتبر الأقدم استخداماً فعلياً
        return result.filter { !it.isSystemApp }.sortedBy { it.lastUsedMillis }
    }

    fun uninstallIntent(packageName: String): Intent {
        return Intent(Intent.ACTION_DELETE, Uri.parse("package:$packageName"))
    }
}
