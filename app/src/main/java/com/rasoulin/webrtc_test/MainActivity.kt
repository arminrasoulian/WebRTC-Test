package com.rasoulin.webrtc_test

import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.internal.LinkedTreeMap
import org.webrtc.*
import org.webrtc.PeerConnection
import org.webrtc.PeerConnection.RTCConfiguration
import org.webrtc.PeerConnectionFactory.InitializationOptions
import kotlin.math.roundToInt


class MainActivity : AppCompatActivity(), SignallingClient.SignalingInterface,
    View.OnClickListener {
    private var visitConnections: Array<ConnectedPhysicianPatientInformation>? = null

    var peerConnectionFactory: PeerConnectionFactory? = null
    var audioConstraints: MediaConstraints? = null
    var videoConstraints: MediaConstraints? = null

    // This variable will be used when we want to add acceptCall button
    var sdpConstraints: MediaConstraints? = null
    var videoSource: VideoSource? = null
    var localVideoTrack: VideoTrack? = null
    var audioSource: AudioSource? = null
    var localAudioTrack: AudioTrack? = null

    var localVideoView: SurfaceViewRenderer? = null
    var remoteVideoView: SurfaceViewRenderer? = null

    var hangup: Button? = null
    var localPeer: PeerConnection? = null
    var rootEglBase: EglBase? = null

    var gotUserMedia: Boolean = false

    var destinationConnection: ConnectedPhysicianPatientInformation? = null

    private val peerIceServers: List<PeerConnection.IceServer> =
        listOf(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302")
                .createIceServer(),
            PeerConnection.IceServer.builder("turn:13.250.13.83:3478?transport=udp")
                .setUsername("YzYNCouZM1mhqhmseWk6")
                .setPassword("YzYNCouZM1mhqhmseWk6")
                .createIceServer(),
            PeerConnection.IceServer.builder("turn:numb.viagenie.ca")
                .setUsername("muazkh")
                .setPassword("webrtc@live.com")
                .createIceServer(),
            PeerConnection.IceServer.builder("turn:192.158.29.39:3478?transport=udp")
                .setUsername("JZEOEt2V3Qb0y27GRntt2u2PAYA=")
                .setPassword("28224511:1379330808")
                .createIceServer(),
            PeerConnection.IceServer.builder("turn:192.158.29.39:3478?transport=tcp")
                .setUsername("JZEOEt2V3Qb0y27GRntt2u2PAYA=")
                .setPassword("28224511:1379330808")
                .createIceServer(),
            PeerConnection.IceServer.builder("turn:turn.bistri.com:80")
                .setUsername("homeo")
                .setPassword("homeo")
                .createIceServer(),
            PeerConnection.IceServer.builder("turn:turn.anyfirewall.com:443?transport=tcp")
                .setUsername("webrtc")
                .setPassword("webrtc")
                .createIceServer(),
            PeerConnection.IceServer.builder("turn:13.250.13.83:3478?transport=udp")
                .setUsername("YzYNCouZM1mhqhmseWk6")
                .setPassword("YzYNCouZM1mhqhmseWk6")
                .createIceServer()
        )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initViews()
        initVideos()
        SignallingClient.init(this)
        start()
    }

    private fun start() {
        //Initialize PeerConnectionFactory globals.
        val initializationOptions: InitializationOptions =
            InitializationOptions.builder(this)
                .createInitializationOptions()
        PeerConnectionFactory.initialize(initializationOptions)

        //Create a new PeerConnectionFactory instance - using Hardware encoder and decoder.
        val options = PeerConnectionFactory.Options()
        val defaultVideoEncoderFactory = DefaultVideoEncoderFactory(
            rootEglBase!!.eglBaseContext,  /* enableIntelVp8Encoder */
            true,  /* enableH264HighProfile */
            true
        )
        val defaultVideoDecoderFactory =
            DefaultVideoDecoderFactory(rootEglBase!!.eglBaseContext)

        peerConnectionFactory =
            PeerConnectionFactory
                .builder()
                .setOptions(options)
                .setVideoEncoderFactory(defaultVideoEncoderFactory)
                .setVideoDecoderFactory(defaultVideoDecoderFactory)
                .createPeerConnectionFactory()

        //Now create a VideoCapturer instance.
        val videoCapturerAndroid: VideoCapturer? = createCameraCapturer(Camera1Enumerator(false))


        //Create MediaConstraints - Will be useful for specifying video and audio constraints.
        audioConstraints = MediaConstraints()
        videoConstraints = MediaConstraints()

        //Create a VideoSource instance

        //Create a VideoSource instance
        if (videoCapturerAndroid != null) {
            val surfaceTextureHelper =
                SurfaceTextureHelper.create("CaptureThread", rootEglBase!!.eglBaseContext)
            videoSource =
                peerConnectionFactory!!.createVideoSource(videoCapturerAndroid.isScreencast)

            videoCapturerAndroid.initialize(
                surfaceTextureHelper,
                applicationContext,
                videoSource!!.capturerObserver
            )
        }

        localVideoTrack = peerConnectionFactory!!.createVideoTrack("100", videoSource)

        //create an AudioSource instance
        audioSource = peerConnectionFactory!!.createAudioSource(audioConstraints)
        localAudioTrack = peerConnectionFactory!!.createAudioTrack("101", audioSource)
        videoCapturerAndroid?.startCapture(1024, 720, 30)
        localVideoView!!.visibility = View.VISIBLE
        // And finally, with our VideoRenderer ready, we
        // can add our renderer to the VideoTrack.
        localVideoTrack!!.addSink(localVideoView)
        localVideoView!!.setMirror(true)
        remoteVideoView!!.setMirror(true)
        gotUserMedia = true
    }

    private fun createCameraCapturer(enumerator: CameraEnumerator): VideoCapturer? {
        val deviceNames = enumerator.deviceNames

        // First, try to find front facing camera
        Logging.d("MainActivity", "Looking for front facing cameras.")
        for (deviceName in deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                Logging.d("MainActivity", "Creating front facing camera capturer.")
                val videoCapturer: VideoCapturer? = enumerator.createCapturer(deviceName, null)
                if (videoCapturer != null) {
                    return videoCapturer
                }
            }
        }

        // Front facing camera not found, try something else
        Logging.d("MainActivity", "Looking for other cameras.")
        for (deviceName in deviceNames) {
            if (!enumerator.isFrontFacing(deviceName)) {
                Logging.d("MainActivity", "Creating other camera capturer.")
                val videoCapturer: VideoCapturer? = enumerator.createCapturer(deviceName, null)
                if (videoCapturer != null) {
                    return videoCapturer
                }
            }
        }
        return null
    }

    override fun onClick(view: View?) {

    }

    private fun initViews() {
        hangup = findViewById(R.id.end_call)
        localVideoView = findViewById(R.id.local_gl_surface_view)
        remoteVideoView = findViewById(R.id.remote_gl_surface_view)
        hangup?.setOnClickListener(this)
    }

    private fun initVideos() {
        rootEglBase = EglBase.create()
        localVideoView!!.init(rootEglBase!!.eglBaseContext, null)
        remoteVideoView!!.init(rootEglBase!!.eglBaseContext, null)
        localVideoView!!.setZOrderMediaOverlay(true)
        remoteVideoView!!.setZOrderMediaOverlay(true)
    }

    override fun error(message: String) {
        Toast.makeText(this, "error: $message", Toast.LENGTH_LONG).show()
    }

    override fun receiveMessage(user: String, message: String) {
        // We do not have chat interface
    }

    override fun receiveCall(connectionId: String, callerFullName: String, callerPicture: String) {
        // we assume that line is not busy at all.
        SignallingClient.destinationConnectionId = connectionId
        // show user interfaces to see caller name
        // play ringing audio
        // get user media
        // create peerConnection
        // add stream
    }

    override fun setConnectionId(connectionInfo: ConnectionInfo) {
        // set local info such as localConnectionId, userFullName, ...
        SignallingClient.localConnectionId = connectionInfo.item1
        // filter list that shows all connection except the current user
    }

    override fun changeRoomMembers(signalRConnections: Array<ConnectedPhysicianPatientInformation>) {
        // get all connection info
        visitConnections = signalRConnections

        // TODO: just for test
        onTryToStart()

    }

    override fun receiveVideoCallRequest(message: Any, connectionId: String) {
        // do nothing
    }

    override fun offerReceived(message: LinkedTreeMap<*,*>) {
        Log.d("offerReceived", message.toString())

        // Todo: in this point, ensure that createPeerConnection is already called.
        val sdp = message["sdp"].toString()
        localPeer!!.setRemoteDescription(
            CustomSdpObserver("localSetRemote"),
            SessionDescription(SessionDescription.Type.OFFER, sdp)
        )
        doAnswer()
        updateVideoViews(true)
    }

    private fun updateVideoViews(remoteVisible: Boolean) {
        runOnUiThread {
            var params = localVideoView!!.layoutParams
            if (remoteVisible) {
                params.height = dpToPx(100)
                params.width = dpToPx(100)
            } else {
                params = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
            localVideoView!!.layoutParams = params
        }
    }

    private fun doAnswer() {
        localPeer!!.createAnswer(object : CustomSdpObserver("localCreateAns") {
            override fun onCreateSuccess(sessionDescription: SessionDescription) {
                super.onCreateSuccess(sessionDescription)
                localPeer!!.setLocalDescription(
                    CustomSdpObserver("localSetLocal"),
                    sessionDescription
                )
                val map = LinkedTreeMap<String, Any>()
                map["type"] = sessionDescription.type.canonicalForm()
                map["sdp"] = sessionDescription.description
                SignallingClient.sendVideoCallRequest(
                    destinationConnection?.connectionId!!,
                    map
                )
            }
        }, MediaConstraints())
    }

    override fun answerReceived(message: LinkedTreeMap<*,*>) {
        Log.d("answerReceived", message.toString())
        val sdp = message["sdp"].toString()
        localPeer!!.setRemoteDescription(
            CustomSdpObserver("localSetRemote"),
            SessionDescription(
                SessionDescription.Type.ANSWER, sdp
            )
        )
        updateVideoViews(true)
    }

    override fun candidateReceived(message: LinkedTreeMap<*,*>) {
        Log.d("candidateReceived", message.toString())
        localPeer!!.addIceCandidate(
            IceCandidate(
                message["id"].toString(),
                message["label"].toString().toDouble().toInt(),
                message["candidate"].toString()
            )
        )
    }

    override fun onTryToStart() {
        runOnUiThread {
            createPeerConnection()
            simulateVideoCallButtonClicked()
        }
    }

    private fun simulateVideoCallButtonClicked() {
        destinationConnection = visitConnections!!.firstOrNull()
        if (destinationConnection != null) {
            SignallingClient.call(destinationConnection!!.connectionId)
        }
    }

    /**
     * Creating the local peerconnection instance
     */
    private fun createPeerConnection() {
        val rtcConfig = RTCConfiguration(peerIceServers)
        // TCP candidates are only useful when connecting to a server that supports
        // ICE-TCP.
        //rtcConfig.tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.DISABLED
        rtcConfig.bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE
        rtcConfig.rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE
        rtcConfig.continualGatheringPolicy =
            PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
        // Use ECDSA encryption.
        rtcConfig.keyType = PeerConnection.KeyType.ECDSA
        localPeer = peerConnectionFactory!!.createPeerConnection(
            rtcConfig,
            object : CustomPeerConnectionObserver("localPeerCreation") {
                override fun onIceCandidate(iceCandidate: IceCandidate) {
                    super.onIceCandidate(iceCandidate)

                    val map = LinkedTreeMap<String, Any>()

                    map["label"] = iceCandidate.sdpMLineIndex
                    map["id"] = iceCandidate.sdpMid
                    map["candidate"] = iceCandidate.sdp
                    map["type"] = "candidate"

                    SignallingClient.sendVideoCallRequest(destinationConnection?.connectionId!!, map)
                }

                override fun onAddStream(mediaStream: MediaStream) {
                    super.onAddStream(mediaStream)

                    SignallingClient.remoteStream = mediaStream
                    //we have remote video stream. add to the renderer.
                    val videoTrack = mediaStream.videoTracks[0]
                    runOnUiThread {
                        remoteVideoView!!.visibility = View.VISIBLE
                        videoTrack.addSink(remoteVideoView)
                    }
                }
            })
        addStreamToLocalPeer()
    }

    /**
     * Adding the stream to the localpeer
     */
    private fun addStreamToLocalPeer() {
        //creating local mediastream
        val stream = peerConnectionFactory!!.createLocalMediaStream("102")
        stream.addTrack(localAudioTrack)
        stream.addTrack(localVideoTrack)
        localPeer!!.addStream(stream)
    }

    /**
     * Util Methods
     */
    private fun dpToPx(dp: Int): Int {
        val displayMetrics = resources.displayMetrics
        return (dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT)).roundToInt()
    }
}
