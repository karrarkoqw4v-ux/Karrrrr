package iq.hala.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import iq.hala.ui.theme.*
import kotlinx.coroutines.delay

private val AvatarColors = listOf(0xFF0FB5A0, 0xFFFF7A59, 0xFF7C5CFF, 0xFFF5A524, 0xFF3B82F6, 0xFFE5484D, 0xFF12A594, 0xFFB04FD9).map { Color(it) }

/** ضغطة نابضة بميلان خفيف (ثلاثي الأبعاد) */
fun Modifier.bounceClick(enabled: Boolean = true, onClick: () -> Unit): Modifier = composed {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.92f else 1f, spring(dampingRatio = 0.42f, stiffness = 520f), label = "s")
    val tilt by animateFloatAsState(if (pressed) 6f else 0f, spring(dampingRatio = 0.5f, stiffness = 420f), label = "t")
    this.graphicsLayer { scaleX = scale; scaleY = scale; rotationX = tilt; cameraDistance = 14f * density }
        .clickable(interactionSource = source, indication = null, enabled = enabled, onClick = onClick)
}

/** دخول متتابع للعناصر */
@Composable
fun Entrance(index: Int, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(index * 55L); shown = true }
    val a by animateFloatAsState(if (shown) 1f else 0f, tween(420), label = "a")
    val dy by animateDpAsState(if (shown) 0.dp else 26.dp, spring(dampingRatio = 0.72f, stiffness = 300f), label = "y")
    Box(modifier.graphicsLayer { alpha = a; translationY = dy.toPx() }) { content() }
}

@Composable
fun rememberFloat(amp: Float = 5f, ms: Int = 2400): Float {
    val t = rememberInfiniteTransition(label = "f")
    val v by t.animateFloat(-amp, amp, infiniteRepeatable(tween(ms, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "v")
    return v
}

/** شعار هلا: مربع بتدرج تركواز وفقاعة محادثة بيضاء بثلاث نقاط */
@Composable
fun HalaLogo(size: Dp, modifier: Modifier = Modifier, onGradient: Boolean = false) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension
        val bg = if (onGradient) Color.White else Color.Unspecified
        if (onGradient) drawRoundRect(bg, size = this.size, cornerRadius = CornerRadius(s * 0.3f))
        else drawRoundRect(Brush.linearGradient(listOf(Primary, PrimaryDark)), size = this.size, cornerRadius = CornerRadius(s * 0.3f))
        val bw = s * 0.54f
        val left = (s - bw) / 2f
        val top = s * 0.28f
        val fill = if (onGradient) Primary else Color.White
        drawRoundRect(fill, Offset(left, top), Size(bw, bw * 0.72f), CornerRadius(bw * 0.28f))
        val tail = Path().apply {
            moveTo(left + bw * 0.62f, top + bw * 0.66f); lineTo(left + bw * 0.88f, top + bw * 0.98f); lineTo(left + bw * 0.42f, top + bw * 0.72f); close()
        }
        drawPath(tail, fill)
        val dot = if (onGradient) Color.White else Primary
        for (i in -1..1) drawCircle(dot, s * 0.035f, Offset(s / 2f + i * bw * 0.22f, top + bw * 0.34f))
    }
}

enum class Ring { None, New, Seen }

@Composable
fun Avatar(name: String, idx: Int, size: Dp, modifier: Modifier = Modifier, ring: Ring = Ring.None, online: Boolean = false) {
    val col = AvatarColors[idx.mod(AvatarColors.size)]
    val p = pal
    Box(modifier.size(size + if (ring != Ring.None) 10.dp else 0.dp), contentAlignment = Alignment.Center) {
        if (ring != Ring.None) {
            Box(
                Modifier.matchParentSize().border(
                    2.5.dp,
                    if (ring == Ring.New) Brush.sweepGradient(listOf(Primary, Accent, Primary)) else Brush.linearGradient(listOf(p.line, p.line)),
                    CircleShape,
                ),
            )
        }
        Box(
            Modifier.size(size).clip(CircleShape).background(Brush.linearGradient(listOf(col.copy(alpha = 0.75f), col))),
            contentAlignment = Alignment.Center,
        ) { Text(name.take(1), color = Color.White, style = MaterialTheme.typography.titleMedium.copy(fontSize = (size.value * 0.42f).sp)) }
        if (online) Box(Modifier.align(Alignment.BottomEnd).size(size * 0.28f).clip(CircleShape).background(p.bg).padding(2.dp).clip(CircleShape).background(Success))
    }
}

