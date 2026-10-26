package com.mohamed.phone.ui

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import android.provider.Telephony
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
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
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mohamed.phone.data.Fmt
import com.mohamed.phone.data.Repo
import com.mohamed.phone.data.SmsItem
import com.mohamed.phone.sms.SmsSender
import com.mohamed.phone.telecom.Roles

private sealed interface ChatItem {
    data class Header(val text: String) : ChatItem
    data class Msg(val m: SmsItem) : ChatItem
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ConversationScreen(threadId: Long, address: String, draft: String) {
    val cs = MaterialTheme.colorScheme
    val ctx = LocalContext.current
    val nav = LocalNavigator.current
    val call = LocalCaller.current
    val threads by Repo.threads.collectAsState()
    val tid = if (threadId > 0) threadId
    else threads.firstOrNull { Repo.key(it.address) == Repo.key(address) && Repo.key(address).isNotEmpty() }?.threadId ?: -1L

    val msgs by remember(tid, address) { Repo.messages(tid, address) }.collectAsState(emptyList())
    var text by remember { mutableStateOf(draft) }
    var menuFor by remember { mutableStateOf<Long?>(null) }
    val name = Repo.nameFor(address) ?: address
    val contact = Repo.lookup(address)
    val roles = rememberRoleState()
    val requestRole = rememberRoleRequester()

    LaunchedEffect(tid, msgs.size) { Repo.markRead(tid) }

    val chat = remember(msgs) {
        val out = ArrayList<ChatItem>()
        var last = 0L
        for (m in msgs) {
            if (last == 0L || !Fmt.sameDay(last, m.date)) out.add(ChatItem.Header(Fmt.dayHeader(m.date)))
            out.add(ChatItem.Msg(m)); last = m.date
        }
        out.reversed()
    }

    Surface(Modifier.fillMaxSize(), color = cs.surface) {
        Column(Modifier.fillMaxSize().statusBarsPadding().imePadding().navigationBarsPadding()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                BackPill { nav.pop() }
                Spacer(Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Avatar(name, contact?.photo, 36.dp, seed = address)
                    Text(
                        name, style = MaterialTheme.typography.labelMedium.copy(textDirection = TextDirection.Content),
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 160.dp)
                    )
                }
                Spacer(Modifier.weight(1f))
                if (address.any { it.isDigit() }) IconBtn(Ic.Phone, "اتصال", tint = cs.primary) { call(address) } else Spacer(Modifier.size(36.dp))
                Spacer(Modifier.size(4.dp))
            }
            RowDivider()

            if (!roles.second) Box(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                RoleBanner(Ic.Message, "خليه تطبيق الرسائل الافتراضي", "عشان الرسايل اللي بتبعتها تتحفظ في المحادثة") { requestRole(false) }
            }

            LazyColumn(
                Modifier.weight(1f).fillMaxWidth(), reverseLayout = true,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(chat, key = { if (it is ChatItem.Msg) "m${it.m.id}" else "h${(it as ChatItem.Header).text}" }) { it ->
                    when (it) {
                        is ChatItem.Header -> Box(Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                            Text(it.text, style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant)
                        }
                        is ChatItem.Msg -> {
                            val m = it.m
                            val mine = m.type != Telephony.Sms.MESSAGE_TYPE_INBOX
                            val failed = m.type == Telephony.Sms.MESSAGE_TYPE_FAILED
                            val sending = m.type == Telephony.Sms.MESSAGE_TYPE_OUTBOX || m.type == Telephony.Sms.MESSAGE_TYPE_QUEUED
                            Box(Modifier.fillMaxWidth(), contentAlignment = if (mine) Alignment.CenterEnd else Alignment.CenterStart) {
                                Column(horizontalAlignment = if (mine) Alignment.End else Alignment.Start) {
                                    Box {
                                        Text(
                                            m.body,
                                            Modifier.widthIn(max = 280.dp)
                                                .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = if (mine) 18.dp else 4.dp, bottomEnd = if (mine) 4.dp else 18.dp))
                                                .background(if (failed) cs.errorContainer else if (mine) cs.primary else cs.surfaceContainerHigh)
                                                .combinedClickable(onClick = {}, onLongClick = { menuFor = m.id })
                                                .padding(horizontal = 14.dp, vertical = 8.dp),
                                            color = if (failed) cs.onErrorContainer else if (mine) cs.onPrimary else cs.onSurface,
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                        DropdownMenu(menuFor == m.id, { menuFor = null }) {
                                            MenuItem("نسخ", Ic.Copy) { menuFor = null; Intents.copy(ctx, m.body) }
                                            MenuItem("حذف", Ic.Trash, danger = true) {
                                                menuFor = null
                                                if (!Repo.deleteMessage(m.id) || !roles.second) toast(ctx, "لازم التطبيق يبقى الرسائل الافتراضي عشان تمسح")
                                            }
                                        }
                                    }
                                    Text(
                                        if (failed) "فشل الإرسال" else if (sending) "بيتبعت..." else Fmt.time(m.date),
                                        Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (failed) cs.error else cs.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TextField(
                    value = text, onValueChange = { text = it }, maxLines = 5,
                    modifier = Modifier.weight(1f).heightIn(min = 44.dp),
                    shape = RoundedCornerShape(22.dp), colors = fieldColors(),
                    placeholder = { Text("اكتب رسالة") }
                )
                RoundAction(
                    Ic.Send, 40.dp,
                    if (text.isBlank()) cs.surfaceContainerHigh else cs.primary,
                    if (text.isBlank()) cs.onSurfaceVariant else cs.onPrimary, desc = "إرسال", iconSize = 20.dp,
                    modifier = Modifier.padding(bottom = 2.dp)
                ) {
                    if (text.isNotBlank()) {
                        if (SmsSender.send(ctx, address, text)) text = "" else toast(ctx, "مش قادر أبعت الرسالة")
                    }
                }
            }
        }
    }
}

@Composable
fun NewMessageScreen() {
    val cs = MaterialTheme.colorScheme
    val nav = LocalNavigator.current
    val contacts by Repo.contacts.collectAsState()
    var q by remember { mutableStateOf("") }
    val digits = q.filter { it.isDigit() || it == '+' }
    val list = remember(contacts, q) {
        if (q.isBlank()) contacts.take(60) else contacts.filter {
            it.name.contains(q, ignoreCase = true) || it.numbers.any { n -> n.filter { d -> d.isDigit() }.contains(q.filter { d -> d.isDigit() }.ifEmpty { "\u0000" }) }
        }
    }
    fun open(addr: String) { nav.pop(); nav.push(Overlay.Conversation(-1L, addr)) }

    Surface(Modifier.fillMaxSize(), color = cs.surface) {
        Column(Modifier.fillMaxSize().statusBarsPadding().imePadding().navigationBarsPadding().padding(horizontal = 16.dp)) {
            Row(Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BackPill { nav.pop() }
                Text("رسالة جديدة", style = MaterialTheme.typography.titleMedium)
            }
            TextField(
                value = q, onValueChange = { q = it }, singleLine = true,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                shape = RoundedCornerShape(12.dp), colors = fieldColors(),
                placeholder = { Text("اكتب اسم أو رقم") }
            )
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(vertical = 12.dp)) {
                if (digits.length >= 3) item {
                    Row(
                        Modifier.fillMaxWidth().clickable { open(digits) }.padding(horizontal = 4.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Avatar("", null, 42.dp, seed = digits)
                        Text("إرسال إلى $digits", style = MaterialTheme.typography.titleSmall)
                    }
                }
                items(list, key = { it.id }) { c ->
                    ContactRow(c) { open(c.numbers.first()) }
                }
            }
        }
    }
}
