package iq.lawha

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Bg = Color(0xFF0E1512)
private val Card = Color(0xFF162420)
private val Accent = Color(0xFF0EA5B7)
private val TextPrimary = Color(0xFFEFFBF7)
private val TextSecondary = Color(0xFF8FA69D)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        CrashLog.install(this)
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme { SettingsScreen() }
        }
    }
}

@Composable
private fun SettingsScreen() {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var skin by remember { mutableStateOf(Prefs.getSkin(ctx)) }
    var crash by remember { mutableStateOf(CrashLog.read(ctx)) }

    Box(Modifier.fillMaxSize().background(Bg)) {
        Column(Modifier.fillMaxSize().padding(24.dp)) {
            Spacer(Modifier.height(24.dp))
            Text("لوحتي", color = TextPrimary, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text("كيبورد عربي بثلاث أشكال: مائي، آيفون فاتح، آيفون داكن", color = TextSecondary, fontSize = 14.sp)
            Spacer(Modifier.height(24.dp))

            Surface(color = Card, shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp)) {
                    Text("خطوة 1: فعّل لوحتي من إعدادات الجهاز", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = { ctx.startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) },
                        colors = ButtonDefaults.buttonColors(containerColor = Accent),
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("فتح إعدادات لوحات المفاتيح", color = Bg) }
                }
            }
            Spacer(Modifier.height(14.dp))
            Surface(color = Card, shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp)) {
                    Text("خطوة 2: اختارها كلوحة حالية", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = {
                            val imm = ctx.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as InputMethodManager
                            imm.showInputMethodPicker()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Accent),
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("اختيار لوحة المفاتيح الآن", color = Bg) }
                }
            }

            if (crash != null) {
                Spacer(Modifier.height(14.dp))
                Surface(color = Color(0xFF3A1414), shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp)) {
                        Text("آخر خطأ/كراش محفوظ (انسخه وأرسله حتى أصلحه)", color = Color(0xFFFFB4AB), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            crash.orEmpty(), color = Color(0xFFFFDAD6), fontSize = 11.sp,
                            modifier = Modifier.fillMaxWidth().clickable {
                                val cm = ctx.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                cm.setPrimaryClip(android.content.ClipData.newPlainText("crash", crash))
                                android.widget.Toast.makeText(ctx, "تم نسخ نص الخطأ", android.widget.Toast.LENGTH_SHORT).show()
                            },
                        )
                        Spacer(Modifier.height(10.dp))
                        Text("مسح", color = Color(0xFFFFB4AB), modifier = Modifier.clickable { CrashLog.clear(ctx); crash = null })
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Text("شكل اللوحة", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.height(12.dp))
            SkinRow("مائي شفاف", Skin.WATER, skin, Color(0xFF0EA5B7)) { skin = it; Prefs.setSkin(ctx, it) }
            Spacer(Modifier.height(10.dp))
            SkinRow("آيفون فاتح", Skin.IOS_LIGHT, skin, Color(0xFFD1D3D9)) { skin = it; Prefs.setSkin(ctx, it) }
            Spacer(Modifier.height(10.dp))
            SkinRow("آيفون داكن", Skin.IOS_DARK, skin, Color(0xFF434343)) { skin = it; Prefs.setSkin(ctx, it) }
        }
    }
}

@Composable
private fun SkinRow(title: String, value: Skin, selected: Skin, swatch: Color, onPick: (Skin) -> Unit) {
    val isSel = value == selected
    Surface(
        color = Card, shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth().clickable { onPick(value) },
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(34.dp).background(swatch, RoundedCornerShape(10.dp)))
            Spacer(Modifier.width(14.dp))
            Text(title, color = TextPrimary, fontSize = 16.sp, modifier = Modifier.weight(1f))
            if (isSel) Text("✓ مفعّل", color = Accent, fontWeight = FontWeight.Bold)
        }
    }
}
