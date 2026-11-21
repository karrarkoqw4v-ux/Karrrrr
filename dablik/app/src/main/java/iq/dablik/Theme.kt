package iq.dablik

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.Icon

/** ألوان وأبعاد مستوحاة من واجهة One UI (الوضع الداكن AMOLED) */
object OneUi {
    val Bg = Color(0xFF000000)
    val Card = Color(0xFF1C1C1E)
    val Card2 = Color(0xFF2A2A2D)
    val Blue = Color(0xFF3E91FF)
    val BlueSoft = Color(0xFF1B3556)
    val Ink = Color(0xFFF5F5F5)
    val Muted = Color(0xFFA0A0A5)
    val Faint = Color(0xFF6E6E73)
    val Line = Color(0xFF2E2E31)
    val Danger = Color(0xFFFF5A5A)
    val Green = Color(0xFF3DDC84)
    val RCard = 28.dp
    val RButton = 28.dp
}

val AppFont = FontFamily(
    Font(R.font.tajawal_medium, FontWeight.Normal),
    Font(R.font.tajawal_bold, FontWeight.Medium),
    Font(R.font.tajawal_bold, FontWeight.SemiBold),
    Font(R.font.tajawal_extrabold, FontWeight.Bold),
)

private fun st(size: Float, w: FontWeight) = TextStyle(fontFamily = AppFont, fontSize = size.sp, fontWeight = w, lineHeight = (size * 1.45f).sp)

val AppTypography = Typography(
    displaySmall = st(38f, FontWeight.Bold),
    headlineSmall = st(26f, FontWeight.Bold),
    titleLarge = st(21f, FontWeight.Bold),
    titleMedium = st(18f, FontWeight.SemiBold),
    bodyLarge = st(17f, FontWeight.Medium),
    bodyMedium = st(15f, FontWeight.Normal),
    labelLarge = st(17f, FontWeight.Bold),
    labelMedium = st(14f, FontWeight.Medium),
    labelSmall = st(12.5f, FontWeight.Normal),
)

@Composable
fun DablikTheme(content: @Composable () -> Unit) {
    val d = LocalDensity.current
    val capped = Density(d.density, fontScale = d.fontScale.coerceIn(0.9f, 1.2f))
    androidx.compose.runtime.CompositionLocalProvider(LocalDensity provides capped) {
        MaterialTheme(
            colorScheme = darkColorScheme(primary = OneUi.Blue, background = OneUi.Bg, surface = OneUi.Card, onSurface = OneUi.Ink, onBackground = OneUi.Ink),
            typography = AppTypography, content = content,
        )
    }
}

/** ضغطة بأسلوب One UI: تصغير ناعم بنابض بدون تموّج، ورجوع سريع */
fun Modifier.oneClick(enabled: Boolean = true, onClick: () -> Unit): Modifier = composed {
    var pressed by remember { mutableStateOf(false) }
    val s by animateFloatAsState(if (pressed) 0.95f else 1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium), label = "press")
    this.scale(s).pointerInput(enabled) {
        if (enabled) detectTapGestures(onPress = { pressed = true; tryAwaitRelease(); pressed = false }, onTap = { onClick() })
    }
}

/** سطح زجاجي: شفافية خفيفة مع تدرج لامع وحد مضيء */
fun Modifier.glass(shape: Shape, tint: Color = Color.White, a1: Float = 0.14f, a2: Float = 0.07f): Modifier =
    this.clip(shape)
        .background(Brush.verticalGradient(listOf(tint.copy(alpha = a1), tint.copy(alpha = a2))))
        .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.38f), Color.White.copy(alpha = 0.05f))), shape)

/** زر One UI زجاجي: كبسولة كبيرة شفافة قليلاً. primary = أزرق، غير ذلك رمادي زجاجي */
@Composable
fun OneButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, primary: Boolean = true, enabled: Boolean = true, icon: ImageVector? = null) {
    val shape = RoundedCornerShape(OneUi.RButton)
    val base = if (!enabled) Color(0xFF3A3A3D) else if (primary) OneUi.Blue else Color.White
    val a1 = if (!enabled) 0.6f else if (primary) 0.88f else 0.16f
    val a2 = if (!enabled) 0.5f else if (primary) 0.74f else 0.08f
    Box(modifier.fillMaxWidth().height(58.dp).oneClick(enabled, onClick).glass(shape, base, a1, a2), contentAlignment = Alignment.Center) {
        Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.20f), Color.Transparent), endY = 90f)))
        Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) { Icon(icon, null, tint = if (enabled) Color.White else OneUi.Faint, modifier = Modifier.size(24.dp)); Spacer(Modifier.width(8.dp)) }
            Text(text, style = MaterialTheme.typography.labelLarge, color = if (enabled) Color.White else OneUi.Faint)
        }
    }
}

