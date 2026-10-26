package com.mohamed.phone

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.telecom.Call
import android.telecom.CallAudioState
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.mohamed.phone.data.Fmt
import com.mohamed.phone.telecom.CallHub
import com.mohamed.phone.telecom.Dialing
import com.mohamed.phone.ui.AppTheme
import com.mohamed.phone.ui.Avatar
import com.mohamed.phone.ui.BigButton
import com.mohamed.phone.ui.Ic
import com.mohamed.phone.ui.PillBtn
import com.mohamed.phone.ui.callColorScheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class InCallActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true); setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enableEdgeToEdge()
        setContent {
            AppTheme {
                InCallScreen(
                    onFinish = { finish() },
                    onAddCall = {
                        startActivity(
                            Intent(this, MainActivity::class.java).putExtra("open_dialer", true)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        )
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}

private val EndRed = Color(0xFFFF3B30)
private val AnswerGreen = Color(0xFF34C759)
private val GlassBg = Color.White.copy(alpha = 0.16f)
private val DialogBg = Color(0xFF2C2C2E)

private val KeySubs = mapOf(
    '2' to "ABC", '3' to "DEF", '4' to "GHI", '5' to "JKL", '6' to "MNO", '7' to "PQRS", '8' to "TUV", '9' to "WXYZ", '0' to "+"
)

private tailrec fun Context.hostActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.hostActivity()
    else -> null
}

/** زرار دايري زي iOS: دايرة كبيرة والاسم تحتها */
@Composable
private fun CallCircle(
    icon: ImageVector, label: String?, bg: Color, fg: Color, modifier: Modifier = Modifier,
    size: Dp = 76.dp, iconSize: Dp = 30.dp, onClick: () -> Unit
) {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val a by animateFloatAsState(if (pressed) 0.6f else 1f, tween(90), label = "cc")
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(size).alpha(a).clip(CircleShape).background(bg)
                .clickable(interactionSource = src, indication = null, onClick = onClick),
            contentAlignment = Alignment.Center
        ) { Icon(icon, label, Modifier.size(iconSize), tint = fg) }
        if (label != null) {
            Spacer(Modifier.height(8.dp))
            Text(label, color = Color.White, style = MaterialTheme.typography.labelMedium, maxLines = 1)
        }
    }
}

/** زرار تشغيل/إيقاف: لما يشتغل بيتحول أبيض (زي iOS) */
@Composable
private fun ToggleCircle(icon: ImageVector, label: String, active: Boolean, onClick: () -> Unit) {
    CallCircle(
        icon, label, if (active) Color.White else GlassBg, if (active) Color(0xFF1C1C1E) else Color.White,
        onClick = onClick
    )
}

@Composable
private fun CallKey(main: String, sub: String?, onClick: () -> Unit) {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    Column(
        Modifier.size(68.dp).clip(CircleShape).background(if (pressed) Color.White.copy(alpha = 0.34f) else GlassBg)
            .clickable(interactionSource = src, indication = null, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
    ) {
        Text(main, color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Normal, lineHeight = 32.sp)
        if (sub != null) Text(sub, color = Color.White.copy(alpha = 0.7f), fontSize = 9.sp, fontWeight = FontWeight.Bold, lineHeight = 10.sp)
    }
}

@Composable
fun InCallScreen(onFinish: () -> Unit, onAddCall: () -> Unit, preview: Boolean = false, previewRinging: Boolean = true) {
    val appCs = MaterialTheme.colorScheme
    val callCs = remember(appCs.primary, appCs.onPrimary) { callColorScheme(appCs) }
    val view = LocalView.current

    // شاشة المكالمة دايمًا داكنة: أيقونات الستاتس بار بيضا، وبنرجّعها لما نخرج (مهم في المعاينة)
    DisposableEffect(Unit) {
        val w = view.context.hostActivity()?.window
        val c = w?.let { WindowCompat.getInsetsController(it, view) }
        val prevStatus = c?.isAppearanceLightStatusBars
        val prevNav = c?.isAppearanceLightNavigationBars
        c?.isAppearanceLightStatusBars = false
        c?.isAppearanceLightNavigationBars = false
        onDispose {
            if (c != null && prevStatus != null && prevNav != null) {
                c.isAppearanceLightStatusBars = prevStatus
                c.isAppearanceLightNavigationBars = prevNav
            }
        }
    }

    MaterialTheme(colorScheme = callCs, typography = MaterialTheme.typography) {
        Box(
            Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF2A2A2D), Color(0xFF09090A))))
        ) {
            Surface(Modifier.fillMaxSize(), color = Color.Transparent, contentColor = Color.White) {
                CallBody(onFinish, onAddCall, preview, previewRinging)
            }
        }
    }
}

