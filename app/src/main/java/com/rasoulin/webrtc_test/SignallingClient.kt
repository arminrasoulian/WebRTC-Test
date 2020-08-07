package com.rasoulin.webrtc_test

import android.util.Log
import com.google.gson.internal.LinkedTreeMap
import com.microsoft.signalr.HubConnection
import com.microsoft.signalr.HubConnectionBuilder
import com.microsoft.signalr.HubConnectionState
import io.reactivex.Single
import org.json.JSONObject
import org.webrtc.MediaStream
import java.util.*
import kotlin.concurrent.thread

data class ConnectionInfo(
    val item1: String, // connectionId
    val item2: String, // userFullName
    val item3: String, // userPicture
    val item4: String, // listTitle
    val item5: Boolean // listIsVisible
)

data class ConnectedPhysicianPatientInformation(
    val connectionId: String,
    val userName: String,
    val userFullName: String,
    val userPicture: String,
    val joinedTime: Date,
    val visitId: UUID?,
    val physicianId: UUID?,
    val visitPhysicianId: UUID?
)

object SignallingClient {
    lateinit var remoteStream: MediaStream
    var destinationConnectionId: String? = null
    private const val baseServerUrl: String = "https://iranmedical.equipment"
    var localConnectionId: String? = null
    private var visitId: UUID? = UUID.fromString("81a8a2b0-8810-dd95-a6c5-90f4fc881b85")
    private lateinit var hubConnection: HubConnection
    private var callback: SignalingInterface? = null

    private const val token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJmdWxsX25hbWUiOiLYrdiz24zZhiDZhdmE2KfbjNuMIiwicm9sZV9pZHMiOiJhYjEyNDczYy02NWQ3LTRkOGYtYWQ5MC0wMGNkMGY5ZWI5ZGYiLCJpc19hZG1pbiI6InRydWUiLCJuYW1laWQiOiI3ODk0Y2I1MC1kZjU3LTRlOTgtYjM0Mi1hMWZkZWI5MGI3ZjUiLCJ1bmlxdWVfbmFtZSI6Ik1vbGxhZWUiLCJyZWZyZXNoX3Rva2VuX2lkbGVfdGltZV9pbl9taW51dGUiOiIxMjAiLCJyb2xlIjoiUGh5c2ljaWFuIiwiZXhwIjoxNTk2Nzg3MTg2LCJpc3MiOiI5WEdWZWRTdDZlTkVQd1l6cTdnUTVWaEgyUlA3a256NmE2Q2U5N3g1V1Zrbmc5TCIsImF1ZCI6ImdLNkZwdXk3NWQ2M2hFOU40aFpLZWp0V3RuNHZTTEZLOEJnbVVFaFF1ZHVCdWVaIn0.8T5mLlEVtjee6OMEkZ6D3tDTFClD3flibK253V12CKc";

