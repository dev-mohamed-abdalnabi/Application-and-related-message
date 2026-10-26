package com.mohamed.phone.ui

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.LruCache
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.draw.alpha
import com.mohamed.phone.Prefs
import com.mohamed.phone.data.Fmt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

// ------------------------------------------------------------------ cookie (هوية التطبيق: شكل الكوكي في الحالات الفاضية)
class CookieShape(private val lobes: Int = 9, private val amp: Float = 0.06f) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val r = min(size.width, size.height) / 2f
        val cx = size.width / 2f
        val cy = size.height / 2f
        val p = Path()
        val steps = 180
        for (i in 0..steps) {
            val a = (2.0 * PI * i / steps).toFloat()
            val rad = r * (1f - amp + amp * cos(lobes * a))
            val x = cx + rad * cos(a)
            val y = cy + rad * sin(a)
            if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
        }
        p.close()
        return Outline.Generic(p)
    }
}

@Composable
fun CookieIcon(
    icon: ImageVector, size: Dp, container: Color, tint: Color, modifier: Modifier = Modifier,
    spin: Boolean = false, desc: String? = null, onClick: (() -> Unit)? = null
) {
    val shape = remember { CookieShape() }
    val rot by rememberInfiniteTransition(label = "cookie").animateFloat(
        0f, 360f, infiniteRepeatable(tween(26000, easing = LinearEasing)), label = "rot"
    )
    val click = if (onClick != null) Modifier.clip(CircleShape).clickable(onClick = onClick) else Modifier
    Box(modifier.size(size).then(click), contentAlignment = Alignment.Center) {
        Box(Modifier.fillMaxSize().graphicsLayer { rotationZ = if (spin) rot else 0f }.clip(shape).background(container))
        Icon(icon, desc, Modifier.size(size * 0.44f), tint = tint)
    }
}

// ------------------------------------------------------------------ iOS palette (لأيقونات الإعدادات)
object IosColors {
    val Blue = Color(0xFF007AFF)
    val Green = Color(0xFF34C759)
    val Orange = Color(0xFFFF9500)
    val Red = Color(0xFFFF3B30)
    val Purple = Color(0xFFAF52DE)
    val Teal = Color(0xFF30B0C7)
    val Gray = Color(0xFF8E8E93)
}

// ------------------------------------------------------------------ avatar
object Photos {
    lateinit var app: Context
    private val cache = object : LruCache<String, ImageBitmap>(150) {}

    fun load(uri: String): ImageBitmap? {
        cache.get(uri)?.let { return it }
        return try {
            val bmp = app.contentResolver.openInputStream(Uri.parse(uri))?.use { BitmapFactory.decodeStream(it) }
            val img = bmp?.asImageBitmap()
            if (img != null) cache.put(uri, img)
            img
        } catch (e: Exception) { null }
    }
}

