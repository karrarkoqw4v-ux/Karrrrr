package iq.student.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import iq.student.ui.theme.AppColors
import iq.student.ui.theme.Tone
import iq.student.ui.theme.dimens

/** زر رئيسي (ممتلئ) أو ثانوي (outline) بارتفاع ثابت 56dp */
@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    outlined: Boolean = false,
) {
    val shape = RoundedCornerShape(dimens.radiusButton)
    val bg = if (outlined) AppColors.Surface else AppColors.Primary
    val fg = if (outlined) AppColors.Ink else Color.White
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(dimens.buttonHeight)
            .bounceClick(onClick = onClick)
            .clip(shape)
            .background(bg)
            .then(if (outlined) Modifier.border(1.2.dp, AppColors.Line, shape) else Modifier),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = fg, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
        }
        Text(text, color = fg, style = MaterialTheme.typography.labelLarge)
    }
}

/** حقل إدخال مع عنوان صغير وأيقونة بداية (يمين) وأيقونة نهاية اختيارية (يسار) */
@Composable
fun AppField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    leading: ImageVector,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    trailing: ImageVector? = null,
    onTrailing: (() -> Unit)? = null,
    password: Boolean = false,
    showText: Boolean = true,
) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = AppColors.Muted)
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            modifier = Modifier.fillMaxWidth().heightIn(min = dimens.fieldHeight),
            singleLine = true,
            shape = RoundedCornerShape(dimens.radiusField),
            textStyle = MaterialTheme.typography.bodyMedium,
            placeholder = { Text(placeholder, color = AppColors.Faint, style = MaterialTheme.typography.bodyMedium) },
            leadingIcon = { Icon(leading, null, modifier = Modifier.size(22.dp)) },
            trailingIcon = if (trailing != null) {
                { Icon(trailing, null, modifier = Modifier.size(22.dp).clickable(enabled = onTrailing != null) { onTrailing?.invoke() }) }
            } else null,
            visualTransformation = if (password && !showText)
                androidx.compose.ui.text.input.PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AppColors.Primary,
                unfocusedBorderColor = AppColors.Line,
                focusedContainerColor = AppColors.Surface,
                unfocusedContainerColor = AppColors.Surface,
                focusedLeadingIconColor = AppColors.Primary,
                unfocusedLeadingIconColor = AppColors.Faint,
                unfocusedTrailingIconColor = AppColors.Faint,
            ),
        )
    }
}

/** أيقونة واضحة: مربع ملوّن بتدرج + أيقونة بيضاء كبيرة */
@Composable
fun IconTile(icon: ImageVector, tone: Tone, modifier: Modifier = Modifier, size: androidx.compose.ui.unit.Dp = dimens.iconTile) {
    Box(
        modifier.size(size).clip(RoundedCornerShape(size * 0.3f))
            .background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(tone.fg, tone.fg.copy(alpha = 0.78f)))),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = Color.White, modifier = Modifier.size(size * 0.56f)) }
}

@Composable
fun AppChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Box(
        Modifier.height(36.dp).bounceClick(onClick = onClick).clip(shape)
            .background(if (selected) AppColors.Primary else AppColors.Surface)
            .then(if (selected) Modifier else Modifier.border(1.dp, AppColors.Line, shape))
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp),
            color = if (selected) Color.White else AppColors.Muted)
    }
}

@Composable
fun AppCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(dimens.radiusCard),
        color = AppColors.Surface,
        border = BorderStroke(1.dp, AppColors.Line),
        shadowElevation = 2.dp,
    ) { Column(content = content) }
}

/** ترويسة الشاشة: زر رجوع دائري + عنوان بالوسط */
@Composable
fun ScreenHeader(title: String, onBack: (() -> Unit)?) {
    Box(
        Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = dimens.pad).padding(top = 8.dp).height(48.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, color = AppColors.Ink, textAlign = TextAlign.Center)
        if (onBack != null) {
            Box(
                Modifier.align(Alignment.CenterStart).size(dimens.touchMin).bounceClick(onClick = onBack).clip(CircleShape)
                    .background(AppColors.Surface).border(1.dp, AppColors.Line, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                // في RTL يشير السهم لليمين تلقائياً (AutoMirrored)
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, "رجوع", modifier = Modifier.size(22.dp))
            }
        }
    }
}

data class NavItem(val icon: ImageVector, val label: String)

val NavItems = listOf(
    NavItem(Icons.Outlined.Home, "الرئيسية"),
    NavItem(Icons.Outlined.MenuBook, "الكتب"),
    NavItem(Icons.Outlined.Description, "الملازم"),
    NavItem(Icons.Outlined.BarChart, "تقدمي"),
    NavItem(Icons.Outlined.Person, "حسابي"),
)

@Composable
fun AppBottomBar(selected: Int, onSelect: (Int) -> Unit) {
    Surface(color = AppColors.Surface, shadowElevation = 8.dp, shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)) {
        Row(
            Modifier.fillMaxWidth().navigationBarsPadding().height(dimens.navHeight).padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NavItems.forEachIndexed { i, item ->
                val on = i == selected
                val col by androidx.compose.animation.animateColorAsState(if (on) AppColors.Primary else AppColors.Faint, label = "navcol")
                Column(
                    Modifier.weight(1f).bounceClick { onSelect(i) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        Modifier.clip(RoundedCornerShape(14.dp))
                            .background(androidx.compose.animation.animateColorAsState(if (on) AppColors.PrimarySoft else Color.Transparent, label = "navbg").value)
                            .padding(horizontal = 18.dp, vertical = 5.dp),
                    ) { Icon(item.icon, null, tint = col, modifier = Modifier.size(dimens.iconNav)) }
                    Spacer(Modifier.height(2.dp))
                    Text(item.label, color = col, style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (on) FontWeight.Bold else FontWeight.Medium)
                }
            }
        }
    }
}
