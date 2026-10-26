package com.mohamed.phone.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.mohamed.phone.Prefs
import com.mohamed.phone.R

// ألوان iOS: أسطح محايدة (أبيض/أسود + رمادي هادي) وأخضر/أحمر ثابتين للمكالمات
val IosGreenLight = Color(0xFF34C759)
val IosGreenDark = Color(0xFF30D158)
val IosRedLight = Color(0xFFFF3B30)
val IosRedDark = Color(0xFFFF453A)

private val LightFallback = lightColorScheme(
    primary = Color(0xFF007AFF), onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E8FF), onPrimaryContainer = Color(0xFF002F6C),
    secondary = Color(0xFF8E8E93), onSecondary = Color.White,
    secondaryContainer = Color(0xFFE5E5EA), onSecondaryContainer = Color(0xFF1C1C1E),
    tertiary = IosGreenLight, onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD4F5DD), onTertiaryContainer = Color(0xFF0B3D1A),
    background = Color.White, onBackground = Color.Black,
    surface = Color.White, onSurface = Color.Black,
    surfaceVariant = Color(0xFFE5E5EA), onSurfaceVariant = Color(0xFF8A8A8E),
    outline = Color(0xFFC6C6C8), outlineVariant = Color(0xFFD9D9DD),
    surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFF2F2F7),
    surfaceContainer = Color(0xFFF2F2F7), surfaceContainerHigh = Color(0xFFE9E9EE),
    surfaceContainerHighest = Color(0xFFE0E0E5),
    error = IosRedLight, onError = Color.White,
    errorContainer = Color(0xFFFFE0DE), onErrorContainer = Color(0xFF5C0A05)
)

private val DarkFallback = darkColorScheme(
    primary = Color(0xFF0A84FF), onPrimary = Color.White,
    primaryContainer = Color(0xFF0A3A75), onPrimaryContainer = Color(0xFFD6E8FF),
    secondary = Color(0xFF8E8E93), onSecondary = Color.White,
    secondaryContainer = Color(0xFF2C2C2E), onSecondaryContainer = Color(0xFFF2F2F7),
    tertiary = IosGreenDark, onTertiary = Color.White,
    tertiaryContainer = Color(0xFF12361E), onTertiaryContainer = Color(0xFFD4F5DD),
    background = Color.Black, onBackground = Color.White,
    surface = Color.Black, onSurface = Color.White,
    surfaceVariant = Color(0xFF2C2C2E), onSurfaceVariant = Color(0xFF98989F),
    outline = Color(0xFF48484A), outlineVariant = Color(0xFF38383A),
    surfaceContainerLowest = Color.Black, surfaceContainerLow = Color(0xFF1C1C1E),
    surfaceContainer = Color(0xFF1C1C1E), surfaceContainerHigh = Color(0xFF2C2C2E),
    surfaceContainerHighest = Color(0xFF3A3A3C),
    error = IosRedDark, onError = Color.White,
    errorContainer = Color(0xFF5C1511), onErrorContainer = Color(0xFFFFDAD6)
)

/** شاشة المكالمة دايمًا داكنة (زي iOS) بغض النظر عن ثيم التطبيق */
fun callColorScheme(from: ColorScheme): ColorScheme = DarkFallback.copy(
    primary = from.primary, onPrimary = from.onPrimary,
    surface = Color.Transparent, background = Color.Transparent,
    surfaceContainerHigh = Color.White.copy(alpha = 0.16f),
    surfaceContainerHighest = Color.White.copy(alpha = 0.24f),
    surfaceContainerLow = Color.White.copy(alpha = 0.10f),
    surfaceContainer = Color.White.copy(alpha = 0.12f),
    onSurfaceVariant = Color.White.copy(alpha = 0.62f)
)

/** ألوان مميزة اختيارية: (فاتح) primary/container/onContainer، (داكن) primary/onPrimary/container/onContainer */
class Accent(
    val lp: Color, val lc: Color, val lo: Color,
    val dp: Color, val dOn: Color, val dc: Color, val dco: Color
)

val Accents = listOf(
    Accent(Color(0xFF007AFF), Color(0xFFD6E8FF), Color(0xFF002F6C), Color(0xFF0A84FF), Color.White, Color(0xFF0A3A75), Color(0xFFD6E8FF)),
    Accent(Color(0xFF248A3D), Color(0xFFD4F5DD), Color(0xFF0B3D1A), Color(0xFF30D158), Color.Black, Color(0xFF12361E), Color(0xFFD4F5DD)),
    Accent(Color(0xFF8944AB), Color(0xFFF1DDFA), Color(0xFF3A0F52), Color(0xFFBF5AF2), Color.White, Color(0xFF4A1F63), Color(0xFFF1DDFA)),
    Accent(Color(0xFFE07B00), Color(0xFFFFE6C7), Color(0xFF4A2800), Color(0xFFFF9F0A), Color.Black, Color(0xFF5C3300), Color(0xFFFFE6C7)),
    Accent(Color(0xFFD63A6B), Color(0xFFFFDCE6), Color(0xFF4A0620), Color(0xFFFF375F), Color.White, Color(0xFF5E1230), Color(0xFFFFDCE6))
)