@Composable
fun OneCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.clip(RoundedCornerShape(OneUi.RCard)).background(OneUi.Card), content = content)
}

/** حقل نص One UI: خلفية رمادية داكنة، عنوان صغير فوق النص، خط سفلي أزرق عند التركيز */
@Composable
fun OneField(
    value: String, onChange: (String) -> Unit, label: String, modifier: Modifier = Modifier, hint: String = "",
    keyboard: KeyboardType = KeyboardType.Text, singleLine: Boolean = true, error: String? = null, minHeight: Int = 68,
) {
    var focused by remember { mutableStateOf(false) }
    Column(modifier) {
        Column(
            Modifier.fillMaxWidth().heightIn(min = minHeight.dp).clip(RoundedCornerShape(22.dp)).background(OneUi.Card)
                .border(if (error != null) 1.5.dp else if (focused) 1.5.dp else 0.dp, if (error != null) OneUi.Danger else OneUi.Blue, RoundedCornerShape(22.dp))
                .padding(horizontal = 18.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = if (focused) OneUi.Blue else OneUi.Muted)
            BasicTextField(
                value, onChange, Modifier.fillMaxWidth().onFocusChangedCompat { focused = it }, singleLine = singleLine,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = OneUi.Ink), cursorBrush = SolidColor(OneUi.Blue),
                keyboardOptions = KeyboardOptions(keyboardType = keyboard),
                decorationBox = { inner -> if (value.isEmpty()) Text(hint, style = MaterialTheme.typography.bodyLarge, color = OneUi.Faint); inner() },
            )
        }
        if (error != null) Text(error, style = MaterialTheme.typography.labelSmall, color = OneUi.Danger, modifier = Modifier.padding(start = 18.dp, top = 4.dp))
    }
}

private fun Modifier.onFocusChangedCompat(cb: (Boolean) -> Unit): Modifier = this.onFocusChanged { cb(it.isFocused) }

/** تظليل (تغويش) أعلى وأسفل الشاشة عند تمرير المحتوى تحتها */
@Composable
fun BoxScope.EdgeShade(top: androidx.compose.ui.unit.Dp, bottom: androidx.compose.ui.unit.Dp) {
    Box(Modifier.align(Alignment.TopCenter).fillMaxWidth().height(top).background(Brush.verticalGradient(listOf(Color(0xB3000000), Color(0x66000000), Color.Transparent))))
    Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(bottom).background(Brush.verticalGradient(listOf(Color.Transparent, Color(0x80000000), Color(0xB8000000)))))
}


/** ظهور متدرج: يتلاشى ويصعد من الأسفل بتأخير حسب الترتيب */
@Composable
fun Entrance(index: Int, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { kotlinx.coroutines.delay(index * 55L); shown = true }
    val a by animateFloatAsState(if (shown) 1f else 0f, tween(450), label = "a")
    val dy by animateFloatAsState(if (shown) 0f else 28f, tween(480, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)), label = "dy")
    Box(modifier.graphicsLayer { alpha = a; translationY = dy * density }) { content() }
}

/** مفتاح تبديل بأسلوب One UI مع نابض */
@Composable
fun OneSwitch(checked: Boolean, onChange: (Boolean) -> Unit) {
    val x by animateDpAsState(if (checked) 24.dp else 3.dp, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium), label = "x")
    val track by animateColorAsState(if (checked) OneUi.Blue else Color(0xFF3A3A3D), tween(220), label = "track")
    Box(Modifier.size(width = 54.dp, height = 32.dp).clip(CircleShape).background(track).pointerInput(Unit) { detectTapGestures { onChange(!checked) } }) {
        Box(Modifier.offset(x = x, y = 4.dp).size(24.dp).clip(CircleShape).background(Color.White))
    }
}

/** صف إعداد: أيقونة + عنوان + شرح + مفتاح */
@Composable
fun ToggleRow(icon: ImageVector, tint: Color, title: String, sub: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(44.dp).clip(CircleShape).background(tint.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) { Icon(icon, null, tint = tint, modifier = Modifier.size(24.dp)) }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = OneUi.Ink)
            if (sub.isNotEmpty()) Text(sub, style = MaterialTheme.typography.labelSmall, color = OneUi.Muted)
        }
        Spacer(Modifier.width(10.dp))
        OneSwitch(checked, onChange)
    }
}
