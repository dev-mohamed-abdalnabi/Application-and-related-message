package com.mohamed.phone.sms

import android.app.Activity
import android.app.Service
import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.provider.Telephony
import androidx.core.app.RemoteInput
import com.mohamed.phone.Notifs
import com.mohamed.phone.data.Repo

class SmsDeliverReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val msgs = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (msgs == null || msgs.isEmpty()) return
        val addr = msgs[0].displayOriginatingAddress ?: return
        val body = msgs.joinToString("") { it.displayMessageBody ?: "" }
        val v = ContentValues().apply {
            put("address", addr); put("body", body)
            put("date", System.currentTimeMillis()); put("date_sent", msgs[0].timestampMillis)
            put("read", 0); put("seen", 0); put("type", Telephony.Sms.MESSAGE_TYPE_INBOX)
        }
        try { ctx.contentResolver.insert(Telephony.Sms.Inbox.CONTENT_URI, v) } catch (e: Exception) { }
        if (Repo.isBlocked(addr)) return
        Notifs.message(ctx, addr, body)
    }
}

/** استقبال MMS: مطلوب عشان التطبيق يتأهل يبقى افتراضي، ومش بنعرض الوسائط حاليًا */
class MmsReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) { }
}

class SmsSentReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val uri = intent.data ?: return
        val last = intent.getBooleanExtra("last", false)
        try {
            if (resultCode != Activity.RESULT_OK) {
                val v = ContentValues().apply { put("type", Telephony.Sms.MESSAGE_TYPE_FAILED) }
                ctx.contentResolver.update(uri, v, null, null)
            } else if (last) {
                val v = ContentValues().apply { put("type", Telephony.Sms.MESSAGE_TYPE_SENT) }
                ctx.contentResolver.update(uri, v, "type=?", arrayOf(Telephony.Sms.MESSAGE_TYPE_OUTBOX.toString()))
            }
        } catch (e: Exception) { }
    }
}

class ReplyReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val addr = intent.getStringExtra("address") ?: return
        val id = intent.getIntExtra("nid", 0)
        val text = RemoteInput.getResultsFromIntent(intent)?.getCharSequence(Notifs.KEY_REPLY)?.toString() ?: return
        SmsSender.send(ctx, addr, text)
        Notifs.cancel(ctx, id)
    }
}

class RespondViaMessageService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            val addr = intent?.data?.schemeSpecificPart?.substringBefore('?')
            val text = intent?.getStringExtra(Intent.EXTRA_TEXT)
            if (!addr.isNullOrBlank() && !text.isNullOrBlank()) SmsSender.send(this, addr, text)
        } catch (e: Exception) { }
        stopSelf(startId)
        return START_NOT_STICKY
    }
}
