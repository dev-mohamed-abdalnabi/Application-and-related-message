package com.mohamed.phone.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mohamed.phone.data.ContactItem
import com.mohamed.phone.data.Fmt
import com.mohamed.phone.data.Repo

@Composable
fun ContactsScreen(bottom: Dp) {
    val cs = MaterialTheme.colorScheme
    val ctx = LocalContext.current
    val nav = LocalNavigator.current
    val all by Repo.contacts.collectAsState()
    var q by remember { mutableStateOf("") }
    var sub by remember { mutableIntStateOf(0) }
    var menu by remember { mutableStateOf(false) }

    val list = remember(all, q, sub) {
        val digits = q.filter { it.isDigit() }
        all.filter { c ->
            (sub == 0 || c.starred) && (q.isBlank() || c.name.contains(q, ignoreCase = true) ||
                (digits.isNotEmpty() && c.numbers.any { n -> n.filter { it.isDigit() }.contains(digits) }))
        }
    }
    val grouped = remember(list) { list.groupBy { Fmt.section(it.name) } }

    Box(Modifier.fillMaxSize().glow(cs.primaryContainer)) {
        LazyColumn(
            Modifier.fillMaxSize().imePadding(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = bottom)
        ) {
            item {
                TabHeader("جهات الاتصال", "الهاتف · ${all.size} جهة") {
                    Box {
                        CookieIcon(Ic.Menu, 40.dp, cs.primary, cs.onPrimary, desc = "القايمة", onClick = { menu = true })
                        DropdownMenu(menu, { menu = false }) {
                            MenuItem("جهة اتصال جديدة", Ic.Add) { menu = false; Intents.addContact(ctx) }
                            MenuItem("تحديث", Ic.Clock) { menu = false; Repo.reloadAll() }
                        }
                    }
                }
            }
            item { SearchBar(q, { q = it }, "ابحث في جهات الاتصال", Modifier.padding(top = 10.dp, bottom = 2.dp)) }
            item { Switcher(listOf("الكل", "المفضلة"), sub, Modifier.padding(vertical = 8.dp)) { sub = it } }

            if (list.isEmpty()) item {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    EmptyState(
                        Ic.Person, if (q.isNotBlank()) "مفيش نتائج" else if (sub == 1) "مفيش مفضلة" else "مفيش جهات اتصال",
                        if (q.isNotBlank()) "جرّب كلمة تانية." else if (sub == 1) "علّم جهة اتصال بالنجمة وهتظهر هنا." else "ضيف أول جهة اتصال."
                    )
                    if (all.isEmpty()) BigButton("جهة اتصال جديدة", icon = Ic.Add) { Intents.addContact(ctx) }
                }
            }

            grouped.forEach { (letter, people) ->
                item(key = "h_$letter") { SectionTitle(letter, first = false) }
                items(people, key = { it.id }) { c -> ContactRow(c) { nav.push(Overlay.ContactDetail(c.id)) } }
            }
        }
    }
}

@Composable
fun ContactRow(c: ContactItem, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().padding(vertical = 2.dp).clip(RoundedCornerShape(22.dp)).clickable(onClick = onClick).padding(6.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Avatar(c.name, c.photo, 48.dp)
        Column(Modifier.weight(1f)) {
            Text(c.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                c.numbers.firstOrNull() ?: "",
                style = MaterialTheme.typography.bodySmall.copy(textDirection = TextDirection.Ltr),
                color = cs.onSurfaceVariant, maxLines = 1
            )
        }
        if (c.starred) Icon(Ic.StarFill, null, Modifier.size(20.dp), tint = cs.primary)
    }
}