private val Int.sp get() = androidx.compose.ui.unit.TextUnit(this.toFloat(), androidx.compose.ui.unit.TextUnitType.Sp)
private val Float.sp get() = androidx.compose.ui.unit.TextUnit(this, androidx.compose.ui.unit.TextUnitType.Sp)

/** صورة مرسومة بالكود (غروب/جبال/مدينة/بحر/صحراء/حديقة) للمنشورات التجريبية */
@Composable
fun PhotoArt(kind: Int, modifier: Modifier = Modifier) {
    val k = kind.mod(6)
    val sets = listOf(
        listOf(0xFFFFB36B, 0xFFFF6F91, 0xFF2B2D5B, 0xFF3D3B7A), listOf(0xFF8EC5FF, 0xFFE3F2FF, 0xFF2F6F5B, 0xFF3F8F76),
        listOf(0xFF1E2A4A, 0xFF5B6BA8, 0xFF10162B, 0xFF1B2440), listOf(0xFF7DE0D0, 0xFFCFF5EE, 0xFF0FA3A3, 0xFF0A7F86),
        listOf(0xFFFFD79A, 0xFFFFF1D6, 0xFFD98A3D, 0xFFB96E2B), listOf(0xFFFFB5C8, 0xFFFFE4EC, 0xFF5BBF8A, 0xFF3E9D6C),
    ).map { l -> l.map { Color(it) } }[k]
    Canvas(modifier) {
        val w = size.width; val h = size.height
        drawRect(Brush.verticalGradient(listOf(sets[0], sets[1])))
        drawCircle(if (k == 2) Color(0xFFE8ECF5) else Color(0xFFFFF3D1), h * 0.1f, Offset(if (k % 2 == 0) w * 0.72f else w * 0.28f, h * 0.3f))
        if (k == 2) {
            for (i in 0 until 9) {
                val bw = w * 0.09f; val bx = w * 0.04f + i * w * 0.108f; val bh = h * (0.18f + 0.22f * ((i * 37) % 7) / 7f)
                drawRect(sets[2], Offset(bx, h - bh), Size(bw, bh))
                for (r in 0 until 3) drawRect(Color(0xFFFFD76A), Offset(bx + bw * 0.25f, h - bh + 8 + r * 12), Size(bw * 0.3f, 4f))
            }
        } else {
            val p1 = Path().apply { moveTo(0f, h); lineTo(0f, h * 0.72f); lineTo(w * 0.3f, h * 0.58f); lineTo(w * 0.55f, h * 0.74f); lineTo(w, h * 0.62f); lineTo(w, h); close() }
            val p2 = Path().apply { moveTo(0f, h); lineTo(0f, h * 0.86f); lineTo(w * 0.4f, h * 0.74f); lineTo(w * 0.8f, h * 0.88f); lineTo(w, h * 0.8f); lineTo(w, h); close() }
            drawPath(p1, sets[2]); drawPath(p2, sets[3])
        }
    }
}

