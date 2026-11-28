package iq.suls

import android.text.format.DateUtils
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AppsScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    var apps by remember { mutableStateOf<List<InstalledAppInfo>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var needsUsageAccess by remember { mutableStateOf(!Permissions.hasUsageAccess(ctx)) }
    var tick by remember { mutableStateOf(0) }

    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { tick++ }

    LaunchedEffect(tick) {
        needsUsageAccess = !Permissions.hasUsageAccess(ctx)
        if (!needsUsageAccess) {
            loading = true
            apps = AppsScanner.getLeastRecentlyUsedApps(ctx)
            loading = false
        }
    }

    Column(Modifier.fillMaxSize()) {
        TopBarSuls("تطبيقات ما تستخدمها", onBack)
        if (needsUsageAccess) {
            Column(
                Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.weight(1f))
                Text(
                    "أحتاج صلاحية إحصائيات الاستخدام حتى أعرف أي تطبيق ما فتحته من زمان",
                    color = Suls.TextSecondary, textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { launcher.launch(Permissions.requestUsageAccessIntent()) },
                    colors = ButtonDefaults.buttonColors(containerColor = Suls.Accent)
                ) { Text("تفعيل الصلاحية", color = Suls.Bg) }
                Spacer(Modifier.weight(1f))
            }
        } else if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Suls.Accent)
            }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
                item {
                    Text(
                        "مرتبة من الأقل استخداماً — أول شي بالقائمة ما فتحته من أطول فترة",
                        color = Suls.TextSecondary, fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }
                items(apps, key = { it.packageName }) { app ->
                    AppRow(app, onUninstall = {
                        ctx.startActivity(AppsScanner.uninstallIntent(app.packageName))
                    })
                    Spacer(Modifier.height(10.dp))
                }
                item { Spacer(Modifier.height(40.dp)) }
            }
        }
    }
}

@Composable
private fun AppRow(app: InstalledAppInfo, onUninstall: () -> Unit) {
    Surface(color = Suls.Card, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            val bmp = remember(app.packageName) { app.icon?.toBitmap(96, 96)?.asImageBitmap() }
            if (bmp != null) {
                Image(bmp, null, modifier = Modifier.size(44.dp))
            } else {
                Icon(Icons.Default.Android, null, tint = Suls.TextSecondary, modifier = Modifier.size(44.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(app.label, color = Suls.TextPrimary, fontSize = 15.sp, maxLines = 1)
                Text(
                    if (app.lastUsedMillis == 0L) "ما فيه سجل استخدام"
                    else "آخر استخدام: " + DateUtils.getRelativeTimeSpanString(app.lastUsedMillis),
                    color = Suls.TextSecondary, fontSize = 12.sp
                )
            }
            TextButton(onClick = onUninstall) { Text("إلغاء التثبيت", color = Suls.Danger, fontSize = 13.sp) }
        }
    }
}
