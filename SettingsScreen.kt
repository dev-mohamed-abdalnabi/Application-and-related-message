package com.mohamed.phone.ui

import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mohamed.phone.Prefs
import com.mohamed.phone.data.Repo
import androidx.compose.foundation.shape.RoundedCornerShape
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

@Composable
private fun Chevron() {
    Icon(
        Ic.Back, null, Modifier.size(18.dp).graphicsLayer { rotationZ = 180f },
        tint = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
fun SettingsScreen(bottom: Dp) {
    val cs = MaterialTheme.colorScheme
    val ctx = LocalContext.current
    val nav = LocalNavigator.current
    val roles = rememberRoleState()
    val requestRole = rememberRoleRequester()
    val theme by Prefs.theme.collectAsState()
    val amoled by Prefs.amoled.collectAsState()
    val accent by Prefs.accent.collectAsState()
    val haptic by Prefs.haptic.collectAsState()
    val tones by Prefs.keypadTones.collectAsState()
    var blockedTick by remember { mutableIntStateOf(0) }
    val blocked = remember(blockedTick, roles.first) { Repo.blocked() }
    var addBlock by remember { mutableStateOf(false) }
    var blockText by remember { mutableStateOf("") }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = 52.dp, bottom = bottom)
        ) {
            Text("الإعدادات", Modifier.padding(start = 4.dp, bottom = 18.dp), style = MaterialTheme.typography.displaySmall)

            SectionTitle("التطبيقات الافتراضية", first = true)
            GroupCard {
                SettingRow(
                    Ic.Phone, "تطبيق الاتصال", if (roles.first) "التطبيق ده هو الافتراضي" else "اضغط عشان تفعّله",
                    iconBg = IosColors.Green, onClick = { if (!roles.first) requestRole(true) }
                ) { IosSwitch(roles.first) { if (!roles.first) requestRole(true) } }
                RowDivider(60.dp)
                SettingRow(
                    Ic.Message, "تطبيق الرسائل", if (roles.second) "التطبيق ده هو الافتراضي" else "اضغط عشان تفعّله",
                    iconBg = IosColors.Green, onClick = { if (!roles.second) requestRole(false) }
                ) { IosSwitch(roles.second) { if (!roles.second) requestRole(false) } }
            }

            SectionTitle("المظهر")
            Switcher(listOf("تلقائي", "فاتح", "داكن"), theme) { Prefs.setTheme(it) }
            Spacer(Modifier.height(12.dp))
            GroupCard {
                SettingRow(
                    Ic.Moon, "أسود كامل (AMOLED)", "للوضع الداكن، بيوفر بطارية",
                    iconBg = IosColors.Purple, onClick = { Prefs.setAmoled(!amoled) }
                ) { IosSwitch(amoled) { Prefs.setAmoled(it) } }
                RowDivider(60.dp)
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
                    Text("اللون المميز", style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (Build.VERSION.SDK_INT >= 31) {
                            Box(
                                Modifier.size(38.dp).clip(CircleShape).background(cs.surfaceContainerHigh)
                                    .border(if (accent < 0) 2.5.dp else 0.dp, cs.onSurface, CircleShape).clickable { Prefs.setAccent(-1) },
                                contentAlignment = Alignment.Center
                            ) { Text("تلقائي", style = MaterialTheme.typography.labelSmall) }
                        }
                        Accents.forEachIndexed { i, a ->
                            Box(
                                Modifier.size(38.dp).clip(CircleShape).background(a.lp)
                                    .border(if (accent == i) 2.5.dp else 0.dp, cs.onSurface, CircleShape).clickable { Prefs.setAccent(i) }
                            )
                        }
                    }
                }
            }

            SectionTitle("الاتصال")
            GroupCard {
                SettingRow(
                    Ic.Keypad, "نغمات الكيباد", "صوت لما تضغط على الأرقام",
                    iconBg = IosColors.Blue, onClick = { Prefs.setTones(!tones) }
                ) { IosSwitch(tones) { Prefs.setTones(it) } }
                RowDivider(60.dp)
                SettingRow(
                    Ic.Phone, "اهتزاز اللمس", "اهتزاز خفيف عند الضغط",
                    iconBg = IosColors.Orange, onClick = { Prefs.setHaptic(!haptic) }
                ) { IosSwitch(haptic) { Prefs.setHaptic(it) } }
            }

            SectionTitle("شاشة المكالمة")
            GroupCard {
                SettingRow(
                    Ic.Phone, "معاينة المكالمة الواردة", "شوف شكل الشاشة لما حد يرن عليك",
                    iconBg = IosColors.Green, onClick = { nav.push(Overlay.CallPreview(true)) }
                ) { Chevron() }
                RowDivider(60.dp)
                SettingRow(
                    Ic.Phone, "معاينة المكالمة الجارية", "شكل الشاشة أثناء المكالمة",
                    iconBg = IosColors.Green, onClick = { nav.push(Overlay.CallPreview(false)) }
                ) { Chevron() }
                RowDivider(60.dp)
                SettingRow(
                    Ic.Info, "إذن الشاشة الكاملة", "لو شاشة الاتصال الوارد مش بتظهر والموبايل مقفول",
                    iconBg = IosColors.Gray, onClick = { openFullScreenSettings(ctx) }
                ) { Chevron() }
            }

            SectionTitle("الأرقام المحظورة")
            if (!roles.first) {
                GroupCard {
                    Text(
                        "الحظر بيشتغل بس لما يبقى التطبيق هو تطبيق الاتصال الافتراضي.",
                        Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant
                    )
                }
            } else {
                GroupCard {
                    blocked.forEach { (id, n) ->
                        SettingRow(Ic.Block, n, "محظور", iconBg = IosColors.Red) {
                            IconBtn(Ic.Close, "إلغاء الحظر") { Repo.unblock(id); blockedTick++ }
                        }
                        RowDivider(60.dp)
                    }
                    Row(
                        Modifier.fillMaxWidth().clickable { addBlock = true }.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Ic.Add, null, Modifier.size(20.dp), tint = cs.primary)
                        Text("حظر رقم", style = MaterialTheme.typography.bodyLarge, color = cs.primary)
                    }
                }
            }

            SectionTitle("الصلاحيات")
            GroupCard {
                SettingRow(
                    Ic.Settings, "صلاحيات التطبيق", "افتح إعدادات النظام",
                    iconBg = IosColors.Gray, onClick = { openAppSettings(ctx) }
                ) { Chevron() }
            }

            Spacer(Modifier.height(28.dp))
            Text(
                "الهاتف · النسخة 1.0.0", Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant
            )
        }
    }

    if (addBlock) {
        AlertDialog(
            onDismissRequest = { addBlock = false },
            title = { Text("حظر رقم", style = MaterialTheme.typography.titleLarge) },
            text = {
                TextField(
                    blockText, { blockText = it }, singleLine = true, colors = fieldColors(),
                    shape = RoundedCornerShape(12.dp), placeholder = { Text("الرقم") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val n = blockText.trim(); addBlock = false; blockText = ""
                    if (n.isNotEmpty()) { if (!Repo.block(n)) toast(ctx, "مش قادر أحظر الرقم"); blockedTick++ }
                }) { Text("حظر", style = MaterialTheme.typography.titleMedium) }
            },
            dismissButton = { TextButton(onClick = { addBlock = false }) { Text("إلغاء", style = MaterialTheme.typography.bodyLarge) } },
            shape = RoundedCornerShape(14.dp)
        )
    }
}


private fun openFullScreenSettings(ctx: android.content.Context) {
    try {
        if (Build.VERSION.SDK_INT >= 34) {
            ctx.startActivity(
                Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, Uri.fromParts("package", ctx.packageName, null))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } else openAppSettings(ctx)
    } catch (e: Exception) { openAppSettings(ctx) }
}
