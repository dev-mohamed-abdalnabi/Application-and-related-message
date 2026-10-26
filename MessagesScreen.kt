package com.mohamed.phone.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mohamed.phone.data.Fmt
import com.mohamed.phone.data.Repo
import com.mohamed.phone.data.ThreadItem

@Composable
fun MessagesScreen(bottom: Dp) {
    val cs = MaterialTheme.colorScheme
    val ctx = LocalContext.current
    val nav = LocalNavigator.current
    val call = LocalCaller.current
    val all by Repo.threads.collectAsState()
    val contacts by Repo.contacts.collectAsState()
    var q by remember { mutableStateOf("") }
    var menu by remember { mutableStateOf(false) }
    var rowMenu by remember { mutableStateOf<Long?>(null) }
    var deleting by remember { mutableStateOf<ThreadItem?>(null) }
    val roles = rememberRoleState()
    val requestRole = rememberRoleRequester()

    val list = remember(all, contacts, q) {
        if (q.isBlank()) all else all.filter {
            (Repo.nameFor(it.address) ?: it.address).contains(q, ignoreCase = true) ||
                it.snippet.contains(q, ignoreCase = true) || it.address.contains(q)
        }
    }
    val unread = all.sumOf { it.unread }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize().imePadding(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = bottom)
        ) {
            item {
                TabHeader("الرسائل", "الهاتف · ${all.size} محادثة" + if (unread > 0) " · $unread جديدة" else "") {
                    Box {
                        HeaderBtn(Ic.More, "القايمة") { menu = true }
                        DropdownMenu(menu, { menu = false }) {
                            MenuItem("رسالة جديدة", Ic.Add) { menu = false; nav.push(Overlay.NewMessage) }
                            MenuItem("تحديث", Ic.Clock) { menu = false; Repo.reloadAll() }
                        }
                    }
                }
            }
            item { SearchBar(q, { q = it }, "ابحث في الرسائل", Modifier.padding(top = 10.dp, bottom = 8.dp)) }
            if (!roles.second) item {
                Box(Modifier.padding(bottom = 12.dp)) {
                    RoleBanner(Ic.Message, "خليه تطبيق الرسائل الافتراضي", "عشان تستقبل الرسائل وتحفظ اللي بتبعتها وتقدر تمسح المحادثات") {
                        requestRole(false)
                    }
                }
            }
            if (list.isEmpty()) item {
                EmptyState(
                    Ic.Message, if (q.isNotBlank()) "مفيش نتائج" else "مفيش رسايل",
                    if (q.isNotBlank()) "جرّب كلمة تانية." else "ابدأ محادثة جديدة بالزرار اللي تحت."
                )
            }
            items(list, key = { it.threadId }) { t ->
                val name = Repo.nameFor(t.address) ?: t.address
                val contact = Repo.lookup(t.address)
                val unreadRow = t.unread > 0
                Column(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.fillMaxWidth().clickable { nav.push(Overlay.Conversation(t.threadId, t.address)) }.padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(Modifier.size(10.dp).clip(CircleShape).background(if (unreadRow) cs.primary else Color.Transparent))
                        Avatar(name, contact?.photo, 48.dp, seed = t.address)
                        Spacer(Modifier.size(2.dp))
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    name, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, textDirection = TextDirection.Content)
                                )
                                Text(
                                    Fmt.day(t.date), style = MaterialTheme.typography.bodySmall,
                                    color = if (unreadRow) cs.primary else cs.onSurfaceVariant
                                )
                            }
                            Text(
                                t.snippet, maxLines = 2, overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (unreadRow) cs.onSurface else cs.onSurfaceVariant
                            )
                        }
                        Box {
                            IconBtn(Ic.More, "المزيد") { rowMenu = t.threadId }
                            DropdownMenu(rowMenu == t.threadId, { rowMenu = null }) {
                                if (t.address.any { it.isDigit() }) MenuItem("اتصال", Ic.Phone) { rowMenu = null; call(t.address) }
                                MenuItem("حذف المحادثة", Ic.Trash, danger = true) { rowMenu = null; deleting = t }
                            }
                        }
                    }
                    RowDivider(start = 76.dp)
                }
            }
        }
    }

    val d = deleting
    if (d != null) {
        ConfirmDialog(
            "مسح المحادثة؟", "كل الرسايل في المحادثة دي هتتمسح.", "امسح", danger = true,
            onConfirm = {
                deleting = null
                if (!Repo.deleteThread(d.threadId)) toast(ctx, "لازم التطبيق يبقى الرسائل الافتراضي عشان تمسح")
                else if (!roles.second) toast(ctx, "لازم التطبيق يبقى الرسائل الافتراضي عشان تمسح")
            },
            onDismiss = { deleting = null }
        )
    }
}
