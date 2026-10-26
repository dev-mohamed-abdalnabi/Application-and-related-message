package com.mohamed.phone.sms

import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Telephony
import android.telephony.SmsManager

object SmsSender {
    const val ACTION_SENT = "com.mohamed.phone.SMS_SENT"

    @Suppress("DEPRECATION")
    private fun manager(ctx: Context): SmsManager =
        if (Build.VERSION.SDK_INT >= 31) ctx.getSystemService(SmsManager::class.java) else SmsManager.getDefault()

    fun isDefault(ctx: Context): Boolean = try {
        Telephony.Sms.getDefaultSmsPackage(ctx) == ctx.packageName
    } catch (e: Exception) { false }

    fun send(ctx: Context, address: String, body: String): Boolean {
        val text = body.trim()
        val to = address.trim()
        if (text.isEmpty() || to.isEmpty()) return false

        var uri: Uri? = null
        if (isDefault(ctx)) {
            val v = ContentValues().apply {
                put("address", to); put("body", text); put("date", System.currentTimeMillis())
                put("read", 1); put("seen", 1); put("type", Telephony.Sms.MESSAGE_TYPE_OUTBOX)
            }
            uri = try { ctx.contentResolver.insert(Telephony.Sms.CONTENT_URI, v) } catch (e: Exception) { null }
        }

        return try {
            val sm = manager(ctx)
            val parts = sm.divideMessage(text)
            if (uri != null) {
                val sent = ArrayList<PendingIntent>()
                for (i in parts.indices) {
                    val it = Intent(ctx, SmsSentReceiver::class.java)
                        .setAction("$ACTION_SENT.$i").setData(uri).putExtra("last", i == parts.size - 1)
                    sent.add(PendingIntent.getBroadcast(ctx, i, it, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
                }
                if (parts.size == 1) sm.sendTextMessage(to, null, text, sent[0], null)
                else sm.sendMultipartTextMessage(to, null, parts, sent, null)
            } else {
                if (parts.size == 1) sm.sendTextMessage(to, null, text, null, null)
                else sm.sendMultipartTextMessage(to, null, parts, null, null)
            }
            true
        } catch (e: Exception) {
            uri?.let {
                try {
                    ctx.contentResolver.update(it, ContentValues().apply { put("type", Telephony.Sms.MESSAGE_TYPE_FAILED) }, null, null)
                } catch (e2: Exception) { }
            }
            false
        }
    }
}
