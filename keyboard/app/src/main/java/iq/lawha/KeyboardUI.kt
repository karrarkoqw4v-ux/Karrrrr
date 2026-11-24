package iq.lawha

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private class SkinColors(
    val kbBg: Brush,
    val keyBg: Color,
    val keyBgModifier: (Modifier) -> Modifier,
    val delBg: Color,
    val enterBg: Color,
    val text: Color,
    val delText: Color,
    val border: Color?,
)

@Composable
private fun skinFor(skin: Skin): SkinColors = when (skin) {
    Skin.WATER -> SkinColors(
        kbBg = Brush.linearGradient(listOf(Color(0xFF052830), Color(0xFF020E12))),
        keyBg = Color(0x662DD4E8),
        keyBgModifier = { it },
        delBg = Color(0x99B4FFFA),
        enterBg = Color(0x992DD4E8),
        text = Color.White,
        delText = Color(0xFF053038),
        border = Color(0x59B4FFFA),
    )
    Skin.IOS_LIGHT -> SkinColors(
        kbBg = Brush.linearGradient(listOf(Color(0xFFD1D3D9), Color(0xFFD1D3D9))),
        keyBg = Color.White,
        keyBgModifier = { it },
        delBg = Color(0xFFAEB1BA),
        enterBg = Color.White,
        text = Color.Black,
        delText = Color.Black,
        border = null,
    )
    Skin.IOS_DARK -> SkinColors(
        // قيم مسحوبة حرفياً من لقطة شاشة حقيقية لكيبورد آيفون
        kbBg = Brush.linearGradient(listOf(Color(0xFF1F1F1F), Color(0xFF1F1F1F))),
        keyBg = Color(0xFF434343),
        keyBgModifier = { it },
        delBg = Color(0xFF909090),
        enterBg = Color(0xFF434343),
        text = Color.White,
        delText = Color(0xFF1F1F1F),
        border = null,
    )
}

@Composable
fun KeyboardScreen(
    skin: Skin,
    onChar: (String) -> Unit,
    onDelete: () -> Unit,
    onSpace: () -> Unit,
    onEnter: () -> Unit,
    onGlobe: () -> Unit,
    onMic: () -> Unit,
) {
    var page by remember { mutableStateOf(Page.LETTERS) }
    var emojiOpen by remember { mutableStateOf(false) }
    val c = skinFor(skin)

    Column(Modifier.fillMaxWidth().background(c.kbBg).padding(top = 6.dp, bottom = 10.dp)) {
        if (emojiOpen) {
            EmojiPanel(c, onPick = { onChar(it) }, onClose = { emojiOpen = false })
        } else {
            Layout.rowsFor(page).forEach { row ->
                Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 3.dp)) {
                    row.forEach { k -> KeyCell(k, c, page) { def ->
                        when (def) {
                            is KeyDef.Ch -> onChar(def.char)
                            KeyDef.Shift -> page = if (page == Page.LETTERS) Page.DIACRITICS else Page.LETTERS
                            KeyDef.Delete -> onDelete()
                            else -> Unit
                        }
                    } }
                }
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 3.dp)) {
                Layout.bottomRow(page).forEach { k -> KeyCell(k, c, page) { def ->
                    when (def) {
                        KeyDef.Mode123 -> page = Page.NUMBERS
                        KeyDef.ModeAbc -> page = Page.LETTERS
                        KeyDef.ModeSymDiac -> page = Page.DIACRITICS
                        KeyDef.Space -> onSpace()
                        KeyDef.Enter -> onEnter()
                        else -> Unit
                    }
                } }
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    Icon(Icons.Outlined.EmojiEmotions, "إيموجي", tint = c.text, modifier = Modifier.size(26.dp).clickable { emojiOpen = true })
                    Icon(Icons.Outlined.Language, "تبديل اللوحة", tint = c.text, modifier = Modifier.size(26.dp).clickable(onClick = onGlobe))
                }
                Icon(Icons.Outlined.Mic, "إملاء صوتي", tint = c.text, modifier = Modifier.size(26.dp).clickable(onClick = onMic))
            }
        }
    }
}

@Composable
private fun RowScope.KeyCell(key: KeyDef, c: SkinColors, page: Page, onClick: (KeyDef) -> Unit) {
    val wide = key is KeyDef.Shift || key is KeyDef.Delete || key is KeyDef.Mode123 || key is KeyDef.ModeAbc || key is KeyDef.ModeSymDiac || key is KeyDef.Enter
    val isSpace = key is KeyDef.Space
    val bg = when (key) {
        KeyDef.Delete -> c.delBg
        KeyDef.Enter -> c.enterBg
        else -> c.keyBg
    }
    val fg = if (key is KeyDef.Delete) c.delText else c.text
    Box(
        Modifier
            .weight(if (isSpace) 5f else if (wide) 1.5f else 1f)
            .padding(horizontal = 3.dp)
            .height(50.dp)
            .background(bg, RoundedCornerShape(10.dp))
            .then(if (c.border != null) Modifier.border(1.dp, c.border, RoundedCornerShape(10.dp)) else Modifier)
            .clickable { onClick(key) },
        contentAlignment = Alignment.Center,
    ) {
        when (key) {
            is KeyDef.Ch -> Text(key.char, color = fg, fontSize = 19.sp)
            KeyDef.Shift -> Icon(Icons.Outlined.ArrowUpward, "تبديل", tint = fg, modifier = Modifier.size(20.dp))
            KeyDef.Delete -> Icon(Icons.Outlined.Backspace, "حذف", tint = fg, modifier = Modifier.size(20.dp))
            KeyDef.Mode123 -> Text("123", color = fg, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            KeyDef.ModeAbc -> Text("ابج", color = fg, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            KeyDef.ModeSymDiac -> Text("؟َ#", color = fg, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            KeyDef.Enter -> Icon(Icons.AutoMirrored.Outlined.KeyboardReturn, "إدخال", tint = fg, modifier = Modifier.size(20.dp))
            else -> {}
        }
    }
}

private val COMMON_EMOJI = listOf(
    "😀","😂","🥰","😍","😅","😊","😉","😎","🤔","😴","😢","😭","😡","😱","🙏","👍","👎","👏","🙌","💪",
    "❤️","💔","🔥","⭐","🎉","✨","☀️","🌙","☕","🍵","🍎","🍉","⚽","🎵","📍","✅","❌","⏰","💬","📷",
)

@Composable
private fun ColumnScope.EmojiPanel(c: SkinColors, onPick: (String) -> Unit, onClose: () -> Unit) {
    Column(Modifier.fillMaxWidth().height(220.dp)) {
        LazyVerticalGrid(GridCells.Fixed(8), Modifier.weight(1f).padding(horizontal = 8.dp)) {
            items(COMMON_EMOJI) { e ->
                Box(Modifier.padding(4.dp).clickable { onPick(e) }, contentAlignment = Alignment.Center) {
                    Text(e, fontSize = 24.sp)
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.End) {
            Text("رجوع", color = c.text, modifier = Modifier.clickable(onClick = onClose).padding(8.dp))
        }
    }
}