private val AppFont = FontFamily(
    Font(R.font.tajawal_regular, FontWeight.Normal),
    Font(R.font.tajawal_medium, FontWeight.Medium),
    Font(R.font.tajawal_bold, FontWeight.Bold),
    Font(R.font.tajawal_extrabold, FontWeight.ExtraBold)
)

private fun TextStyle.f(w: FontWeight? = null) = copy(fontFamily = AppFont, fontWeight = w ?: fontWeight)

private val T0 = Typography()
private val AppType = Typography(
    displayLarge = T0.displayLarge.f(FontWeight.ExtraBold),
    displayMedium = T0.displayMedium.f(FontWeight.ExtraBold),
    displaySmall = T0.displaySmall.copy(fontSize = 34.sp, lineHeight = 44.sp).f(FontWeight.ExtraBold),
    headlineLarge = T0.headlineLarge.copy(fontSize = 32.sp, lineHeight = 42.sp).f(FontWeight.ExtraBold),
    headlineMedium = T0.headlineMedium.copy(fontSize = 28.sp, lineHeight = 38.sp).f(FontWeight.Bold),
    headlineSmall = T0.headlineSmall.copy(fontSize = 22.sp, lineHeight = 30.sp).f(FontWeight.Bold),
    titleLarge = T0.titleLarge.copy(fontSize = 20.sp, lineHeight = 28.sp).f(FontWeight.Bold),
    titleMedium = T0.titleMedium.copy(fontSize = 17.sp, lineHeight = 24.sp).f(FontWeight.Bold),
    titleSmall = T0.titleSmall.copy(fontSize = 16.sp, lineHeight = 22.sp).f(FontWeight.Medium),
    bodyLarge = T0.bodyLarge.copy(fontSize = 17.sp, lineHeight = 24.sp).f(FontWeight.Normal),
    bodyMedium = T0.bodyMedium.copy(fontSize = 15.sp, lineHeight = 21.sp).f(FontWeight.Normal),
    bodySmall = T0.bodySmall.copy(fontSize = 13.sp, lineHeight = 18.sp).f(FontWeight.Normal),
    labelLarge = T0.labelLarge.copy(fontSize = 15.sp, lineHeight = 20.sp).f(FontWeight.Medium),
    labelMedium = T0.labelMedium.copy(fontSize = 13.sp, lineHeight = 18.sp).f(FontWeight.Medium),
    labelSmall = T0.labelSmall.copy(fontSize = 11.sp, lineHeight = 14.sp).f(FontWeight.Medium)
)

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    val mode by Prefs.theme.collectAsState()
    val amoled by Prefs.amoled.collectAsState()
    val accent by Prefs.accent.collectAsState()
    val dark = when (mode) { 1 -> false; 2 -> true; else -> isSystemInDarkTheme() }
    val ctx = LocalContext.current

    val ios = if (dark) DarkFallback else LightFallback
    // الأسطح والنصوص دايمًا محايدة (iOS)، والاسم "تلقائي" بياخد لون النظام للأزرار بس
    val base: ColorScheme = when {
        accent < 0 && Build.VERSION.SDK_INT >= 31 -> {
            val d = if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
            ios.copy(primary = d.primary, onPrimary = d.onPrimary, primaryContainer = d.primaryContainer, onPrimaryContainer = d.onPrimaryContainer)
        }
        else -> ios
    }
    val withAccent = if (accent in Accents.indices) {
        val a = Accents[accent]
        if (dark) base.copy(primary = a.dp, onPrimary = a.dOn, primaryContainer = a.dc, onPrimaryContainer = a.dco)
        else base.copy(primary = a.lp, onPrimary = Color.White, primaryContainer = a.lc, onPrimaryContainer = a.lo)
    } else base
    val cs = if (dark && amoled) withAccent.copy(
        background = Color.Black, surface = Color.Black, surfaceContainerLowest = Color.Black,
        surfaceContainerLow = Color(0xFF111111), surfaceContainer = Color(0xFF151515),
        surfaceContainerHigh = Color(0xFF222222), surfaceContainerHighest = Color(0xFF2C2C2C)
    ) else withAccent

    val view = LocalView.current
    SideEffect {
        val w = view.context.findActivity()?.window
        if (w != null) {
            val c = WindowCompat.getInsetsController(w, view)
            c.isAppearanceLightStatusBars = !dark
            c.isAppearanceLightNavigationBars = !dark
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(colorScheme = cs, typography = AppType, content = content)
    }
}
