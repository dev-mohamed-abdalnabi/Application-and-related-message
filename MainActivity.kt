package com.mohamed.phone

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.mohamed.phone.ui.AppTheme
import com.mohamed.phone.ui.Nav
import com.mohamed.phone.ui.Overlay
import com.mohamed.phone.ui.Root

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) handle(intent)
        setContent { AppTheme { Root() } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handle(intent)
    }

    override fun onResume() {
        super.onResume()
        Nav.resume.value = Nav.resume.value + 1
    }

    private fun handle(i: Intent?) {
        if (i == null) return
        val data: Uri? = i.data
        when (i.action) {
            Intent.ACTION_DIAL, Intent.ACTION_VIEW -> {
                if (data != null && data.scheme == "tel") {
                    Nav.pending.value = Overlay.Dialer(Uri.decode(data.schemeSpecificPart ?: ""))
                } else if (i.action == Intent.ACTION_DIAL) {
                    Nav.pending.value = Overlay.Dialer("")
                }
            }
            Intent.ACTION_CALL_BUTTON -> Nav.pending.value = Overlay.Dialer("")
            Intent.ACTION_SENDTO, Intent.ACTION_SEND -> {
                val body = i.getStringExtra("sms_body") ?: i.getStringExtra(Intent.EXTRA_TEXT) ?: ""
                if (data != null && (data.scheme == "sms" || data.scheme == "smsto" || data.scheme == "mms" || data.scheme == "mmsto")) {
                    val addr = Uri.decode((data.schemeSpecificPart ?: "").substringBefore('?'))
                    Nav.pending.value = Overlay.Conversation(-1L, addr, body)
                } else {
                    Nav.pending.value = Overlay.NewMessage
                }
            }
            Notifs.ACTION_OPEN_THREAD -> {
                val addr = i.getStringExtra("address")
                if (!addr.isNullOrEmpty()) Nav.pending.value = Overlay.Conversation(-1L, addr)
            }
        }
        if (i.getBooleanExtra("open_dialer", false)) Nav.pending.value = Overlay.Dialer("")
    }
}
