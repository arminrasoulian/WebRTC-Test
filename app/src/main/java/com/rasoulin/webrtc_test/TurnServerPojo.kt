package com.rasoulin.webrtc_test

import org.webrtc.PeerConnection
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class TurnServerPojo {

    @SerializedName("s")
    @Expose
    var s: Int? = null

    @SerializedName("p")
    @Expose
    var p: String? = null

    @SerializedName("e")
    @Expose
    var e: Any? = null

    @SerializedName("v")
    @Expose
    var iceServerList: IceServerList? = null

    class IceServerList {

        @SerializedName("iceServers")
        @Expose
        var iceServers: List<PeerConnection.IceServer>? = null
    }
}