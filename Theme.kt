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

private val LightFallback = lightColorScheme(
    primary = Color(0xFF0B57D0), onPrimary = Color.White,
    primaryContainer = Color(0xFFD3E3FD), onPrimaryContainer = Color(0xFF041E49),
    secondary = Color(0xFF565F71), onSecondary = Color.White,
    secondaryContainer = Color(0xFFDAE2F9), onSecondaryContainer = Color(0xFF131C2B),
    tertiary = Color(0xFF146C2E), onTertiary = Color.White,
    tertiaryContainer = Color(0xFFC4EED0), onTertiaryContainer = Color(0xFF072711),
    background = Color(0xFFF8F9FF), onBackground = Color(0xFF191C20),
    surface = Color(0xFFF8F9FF), onSurface = Color(0xFF191C20),
    surfaceVariant = Color(0xFFE0E2EC), onSurfaceVariant = Color(0xFF44474E),
    outline = Color(0xFF74777F), outlineVariant = Color(0xFFC4C6D0),
    surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFF2F3F9),
    surfaceContainer = Color(0xFFECEEF4), surfaceContainerHigh = Color(0xFFE6E8EE),
    surfaceContainerHighest = Color(0xFFE1E2E8),
    error = Color(0xFFBA1A1A), onError = Color.White,
    errorContainer = Color(0xFFFFDAD6), onErrorContainer = Color(0xFF410002)
)

private val DarkFallback = darkColorScheme(
    primary = Color(0xFFA8C7FA), onPrimary = Color(0xFF062E6F),
    primaryContainer = Color(0xFF0842A0), onPrimaryContainer = Color(0xFFD3E3FD),
    secondary = Color(0xFFBDC7DC), onSecondary = Color(0xFF283141),
    secondaryContainer = Color(0xFF3F4759), onSecondaryContainer = Color(0xFFDAE2F9),
    tertiary = Color(0xFF6DD58C), onTertiary = Color(0xFF003915),
    tertiaryContainer = Color(0xFF0F5223), onTertiaryContainer = Color(0xFFC4EED0),
    background = Color(0xFF111318), onBackground = Color(0xFFE2E2E9),
    surface = Color(0xFF111318), onSurface = Color(0xFFE2E2E9),
    surfaceVariant = Color(0xFF44474E), onSurfaceVariant = Color(0xFFC4C6D0),
    outline = Color(0xFF8E9099), outlineVariant = Color(0xFF44474E),
    surfaceContainerLowest = Color(0xFF0C0E13), surfaceContainerLow = Color(0xFF191C20),
    surfaceContainer = Color(0xFF1D2024), surfaceContainerHigh = Color(0xFF282A2F),
    surfaceContainerHighest = Color(0xFF33353A),
    error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A), onErrorContainer = Color(0xFFFFDAD6)
)

/** ألوان مميزة اختيارية: (فاتح) primary/container/onContainer، (داكن) primary/onPrimary/container/onContainer */
class Accent(
    val lp: Color, val lc: Color, val lo: Color,
    val dp: Color, val dOn: Color, val dc: Color, val dco: Color
)

val Accents = listOf(
    Accent(Color(0xFF0B57D0), Color(0xFFD3E3FD), Color(0xFF041E49), Color(0xFFA8C7FA), Color(0xFF062E6F), Color(0xFF0842A0), Color(0xFFD3E3FD)),
    Accent(Color(0xFF1B6C3A), Color(0xFFC4EED0), Color(0xFF072711), Color(0xFF6DD58C), Color(0xFF003915), Color(0xFF0F5223), Color(0xFFC4EED0)),
    Accent(Color(0xFF6750A4), Color(0xFFEADDFF), Color(0xFF21005D), Color(0xFFCFBCFF), Color(0xFF381E72), Color(0xFF4F378B), Color(0xFFEADDFF)),
    Accent(Color(0xFF9A4600), Color(0xFFFFDCC3), Color(0xFF321300), Color(0xFFFFB784), Color(0xFF4F2500), Color(0xFF723600), Color(0xFFFFDCC3)),
    Accent(Color(0xFFA8305F), Color(0xFFFFD9E2), Color(0xFF3E001D), Color(0xFFFFB1C8), Color(0xFF650033), Color(0xFF8C1747), Color(0xFFFFD9E2))
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
    displaySmall = T0.displaySmall.copy(fontSize = 36.sp, lineHeight = 52.sp).f(FontWeight.ExtraBold),
    headlineLarge = T0.headlineLarge.f(FontWeight.ExtraBold),
    headlineMedium = T0.headlineMedium.f(FontWeight.ExtraBold),
    headlineSmall = T0.headlineSmall.f(FontWeight.Bold),
    titleLarge = T0.titleLarge.f(FontWeight.Bold),
    titleMedium = T0.titleMedium.f(FontWeight.Bold),
    titleSmall = T0.titleSmall.f(FontWeight.Bold),
    bodyLarge = T0.bodyLarge.f(FontWeight.Normal),
    bodyMedium = T0.bodyMedium.f(FontWeight.Normal),
    bodySmall = T0.bodySmall.f(FontWeight.Normal),
    labelLarge = T0.labelLarge.f(FontWeight.Bold),
    labelMedium = T0.labelMedium.f(FontWeight.Medium),
    labelSmall = T0.labelSmall.f(FontWeight.Medium)
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

    val base: ColorScheme = when {
        accent < 0 && Build.VERSION.SDK_INT >= 31 -> if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        else -> if (dark) DarkFallback else LightFallback
    }
    val withAccent = if (accent in Accents.indices) {
        val a = Accents[accent]
        if (dark) base.copy(primary = a.dp, onPrimary = a.dOn, primaryContainer = a.dc, onPrimaryContainer = a.dco)
        else base.copy(primary = a.lp, onPrimary = Color.White, primaryContainer = a.lc, onPrimaryContainer = a.lo)
    } else base
    val cs = if (dark && amoled) withAccent.copy(
        background = Color.Black, surface = Color.Black, surfaceContainerLowest = Color.Black,
        surfaceContainerLow = Color(0xFF0A0A0A), surfaceContainer = Color(0xFF121212),
        surfaceContainerHigh = Color(0xFF1A1A1A), surfaceContainerHighest = Color(0xFF222222)
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