@Composable
fun Avatar(name: String, photo: String?, size: Dp, modifier: Modifier = Modifier, seed: String = name) {
    val cs = MaterialTheme.colorScheme
    val bmp by produceState<ImageBitmap?>(null, photo) {
        value = if (photo == null) null else withContext(Dispatchers.IO) { Photos.load(photo) }
    }
    Box(
        modifier.size(size).clip(CircleShape).background(cs.surfaceContainerHighest),
        contentAlignment = Alignment.Center
    ) {
        val b = bmp
        if (b != null) {
            Image(b, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            val ini = Fmt.initials(name)
            if (ini.isNotEmpty()) {
                Text(ini, color = cs.onSurfaceVariant, fontSize = (size.value * 0.38f).sp, fontWeight = FontWeight.Medium)
            } else {
                Icon(Ic.Person, null, Modifier.size(size * 0.52f), tint = cs.onSurfaceVariant)
            }
        }
    }
}

// ------------------------------------------------------------------ navigation
class NavTab(val label: String, val icon: ImageVector, val badge: Int = 0)

/** تاب بار iOS: مسطّح، أيقونة فوق الاسم، خط رفيع فوقه */
@Composable
fun TabBar(items: List<NavTab>, sel: Int, on: (Int) -> Unit) {
    val cs = MaterialTheme.colorScheme
    val hap = LocalHapticFeedback.current
    Column(Modifier.fillMaxWidth().background(cs.surface)) {
        Box(Modifier.fillMaxWidth().height(0.5.dp).background(cs.outlineVariant))
        Row(Modifier.fillMaxWidth().height(52.dp)) {
            items.forEachIndexed { i, t ->
                val a = i == sel
                val tint by animateColorAsState(if (a) cs.primary else cs.onSurfaceVariant, label = "tab")
                Column(
                    Modifier.weight(1f).fillMaxHeight()
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                            if (Prefs.haptic.value) hap.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            on(i)
                        },
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
                ) {
                    if (t.badge > 0) {
                        BadgedBox(badge = { Badge { Text(if (t.badge > 99) "99+" else t.badge.toString()) } }) {
                            Icon(t.icon, t.label, Modifier.size(24.dp), tint = tint)
                        }
                    } else {
                        Icon(t.icon, t.label, Modifier.size(24.dp), tint = tint)
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(t.label, color = tint, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                }
            }
        }
    }
}

/** سيجمنت كنترول iOS */
@Composable
fun Switcher(opts: List<String>, sel: Int, modifier: Modifier = Modifier, on: (Int) -> Unit) {
    val cs = MaterialTheme.colorScheme
    val light = cs.surface.luminance() > 0.5f
    val thumb = if (light) Color.White else Color(0xFF636366)
    BoxWithConstraints(modifier.fillMaxWidth().height(34.dp).clip(RoundedCornerShape(9.dp)).background(cs.surfaceContainerHigh).padding(2.dp)) {
        val w = maxWidth / opts.size
        val x by animateDpAsState(w * sel.coerceAtLeast(0), spring(0.85f, 500f), label = "sw")
        if (sel >= 0) Box(
            Modifier.offset(x = x).width(w).fillMaxHeight()
                .shadow(1.dp, RoundedCornerShape(7.dp)).clip(RoundedCornerShape(7.dp)).background(thumb)
        )
        Row(Modifier.fillMaxSize()) {
            opts.forEachIndexed { i, o ->
                Box(
                    Modifier.weight(1f).fillMaxHeight()
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { on(i) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        o, color = cs.onSurface, maxLines = 1,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = if (i == sel) FontWeight.Bold else FontWeight.Medium)
                    )
                }
            }
        }
    }
}

// ------------------------------------------------------------------ buttons
@Composable
fun BigButton(
    text: String, modifier: Modifier = Modifier, icon: ImageVector? = null, tonal: Boolean = false,
    danger: Boolean = false, height: Dp = 52.dp, onClick: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val a by animateFloatAsState(if (pressed) 0.65f else 1f, tween(90), label = "a")
    val bg = when { danger -> cs.error; tonal -> cs.surfaceContainerHigh; else -> cs.primary }
    val fg = when { danger -> cs.onError; tonal -> cs.primary; else -> cs.onPrimary }
    Row(
        modifier.fillMaxWidth().height(height).alpha(a)
            .clip(RoundedCornerShape(14.dp)).background(bg)
            .clickable(interactionSource = src, indication = null, onClick = onClick),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, null, Modifier.size(22.dp), tint = fg)
            Spacer(Modifier.width(8.dp))
        }
        Text(text, color = fg, style = MaterialTheme.typography.titleMedium)
    }
}

/** زر دايري (الكيباد، الإرسال، زرار الاتصال) */
@Composable
fun RoundAction(
    icon: ImageVector, size: Dp, container: Color, tint: Color, modifier: Modifier = Modifier,
    desc: String? = null, iconSize: Dp = 26.dp, onClick: () -> Unit
) {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val a by animateFloatAsState(if (pressed) 0.65f else 1f, tween(90), label = "ra")
    Box(
        modifier.size(size).alpha(a).clip(CircleShape).background(container)
            .clickable(interactionSource = src, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Icon(icon, desc, Modifier.size(iconSize), tint = tint) }
}

@Composable
fun IconBtn(icon: ImageVector, desc: String, modifier: Modifier = Modifier, tint: Color = MaterialTheme.colorScheme.onSurfaceVariant, onClick: () -> Unit) {
    Box(modifier.size(36.dp).clip(CircleShape).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, desc, Modifier.size(20.dp), tint = tint)
    }
}

