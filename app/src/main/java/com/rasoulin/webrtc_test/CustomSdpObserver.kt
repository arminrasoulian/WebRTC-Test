package com.rasoulin.webrtc_test

import android.util.Log
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription


internal open class CustomSdpObserver(logTag: String) : SdpObserver {
    private var tag: String? = this.javaClass.canonicalName
    override fun onCreateSuccess(sessionDescription: SessionDescription) {
        Log.d(
            tag,
            "onCreateSuccess() called with: sessionDescription = [$sessionDescription]"
        )
    }

    override fun onSetSuccess() {
        Log.d(tag, "onSetSuccess() called")
    }

    override fun onCreateFailure(s: String) {
        Log.d(tag, "onCreateFailure() called with: s = [$s]")
    }

    override fun onSetFailure(s: String) {
        Log.d(tag, "onSetFailure() called with: s = [$s]")
    }

    init {
        tag = "$tag $logTag"
    }
}
