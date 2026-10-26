package com.mohamed.phone.telecom

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mohamed.phone.Notifs

class CallActionReceiver : BroadcastReceiver() {
    companion object {
        const val ANSWER = "com.mohamed.phone.ANSWER"
        const val DECLINE = "com.mohamed.phone.DECLINE"
        const val HANGUP = "com.mohamed.phone.HANGUP"
        const val CALLBACK = "com.mohamed.phone.CALLBACK"
    }

    override fun onReceive(ctx: Context, intent: Intent) {
        when (intent.action) {
            ANSWER -> { CallHub.answerRinging(); CallHub.launchUi() }
            DECLINE -> CallHub.rejectRinging()
            HANGUP -> CallHub.hangupAll()
            CALLBACK -> {
                val n = intent.getStringExtra("number") ?: return
                Notifs.cancel(ctx, 2000 + (n.hashCode() and 0xfff))
                Dialing.place(ctx, n, Dialing.defaultAccount(ctx))
            }
        }
    }
}
