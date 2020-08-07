package com.rasoulin.webrtc_test

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object Utils {
    private var retrofitInstance: Retrofit? = null
    private const val API_ENDPOINT = "https://global.xirsys.net"

    val turnServer: TurnServer
        get() {
            if (retrofitInstance == null) {
                retrofitInstance = Retrofit.Builder()
                    .baseUrl(API_ENDPOINT)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
            }
            return retrofitInstance!!.create(TurnServer::class.java)
        }
}