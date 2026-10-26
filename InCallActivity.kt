package com.mohamed.phone

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.telecom.Call
import android.telecom.CallAudioState
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.mohamed.phone.data.Fmt
import com.mohamed.phone.telecom.CallHub
import com.mohamed.phone.telecom.Dialing
import com.mohamed.phone.ui.AppTheme
import com.mohamed.phone.ui.ActionTile
import com.mohamed.phone.ui.Avatar
import com.mohamed.phone.ui.BigButton
import com.mohamed.phone.ui.Ic
import com.mohamed.phone.ui.PillBtn
import com.mohamed.phone.ui.RoundAction
import com.mohamed.phone.ui.glow
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

@Composable
private fun WideButton(
    icon: ImageVector, text: String, bg: Color, fg: Color, modifier: Modifier = Modifier,
    h: Dp = 68.dp, onClick: () -> Unit
) {
    Row(
        modifier.fillMaxWidth().height(h).clip(CircleShape).background(bg).clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, Modifier.size(26.dp), tint = fg)
        Spacer(Modifier.width(10.dp))
        Text(text, color = fg, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun BigKey(label: String, size: Dp = 56.dp, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Box(
        Modifier.size(size).clip(CircleShape).background(cs.surfaceContainerHigh).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Text(label, style = MaterialTheme.typography.titleLarge) }
}

@Suppress("DEPRECATION")
@Composable
fun InCallScreen(onFinish: () -> Unit, onAddCall: () -> Unit, preview: Boolean = false, previewRinging: Boolean = true) {
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

    Surface(Modifier.fillMaxSize(), color = cs.surface) {
        Box(Modifier.fillMaxSize().glow(if (cur?.state == Call.STATE_RINGING) cs.tertiaryContainer else cs.primaryContainer)) {
            if (cur == null) return@Box
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
            val routeLabel = when (route) {
                CallAudioState.ROUTE_SPEAKER -> "السماعة"
                CallAudioState.ROUTE_BLUETOOTH -> "بلوتوث"
                else -> "الأذن"
            }
            val padMode = showPad && !ringing && !ended

            Column(
                Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    Modifier.weight(1f).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
                ) {
                    if (padMode) {
                        Text(
                            cur.label + " · " + status, style = MaterialTheme.typography.titleSmall,
                            color = cs.primary, maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            typed.takeLast(20).ifEmpty { " " },
                            style = MaterialTheme.typography.headlineSmall.copy(textDirection = TextDirection.Ltr),
                            modifier = Modifier.padding(vertical = 10.dp)
                        )
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("123", "456", "789", "*0#").forEach { row ->
                                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                        row.forEach { ch ->
                                            BigKey(ch.toString(), 54.dp) {
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
                    } else {
                        Avatar(cur.label, cur.photo, if (ringing) 132.dp else 104.dp, seed = cur.number)
                        Spacer(Modifier.height(16.dp))
                        Text(
                            cur.label, style = MaterialTheme.typography.headlineMedium.copy(textDirection = TextDirection.Content),
                            textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis
                        )
                        if (cur.name != null && cur.number.isNotEmpty()) {
                            Text(
                                cur.number, style = MaterialTheme.typography.bodyMedium.copy(textDirection = TextDirection.Ltr),
                                color = cs.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(status, style = MaterialTheme.typography.titleMedium, color = if (ended) cs.error else cs.primary)
                        others.forEach { o ->
                            Spacer(Modifier.height(12.dp))
                            Row(
                                Modifier.fillMaxWidth().clip(CircleShape).background(cs.surfaceContainerHigh).padding(start = 18.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
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
                }

                if (ringing) {
                    PillBtn(Ic.Message, "رفض برسالة") { quick = true }
                    Spacer(Modifier.height(16.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        WideButton(Ic.PhoneEnd, "رفض", cs.error, cs.onError, Modifier.weight(1f), 72.dp) { doReject(cur) }
                        WideButton(Ic.Phone, "رد", cs.tertiary, cs.onTertiary, Modifier.weight(1f), 72.dp) { doAnswer(cur) }
                    }
                    Spacer(Modifier.height(8.dp))
                } else if (!ended) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ActionTile(if (isMuted) Ic.MicOff else Ic.Mic, "كتم", Modifier.weight(1f), active = isMuted) {
                            muted = !isMuted
                            if (!preview) CallHub.setMuted(!isMuted)
                        }
                        ActionTile(
                            Ic.Speaker, routeLabel, Modifier.weight(1f),
                            active = route == CallAudioState.ROUTE_SPEAKER || route == CallAudioState.ROUTE_BLUETOOTH
                        ) {
                            val next = when {
                                route == CallAudioState.ROUTE_SPEAKER -> if (btOk) CallAudioState.ROUTE_BLUETOOTH else CallAudioState.ROUTE_WIRED_OR_EARPIECE
                                route == CallAudioState.ROUTE_BLUETOOTH -> CallAudioState.ROUTE_WIRED_OR_EARPIECE
                                else -> CallAudioState.ROUTE_SPEAKER
                            }
                            if (preview) { pvRoute = next } else { CallHub.setRoute(next) }
                        }
                        ActionTile(Ic.Keypad, "الأرقام", Modifier.weight(1f), active = showPad) { showPad = !showPad }
                    }
                    if (!showPad) {
                        Spacer(Modifier.height(10.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            ActionTile(Ic.Pause, "تعليق", Modifier.weight(1f), active = cur.state == Call.STATE_HOLDING) {
                                if (preview) {
                                    pvHold = !pvHold
                                } else if (cur.canHold) {
                                    cur.call?.let { CallHub.hold(it, cur.state != Call.STATE_HOLDING) }
                                }
                            }
                            ActionTile(Ic.Add, "إضافة", Modifier.weight(1f)) { onAddCall() }
                            if (cur.canMerge) {
                                ActionTile(Ic.Merge, "دمج", Modifier.weight(1f)) {
                                    others.firstOrNull()?.call?.let { cur.call?.conference(it) }
                                }
                            } else if (cur.canSwap) {
                                ActionTile(Ic.Swap, "تبديل", Modifier.weight(1f)) { cur.call?.swapConference() }
                            } else {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    WideButton(Ic.PhoneEnd, "إنهاء", cs.error, cs.onError, Modifier, 64.dp) { doHangup(cur) }
                    Spacer(Modifier.height(8.dp))
                }
            }

            if (!preview && !ended && cur.state == Call.STATE_SELECT_PHONE_ACCOUNT) {
                AlertDialog(
                    onDismissRequest = { doHangup(cur) },
                    title = { Text("اتصل بأنهي شريحة؟", style = MaterialTheme.typography.titleLarge) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Dialing.accounts(ctx).forEach { h ->
                                BigButton(Dialing.label(ctx, h), icon = Ic.Phone, tonal = true) { cur.call?.phoneAccountSelected(h, false) }
                            }
                        }
                    },
                    confirmButton = {},
                    dismissButton = { TextButton(onClick = { doHangup(cur) }) { Text("إلغاء") } }
                )
            }

            if (quick && ringing) {
                AlertDialog(
                    onDismissRequest = { quick = false },
                    title = { Text("رفض برسالة", style = MaterialTheme.typography.titleLarge) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            listOf("مش فاضي دلوقتي، هكلمك بعدين.", "ممكن أكلمك بعدين؟", "أنا في اجتماع.").forEach { m ->
                                BigButton(m, tonal = true) { quick = false; doReject(cur, m) }
                            }
                        }
                    },
                    confirmButton = {},
                    dismissButton = { TextButton(onClick = { quick = false }) { Text("إلغاء") } }
                )
            }
        }
    }
}
