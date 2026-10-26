package com.mohamed.phone.telecom

import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.DisconnectCause
import android.telecom.InCallService
import android.telecom.VideoProfile
import com.mohamed.phone.InCallActivity
import com.mohamed.phone.Notifs
import com.mohamed.phone.data.Repo
import kotlinx.coroutines.flow.MutableStateFlow

@Suppress("DEPRECATION")
object CallHub {
    class Info(
        val call: Call, val state: Int, val number: String, val name: String?, val photo: String?,
        val connectMs: Long, val canHold: Boolean, val canMerge: Boolean, val canSwap: Boolean,
        val isConference: Boolean
    ) {
        val label: String get() = name ?: if (number.isEmpty()) "رقم خاص" else number
    }

    val calls = MutableStateFlow<List<Info>>(emptyList())
    val audio = MutableStateFlow<CallAudioState?>(null)
    var service: InCallService? = null

    private val live = ArrayList<Call>()
    private var app: Context? = null
    private var wake: PowerManager.WakeLock? = null

    private val cb = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) { refresh() }
        override fun onDetailsChanged(call: Call, details: Call.Details) { refresh() }
        override fun onCallDestroyed(call: Call) { refresh() }
    }

    private fun info(c: Call): Info {
        val d = c.details
        val number = d.handle?.schemeSpecificPart ?: ""
        val contact = Repo.lookup(number)
        val name = contact?.name ?: Repo.nameFor(number) ?: d.callerDisplayName?.takeIf { it.isNotBlank() }
        return Info(
            c, c.state, number, name, contact?.photo, d.connectTimeMillis,
            d.can(Call.Details.CAPABILITY_HOLD), d.can(Call.Details.CAPABILITY_MERGE_CONFERENCE),
            d.can(Call.Details.CAPABILITY_SWAP_CONFERENCE), d.hasProperty(Call.Details.PROPERTY_CONFERENCE)
        )
    }

    fun add(ctx: Context, call: Call) {
        app = ctx.applicationContext
        if (!live.contains(call)) live.add(call)
        call.registerCallback(cb)
        refresh()
        launchUi()
    }

    fun remove(ctx: Context, call: Call) {
        app = ctx.applicationContext
        try {
            if (call.details.disconnectCause?.code == DisconnectCause.MISSED) {
                val n = call.details.handle?.schemeSpecificPart ?: ""
                Notifs.missed(ctx, Repo.nameFor(n) ?: if (n.isEmpty()) "رقم خاص" else n, n)
            }
        } catch (e: Exception) { }
        try { call.unregisterCallback(cb) } catch (e: Exception) { }
        live.remove(call)
        refresh()
    }

    fun launchUi() {
        val a = app ?: return
        try {
            a.startActivity(
                Intent(a, InCallActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            )
        } catch (e: Exception) { }
    }

    fun refresh() {
        val list = live.map { info(it) }
        calls.value = list
        val a = app ?: return
        val ring = list.firstOrNull { it.state == Call.STATE_RINGING }
        val cur = list.firstOrNull()
        when {
            ring != null -> Notifs.incoming(a, ring.label, ring.number)
            cur == null -> Notifs.cancelCalls(a)
            else -> {
                Notifs.cancelIncoming(a)
                Notifs.ongoing(a, cur.label, stateText(cur.state), cur.connectMs)
            }
        }
        updateProximity(list)
    }

    fun stateText(state: Int): String = when (state) {
        Call.STATE_RINGING -> "مكالمة واردة"
        Call.STATE_DIALING, Call.STATE_CONNECTING, Call.STATE_NEW -> "بيتصل..."
        Call.STATE_HOLDING -> "معلّقة"
        Call.STATE_DISCONNECTING, Call.STATE_DISCONNECTED -> "المكالمة انتهت"
        Call.STATE_SELECT_PHONE_ACCOUNT -> "اختار الشريحة"
        else -> "مكالمة جارية"
    }

    fun updateProximity(list: List<Info> = calls.value) {
        val a = app ?: return
        val au = audio.value
        val active = list.any { it.state == Call.STATE_ACTIVE || it.state == Call.STATE_DIALING || it.state == Call.STATE_CONNECTING }
        val want = active && (au == null || (au.route != CallAudioState.ROUTE_SPEAKER && au.route != CallAudioState.ROUTE_BLUETOOTH))
        try {
            val pm = a.getSystemService(PowerManager::class.java)
            if (want) {
                if (wake == null && pm.isWakeLockLevelSupported(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK)) {
                    val w = pm.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, "phone:proximity")
                    w.acquire(4 * 60 * 60 * 1000L)
                    wake = w
                }
            } else {
                wake?.let { if (it.isHeld) it.release() }
                wake = null
            }
        } catch (e: Exception) { }
    }

    // -------------------------------------------------------------- controls
    fun answer(call: Call) { call.answer(VideoProfile.STATE_AUDIO_ONLY) }
    fun reject(call: Call, msg: String? = null) { call.reject(msg != null, msg) }
    fun hangup(call: Call) { call.disconnect() }
    fun hold(call: Call, on: Boolean) { if (on) call.hold() else call.unhold() }
    fun setMuted(m: Boolean) { service?.setMuted(m) }
    fun setRoute(r: Int) { service?.setAudioRoute(r) }

    fun answerRinging() { calls.value.firstOrNull { it.state == Call.STATE_RINGING }?.let { answer(it.call) } }
    fun rejectRinging() { calls.value.firstOrNull { it.state == Call.STATE_RINGING }?.let { reject(it.call) } }
    fun hangupAll() { for (i in calls.value) i.call.disconnect() }
}
