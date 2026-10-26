package com.mohamed.phone

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import com.mohamed.phone.data.Repo
import com.mohamed.phone.sms.ReplyReceiver
import com.mohamed.phone.telecom.CallActionReceiver

object Notifs {
    const val CH_INCOMING = "incoming_calls"
    const val CH_ONGOING = "ongoing_calls"
    const val CH_MISSED = "missed_calls"
    const val CH_MSG = "messages"
    const val ID_INCOMING = 1001
    const val ID_ONGOING = 1002
    const val KEY_REPLY = "reply_text"
    const val ACTION_OPEN_THREAD = "com.mohamed.phone.OPEN_THREAD"

    fun createChannels(ctx: Context) {
        if (Build.VERSION.SDK_INT < 26) return
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CH_INCOMING, "المكالمات الواردة", NotificationManager.IMPORTANCE_HIGH).apply {
            setSound(null, null); enableVibration(false)
        })
        nm.createNotificationChannel(NotificationChannel(CH_ONGOING, "المكالمة الجارية", NotificationManager.IMPORTANCE_LOW))
        nm.createNotificationChannel(NotificationChannel(CH_MISSED, "المكالمات الفائتة", NotificationManager.IMPORTANCE_HIGH))
        nm.createNotificationChannel(NotificationChannel(CH_MSG, "الرسائل", NotificationManager.IMPORTANCE_HIGH))
    }

    private fun immutable() = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    private fun mutable() = (if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0) or PendingIntent.FLAG_UPDATE_CURRENT

    private fun callUi(ctx: Context): PendingIntent =
        PendingIntent.getActivity(
            ctx, 10,
            Intent(ctx, InCallActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            immutable()
        )

    private fun action(ctx: Context, act: String, code: Int, extra: String? = null): PendingIntent =
        PendingIntent.getBroadcast(
            ctx, code, Intent(ctx, CallActionReceiver::class.java).setAction(act).putExtra("number", extra), immutable()
        )

    @SuppressLint("MissingPermission")
    private fun post(ctx: Context, id: Int, n: android.app.Notification) {
        try { NotificationManagerCompat.from(ctx).notify(id, n) } catch (e: Exception) { }
    }

    fun incoming(ctx: Context, name: String, number: String) {
        val ui = callUi(ctx)
        val n = NotificationCompat.Builder(ctx, CH_INCOMING)
            .setSmallIcon(R.drawable.ic_stat_phone)
            .setContentTitle(name)
            .setContentText("مكالمة واردة" + if (number.isNotEmpty() && number != name) " · $number" else "")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(ui)
            .setFullScreenIntent(ui, true)
            .addAction(0, "رفض", action(ctx, CallActionReceiver.DECLINE, 11))
            .addAction(0, "رد", action(ctx, CallActionReceiver.ANSWER, 12))
            .build()
        post(ctx, ID_INCOMING, n)
    }

    fun ongoing(ctx: Context, title: String, text: String, connectMs: Long) {
        val b = NotificationCompat.Builder(ctx, CH_ONGOING)
            .setSmallIcon(R.drawable.ic_stat_phone)
            .setContentTitle(title)
            .setContentText(text)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(callUi(ctx))
            .addAction(0, "إنهاء", action(ctx, CallActionReceiver.HANGUP, 13))
        if (connectMs > 0) b.setUsesChronometer(true).setWhen(connectMs)
        post(ctx, ID_ONGOING, b.build())
    }

    fun cancelIncoming(ctx: Context) { try { NotificationManagerCompat.from(ctx).cancel(ID_INCOMING) } catch (e: Exception) { } }

    fun cancelCalls(ctx: Context) {
        try {
            val nm = NotificationManagerCompat.from(ctx)
            nm.cancel(ID_INCOMING); nm.cancel(ID_ONGOING)
        } catch (e: Exception) { }
    }

    fun missed(ctx: Context, name: String, number: String) {
        val open = PendingIntent.getActivity(
            ctx, 14, Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP), immutable()
        )
        val b = NotificationCompat.Builder(ctx, CH_MISSED)
            .setSmallIcon(R.drawable.ic_stat_phone)
            .setContentTitle("مكالمة فائتة")
            .setContentText(name)
            .setCategory(NotificationCompat.CATEGORY_MISSED_CALL)
            .setAutoCancel(true)
            .setContentIntent(open)
        if (number.isNotEmpty()) b.addAction(0, "اتصال", action(ctx, CallActionReceiver.CALLBACK, 15 + (number.hashCode() and 0xff), number))
        post(ctx, 2000 + (number.hashCode() and 0xfff), b.build())
    }

    fun message(ctx: Context, address: String, body: String) {
        val title = Repo.nameFor(address) ?: address
        val id = 3000 + (Repo.key(address).hashCode() and 0xfffff)
        val open = PendingIntent.getActivity(
            ctx, id,
            Intent(ctx, MainActivity::class.java).setAction(ACTION_OPEN_THREAD).putExtra("address", address)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            immutable()
        )
        val ri = RemoteInput.Builder(KEY_REPLY).setLabel("رد").build()
        val replyPi = PendingIntent.getBroadcast(
            ctx, id, Intent(ctx, ReplyReceiver::class.java).putExtra("address", address).putExtra("nid", id), mutable()
        )
        val reply = NotificationCompat.Action.Builder(0, "رد", replyPi).addRemoteInput(ri).build()
        val n = NotificationCompat.Builder(ctx, CH_MSG)
            .setSmallIcon(R.drawable.ic_stat_phone)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(open)
            .addAction(reply)
            .build()
        post(ctx, id, n)
    }

    fun cancel(ctx: Context, id: Int) { try { NotificationManagerCompat.from(ctx).cancel(id) } catch (e: Exception) { } }
}