/** زر دايري رمادي بأيقونة زرقا في الهيدر (زي ellipsis.circle) */
@Composable
fun HeaderBtn(icon: ImageVector, desc: String, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Box(
        Modifier.size(34.dp).clip(CircleShape).background(cs.surfaceContainerHigh).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Icon(icon, desc, Modifier.size(18.dp), tint = cs.primary) }
}

/** رجوع على طريقة iOS: سهم + كلمة بلون التطبيق */
@Composable
fun BackPill(onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Row(
        Modifier.height(40.dp).clip(RoundedCornerShape(10.dp)).clickable(onClick = onClick).padding(end = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Ic.Back, "رجوع", Modifier.size(24.dp), tint = cs.primary)
        Text("رجوع", color = cs.primary, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun PillBtn(icon: ImageVector, text: String? = null, active: Boolean = false, tint: Color? = null, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val fg = tint ?: cs.primary
    val bg = if (active) cs.primary.copy(alpha = 0.15f) else cs.surfaceContainerHigh
    Row(
        Modifier.height(36.dp).clip(CircleShape).background(bg).clickable(onClick = onClick)
            .padding(horizontal = if (text == null) 11.dp else 14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(icon, text, Modifier.size(18.dp), tint = fg)
        if (text != null) Text(text, color = fg, style = MaterialTheme.typography.labelLarge)
    }
}

// ------------------------------------------------------------------ text field
@Composable
fun fieldColors(): TextFieldColors {
    val c = MaterialTheme.colorScheme.surfaceContainerHigh
    return TextFieldDefaults.colors(
        focusedContainerColor = c, unfocusedContainerColor = c, disabledContainerColor = c,
        focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
        disabledIndicatorColor = Color.Transparent, errorIndicatorColor = Color.Transparent
    )
}

// ------------------------------------------------------------------ blocks
/** عنوان كبير زي iOS، وزرار الأكشن فوقه */
@Composable
fun TabHeader(title: String, subtitle: String, badge: @Composable () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth().statusBarsPadding().padding(top = 8.dp)) {
        Row(Modifier.fillMaxWidth().height(36.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            badge()
        }
        Text(title, Modifier.padding(top = 6.dp), style = MaterialTheme.typography.displaySmall)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
    }
}

/** عنوان قسم رمادي صغير (فوق الجروبات) */
@Composable
fun SectionTitle(text: String, first: Boolean = false) {
    Text(
        text, Modifier.padding(top = if (first) 0.dp else 26.dp, bottom = 7.dp, start = 16.dp),
        style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/** حرف الفهرس في جهات الاتصال */
@Composable
fun IndexHeader(letter: String) {
    val cs = MaterialTheme.colorScheme
    Text(
        letter, Modifier.fillMaxWidth().padding(top = 6.dp).clip(RoundedCornerShape(8.dp)).background(cs.surfaceContainerLow)
            .padding(horizontal = 14.dp, vertical = 4.dp),
        style = MaterialTheme.typography.titleSmall, color = cs.onSurfaceVariant
    )
}

/** كارت مجمّع زي iOS (الصفوف جواه بينها خط رفيع) */
@Composable
fun GroupCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceContainerLow),
        content = content
    )
}

/** خط فاصل رفيع (start = المسافة من أول الصف) */
@Composable
fun RowDivider(start: Dp = 0.dp) {
    Box(Modifier.fillMaxWidth().padding(start = start).height(0.5.dp).background(MaterialTheme.colorScheme.outlineVariant))
}

@Composable
fun SettingRow(
    icon: ImageVector, title: String, sub: String? = null, modifier: Modifier = Modifier,
    iconBg: Color = IosColors.Gray, onClick: (() -> Unit)? = null, trailing: (@Composable () -> Unit)? = null
) {
    val cs = MaterialTheme.colorScheme
    val click = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Row(
        modifier.fillMaxWidth().then(click).heightIn(min = 52.dp).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(Modifier.size(30.dp).clip(RoundedCornerShape(7.dp)).background(iconBg), contentAlignment = Alignment.Center) {
            Icon(icon, null, Modifier.size(18.dp), tint = Color.White)
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (sub != null) Text(sub, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
        }
        if (trailing != null) trailing()
    }
}

/** سويتش iOS: أخضر، ثامب أبيض */
@Composable
fun IosSwitch(checked: Boolean, onChange: (Boolean) -> Unit) {
    val cs = MaterialTheme.colorScheme
    val track by animateColorAsState(if (checked) cs.tertiary else cs.surfaceContainerHighest, label = "swt")
    val x by animateDpAsState(if (checked) 20.dp else 0.dp, spring(0.75f, 520f), label = "swx")
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(
            Modifier.size(width = 51.dp, height = 31.dp).clip(CircleShape).background(track)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onChange(!checked) }
                .padding(2.dp)
        ) {
            Box(Modifier.offset(x = x).size(27.dp).shadow(2.dp, CircleShape).background(Color.White, CircleShape))
        }
    }
}

@Composable
fun EmptyState(icon: ImageVector, title: String, body: String) {
    val cs = MaterialTheme.colorScheme
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CookieIcon(icon, 96.dp, cs.surfaceContainerHigh, cs.onSurfaceVariant)
        Spacer(Modifier.height(10.dp))
        Text(title, style = MaterialTheme.typography.titleLarge)
        Text(body, style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

@Composable
fun MenuItem(text: String, icon: ImageVector, danger: Boolean = false, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val c = if (danger) cs.error else cs.onSurface
    DropdownMenuItem(
        text = { Text(text, color = c, style = MaterialTheme.typography.bodyLarge) },
        leadingIcon = { Icon(icon, null, Modifier.size(20.dp), tint = c) },
        onClick = onClick
    )
}

@Composable
fun ConfirmDialog(
    title: String, text: String, confirm: String, danger: Boolean = false,
    onConfirm: () -> Unit, onDismiss: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = { Text(text, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirm, color = if (danger) cs.error else cs.primary, style = MaterialTheme.typography.titleMedium)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء", style = MaterialTheme.typography.bodyLarge) } },
        shape = RoundedCornerShape(14.dp)
    )
}

fun toast(ctx: Context, msg: String) = Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()


/** شريط بحث iOS: رمادي، حواف 10 */
@Composable
fun SearchBar(value: String, onChange: (String) -> Unit, hint: String, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier.fillMaxWidth().height(38.dp).clip(RoundedCornerShape(10.dp)).background(cs.surfaceContainerHigh).padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(Ic.Search, null, Modifier.size(18.dp), tint = cs.onSurfaceVariant)
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) Text(hint, style = MaterialTheme.typography.bodyLarge, color = cs.onSurfaceVariant, maxLines = 1)
            BasicTextField(
                value = value, onValueChange = onChange, singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = cs.onSurface),
                cursorBrush = SolidColor(cs.primary), modifier = Modifier.fillMaxWidth()
            )
        }
        if (value.isNotEmpty()) IconBtn(Ic.Close, "مسح", Modifier.size(28.dp)) { onChange("") }
    }
}

/** أزرار الإجراءات في صفحة الجهة: مربع رمادي، أيقونة وكلمة بلون التطبيق */
@Composable
fun ActionTile(
    icon: ImageVector, label: String, modifier: Modifier = Modifier, active: Boolean = false,
    danger: Boolean = false, tileHeight: Dp = 66.dp, onClick: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val a by animateFloatAsState(if (pressed) 0.65f else 1f, tween(90), label = "ta")
    val bg = when { danger -> cs.errorContainer; active -> cs.primary; else -> cs.surfaceContainerLow }
    val fg = when { danger -> cs.onErrorContainer; active -> cs.onPrimary; else -> cs.primary }
    Column(
        modifier.height(tileHeight).alpha(a).clip(RoundedCornerShape(12.dp)).background(bg)
            .clickable(interactionSource = src, indication = null, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, null, Modifier.size(22.dp), tint = fg)
        Spacer(Modifier.height(4.dp))
        Text(label, color = fg, style = MaterialTheme.typography.labelMedium, maxLines = 1)
    }
}
