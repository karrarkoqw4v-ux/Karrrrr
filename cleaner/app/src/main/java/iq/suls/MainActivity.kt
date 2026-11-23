package iq.suls

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

sealed class Screen { object Onboarding: Screen(); object Home: Screen(); object Files: Screen(); object Apps: Screen(); object Junk: Screen() }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var screen by remember { mutableStateOf<Screen>(Screen.Onboarding) }
            var filesPermTick by remember { mutableStateOf(0) }

            MaterialTheme {
                Box(Modifier.fillMaxSize().background(Suls.Bg)) {
                    when (val s = screen) {
                        is Screen.Onboarding -> OnboardingScreen(
                            onContinue = { screen = Screen.Home }
                        )
                        is Screen.Home -> HomeScreen(
                            onOpenFiles = { screen = Screen.Files },
                            onOpenApps = { screen = Screen.Apps },
                            onOpenJunk = { screen = Screen.Junk }
                        )
                        is Screen.Files -> FilesScreen(onBack = { screen = Screen.Home })
                        is Screen.Apps -> AppsScreen(onBack = { screen = Screen.Home })
                        is Screen.Junk -> JunkScreen(onBack = { screen = Screen.Home })
                    }
                }
            }
        }
    }
}

@Composable
fun OnboardingScreen(onContinue: () -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var filesGranted by remember { mutableStateOf(Permissions.hasAllFilesAccess()) }
    var usageGranted by remember { mutableStateOf(Permissions.hasUsageAccess(ctx)) }

    val filesLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { filesGranted = Permissions.hasAllFilesAccess() }

    val usageLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { usageGranted = Permissions.hasUsageAccess(ctx) }

    // إعادة الفحص كل ما يرجع المستخدم من شاشة الإعدادات
    DisposableEffect(Unit) {
        val lifecycleOwner = ctx as? androidx.lifecycle.LifecycleOwner
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                filesGranted = Permissions.hasAllFilesAccess()
                usageGranted = Permissions.hasUsageAccess(ctx)
            }
        }
        lifecycleOwner?.lifecycle?.addObserver(observer)
        onDispose { lifecycleOwner?.lifecycle?.removeObserver(observer) }
    }

    Column(
        Modifier.fillMaxSize().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(60.dp))
        Box(
            Modifier.size(96.dp).clip(CircleShape).background(Suls.Accent),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Default.CleaningServices, null, tint = Suls.Bg, modifier = Modifier.size(48.dp)) }
        Spacer(Modifier.height(20.dp))
        Text("سلس", color = Suls.TextPrimary, fontSize = 34.sp, fontWeight = FontWeight.Bold)
        Text(
            "حتى أقدر أدوّر على الملفات غير المستخدمة والتطبيقات المهملة فعلياً، أحتاج صلاحيتين حقيقيتين من إعدادات الجهاز:",
            color = Suls.TextSecondary, fontSize = 15.sp, textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp)
        )
        Spacer(Modifier.height(28.dp))

        PermissionRow(
            icon = Icons.Default.Folder,
            title = "الوصول لكل الملفات",
            desc = "حتى أعرض الملفات الأخيرة وأحذف اللي تختاره فعلياً",
            granted = filesGranted,
            onClick = { filesLauncher.launch(Permissions.requestAllFilesAccessIntent(ctx)) }
        )
        Spacer(Modifier.height(16.dp))
        PermissionRow(
            icon = Icons.Default.Apps,
            title = "الوصول لإحصائيات الاستخدام",
            desc = "حتى أعرف أي تطبيق ما تستخدمه من زمان",
            granted = usageGranted,
            onClick = { usageLauncher.launch(Permissions.requestUsageAccessIntent()) }
        )

        Spacer(Modifier.weight(1f))
        Text(
            "ملاحظة: أي حذف لملف أو إلغاء تثبيت تطبيق يصير بعد موافقتك المباشرة، ما أسوي شي بالخلفية بدون علمك.",
            color = Suls.TextSecondary, fontSize = 12.sp, textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onContinue,
            enabled = filesGranted,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Suls.Accent)
        ) { Text(if (filesGranted) "متابعة" else "فعّل صلاحية الملفات أولاً", color = Suls.Bg, fontWeight = FontWeight.Bold) }
    }
}

@Composable
fun PermissionRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, desc: String, granted: Boolean, onClick: () -> Unit) {
    SulsCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Suls.Accent, modifier = Modifier.size(32.dp))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = Suls.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(desc, color = Suls.TextSecondary, fontSize = 12.sp)
            }
            if (granted) {
                Icon(Icons.Default.CheckCircle, null, tint = Suls.Accent)
            } else {
                Button(onClick = onClick, colors = ButtonDefaults.buttonColors(containerColor = Suls.AccentSoft)) {
                    Text("تفعيل", color = Color.White, fontSize = 13.sp)
                }
            }
        }
    }
}
