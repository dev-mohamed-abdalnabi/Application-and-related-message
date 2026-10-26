package com.mohamed.phone.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mohamed.phone.InCallScreen
import com.mohamed.phone.data.Repo
import com.mohamed.phone.telecom.Dialing
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

@Composable
fun Root() {
    val cs = MaterialTheme.colorScheme
    var tick by remember { mutableIntStateOf(0) }
    val resume by Nav.resume.collectAsState()
    val ok = remember(tick, resume) { Perms.essential.all { Repo.has(it) } }
    var skipped by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(ok) { if (ok) Repo.start() }

    Surface(Modifier.fillMaxSize(), color = cs.surface, contentColor = cs.onSurface) {
        if (ok || skipped) Shell()
        else PermissionScreen(onResult = { tick++; Repo.start() }, onSkip = { skipped = true })
    }
}

@Composable
fun PermissionScreen(onResult: () -> Unit, onSkip: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val ctx = LocalContext.current
    var asked by rememberSaveable { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        asked = true
        onResult()
    }
    Box(Modifier.fillMaxSize().glow(cs.primaryContainer)) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
        ) {
            CookieIcon(Ic.Phone, 148.dp, cs.primaryContainer, cs.onPrimaryContainer)
            Spacer(Modifier.height(28.dp))
            Text("محتاجين صلاحياتك", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
            Spacer(Modifier.height(10.dp))
            Text(
                "عشان نعرض جهات الاتصال وسجل المكالمات والرسائل، ونقدر نتصل ونبعت رسايل، لازم تسمح بالصلاحيات.",
                style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant, textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(28.dp))
            BigButton("اسمح بالصلاحيات", icon = Ic.Check) { launcher.launch(Perms.all.toTypedArray()) }
            if (asked) {
                Spacer(Modifier.height(12.dp))
                BigButton("افتح إعدادات التطبيق", icon = Ic.Settings, tonal = true) { openAppSettings(ctx) }
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onSkip) {
                Text("مش دلوقتي", style = MaterialTheme.typography.titleSmall, color = cs.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ExtendedFab(icon: ImageVector, text: String, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Row(
        Modifier.height(56.dp).clip(CircleShape).background(cs.primary).clickable(onClick = onClick).padding(horizontal = 22.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(icon, null, Modifier.size(24.dp), tint = cs.onPrimary)
        Text(text, color = cs.onPrimary, style = MaterialTheme.typography.titleSmall)
    }
}

@Composable
fun Shell() {
    val cs = MaterialTheme.colorScheme
    val ctx = LocalContext.current
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val stack = remember { mutableStateListOf<Overlay>() }
    var simFor by remember { mutableStateOf<String?>(null) }
    val imeOpen = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val threads by Repo.threads.collectAsState()
    val unread = threads.sumOf { it.unread }
    val navInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val bottom = if (imeOpen) 16.dp else navInset + 120.dp

    val pending by Nav.pending.collectAsState()
    LaunchedEffect(pending) {
        val p = pending
        if (p != null) { stack.add(p); Nav.pending.value = null }
    }

    BackHandler(enabled = stack.isNotEmpty()) { stack.removeAt(stack.lastIndex) }
    BackHandler(enabled = stack.isEmpty() && tab != 0) { tab = 0 }

    val navigator = remember {
        Navigator(
            push = { stack.add(it) },
            pop = { if (stack.isNotEmpty()) stack.removeAt(stack.lastIndex) }
        )
    }
    val caller = remember<(String) -> Unit> {
        { raw ->
            val n = raw.trim()
            if (n.isNotEmpty()) {
                val def = Dialing.defaultAccount(ctx)
                val accs = Dialing.accounts(ctx)
                if (def != null || accs.size < 2) {
                    Dialing.place(ctx, n, def)
                } else {
                    simFor = n
                }
            }
        }
    }

    CompositionLocalProvider(LocalNavigator provides navigator, LocalCaller provides caller) {
        Box(Modifier.fillMaxSize()) {
            Crossfade(tab, Modifier.fillMaxSize(), label = "tab") { t ->
                when (t) {
                    0 -> RecentsScreen(bottom)
                    1 -> ContactsScreen(bottom)
                    2 -> MessagesScreen(bottom)
                    else -> SettingsScreen(bottom)
                }
            }

            AnimatedVisibility(
                !imeOpen, Modifier.align(Alignment.BottomCenter),
                enter = slideInVertically { it } + fadeIn(), exit = slideOutVertically { it } + fadeOut()
            ) {
                Column(
                    Modifier.fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(Color.Transparent, cs.surface)))
                        .navigationBarsPadding().padding(top = 28.dp, bottom = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    FloatingNav(
                        listOf(
                            NavTab("الأخيرة", Ic.Clock),
                            NavTab("الجهات", Ic.Person),
                            NavTab("الرسائل", Ic.Message, unread),
                            NavTab("الإعدادات", Ic.Settings)
                        ),
                        tab
                    ) { tab = it }
                }
            }

            AnimatedVisibility(
                !imeOpen && stack.isEmpty() && tab <= 2, Modifier.align(Alignment.BottomStart),
                enter = slideInVertically { it } + fadeIn(), exit = slideOutVertically { it } + fadeOut()
            ) {
                Box(Modifier.navigationBarsPadding().padding(start = 20.dp, bottom = 96.dp)) {
                    if (tab == 2) ExtendedFab(Ic.Add, "رسالة جديدة") { stack.add(Overlay.NewMessage) }
                    else RoundAction(Ic.Keypad, 64.dp, cs.primary, cs.onPrimary, desc = "لوحة الأرقام") {
                        stack.add(Overlay.Dialer(""))
                    }
                }
            }

            val top = stack.lastOrNull()
            val holder = remember { arrayOfNulls<Overlay>(1) }
            if (top != null) holder[0] = top
            AnimatedVisibility(
                top != null,
                enter = slideInVertically { it } + fadeIn(), exit = slideOutVertically { it } + fadeOut()
            ) {
                val o = holder[0]
                if (o != null) key(o) { OverlayHost(o) }
            }
        }

        val sim = simFor
        if (sim != null) {
            val accs = Dialing.accounts(ctx)
            AlertDialog(
                onDismissRequest = { simFor = null },
                title = { Text("اتصل بأنهي شريحة؟", style = MaterialTheme.typography.titleLarge) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        accs.forEach { h ->
                            BigButton(Dialing.label(ctx, h), icon = Ic.Phone, tonal = true) {
                                simFor = null
                                Dialing.place(ctx, sim, h)
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = { TextButton(onClick = { simFor = null }) { Text("إلغاء", style = MaterialTheme.typography.titleSmall) } },
                shape = RoundedCornerShape(28.dp)
            )
        }
    }
}

@Composable
private fun OverlayHost(o: Overlay) {
    val nav = LocalNavigator.current
    when (o) {
        is Overlay.Dialer -> DialerScreen(o.number)
        is Overlay.ContactDetail -> ContactDetailScreen(o.id)
        is Overlay.Conversation -> ConversationScreen(o.threadId, o.address, o.draft)
        is Overlay.NewMessage -> NewMessageScreen()
        is Overlay.CallPreview -> InCallScreen(onFinish = { nav.pop() }, onAddCall = { }, preview = true, previewRinging = o.ringing)
    }
}