@Composable
fun GradButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null, enabled: Boolean = true, outlined: Boolean = false) {
    val p = pal
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier.fillMaxWidth().height(52.dp).bounceClick(enabled, onClick).clip(shape)
            .then(if (outlined) Modifier.border(1.4.dp, p.line, shape) else Modifier.background(Brush.horizontalGradient(listOf(Primary, PrimaryDark))))
            .graphicsLayer { alpha = if (enabled) 1f else 0.5f },
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) { Icon(icon, null, tint = if (outlined) p.ink else Color.White, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)) }
        Text(text, color = if (outlined) p.ink else Color.White, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun HalaField(
    value: String, onChange: (String) -> Unit, placeholder: String, leading: ImageVector? = null,
    modifier: Modifier = Modifier, trailing: ImageVector? = null, onTrailing: (() -> Unit)? = null,
    password: Boolean = false, show: Boolean = true, error: Boolean = false, label: String? = null, singleLine: Boolean = true,
) {
    val p = pal
    Column(modifier) {
        if (label != null) { Text(label, style = MaterialTheme.typography.labelMedium, color = p.muted); Spacer(Modifier.height(6.dp)) }
        OutlinedTextField(
            value = value, onValueChange = onChange, singleLine = singleLine, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            shape = RoundedCornerShape(14.dp), textStyle = MaterialTheme.typography.bodyMedium.copy(color = p.ink),
            placeholder = { Text(placeholder, color = p.faint, style = MaterialTheme.typography.bodyMedium) },
            leadingIcon = leading?.let { { Icon(it, null, modifier = Modifier.size(21.dp)) } },
            trailingIcon = if (trailing != null) { { Icon(trailing, null, modifier = Modifier.size(21.dp).clickable(enabled = onTrailing != null) { onTrailing?.invoke() }) } } else null,
            visualTransformation = if (password && !show) androidx.compose.ui.text.input.PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = if (error) Danger else Primary, unfocusedBorderColor = if (error) Danger else p.line,
                focusedContainerColor = p.bg, unfocusedContainerColor = p.bg2,
                focusedLeadingIconColor = Primary, unfocusedLeadingIconColor = p.faint, unfocusedTrailingIconColor = p.faint,
                focusedTextColor = p.ink, unfocusedTextColor = p.ink,
            ),
        )
    }
}

@Composable
fun Chip(text: String, selected: Boolean, onClick: () -> Unit) {
    val p = pal
    val shape = RoundedCornerShape(50)
    Box(
        Modifier.height(34.dp).bounceClick(onClick = onClick).clip(shape)
            .then(if (selected) Modifier.background(Brush.horizontalGradient(listOf(Primary, PrimaryDark))) else Modifier.background(p.bg2))
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, style = MaterialTheme.typography.labelMedium, color = if (selected) Color.White else p.muted) }
}

@Composable
fun TopBack(title: String, onBack: (() -> Unit)?, actions: @Composable RowScope.() -> Unit = {}) {
    val p = pal
    Box(Modifier.fillMaxWidth().statusBarsPadding().height(56.dp).padding(horizontal = 8.dp), contentAlignment = Alignment.Center) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = p.ink, textAlign = TextAlign.Center)
        if (onBack != null) {
            Box(Modifier.align(Alignment.CenterStart).size(44.dp).bounceClick(onClick = onBack), contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, "رجوع", tint = p.ink)
            }
        }
        Row(Modifier.align(Alignment.CenterEnd), verticalAlignment = Alignment.CenterVertically, content = actions)
    }
}

@Composable
fun IconBtn(icon: ImageVector, desc: String, tint: Color = pal.ink, size: Dp = 24.dp, onClick: () -> Unit) {
    Box(Modifier.size(44.dp).bounceClick(onClick = onClick), contentAlignment = Alignment.Center) { Icon(icon, desc, tint = tint, modifier = Modifier.size(size)) }
}

@Composable
fun ErrorBanner(msg: String) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color(0x1FE5484D)).padding(14.dp)) {
        Text(msg, style = MaterialTheme.typography.bodyMedium, color = Danger)
    }
}

@Composable
fun SettingRow(icon: ImageVector, title: String, sub: String? = null, tint: Color = Primary, toggle: Boolean? = null, value: String? = null, onToggle: ((Boolean) -> Unit)? = null, onClick: (() -> Unit)? = null) {
    val p = pal
    Row(
        Modifier.fillMaxWidth().then(if (onClick != null) Modifier.bounceClick(onClick = onClick) else Modifier).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(40.dp).clip(RoundedCornerShape(13.dp)).background(tint.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = p.ink)
            if (sub != null) Text(sub, style = MaterialTheme.typography.labelMedium, color = p.muted)
        }
        if (toggle != null) Switch(checked = toggle, onCheckedChange = onToggle, colors = SwitchDefaults.colors(checkedTrackColor = Primary))
        else if (value != null) Text(value, style = MaterialTheme.typography.labelMedium, color = p.muted)
    }
}
