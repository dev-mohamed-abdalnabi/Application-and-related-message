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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.mohamed.phone.data.Fmt
import com.mohamed.phone.telecom.CallHub
import com.mohamed.phone.telecom.Dialing
import com.mohamed.phone.ui.AppTheme
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
private fun Toggle(icon: ImageVector, label: String, active: Boolean, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        RoundAction(
            icon, 68.dp, if (active) cs.primary else cs.surfaceContainerHigh,
            if (active) cs.onPrimary else cs.onSurface, desc = label, iconSize = 26.dp, onClick = onClick
        )
        Text(label, style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant)
    }
}

@Suppress("DEPRECATION")
@Composable
fun InCallScreen(onFinish: () -> Unit, onAddCall: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val list by CallHub.calls.collectAsState()
    val audio by CallHub.audio.collectAsState()
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showPad by remember { mutableStateOf(false) }
    var typed by remember { mutableStateOf("") }
    var quick by remember { mutableStateOf(false) }
    var muted by remember { mutableStateOf(false) }
    val holder = remember { arrayOfNulls<CallHub.Info>(1) }

    LaunchedEffect(Unit) { while (true) { delay(500); now = System.currentTimeMillis() } }
    LaunchedEffect(list.isEmpty()) { if (list.isEmpty()) { delay(900); onFinish() } }

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

            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(24.dp))
                Avatar(cur.label, cur.photo, 140.dp, seed = cur.number)
                Spacer(Modifier.height(20.dp))
                Text(
                    cur.label, style = MaterialTheme.typography.displaySmall.copy(textDirection = TextDirection.Content),
                    textAlign = TextAlign.Center, maxLines = 2
                )
                if (cur.name != null && cur.number.isNotEmpty()) {
                    Text(cur.number, style = MaterialTheme.typography.bodyMedium.copy(textDirection = TextDirection.Ltr), color = cs.onSurfaceVariant)
                }
                Spacer(Modifier.height(6.dp))
                Text(status, style = MaterialTheme.typography.titleMedium, color = if (ended) cs.error else cs.primary)

                others.forEach { o ->
                    Spacer(Modifier.height(14.dp))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${o.label} · ${CallHub.stateText(o.state)}", style = MaterialTheme.typography.titleSmall)
                        PillBtn(Ic.Swap, "بدّل") { CallHub.hold(o.call, false) }
                    }
                }

                Spacer(Modifier.height(32.dp))
                if (ringing) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            RoundAction(Ic.PhoneEnd, 84.dp, cs.error, cs.onError, desc = "رفض", iconSize = 34.dp) { CallHub.reject(cur.call) }
                            Text("رفض", style = MaterialTheme.typography.labelLarge)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            RoundAction(Ic.Phone, 84.dp, cs.tertiary, cs.onTertiary, desc = "رد", iconSize = 34.dp) { CallHub.answer(cur.call) }
                            Text("رد", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                    PillBtn(Ic.Message, "رفض برسالة") { quick = true }
                } else if (!ended) {
                    if (showPad) {
                        Text(
                            typed.takeLast(20).ifEmpty { " " }, style = MaterialTheme.typography.headlineSmall.copy(textDirection = TextDirection.Ltr),
                            modifier = Modifier.padding(bottom = 10.dp)
                        )
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                listOf("123", "456", "789", "*0#").forEach { row ->
                                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                        row.forEach { ch ->
                                            Box(Modifier.padding(0.dp)) {
                                                BigKey(ch.toString()) {
                                                    typed += ch
                                                    scope.launch {
                                                        cur.call.playDtmfTone(ch); delay(140); cur.call.stopDtmfTone()
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                    }
                    val route = audio?.route ?: CallAudioState.ROUTE_EARPIECE
                    val btOk = ((audio?.supportedRouteMask ?: 0) and CallAudioState.ROUTE_BLUETOOTH) != 0
                    val isMuted = audio?.isMuted ?: muted
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        Toggle(if (isMuted) Ic.MicOff else Ic.Mic, "كتم", isMuted) { muted = !isMuted; CallHub.setMuted(!isMuted) }
                        Toggle(
                            Ic.Speaker,
                            when (route) { CallAudioState.ROUTE_SPEAKER -> "السماعة"; CallAudioState.ROUTE_BLUETOOTH -> "بلوتوث"; else -> "الأذن" },
                            route == CallAudioState.ROUTE_SPEAKER || route == CallAudioState.ROUTE_BLUETOOTH
                        ) {
                            CallHub.setRoute(
                                when {
                                    route == CallAudioState.ROUTE_SPEAKER -> if (btOk) CallAudioState.ROUTE_BLUETOOTH else CallAudioState.ROUTE_WIRED_OR_EARPIECE
                                    route == CallAudioState.ROUTE_BLUETOOTH -> CallAudioState.ROUTE_WIRED_OR_EARPIECE
                                    else -> CallAudioState.ROUTE_SPEAKER
                                }
                            )
                        }
                        Toggle(Ic.Keypad, "الأرقام", showPad) { showPad = !showPad }
                    }
                    Spacer(Modifier.height(18.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        if (cur.canHold) Toggle(Ic.Pause, "تعليق", cur.state == Call.STATE_HOLDING) {
                            CallHub.hold(cur.call, cur.state != Call.STATE_HOLDING)
                        }
                        Toggle(Ic.Add, "إضافة", false) { onAddCall() }
                        if (cur.canMerge) Toggle(Ic.Merge, "دمج", false) {
                            others.firstOrNull()?.let { cur.call.conference(it.call) }
                        } else if (cur.canSwap) Toggle(Ic.Swap, "تبديل", false) { cur.call.swapConference() }
                    }
                    Spacer(Modifier.height(28.dp))
                    RoundAction(Ic.PhoneEnd, 80.dp, cs.error, cs.onError, desc = "إنهاء", iconSize = 34.dp) { CallHub.hangup(cur.call) }
                }
            }

            if (!ended && cur.state == Call.STATE_SELECT_PHONE_ACCOUNT) {
                AlertDialog(
                    onDismissRequest = { CallHub.hangup(cur.call) },
                    title = { Text("اتصل بأنهي شريحة؟", style = MaterialTheme.typography.titleLarge) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Dialing.accounts(ctx).forEach { h ->
                                BigButton(Dialing.label(ctx, h), icon = Ic.Phone, tonal = true) { cur.call.phoneAccountSelected(h, false) }
                            }
                        }
                    },
                    confirmButton = {},
                    dismissButton = { TextButton(onClick = { CallHub.hangup(cur.call) }) { Text("إلغاء") } }
                )
            }

            if (quick && ringing) {
                AlertDialog(
                    onDismissRequest = { quick = false },
                    title = { Text("رفض برسالة", style = MaterialTheme.typography.titleLarge) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            listOf("مش فاضي دلوقتي، هكلمك بعدين.", "ممكن أكلمك بعدين؟", "أنا في اجتماع.").forEach { m ->
                                BigButton(m, tonal = true) { quick = false; CallHub.reject(cur.call, m) }
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

@Composable
private fun BigKey(label: String, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Box(
        Modifier.size(64.dp).clip(CircleShape).background(cs.surfaceContainerHigh).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Text(label, style = MaterialTheme.typography.titleLarge) }
}