@Suppress("DEPRECATION")
@Composable
private fun CallBody(onFinish: () -> Unit, onAddCall: () -> Unit, preview: Boolean, previewRinging: Boolean) {
    val cs = MaterialTheme.colorScheme
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val realList by CallHub.calls.collectAsState()
    val audio by CallHub.audio.collectAsState()
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showPad by remember { mutableStateOf(false) }
    var typed by remember { mutableStateOf("") }
    var quick by remember { mutableStateOf(false) }
    var muted by remember { mutableStateOf(false) }
    var pvState by remember { mutableIntStateOf(if (previewRinging) Call.STATE_RINGING else Call.STATE_ACTIVE) }
    var pvHold by remember { mutableStateOf(false) }
    var pvRoute by remember { mutableIntStateOf(CallAudioState.ROUTE_EARPIECE) }
    val pvStart = remember { System.currentTimeMillis() }
    val holder = remember { arrayOfNulls<CallHub.Info>(1) }

    val list: List<CallHub.Info> = if (preview) {
        listOf(
            CallHub.Info(
                null, if (pvHold) Call.STATE_HOLDING else pvState, "01012345678", "أحمد محمد", null,
                if (pvState == Call.STATE_ACTIVE) pvStart else 0L, true, false, false, false
            )
        )
    } else realList

    fun doAnswer(i: CallHub.Info) { if (preview) { pvState = Call.STATE_ACTIVE } else { i.call?.let { CallHub.answer(it) } } }
    fun doReject(i: CallHub.Info, msg: String? = null) { if (preview) onFinish() else i.call?.let { CallHub.reject(it, msg) } }
    fun doHangup(i: CallHub.Info) { if (preview) onFinish() else i.call?.let { CallHub.hangup(it) } }

    LaunchedEffect(Unit) { while (true) { delay(500); now = System.currentTimeMillis() } }
    LaunchedEffect(list.isEmpty()) { if (!preview && list.isEmpty()) { delay(900); onFinish() } }

    val main = list.firstOrNull { it.state == Call.STATE_RINGING }
        ?: list.firstOrNull { it.state != Call.STATE_HOLDING } ?: list.firstOrNull()
    if (main != null) holder[0] = main
    val cur = holder[0]
    val ended = main == null

    if (cur == null) return
    val ringing = !ended && cur.state == Call.STATE_RINGING
    val others = list.filter { it !== main }
    val status = when {
        ended -> "المكالمة انتهت"
        cur.state == Call.STATE_ACTIVE && cur.connectMs > 0 -> Fmt.clock((now - cur.connectMs) / 1000)
        else -> CallHub.stateText(cur.state)
    }
    val route = if (preview) pvRoute else (audio?.route ?: CallAudioState.ROUTE_EARPIECE)
    val btOk = !preview && ((audio?.supportedRouteMask ?: 0) and CallAudioState.ROUTE_BLUETOOTH) != 0
    val isMuted = if (preview) muted else (audio?.isMuted ?: muted)
    val speakerOn = route == CallAudioState.ROUTE_SPEAKER
    val btOn = route == CallAudioState.ROUTE_BLUETOOTH
    val padMode = showPad && !ringing && !ended

    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
            .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ---------- الاسم والحالة
        Column(
            Modifier.fillMaxWidth().padding(top = if (padMode) 4.dp else 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!padMode) {
                Avatar(cur.label, cur.photo, if (ringing) 120.dp else 92.dp, seed = cur.number)
                Spacer(Modifier.height(18.dp))
            }
            Text(
                cur.label,
                style = (if (padMode) MaterialTheme.typography.titleLarge else if (ringing) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.headlineMedium)
                    .copy(textDirection = TextDirection.Content),
                textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis
            )
            if (!padMode && cur.name != null && cur.number.isNotEmpty()) {
                Text(
                    cur.number, style = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Ltr),
                    color = cs.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                status, style = MaterialTheme.typography.bodyLarge,
                color = if (ended) cs.error else cs.onSurfaceVariant
            )
            others.forEach { o ->
                Spacer(Modifier.height(14.dp))
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(cs.surfaceContainer)
                        .padding(start = 16.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        o.label + " · " + CallHub.stateText(o.state), Modifier.weight(1f),
                        style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    PillBtn(Ic.Swap, "بدّل") { o.call?.let { CallHub.hold(it, false) } }
                }
            }
        }

        Spacer(Modifier.weight(1f))

        // ---------- الأزرار
        if (ringing) {
            CallCircle(Ic.Message, "رسالة", GlassBg, Color.White, size = 56.dp, iconSize = 22.dp) { quick = true }
            Spacer(Modifier.height(40.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                CallCircle(Ic.PhoneEnd, "رفض", EndRed, Color.White) { doReject(cur) }
                CallCircle(Ic.Phone, "رد", AnswerGreen, Color.White) { doAnswer(cur) }
            }
        } else if (!ended) {
            if (padMode) {
                Text(
                    typed.takeLast(20).ifEmpty { " " },
                    style = MaterialTheme.typography.displaySmall.copy(textDirection = TextDirection.Ltr, fontWeight = FontWeight.Normal),
                    maxLines = 1, modifier = Modifier.padding(bottom = 14.dp)
                )
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        listOf("123", "456", "789", "*0#").forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                                row.forEach { ch ->
                                    CallKey(ch.toString(), KeySubs[ch]) {
                                        typed += ch
                                        scope.launch {
                                            cur.call?.playDtmfTone(ch); delay(140); cur.call?.stopDtmfTone()
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(22.dp))
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CallCircle(Ic.PhoneEnd, null, EndRed, Color.White) { doHangup(cur) }
                    Text(
                        "إخفاء", Modifier.align(Alignment.CenterEnd).clip(RoundedCornerShape(10.dp)).clickable { showPad = false }.padding(12.dp),
                        style = MaterialTheme.typography.titleMedium, color = Color.White
                    )
                }
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    ToggleCircle(if (isMuted) Ic.MicOff else Ic.Mic, "كتم", isMuted) {
                        muted = !isMuted
                        if (!preview) CallHub.setMuted(!isMuted)
                    }
                    ToggleCircle(Ic.Keypad, "الأرقام", false) { showPad = true }
                    ToggleCircle(Ic.Speaker, if (btOn) "بلوتوث" else "السماعة", speakerOn || btOn) {
                        val next = when {
                            speakerOn -> if (btOk) CallAudioState.ROUTE_BLUETOOTH else CallAudioState.ROUTE_WIRED_OR_EARPIECE
                            btOn -> CallAudioState.ROUTE_WIRED_OR_EARPIECE
                            else -> CallAudioState.ROUTE_SPEAKER
                        }
                        if (preview) { pvRoute = next } else { CallHub.setRoute(next) }
                    }
                }
                Spacer(Modifier.height(22.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    ToggleCircle(Ic.Add, "إضافة", false) { onAddCall() }
                    ToggleCircle(Ic.Pause, "تعليق", cur.state == Call.STATE_HOLDING) {
                        if (preview) {
                            pvHold = !pvHold
                        } else if (cur.canHold) {
                            cur.call?.let { CallHub.hold(it, cur.state != Call.STATE_HOLDING) }
                        }
                    }
                    if (cur.canMerge) {
                        ToggleCircle(Ic.Merge, "دمج", false) { others.firstOrNull()?.call?.let { cur.call?.conference(it) } }
                    } else if (cur.canSwap) {
                        ToggleCircle(Ic.Swap, "تبديل", false) { cur.call?.swapConference() }
                    } else {
                        Spacer(Modifier.size(76.dp))
                    }
                }
                Spacer(Modifier.height(34.dp))
                CallCircle(Ic.PhoneEnd, null, EndRed, Color.White) { doHangup(cur) }
            }
        }
    }

    if (!preview && !ended && cur.state == Call.STATE_SELECT_PHONE_ACCOUNT) {
        AlertDialog(
            onDismissRequest = { doHangup(cur) },
            containerColor = DialogBg, shape = RoundedCornerShape(14.dp),
            title = { Text("اتصل بأنهي شريحة؟", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Dialing.accounts(ctx).forEach { h ->
                        BigButton(Dialing.label(ctx, h), icon = Ic.Phone, tonal = true) { cur.call?.phoneAccountSelected(h, false) }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { doHangup(cur) }) { Text("إلغاء", style = MaterialTheme.typography.bodyLarge) } }
        )
    }

    if (quick && ringing) {
        AlertDialog(
            onDismissRequest = { quick = false },
            containerColor = DialogBg, shape = RoundedCornerShape(14.dp),
            title = { Text("رفض برسالة", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf("مش فاضي دلوقتي، هكلمك بعدين.", "ممكن أكلمك بعدين؟", "أنا في اجتماع.").forEach { m ->
                        BigButton(m, tonal = true) { quick = false; doReject(cur, m) }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { quick = false }) { Text("إلغاء", style = MaterialTheme.typography.bodyLarge) } }
        )
    }
}
