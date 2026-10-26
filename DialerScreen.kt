package com.mohamed.phone.ui

import android.media.AudioManager
import android.media.ToneGenerator
import android.provider.CallLog
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mohamed.phone.Prefs
import com.mohamed.phone.data.Repo

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DialKey(main: String, sub: String?, onClick: () -> Unit, onLong: (() -> Unit)? = null) {
    val cs = MaterialTheme.colorScheme
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val r by animateDpAsState(if (pressed) 22.dp else 38.dp, spring(0.55f, 600f), label = "kr")
    val s by animateFloatAsState(if (pressed) 0.96f else 1f, spring(0.55f, 600f), label = "ks")
    Column(
        Modifier.size(76.dp).graphicsLayer { scaleX = s; scaleY = s }.clip(RoundedCornerShape(r)).background(cs.surfaceContainerHigh)
            .combinedClickable(interactionSource = src, indication = null, onClick = onClick, onLongClick = onLong),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
    ) {
        Text(main, style = MaterialTheme.typography.headlineMedium)
        if (sub != null) Text(sub, style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DialerScreen(initial: String) {
    val cs = MaterialTheme.colorScheme
    val ctx = LocalContext.current
    val nav = LocalNavigator.current
    val call = LocalCaller.current
    val hap = LocalHapticFeedback.current
    val contacts by Repo.contacts.collectAsState()
    val calls by Repo.calls.collectAsState()
    var number by remember { mutableStateOf(initial) }

    val tg = remember { try { ToneGenerator(AudioManager.STREAM_DTMF, 70) } catch (e: Exception) { null } }
    DisposableEffect(Unit) { onDispose { tg?.release() } }

    fun feedback(c: Char) {
        if (Prefs.haptic.value) hap.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        if (Prefs.keypadTones.value && tg != null) {
            val t = when (c) {
                '*' -> ToneGenerator.TONE_DTMF_S
                '#' -> ToneGenerator.TONE_DTMF_P
                in '0'..'9' -> ToneGenerator.TONE_DTMF_0 + (c - '0')
                else -> -1
            }
            if (t >= 0) try { tg.startTone(t, 110) } catch (e: Exception) { }
        }
    }
    fun press(c: Char) { feedback(c); if (number.length < 30) number += c }

    val lastOut = remember(calls) { calls.firstOrNull { it.type == CallLog.Calls.OUTGOING_TYPE && it.number.isNotEmpty() }?.number }
    val digits = number.filter { it.isDigit() }
    val sugg = remember(number, contacts) {
        if (digits.length < 2) emptyList() else contacts.filter { c ->
            c.numbers.any { n -> n.filter { it.isDigit() }.contains(digits) }
        }.take(3)
    }
    val matched = remember(number, contacts) { Repo.lookup(number)?.name }

    Surface(Modifier.fillMaxSize(), color = cs.surface) {
        Box(Modifier.fillMaxSize().glow(cs.primaryContainer)) {
            Column(
                Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    BackPill { nav.pop() }
                    if (number.isNotEmpty() && matched == null) PillBtn(Ic.Add, "إضافة لجهة اتصال") { Intents.addContact(ctx, number) }
                }

                Column(
                    Modifier.weight(1f).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
                ) {
                    val fs = when { number.length > 16 -> 22.sp; number.length > 12 -> 28.sp; else -> 36.sp }
                    Text(
                        number.ifEmpty { " " }, maxLines = 1, overflow = TextOverflow.Clip, textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.displaySmall.copy(fontSize = fs, textDirection = TextDirection.Ltr),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (matched != null) {
                        Text(matched, style = MaterialTheme.typography.titleMedium, color = cs.primary)
                    }
                    Spacer(Modifier.height(10.dp))
                    sugg.forEach { c ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 2.dp).clip(RoundedCornerShape(22.dp))
                                .clickable { number = c.numbers.firstOrNull { n -> n.filter { it.isDigit() }.contains(digits) } ?: c.numbers.first() }
                                .padding(6.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Avatar(c.name, c.photo, 40.dp)
                            Column(Modifier.weight(1f)) {
                                Text(c.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    c.numbers.first(), color = cs.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall.copy(textDirection = TextDirection.Ltr)
                                )
                            }
                        }
                    }
                }

                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        val rows = listOf(
                            listOf("1" to null, "2" to "ABC", "3" to "DEF"),
                            listOf("4" to "GHI", "5" to "JKL", "6" to "MNO"),
                            listOf("7" to "PQRS", "8" to "TUV", "9" to "WXYZ"),
                            listOf("*" to null, "0" to "+", "#" to null)
                        )
                        rows.forEach { r ->
                            Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                                r.forEach { (k, sub) ->
                                    DialKey(
                                        k, sub, onClick = { press(k[0]) },
                                        onLong = if (k == "0") ({ feedback('0'); number += "+" }) else null
                                    )
                                }
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(22.dp), verticalAlignment = Alignment.CenterVertically) {
                            Spacer(Modifier.size(76.dp))
                            RoundAction(Ic.Phone, 76.dp, cs.tertiary, cs.onTertiary, desc = "اتصال", iconSize = 32.dp) {
                                if (number.isEmpty()) {
                                    if (lastOut != null) number = lastOut
                                } else {
                                    call(number)
                                    nav.pop()
                                }
                            }
                            Box(
                                Modifier.size(76.dp).clip(RoundedCornerShape(38.dp))
                                    .combinedClickable(
                                        onClick = { if (number.isNotEmpty()) number = number.dropLast(1) },
                                        onLongClick = { number = "" }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (number.isNotEmpty()) Icon(Ic.Backspace, "مسح", Modifier.size(28.dp), tint = cs.onSurfaceVariant)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}
