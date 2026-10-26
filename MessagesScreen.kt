package com.mohamed.phone.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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

    Box(Modifier.fillMaxSize().glow(cs.tertiaryContainer, 0.68f)) {
        LazyColumn(
            Modifier.fillMaxSize().imePadding(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = bottom)
        ) {
            item {
                TabHeader("الرسائل", "الهاتف · ${all.size} محادثة" + if (unread > 0) " · $unread جديدة" else "") {
                    Box {
                        CookieIcon(Ic.Menu, 40.dp, cs.tertiary, cs.onTertiary, desc = "القايمة", onClick = { menu = true })
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
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 2.dp).clip(RoundedCornerShape(22.dp))
                        .clickable { nav.push(Overlay.Conversation(t.threadId, t.address)) }.padding(6.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Avatar(name, contact?.photo, 48.dp, seed = t.address)
                    Column(Modifier.weight(1f)) {
                        Text(
                            name, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            t.snippet, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (unreadRow) cs.onSurface else cs.onSurfaceVariant,
                            fontWeight = if (unreadRow) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            Fmt.day(t.date), style = MaterialTheme.typography.labelMedium,
                            color = if (unreadRow) cs.primary else cs.onSurfaceVariant
                        )
                        if (unreadRow) {
                            Box(Modifier.size(22.dp).clip(CircleShape).background(cs.primary), contentAlignment = Alignment.Center) {
                                Text(if (t.unread > 99) "99+" else t.unread.toString(), color = cs.onPrimary, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                    Box {
                        IconBtn(Ic.More, "المزيد") { rowMenu = t.threadId }
                        DropdownMenu(rowMenu == t.threadId, { rowMenu = null }) {
                            if (t.address.any { it.isDigit() }) MenuItem("اتصال", Ic.Phone) { rowMenu = null; call(t.address) }
                            MenuItem("حذف المحادثة", Ic.Trash, danger = true) { rowMenu = null; deleting = t }
                        }
                    }
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
