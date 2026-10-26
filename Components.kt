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
import com.mohamed.phone.Prefs
import com.mohamed.phone.data.Fmt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

// ------------------------------------------------------------------ cookie
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
    spin: Boolean = true, desc: String? = null, onClick: (() -> Unit)? = null
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

// ------------------------------------------------------------------ glow
@Composable
fun Modifier.glow(color: Color, alpha: Float = 0.72f): Modifier {
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    return this.drawBehind {
        drawRect(
            Brush.radialGradient(
                listOf(color.copy(alpha = alpha), Color.Transparent),
                center = Offset(size.width * (if (rtl) 0.12f else 0.88f), size.height * 0.04f),
                radius = size.width * 0.95f
            )
        )
    }
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
    val palette = listOf(
        cs.primaryContainer to cs.onPrimaryContainer,
        cs.tertiaryContainer to cs.onTertiaryContainer,
        cs.secondaryContainer to cs.onSecondaryContainer
    )
    val pick = palette[(seed.hashCode() and 0x7fffffff) % palette.size]
    val bmp by produceState<ImageBitmap?>(null, photo) {
        value = if (photo == null) null else withContext(Dispatchers.IO) { Photos.load(photo) }
    }
    Box(modifier.size(size).clip(CircleShape).background(pick.first), contentAlignment = Alignment.Center) {
        val b = bmp
        if (b != null) {
            Image(b, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            val ini = Fmt.initials(name)
            if (ini.isNotEmpty()) {
                Text(ini, color = pick.second, fontSize = (size.value * 0.36f).sp, fontWeight = FontWeight.Bold)
            } else {
                Icon(Ic.Person, null, Modifier.size(size * 0.5f), tint = pick.second)
            }
        }
    }
}

// ------------------------------------------------------------------ navigation
class NavTab(val label: String, val icon: ImageVector, val badge: Int = 0)

@Composable
fun FloatingNav(items: List<NavTab>, sel: Int, on: (Int) -> Unit) {
    val cs = MaterialTheme.colorScheme
    val hap = LocalHapticFeedback.current
    Surface(shape = CircleShape, color = cs.surfaceContainerHigh, shadowElevation = 10.dp) {
        Row(Modifier.padding(6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            items.forEachIndexed { i, t ->
                val a = i == sel
                val bg by animateColorAsState(if (a) cs.primaryContainer else Color.Transparent, label = "nb")
                Row(
                    Modifier.height(54.dp).clip(CircleShape).background(bg)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                            if (Prefs.haptic.value) hap.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            on(i)
                        }
                        .padding(horizontal = 20.dp).animateContentSize(spring(0.8f, 400f)),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val tint = if (a) cs.onPrimaryContainer else cs.onSurfaceVariant
                    if (t.badge > 0 && !a) {
                        BadgedBox(badge = { Badge() }) { Icon(t.icon, t.label, tint = tint) }
                    } else {
                        Icon(t.icon, t.label, tint = tint)
                    }
                    AnimatedVisibility(a, enter = fadeIn() + expandHorizontally(), exit = fadeOut() + shrinkHorizontally()) {
                        Text(
                            t.label, Modifier.padding(start = 10.dp), color = cs.onPrimaryContainer,
                            style = MaterialTheme.typography.titleSmall, maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun Switcher(opts: List<String>, sel: Int, modifier: Modifier = Modifier, on: (Int) -> Unit) {
    val cs = MaterialTheme.colorScheme
    BoxWithConstraints(modifier.fillMaxWidth().height(54.dp).clip(CircleShape).background(cs.surfaceContainerHigh).padding(4.dp)) {
        val w = maxWidth / opts.size
        val x by animateDpAsState(w * sel.coerceAtLeast(0), spring(0.8f, 420f), label = "sw")
        if (sel >= 0) Box(Modifier.offset(x = x).width(w).fillMaxHeight().clip(CircleShape).background(cs.primary))
        Row(Modifier.fillMaxSize()) {
            opts.forEachIndexed { i, o ->
                val c by animateColorAsState(if (i == sel) cs.onPrimary else cs.onSurface, label = "swc")
                Box(
                    Modifier.weight(1f).fillMaxHeight().clip(CircleShape)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { on(i) },
                    contentAlignment = Alignment.Center
                ) { Text(o, color = c, style = MaterialTheme.typography.titleSmall, maxLines = 1) }
            }
        }
    }
}

// ------------------------------------------------------------------ buttons
@Composable
fun BigButton(
    text: String, modifier: Modifier = Modifier, icon: ImageVector? = null, tonal: Boolean = false,
    danger: Boolean = false, onClick: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val r by animateDpAsState(if (pressed) 18.dp else 32.dp, spring(0.55f, 600f), label = "r")
    val s by animateFloatAsState(if (pressed) 0.97f else 1f, spring(0.55f, 600f), label = "s")
    val bg = when { danger -> cs.error; tonal -> cs.secondaryContainer; else -> cs.primary }
    val fg = when { danger -> cs.onError; tonal -> cs.onSecondaryContainer; else -> cs.onPrimary }
    Row(
        modifier.fillMaxWidth().height(64.dp).graphicsLayer { scaleX = s; scaleY = s }
            .clip(RoundedCornerShape(r)).background(bg)
            .clickable(interactionSource = src, indication = null, onClick = onClick),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, null, Modifier.size(24.dp), tint = fg)
            Spacer(Modifier.width(10.dp))
        }
        Text(text, color = fg, style = MaterialTheme.typography.titleMedium)
    }
}

/** زر دايري بيتحوّل شكله وهو مضغوط (للتحكم في المكالمات والكيباد) */
@Composable
fun RoundAction(
    icon: ImageVector, size: Dp, container: Color, tint: Color, modifier: Modifier = Modifier,
    desc: String? = null, iconSize: Dp = 28.dp, onClick: () -> Unit
) {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val r by animateDpAsState(if (pressed) size * 0.3f else size / 2, spring(0.55f, 600f), label = "ra")
    val s by animateFloatAsState(if (pressed) 0.95f else 1f, spring(0.55f, 600f), label = "rs")
    Box(
        modifier.size(size).graphicsLayer { scaleX = s; scaleY = s }.clip(RoundedCornerShape(r)).background(container)
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

@Composable
fun BackPill(onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Box(
        Modifier.size(44.dp).clip(CircleShape).background(cs.surfaceContainerHigh).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Icon(Ic.Back, "رجوع", Modifier.size(22.dp), tint = cs.onSurface) }
}

@Composable
fun PillBtn(icon: ImageVector, text: String? = null, active: Boolean = false, tint: Color? = null, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val bg = if (active) cs.primaryContainer else cs.surfaceContainerHigh
    val fg = tint ?: if (active) cs.onPrimaryContainer else cs.onSurface
    Row(
        Modifier.height(44.dp).clip(CircleShape).background(bg).clickable(onClick = onClick).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(icon, text, Modifier.size(20.dp), tint = fg)
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
@Composable
fun TabHeader(title: String, subtitle: String, badge: @Composable () -> Unit) {
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(title, style = MaterialTheme.typography.headlineMedium)
            Text(subtitle, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        badge()
    }
}

@Composable
fun SectionTitle(text: String, first: Boolean = false) {
    Text(
        text, Modifier.padding(top = if (first) 0.dp else 28.dp, bottom = 6.dp),
        style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary
    )
}

@Composable
fun SettingRow(
    icon: ImageVector, title: String, sub: String? = null, modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null, trailing: (@Composable () -> Unit)? = null
) {
    val cs = MaterialTheme.colorScheme
    val click = if (onClick != null) Modifier.clip(RoundedCornerShape(24.dp)).clickable(onClick = onClick) else Modifier
    Row(
        modifier.fillMaxWidth().then(click).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(Modifier.size(46.dp).clip(RoundedCornerShape(16.dp)).background(cs.secondaryContainer), contentAlignment = Alignment.Center) {
            Icon(icon, null, Modifier.size(22.dp), tint = cs.onSecondaryContainer)
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (sub != null) Text(sub, style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
        }
        if (trailing != null) trailing()
    }
}

@Composable
fun EmptyState(icon: ImageVector, title: String, body: String) {
    val cs = MaterialTheme.colorScheme
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        CookieIcon(icon, 128.dp, cs.primaryContainer, cs.onPrimaryContainer)
        Spacer(Modifier.height(10.dp))
        Text(title, style = MaterialTheme.typography.headlineSmall)
        Text(body, style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

@Composable
fun MenuItem(text: String, icon: ImageVector, danger: Boolean = false, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val c = if (danger) cs.error else cs.onSurface
    DropdownMenuItem(
        text = { Text(text, color = c, style = MaterialTheme.typography.titleSmall) },
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
                Text(confirm, color = if (danger) cs.error else cs.primary, style = MaterialTheme.typography.titleSmall)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء", style = MaterialTheme.typography.titleSmall) } },
        shape = RoundedCornerShape(28.dp)
    )
}

fun toast(ctx: Context, msg: String) = Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
