package com.rasoulin.webrtc_test

import retrofit2.Call
import retrofit2.http.Header
import retrofit2.http.PUT

interface TurnServer {
    @PUT("/_turn/<xyrsys_channel>")
    fun getIceCandidates(@Header("Authorization") authkey: String?): Call<TurnServerPojo?>?
}