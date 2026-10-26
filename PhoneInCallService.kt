package com.mohamed.phone.telecom

import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService

@Suppress("DEPRECATION")
class PhoneInCallService : InCallService() {
    override fun onCreate() {
        super.onCreate()
        CallHub.service = this
    }

    override fun onDestroy() {
        if (CallHub.service === this) CallHub.service = null
        super.onDestroy()
    }

    override fun onCallAdded(call: Call) {
        CallHub.service = this
        CallHub.audio.value = callAudioState
        CallHub.add(this, call)
    }

    override fun onCallRemoved(call: Call) {
        CallHub.remove(this, call)
    }

    override fun onCallAudioStateChanged(audioState: CallAudioState?) {
        CallHub.audio.value = audioState
        CallHub.updateProximity()
    }
}
