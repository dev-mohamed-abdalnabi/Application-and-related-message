package com.mohamed.phone.ui

import android.provider.CallLog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mohamed.phone.data.CallEntry
import com.mohamed.phone.data.Fmt
import com.mohamed.phone.data.Repo

private class TypeInfo(val icon: ImageVector, val label: String, val bad: Boolean)

private fun typeInfo(type: Int): TypeInfo = when (type) {
    CallLog.Calls.INCOMING_TYPE -> TypeInfo(Ic.ArrowIn, "واردة", false)
    CallLog.Calls.OUTGOING_TYPE -> TypeInfo(Ic.ArrowOut, "صادرة", false)
    CallLog.Calls.MISSED_TYPE -> TypeInfo(Ic.ArrowIn, "فائتة", true)
    CallLog.Calls.VOICEMAIL_TYPE -> TypeInfo(Ic.Voicemail, "بريد صوتي", false)
    CallLog.Calls.REJECTED_TYPE -> TypeInfo(Ic.ArrowIn, "مرفوضة", true)
    CallLog.Calls.BLOCKED_TYPE -> TypeInfo(Ic.Block, "محظورة", true)
    else -> TypeInfo(Ic.Phone, "مكالمة", false)
}

private fun nameOf(e: CallEntry): String =
    Repo.lookup(e.number)?.name ?: e.cachedName?.takeIf { it.isNotBlank() } ?: if (e.number.isEmpty()) "رقم خاص" else e.number

private fun matches(e: CallEntry, q: String): Boolean {
    if (q.isBlank()) return true
    val digits = q.filter { it.isDigit() }
    return nameOf(e).contains(q, ignoreCase = true) || (digits.isNotEmpty() && e.number.filter { it.isDigit() }.contains(digits))
}

@Composable
fun RecentsScreen(bottom: Dp) {
    val cs = MaterialTheme.colorScheme
    val ctx = LocalContext.current
    val nav = LocalNavigator.current
    val call = LocalCaller.current
    val all by Repo.calls.collectAsState()
    val contacts by Repo.contacts.collectAsState()
    var q by remember { mutableStateOf("") }
    var sub by remember { mutableIntStateOf(0) }
    var expanded by remember { mutableStateOf<Long?>(null) }
    var menu by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }
    val roles = rememberRoleState()
    val requestRole = rememberRoleRequester()

    val items = remember(all, contacts, q, sub) {
        all.filter { (sub == 0 || it.type == CallLog.Calls.MISSED_TYPE) && matches(it, q) }
    }

    Box(Modifier.fillMaxSize().glow(cs.primaryContainer)) {
        LazyColumn(
            Modifier.fillMaxSize().imePadding(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = bottom)
        ) {
            item {
                TabHeader("الأخيرة", "الهاتف · ${all.size} مكالمة") {
                    Box {
                        CookieIcon(Ic.Menu, 40.dp, cs.primary, cs.onPrimary, desc = "القايمة", onClick = { menu = true })
                        DropdownMenu(menu, { menu = false }) {
                            MenuItem("تحديث", Ic.Clock) { menu = false; Repo.reloadAll() }
                            MenuItem("مسح السجل", Ic.Trash, danger = true) { menu = false; confirmClear = true }
                        }
                    }
                }
            }
            item { SearchBar(q, { q = it }, "ابحث بالاسم أو الرقم", Modifier.padding(top = 10.dp, bottom = 2.dp)) }
            item { Switcher(listOf("الكل", "الفائتة"), sub, Modifier.padding(vertical = 8.dp)) { sub = it } }
            if (!roles.first) item {
                Box(Modifier.padding(bottom = 12.dp)) {
                    RoleBanner(Ic.Phone, "خليه تطبيق الاتصال الافتراضي", "عشان المكالمات تظهر بشاشة التطبيق ده وتقدر تحظر الأرقام") {
                        requestRole(true)
                    }
                }
            }
            if (items.isEmpty()) item {
                EmptyState(
                    Ic.Clock, if (sub == 1) "مفيش مكالمات فايتة" else "مفيش مكالمات",
                    if (q.isNotBlank()) "جرّب كلمة تانية." else "المكالمات اللي بتعملها وبتستقبلها هتظهر هنا."
                )
            }
            items(items, key = { it.ids.first() }) { e ->
                val info = typeInfo(e.type)
                val contact = Repo.lookup(e.number)
                val name = nameOf(e)
                val open = expanded == e.ids.first()
                val sec = if (e.type == CallLog.Calls.INCOMING_TYPE || e.type == CallLog.Calls.OUTGOING_TYPE) {
                    info.label + " · " + Fmt.duration(e.duration)
                } else info.label
                Column(
                    Modifier.fillMaxWidth().padding(vertical = 2.dp).clip(RoundedCornerShape(22.dp))
                        .background(if (open) cs.surfaceContainerHigh else Color.Transparent)
                        .clickable { expanded = if (open) null else e.ids.first() }
                        .padding(6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Avatar(name, contact?.photo, 48.dp, seed = e.number)
                        Column(Modifier.weight(1f)) {
                            Text(
                                name + if (e.count > 1) " (${e.count})" else "", maxLines = 1, overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.titleSmall, color = if (info.bad) cs.error else cs.onSurface
                            )
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(info.icon, null, Modifier.size(16.dp), tint = if (info.bad) cs.error else cs.onSurfaceVariant)
                                Text(sec, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        Text(Fmt.day(e.date), style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant)
                    }
                    AnimatedVisibility(open) {
                        Row(
                            Modifier.padding(top = 10.dp, bottom = 4.dp).horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (e.number.isNotEmpty()) {
                                PillBtn(Ic.Phone, "اتصال", active = true) { call(e.number) }
                                PillBtn(Ic.Message, "رسالة") { nav.push(Overlay.Conversation(-1L, e.number)) }
                            }
                            if (contact != null) PillBtn(Ic.Person, "الجهة") { nav.push(Overlay.ContactDetail(contact.id)) }
                            else if (e.number.isNotEmpty()) PillBtn(Ic.Add, "إضافة") { Intents.addContact(ctx, e.number) }
                            if (e.number.isNotEmpty()) PillBtn(Ic.Block, tint = cs.error) {
                                if (Repo.block(e.number)) toast(ctx, "اتحظر الرقم") else toast(ctx, "لازم التطبيق يبقى الاتصال الافتراضي عشان تحظر")
                            }
                            PillBtn(Ic.Trash, tint = cs.error) {
                                if (!Repo.deleteCalls(e.ids)) toast(ctx, "مش قادر أمسح")
                            }
                        }
                    }
                }
            }
        }
    }

    if (confirmClear) {
        ConfirmDialog(
            "مسح سجل المكالمات؟", "كل المكالمات هتتمسح ومش هترجع تاني.", "امسح", danger = true,
            onConfirm = { confirmClear = false; if (!Repo.clearCallLog()) toast(ctx, "مش قادر أمسح السجل") },
            onDismiss = { confirmClear = false }
        )
    }
}
