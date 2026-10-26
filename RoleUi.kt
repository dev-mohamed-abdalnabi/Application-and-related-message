package com.mohamed.phone.ui

import androidx.compose.foundation.layout.Box
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.mohamed.phone.telecom.Roles

/** (تطبيق اتصال افتراضي؟ ، تطبيق رسائل افتراضي؟) */
@Composable
fun rememberRoleState(): Pair<Boolean, Boolean> {
    val ctx = LocalContext.current
    val t by Nav.resume.collectAsState()
    return remember(t) { Roles.isDefaultDialer(ctx) to Roles.isDefaultSms(ctx) }
}

/** بترجّع دالة: true = طلب الاتصال الافتراضي، false = طلب الرسائل الافتراضي */
@Composable
fun rememberRoleRequester(): (Boolean) -> Unit {
    val ctx = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        Nav.resume.value = Nav.resume.value + 1
    }
    return { dialer ->
        try {
            launcher.launch(if (dialer) Roles.dialerIntent(ctx) else Roles.smsIntent(ctx))
        } catch (e: Exception) {
            toast(ctx, "مش قادر أفتح الإعداد ده على جهازك")
        }
    }
}

fun openAppSettings(ctx: android.content.Context) {
    try {
        ctx.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", ctx.packageName, null))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    } catch (e: Exception) { }
}

@Composable
fun RoleBanner(icon: ImageVector, title: String, body: String, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(cs.surfaceContainerLow)
            .clickable(onClick = onClick).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(Modifier.size(36.dp).clip(RoundedCornerShape(9.dp)).background(cs.primary), contentAlignment = Alignment.Center) {
            Icon(icon, null, Modifier.size(20.dp), tint = cs.onPrimary)
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(body, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
        }
    }
}
