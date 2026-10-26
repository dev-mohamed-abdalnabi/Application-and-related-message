package com.mohamed.phone.ui

import android.provider.CallLog
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.text.style.TextOverflow
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
                        CookieIcon(Ic.More, 40.dp, cs.primary, cs.onPrimary, desc = "المزيد", onClick = { menu = true })
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

                Spacer(Modifier.height(4.dp))
                Avatar(c.name, c.photo, 104.dp)
                Spacer(Modifier.height(12.dp))
                Text(
                    c.name, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center,
                    maxLines = 2, overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(16.dp))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ActionTile(Ic.Phone, "اتصال", Modifier.weight(1f), active = true) { c.numbers.firstOrNull()?.let { call(it) } }
                    ActionTile(Ic.Message, "رسالة", Modifier.weight(1f)) {
                        c.numbers.firstOrNull()?.let { nav.push(Overlay.Conversation(-1L, it)) }
                    }
                    ActionTile(if (starred) Ic.StarFill else Ic.Star, if (starred) "مفضلة" else "تفضيل", Modifier.weight(1f), active = starred) {
                        starOverride = !starred
                        Repo.setStarred(c, !starred)
                    }
                }

                Text(
                    "الأرقام", Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 8.dp, start = 4.dp),
                    style = MaterialTheme.typography.titleSmall, color = cs.primary
                )
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(cs.surfaceContainer)) {
                    c.numbers.forEach { n ->
                        Row(
                            Modifier.fillMaxWidth()
                                .combinedClickable(onClick = { call(n) }, onLongClick = { Intents.copy(ctx, n) })
                                .padding(start = 18.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                                Text(n, style = MaterialTheme.typography.titleMedium.copy(textDirection = TextDirection.Ltr))
                                Text("اضغط للاتصال", style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
                            }
                            IconBtn(Ic.Message, "رسالة") { nav.push(Overlay.Conversation(-1L, n)) }
                            Spacer(Modifier.width(6.dp))
                            IconBtn(Ic.Copy, "نسخ") { Intents.copy(ctx, n) }
                        }
                    }
                }

                if (recent.isNotEmpty()) {
                    Text(
                        "آخر المكالمات", Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 8.dp, start = 4.dp),
                        style = MaterialTheme.typography.titleSmall, color = cs.primary
                    )
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(cs.surfaceContainer).padding(vertical = 6.dp)) {
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
                                Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    if (e.type == CallLog.Calls.OUTGOING_TYPE) Ic.ArrowOut else Ic.ArrowIn, null,
                                    Modifier.size(18.dp), tint = if (bad) cs.error else cs.onSurfaceVariant
                                )
                                Text(label, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, color = if (bad) cs.error else cs.onSurface)
                                Text(Fmt.day(e.date), style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    BigButton("تعديل", Modifier.weight(1f), icon = Ic.Edit, tonal = true, height = 52.dp) { Intents.editContact(ctx, c) }
                    BigButton("حذف", Modifier.weight(1f), icon = Ic.Trash, danger = true, height = 52.dp) { confirmDelete = true }
                }
                Spacer(Modifier.height(16.dp))
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