    fun init(signalingInterface: SignalingInterface?) {
        callback = signalingInterface

        try {
            hubConnection = HubConnectionBuilder
                .create("$baseServerUrl/PhysicianPatientHub?VisitId=$visitId")
                .withAccessTokenProvider(Single.defer {
                    return@defer Single.just(token)
                })
                .build()

            Log.d("SignallingClient", "init() called")

            hubConnection.on(
                "Error",
                { message ->
                    // we want to show this error on UI
                    Log.d(
                        "SignallingClient",
                        "signalR Error, $message"
                    )
                    callback?.error(message)
                },
                String::class.java
            )

            hubConnection.on(
                "ReceiveMessage",
                { user, message ->
                    Log.d(
                        "SignallingClient",
                        "signalR ReceiveMessage, $message **** from $user"
                    )
                    callback?.receiveMessage(user, message)
                },
                String::class.java, String::class.java
            )

            hubConnection.on(
                "ReceiveCall",
                { connectionId, callerFullName, callerPicture ->
                    Log.d(
                        "SignallingClient",
                        "connectionId: $connectionId, callerFullName: $callerFullName, callerPicture: $callerPicture"
                    )
                    callback?.receiveCall(connectionId, callerFullName, callerPicture)
                },
                String::class.java,
                String::class.java,
                String::class.java
            )

            hubConnection.on("StopCall") {
                Log.d("SignallingClient", "StopCall")

            }

            hubConnection.on("LineIsBusy") {
                Log.d("SignallingClient", "LineIsBusy")

            }

            hubConnection.on(
                "SetConnectionId",
                { connectionInfo ->
                    Log.d(
                        "SignallingClient",
                        "SetConnectionId, $connectionInfo")
                    callback?.setConnectionId(connectionInfo)
                },
                ConnectionInfo::class.java
            )

            hubConnection.on(
                "ChangeRoomMembers",
                { signalRConnections ->
                    Log.d(
                        "SignallingClient",
                        "ChangeRoomMembers, $signalRConnections")

                    callback?.changeRoomMembers(signalRConnections)
                },
                Array<ConnectedPhysicianPatientInformation>::class.java
            )
            hubConnection.on("TestLog",
                { message ->
                    Log.d(
                        "SignallingClient",
                        "$message")


                },
                Any::class.java
            )

            hubConnection.on(
                "ReceiveVideoCallRequest",
                { message, connectionId ->
                    Log.d(
                        "SignallingClient",
                        "ReceiveVideoCallRequest")
                    if(message !is LinkedTreeMap<*, *>)
                    {
                        Log.d("MSGIsNOTLinkedTreeMap", message.toString())
                        return@on
                    }

                    when(message["type"]) {
                        "offer" -> callback?.offerReceived(message)
                        "answer" -> callback?.answerReceived(message)
                        "candidate" -> callback?.candidateReceived(message)
                    }
                    callback?.receiveVideoCallRequest(message, connectionId)
                },
                Any::class.java,
                String::class.java
            )

            hubConnection.onClosed {
                Log.d(
                    "hubConnectionClosed",
                    it.message!!
                )
                it.printStackTrace()
            }

            hubConnection.start().blockingAwait()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    fun sendVideoCallRequest(destinationConnectionId: String, request: LinkedTreeMap<*, *>) {
        val state = hubConnection.connectionState
        Log.d("SendVideoCallReqCalled", request.toString())
        //val jsonString = request.toString()

        hubConnection.send("SendVideoCallRequest", destinationConnectionId, request)
    }

    fun call(destinationConnectionId: String) {
        hubConnection.send("Call", destinationConnectionId)
    }

   /* private fun emitInitStatement(message: String) {
        Log.d(
            "SignallingClient",
            "emitInitStatement() called with: event = [create or join], message = [$message]"
        )
        // Todo: send message through signalR

        hubConnection.send("create or join", message)
        //socket.emit("create or join", message)
    }



      fun emitMessage(message: String) {
        Log.d(
            "SignallingClient",
            "emitMessage() called with: message = [$message]"
        )
        // Todo: send message through signalR
        //socket.emit("message", message)
    }

    fun emitMessage(message: SessionDescription) {
        try {
            Log.d(
                "SignallingClient",
                "emitMessage() called with: message = [$message]"
            )
            val obj = JSONObject()
            obj.put("type", message.type.canonicalForm())
            obj.put("sdp", message.description)
            Log.d("emitMessage", obj.toString())
            // Todo: send message through signalR
            //socket.emit("message", obj)
            Log.d("vivek1794", obj.toString())
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    fun emitIceCandidate(iceCandidate: IceCandidate) {
        try {
            val jsonObject = JSONObject()
            jsonObject.put("type", "candidate")
            jsonObject.put("label", iceCandidate.sdpMLineIndex)
            jsonObject.put("id", iceCandidate.sdpMid)
            jsonObject.put("candidate", iceCandidate.sdp)
            // Todo: send message through signalR
            //socket.emit("message", `object`)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun close() {
        // Todo: disconnect signalR
//        socket.emit("bye", roomName)
//        socket.disconnect()
//        socket.close()
    } */

    interface SignalingInterface {
        fun error(message: String)
        fun receiveMessage(user: String, message: String)
        fun receiveCall(connectionId: String, callerFullName: String, callerPicture: String)
        fun setConnectionId(connectionInfo: ConnectionInfo)
        fun changeRoomMembers(signalRConnections: Array<ConnectedPhysicianPatientInformation>)
        fun receiveVideoCallRequest(message: Any, connectionId: String)
        fun offerReceived(message: LinkedTreeMap<*, *>)
        fun answerReceived(message: LinkedTreeMap<*, *>)
        fun candidateReceived(message: LinkedTreeMap<*, *>)
        fun onTryToStart()
    }
}