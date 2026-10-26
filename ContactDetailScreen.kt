package com.mohamed.phone.ui

import android.provider.CallLog
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import com.mohamed.phone.data.Fmt
import com.mohamed.phone.data.Repo

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ContactDetailScreen(id: Long) {
    val cs = MaterialTheme.colorScheme
    val ctx = LocalContext.current
    val nav = LocalNavigator.current
    val call = LocalCaller.current
    val all by Repo.contacts.collectAsState()
    val calls by Repo.calls.collectAsState()
    val c = all.firstOrNull { it.id == id }
    var menu by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var starOverride by remember { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(c == null) { if (c == null) nav.pop() }
    if (c == null) return

    val keys = c.numbers.map { Repo.key(it) }.toSet()
    val recent = remember(calls, c) { calls.filter { Repo.key(it.number) in keys }.take(6) }
    val starred = starOverride ?: c.starred

    Surface(Modifier.fillMaxSize(), color = cs.surface) {
        Box(Modifier.fillMaxSize().glow(cs.primaryContainer)) {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    BackPill { nav.pop() }
                    Box {
                        CookieIcon(Ic.More, 44.dp, cs.primary, cs.onPrimary, desc = "المزيد", onClick = { menu = true })
                        DropdownMenu(menu, { menu = false }) {
                            MenuItem("تعديل", Ic.Edit) { menu = false; Intents.editContact(ctx, c) }
                            MenuItem("حظر الرقم", Ic.Block) {
                                menu = false
                                if (c.numbers.all { Repo.block(it) }) toast(ctx, "اتحظر") else toast(ctx, "لازم التطبيق يبقى الاتصال الافتراضي")
                            }
                            MenuItem("حذف جهة الاتصال", Ic.Trash, danger = true) { menu = false; confirmDelete = true }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                Avatar(c.name, c.photo, 128.dp)
                Spacer(Modifier.height(16.dp))
                Text(c.name, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PillBtn(Ic.Phone, "اتصال", active = true) { c.numbers.firstOrNull()?.let { call(it) } }
                    PillBtn(Ic.Message, "رسالة") { c.numbers.firstOrNull()?.let { nav.push(Overlay.Conversation(-1L, it)) } }
                    PillBtn(if (starred) Ic.StarFill else Ic.Star, null, active = starred) {
                        starOverride = !starred
                        Repo.setStarred(c, !starred)
                    }
                }

                Column(Modifier.fillMaxWidth()) {
                    SectionTitle("الأرقام")
                    c.numbers.forEach { n ->
                        Box(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
                                .combinedClickable(onClick = { call(n) }, onLongClick = { Intents.copy(ctx, n) })
                        ) {
                            SettingRow(Ic.Phone, n, "اضغط للاتصال · اضغط مطوّلًا للنسخ", trailing = {
                                IconBtn(Ic.Message, "رسالة") { nav.push(Overlay.Conversation(-1L, n)) }
                                IconBtn(Ic.Copy, "نسخ") { Intents.copy(ctx, n) }
                            })
                        }
                    }

                    if (recent.isNotEmpty()) {
                        SectionTitle("آخر المكالمات")
                        recent.forEach { e ->
                            val bad = e.type == CallLog.Calls.MISSED_TYPE || e.type == CallLog.Calls.REJECTED_TYPE
                            val label = when (e.type) {
                                CallLog.Calls.INCOMING_TYPE -> "واردة"
                                CallLog.Calls.OUTGOING_TYPE -> "صادرة"
                                CallLog.Calls.MISSED_TYPE -> "فائتة"
                                CallLog.Calls.REJECTED_TYPE -> "مرفوضة"
                                else -> "مكالمة"
                            }
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Icon(
                                    if (e.type == CallLog.Calls.OUTGOING_TYPE) Ic.ArrowOut else Ic.ArrowIn, null,
                                    Modifier.size(20.dp), tint = if (bad) cs.error else cs.onSurfaceVariant
                                )
                                Text(label, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, color = if (bad) cs.error else cs.onSurface)
                                Text(Fmt.full(e.date), style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant)
                            }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    BigButton("تعديل جهة الاتصال", icon = Ic.Edit, tonal = true) { Intents.editContact(ctx, c) }
                    Spacer(Modifier.height(12.dp))
                    BigButton("حذف جهة الاتصال", icon = Ic.Trash, danger = true) { confirmDelete = true }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            "حذف ${c.name}؟", "جهة الاتصال دي هتتمسح من الجهاز.", "احذف", danger = true,
            onConfirm = {
                confirmDelete = false
                if (Repo.deleteContact(c)) { toast(ctx, "اتمسحت"); nav.pop() } else toast(ctx, "مش قادر أمسحها")
            },
            onDismiss = { confirmDelete = false }
        )
    }
}
