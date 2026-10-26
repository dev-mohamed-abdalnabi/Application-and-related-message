package com.mohamed.phone.ui

import android.Manifest
import android.os.Build
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.MutableStateFlow

sealed interface Overlay {
    data class Dialer(val number: String = "") : Overlay
    data class ContactDetail(val id: Long) : Overlay
    data class Conversation(val threadId: Long, val address: String, val draft: String = "") : Overlay
    data object NewMessage : Overlay
    data class CallPreview(val ringing: Boolean) : Overlay
}

object Nav {
    /** شاشة مطلوب فتحها من برّه (intent / إشعار) */
    val pending = MutableStateFlow<Overlay?>(null)
    /** بيزيد كل ما التطبيق يرجع للواجهة (عشان نعيد فحص الصلاحيات والأدوار) */
    val resume = MutableStateFlow(0)
}

class Navigator(val push: (Overlay) -> Unit, val pop: () -> Unit)

val LocalNavigator = staticCompositionLocalOf<Navigator> { error("no navigator") }
val LocalCaller = staticCompositionLocalOf<(String) -> Unit> { { } }

object Perms {
    val essential = listOf(
        Manifest.permission.READ_CONTACTS, Manifest.permission.READ_CALL_LOG, Manifest.permission.CALL_PHONE,
        Manifest.permission.READ_PHONE_STATE, Manifest.permission.READ_SMS, Manifest.permission.SEND_SMS,
        Manifest.permission.RECEIVE_SMS
    )

    val all: List<String> = buildList {
        addAll(essential)
        add(Manifest.permission.WRITE_CONTACTS)
        add(Manifest.permission.WRITE_CALL_LOG)
        add(Manifest.permission.READ_PHONE_NUMBERS)
        add(Manifest.permission.ANSWER_PHONE_CALLS)
        add(Manifest.permission.RECEIVE_MMS)
        add(Manifest.permission.RECEIVE_WAP_PUSH)
        if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
    }
}
